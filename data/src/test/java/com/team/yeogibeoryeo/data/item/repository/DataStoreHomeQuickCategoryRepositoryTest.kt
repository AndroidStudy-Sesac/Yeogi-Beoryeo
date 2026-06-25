package com.team.yeogibeoryeo.data.item.repository

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import com.team.yeogibeoryeo.domain.item.model.DisposalCategory
import java.io.File
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test

class DataStoreHomeQuickCategoryRepositoryTest {

    @Test
    fun `저장된 분류가 없으면 빈 목록을 반환한다`() =
        runBlocking {
            withRepository { repository ->
                assertEquals(emptyList<DisposalCategory>(), repository.observeHomeQuickCategories().first())
            }
        }

    @Test
    fun `분류를 토글하면 선택 상태를 저장하고 다시 토글하면 제거한다`() =
        runBlocking {
            withRepository { repository ->
                repository.toggleHomeQuickCategory(DisposalCategory.BATTERY, maxSelectedCount = 2)

                assertEquals(
                    listOf(DisposalCategory.BATTERY),
                    repository.observeHomeQuickCategories().first(),
                )

                repository.toggleHomeQuickCategory(DisposalCategory.BATTERY, maxSelectedCount = 2)

                assertEquals(emptyList<DisposalCategory>(), repository.observeHomeQuickCategories().first())
            }
        }

    @Test
    fun `최대 개수에 도달하면 새 분류를 추가하지 않는다`() =
        runBlocking {
            withRepository { repository ->
                repository.toggleHomeQuickCategory(DisposalCategory.BATTERY, maxSelectedCount = 1)
                repository.toggleHomeQuickCategory(DisposalCategory.ELECTRONICS, maxSelectedCount = 1)

                assertEquals(
                    listOf(DisposalCategory.BATTERY),
                    repository.observeHomeQuickCategories().first(),
                )
            }
        }

    @Test
    fun `표시 개수를 제한하면 앞에서부터 지정한 개수만 유지한다`() =
        runBlocking {
            withRepository { repository ->
                repository.toggleHomeQuickCategory(DisposalCategory.BATTERY, maxSelectedCount = 2)
                repository.toggleHomeQuickCategory(DisposalCategory.ELECTRONICS, maxSelectedCount = 2)

                repository.limitHomeQuickCategories(maxSelectedCount = 1)

                assertEquals(
                    listOf(DisposalCategory.BATTERY),
                    repository.observeHomeQuickCategories().first(),
                )
            }
        }

    private suspend fun withRepository(
        block: suspend (DataStoreHomeQuickCategoryRepository) -> Unit,
    ) {
        val file = File.createTempFile("home-quick-category", ".preferences_pb")
        file.delete()
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        val dataStore =
            PreferenceDataStoreFactory.create(
                scope = scope,
                produceFile = { file },
            )
        val repository = DataStoreHomeQuickCategoryRepository(dataStore)

        try {
            block(repository)
        } finally {
            scope.cancel()
            file.delete()
        }
    }
}
