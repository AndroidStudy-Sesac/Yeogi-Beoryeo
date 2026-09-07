package com.team.yeogibeoryeo.data.item.local

import com.team.yeogibeoryeo.domain.diagnostics.NonFatalApi
import com.team.yeogibeoryeo.domain.diagnostics.NonFatalCategory
import com.team.yeogibeoryeo.domain.diagnostics.NonFatalErrorContext
import com.team.yeogibeoryeo.domain.diagnostics.NonFatalErrorReporter
import com.team.yeogibeoryeo.domain.diagnostics.NonFatalStage
import java.io.IOException
import kotlinx.coroutines.CancellationException
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class ItemCategoryLocalDataSourceTest {
    @Test
    fun `asset 읽기 실패를 한 번 기록하고 원래 예외를 전파한다`() {
        val failure = IOException("private asset path")
        val reporter = RecordingNonFatalErrorReporter()
        val source = ItemCategoryLocalDataSource(
            readAssetText = { throw failure },
            reporter = reporter,
        )

        val thrown = assertThrows(IOException::class.java) {
            source.getWasteDictionaryItems()
        }

        assertSame(failure, thrown)
        assertEquals(
            listOf(
                RecordedError(
                    error = failure,
                    context =
                        NonFatalErrorContext(
                            api = NonFatalApi.ITEM_GUIDE,
                            stage = NonFatalStage.ASSET_LOAD,
                            category = NonFatalCategory.IO,
                        ),
                ),
            ),
            reporter.errors,
        )
    }

    @Test
    fun `asset 변환 실패를 한 번 기록하고 원래 예외를 전파한다`() {
        val reporter = RecordingNonFatalErrorReporter()
        val source = ItemCategoryLocalDataSource(
            readAssetText = { "[" },
            reporter = reporter,
        )

        val thrown = assertThrows(Exception::class.java) {
            source.getWasteDictionaryItems()
        }

        assertSame(thrown, reporter.errors.single().error)
        assertEquals(
            NonFatalErrorContext(
                api = NonFatalApi.ITEM_GUIDE,
                stage = NonFatalStage.ASSET_LOAD,
                category = NonFatalCategory.PARSING,
            ),
            reporter.errors.single().context,
        )
    }

    @Test
    fun `정상 asset은 오류를 기록하지 않는다`() {
        val reporter = RecordingNonFatalErrorReporter()
        val source = ItemCategoryLocalDataSource(
            readAssetText = { "{}" },
            reporter = reporter,
        )

        assertTrue(source.getSynonyms().isEmpty())
        assertTrue(reporter.errors.isEmpty())
    }

    @Test
    fun `asset 읽기 취소는 기록하지 않고 원래 취소를 전파한다`() {
        val cancellation = CancellationException("cancelled")
        val reporter = RecordingNonFatalErrorReporter()
        val source = ItemCategoryLocalDataSource(
            readAssetText = { throw cancellation },
            reporter = reporter,
        )

        val thrown = assertThrows(CancellationException::class.java) {
            source.getWasteDictionaryItems()
        }

        assertSame(cancellation, thrown)
        assertTrue(reporter.errors.isEmpty())
    }

    @Test
    fun `asset 읽기 치명 오류는 기록하지 않고 원래 오류를 전파한다`() {
        val failure = LinkageError("fatal")
        val reporter = RecordingNonFatalErrorReporter()
        val source = ItemCategoryLocalDataSource(
            readAssetText = { throw failure },
            reporter = reporter,
        )

        val thrown = assertThrows(LinkageError::class.java) {
            source.getWasteDictionaryItems()
        }

        assertSame(failure, thrown)
        assertTrue(reporter.errors.isEmpty())
    }

    private class RecordingNonFatalErrorReporter : NonFatalErrorReporter {
        val errors = mutableListOf<RecordedError>()

        override fun report(error: Throwable, context: NonFatalErrorContext) {
            errors += RecordedError(error, context)
        }
    }

    private data class RecordedError(
        val error: Throwable,
        val context: NonFatalErrorContext,
    )
}
