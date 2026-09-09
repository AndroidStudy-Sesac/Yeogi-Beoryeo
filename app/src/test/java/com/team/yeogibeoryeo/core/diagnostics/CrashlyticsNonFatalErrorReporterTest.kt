package com.team.yeogibeoryeo.core.diagnostics

import com.team.yeogibeoryeo.domain.diagnostics.NonFatalApi
import com.team.yeogibeoryeo.domain.diagnostics.NonFatalCategory
import com.team.yeogibeoryeo.domain.diagnostics.NonFatalErrorContext
import com.team.yeogibeoryeo.domain.diagnostics.NonFatalHttpStatusClass
import com.team.yeogibeoryeo.domain.diagnostics.NonFatalRetryCount
import com.team.yeogibeoryeo.domain.diagnostics.NonFatalStage
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotSame
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.IOException
import java.net.SocketTimeoutException
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.CyclicBarrier
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

class CrashlyticsNonFatalErrorReporterTest {
    private val context = NonFatalErrorContext(
        api = NonFatalApi.ITEM_GUIDE,
        stage = NonFatalStage.REMOTE_REQUEST,
        category = NonFatalCategory.TIMEOUT,
    )

    @Test
    fun `원본 예외 대신 허용한 분류값과 새 예외만 전달한다`() {
        val events = mutableListOf<RecordedEvent>()
        val reporter = reporter(events)
        val secret = "private-query-address-coordinate-api-key-response"
        val original = IOException(secret, IllegalArgumentException(secret)).apply {
            addSuppressed(IllegalStateException(secret))
            stackTrace = arrayOf(StackTraceElement(secret, secret, secret, 1))
        }

        reporter.report(original, context)

        val event = events.single()
        assertNotSame(original, event.error)
        assertEquals("ITEM_GUIDE/REMOTE_REQUEST/TIMEOUT", event.error.message)
        assertNull(event.error.cause)
        assertTrue(event.error.suppressed.isEmpty())
        assertTrue(event.error.stackTrace.isEmpty())
        assertEquals(
            mapOf(
                "failure_api" to "ITEM_GUIDE",
                "failure_stage" to "REMOTE_REQUEST",
                "failure_category" to "TIMEOUT",
                "failure_http_status_class" to "NOT_AVAILABLE",
                "failure_retry_count" to "NONE",
                "failure_partial_result" to "false",
            ),
            event.keys,
        )
        assertFalse(event.error.toString().contains(secret))
        assertFalse(event.keys.toString().contains(secret))
        assertEquals(secret, original.message)
        assertEquals(secret, original.cause?.message)
        assertEquals(1, original.suppressed.size)
        assertEquals(secret, original.stackTrace.single().className)
    }

    @Test
    fun `debug에서는 기록 함수를 호출하지 않는다`() {
        val events = mutableListOf<RecordedEvent>()

        reporter(events, isDebug = true).report(IOException(), context)

        assertTrue(events.isEmpty())
    }

    @Test
    fun `기능과 실패 분류를 해당 이벤트 필드의 고정값으로 전달한다`() {
        val cases = listOf(
            Triple(context.copy(api = NonFatalApi.COLLECTION_SPOT), "failure_api", "COLLECTION_SPOT"),
            Triple(context.copy(stage = NonFatalStage.RESPONSE_PARSING), "failure_stage", "RESPONSE_PARSING"),
            Triple(context.copy(stage = NonFatalStage.ASSET_LOAD), "failure_stage", "ASSET_LOAD"),
            Triple(context.copy(stage = NonFatalStage.CACHE_WRITE), "failure_stage", "CACHE_WRITE"),
            Triple(context.copy(category = NonFatalCategory.NETWORK), "failure_category", "NETWORK"),
            Triple(context.copy(category = NonFatalCategory.HTTP), "failure_category", "HTTP"),
            Triple(context.copy(category = NonFatalCategory.PARSING), "failure_category", "PARSING"),
            Triple(context.copy(category = NonFatalCategory.IO), "failure_category", "IO"),
            Triple(context.copy(retryCount = NonFatalRetryCount.ONE), "failure_retry_count", "ONE"),
            Triple(context.copy(retryCount = NonFatalRetryCount.TWO), "failure_retry_count", "TWO"),
        )

        for ((metadata, key, value) in cases) {
            val events = mutableListOf<RecordedEvent>()
            reporter(events).report(IOException(), metadata)

            assertEquals(value, events.single().keys[key])
        }
    }

    @Test
    fun `HTTP 상태 분류는 응답 원문 없이 고정값으로 전달한다`() {
        val cases = mapOf(
            NonFatalHttpStatusClass.NOT_AVAILABLE to "NOT_AVAILABLE",
            NonFatalHttpStatusClass.INFORMATIONAL to "INFORMATIONAL",
            NonFatalHttpStatusClass.SUCCESS to "SUCCESS",
            NonFatalHttpStatusClass.REDIRECTION to "REDIRECTION",
            NonFatalHttpStatusClass.CLIENT_ERROR to "CLIENT_ERROR",
            NonFatalHttpStatusClass.SERVER_ERROR to "SERVER_ERROR",
        )

        for ((status, value) in cases) {
            val events = mutableListOf<RecordedEvent>()
            reporter(events).report(IOException(), context.copy(httpStatusClass = status))

            assertEquals(value, events.single().keys["failure_http_status_class"])
        }
    }

    @Test
    fun `기본 생성자는 Firebase를 초기화하지 않는다`() {
        CrashlyticsNonFatalErrorReporter()
    }

