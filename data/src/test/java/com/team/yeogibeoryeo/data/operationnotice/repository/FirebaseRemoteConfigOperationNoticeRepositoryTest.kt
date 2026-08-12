package com.team.yeogibeoryeo.data.operationnotice.repository

import com.google.android.gms.tasks.Tasks
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertThrows
import org.junit.Assert.assertEquals
import org.junit.Test

class FirebaseRemoteConfigOperationNoticeRepositoryTest {

    @Test
    fun `Remote Config JSON을 운영 공지 목록으로 변환한다`() = runBlocking {
        val repository = createRepository(
            remoteJson = """
                {
                  "schemaVersion": 1,
                  "notices": [
                    {
                      "id": "notice-1",
                      "enabled": true,
                      "severity": "warning",
                      "priority": 3,
                      "title": "운영 공지",
                      "message": "지도 검색 안내",
                      "affectedFeatures": ["home", "collection_spot_map"],
                      "startsAt": "2026-08-10T00:00:00+09:00",
                      "endsAt": "2026-08-11T00:00:00+09:00",
                      "actionLabel": "자세히 보기",
                      "actionUrl": "https://www.data.go.kr"
                    }
                  ]
                }
            """.trimIndent(),
        )

        val notices = repository.observeOperationNotices().first()

        assertEquals(listOf("notice-1"), notices.map { notice -> notice.id })
        assertEquals("운영 공지", notices.first().title)
        assertEquals("자세히 보기", notices.first().actionLabel)
    }

    @Test
    fun `빈 JSON이나 파싱 실패 JSON은 빈 공지 목록으로 처리한다`() = runBlocking {
        assertEquals(emptyList<String>(), createRepository(remoteJson = "").noticeIds())
        assertEquals(emptyList<String>(), createRepository(remoteJson = "{").noticeIds())
    }

    @Test
    fun `지원하지 않는 schemaVersion은 빈 공지 목록으로 처리한다`() = runBlocking {
        val repository = createRepository(
            remoteJson = """
                {
                  "schemaVersion": 2,
                  "notices": [
                    {
                      "id": "notice-1",
                      "enabled": true,
                      "severity": "warning",
                      "title": "운영 공지",
                      "message": "공지 내용"
                    }
                  ]
                }
            """.trimIndent(),
        )

        assertEquals(emptyList<String>(), repository.noticeIds())
    }

    @Test
    fun `refresh는 debug 설정과 기본값을 적용한 뒤 최신 값을 다시 방출한다`() = runBlocking {
        var remoteJson = """{"schemaVersion":1,"notices":[]}"""
        val settings = mutableListOf<Long>()
        val defaults = mutableListOf<Map<String, String>>()
        val repository = createRepository(
            getRemoteJson = { remoteJson },
            onSetConfigSettings = { interval -> settings += interval },
            onSetDefaults = { value -> defaults += value },
            onFetchAndActivate = {
                remoteJson = """
                    {
                      "schemaVersion": 1,
                      "notices": [
                        {
                          "id": "notice-2",
                          "enabled": true,
                          "severity": "info",
                          "title": "새 공지",
                          "message": "새 내용"
                        }
                      ]
                    }
                """.trimIndent()
            },
            isDebug = true,
        )

        repository.refreshOperationNotices()

        assertEquals(listOf(0L), settings)
        assertEquals("""{"schemaVersion":1,"notices":[]}""", defaults.single().getValue("operation_notices"))
        assertEquals(listOf("notice-2"), repository.noticeIds())
    }

    @Test
    fun `refresh 실패는 예외를 삼키고 현재 값을 다시 방출한다`() = runBlocking {
        val repository = createRepository(
            remoteJson = """
                {
                  "schemaVersion": 1,
                  "notices": [
                    {
                      "id": "notice-1",
                      "enabled": true,
                      "severity": "warning",
                      "title": "운영 공지",
                      "message": "공지 내용"
                    }
                  ]
                }
            """.trimIndent(),
            onFetchAndActivate = { error("network") },
            isDebug = false,
        )

        repository.refreshOperationNotices()

        assertEquals(listOf("notice-1"), repository.noticeIds())
    }

    @Test
    fun `refresh 취소 예외는 다시 던진다`() {
        val repository = createRepository(
            remoteJson = """{"schemaVersion":1,"notices":[]}""",
            onFetchAndActivate = { throw CancellationException("cancel") },
        )

        assertThrows(CancellationException::class.java) {
            runBlocking {
                repository.refreshOperationNotices()
            }
        }
    }

    @Test
    fun `Remote Config Task 성공 결과를 suspend로 반환한다`() = runBlocking {
        val result = Tasks.forResult(true).awaitRemoteConfigTask()

        assertEquals(true, result)
    }

    @Test
    fun `Remote Config Task 실패 결과를 suspend 예외로 전달한다`() {
        assertThrows(IllegalStateException::class.java) {
            runBlocking {
                Tasks.forException<Boolean>(IllegalStateException("fail")).awaitRemoteConfigTask()
            }
        }
    }

    private suspend fun FirebaseRemoteConfigOperationNoticeRepository.noticeIds(): List<String> =
        observeOperationNotices().first().map { notice -> notice.id }

    private fun createRepository(
        remoteJson: String,
        onFetchAndActivate: suspend () -> Unit = {},
        isDebug: Boolean = true,
    ): FirebaseRemoteConfigOperationNoticeRepository =
        createRepository(
            getRemoteJson = { remoteJson },
            onFetchAndActivate = onFetchAndActivate,
            isDebug = isDebug,
        )

    private fun createRepository(
        getRemoteJson: () -> String,
        onSetConfigSettings: suspend (Long) -> Unit = {},
        onSetDefaults: suspend (Map<String, String>) -> Unit = {},
        onFetchAndActivate: suspend () -> Unit = {},
        isDebug: Boolean = true,
    ): FirebaseRemoteConfigOperationNoticeRepository =
        FirebaseRemoteConfigOperationNoticeRepository(
            getRemoteConfigString = { getRemoteJson() },
            setConfigSettings = onSetConfigSettings,
            setDefaults = onSetDefaults,
            fetchAndActivate = onFetchAndActivate,
            isDebug = isDebug,
        )
}