    @Test
    fun `실제 coroutine timeout은 debug와 release 모두 원객체로 전파한다`() {
        val timeout = assertThrows(TimeoutCancellationException::class.java) {
            runTest {
                withTimeout(1) { awaitCancellation() }
            }
        }
        val events = mutableListOf<RecordedEvent>()

        for (isDebug in listOf(true, false)) {
            val forwarded = assertThrows(CancellationException::class.java) {
                reporter(events, isDebug).report(timeout, context)
            }
            assertSame(timeout, forwarded)
        }
        assertTrue(events.isEmpty())
    }

    @Test
    fun `parent scope가 취소한 오류와 제어 흐름을 보존한다`() = runTest {
        val events = mutableListOf<RecordedEvent>()
        val reporter = reporter(events)
        val observed = CompletableDeferred<CancellationException>()
        var continuedAfterReport = false
        val child = launch(start = CoroutineStart.UNDISPATCHED) {
            try {
                awaitCancellation()
            } catch (original: CancellationException) {
                try {
                    reporter.report(original, context)
                    continuedAfterReport = true
                } catch (forwarded: CancellationException) {
                    assertSame(original, forwarded)
                    observed.complete(forwarded)
                    throw forwarded
                }
            }
        }

        child.cancel()
        child.join()

        assertTrue(observed.isCompleted)
        assertFalse(continuedAfterReport)
        assertTrue(events.isEmpty())
    }

    @Test
    fun `network timeout은 coroutine 취소와 구분해 기록한다`() {
        val events = mutableListOf<RecordedEvent>()

        reporter(events).report(SocketTimeoutException("private URL"), context)

        assertEquals("TIMEOUT", events.single().keys["failure_category"])
    }

    @Test
    fun `wrapper의 cause를 임의로 취소 신호로 해석하지 않는다`() {
        val events = mutableListOf<RecordedEvent>()
        val wrapper = IOException("handled failure", CancellationException("nested cause"))

        reporter(events).report(wrapper, context)

        assertNull(events.single().error.cause)
    }

    @Test
    fun `치명 오류는 debug에서도 원객체로 전파한다`() {
        val fatal = LinkageError("fatal")
        val events = mutableListOf<RecordedEvent>()

        for (isDebug in listOf(true, false)) {
            val forwarded = assertThrows(LinkageError::class.java) {
                reporter(events, isDebug).report(fatal, context)
            }
            assertSame(fatal, forwarded)
        }
        assertTrue(events.isEmpty())
    }

    @Test
    fun `기록 장치의 runtime 오류는 사용자 오류 처리에 영향을 주지 않는다`() {
        var attempts = 0
        val reporter = CrashlyticsNonFatalErrorReporter(isDebug = false) { _, _ ->
            attempts++
            throw IllegalStateException("private SDK error")
        }

        reporter.report(IOException(), context)

        assertEquals(1, attempts)
    }

    @Test
    fun `기록 장치의 취소와 치명 오류는 억제하지 않는다`() {
        for (error in listOf(CancellationException("cancelled"), LinkageError("fatal"))) {
            val reporter = CrashlyticsNonFatalErrorReporter(isDebug = false) { _, _ ->
                throw error
            }

            val forwarded = assertThrows(error.javaClass) {
                reporter.report(IOException(), context)
            }

            assertSame(error, forwarded)
        }
    }

    @Test
    fun `동시 기록은 예외와 필드 저장소를 공유하지 않는다`() {
        val events = ConcurrentLinkedQueue<RecordedEvent>()
        val barrier = CyclicBarrier(2)
        val reporter = CrashlyticsNonFatalErrorReporter(isDebug = false) { error, keys ->
            barrier.await(5, TimeUnit.SECONDS)
            events.add(RecordedEvent(error, keys))
        }
        val otherContext = NonFatalErrorContext(
            api = NonFatalApi.REGIONAL_GUIDE,
            stage = NonFatalStage.CACHE_READ,
            category = NonFatalCategory.CACHE,
            httpStatusClass = NonFatalHttpStatusClass.SERVER_ERROR,
            retryCount = NonFatalRetryCount.THREE_OR_MORE,
            isPartialResult = true,
        )

        Executors.newFixedThreadPool(2).use { executor ->
            val futures = listOf(context, otherContext).map { metadata ->
                executor.submit { reporter.report(IOException(), metadata) }
            }
            futures.forEach { it.get(10, TimeUnit.SECONDS) }
        }

        assertEquals(2, events.size)
        val first = events.single { it.keys["failure_api"] == "ITEM_GUIDE" }
        val second = events.single { it.keys["failure_api"] == "REGIONAL_GUIDE" }
        assertNotSame(first.error, second.error)
        assertNotSame(first.keys, second.keys)
        assertEquals("NONE", first.keys["failure_retry_count"])
        assertEquals("false", first.keys["failure_partial_result"])
        assertEquals("SERVER_ERROR", second.keys["failure_http_status_class"])
        assertEquals("THREE_OR_MORE", second.keys["failure_retry_count"])
        assertEquals("true", second.keys["failure_partial_result"])
    }

    private fun reporter(
        events: MutableList<RecordedEvent>,
        isDebug: Boolean = false,
    ) = CrashlyticsNonFatalErrorReporter(isDebug) { error, keys ->
        events.add(RecordedEvent(error, keys))
    }

    private data class RecordedEvent(
        val error: Throwable,
        val keys: Map<String, String>,
    )
}
