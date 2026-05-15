package com.team.yeogibeoryeo.domain.item.usecase

import com.team.yeogibeoryeo.domain.item.model.DisposalCategory
import com.team.yeogibeoryeo.domain.item.model.DisposalInstruction
import com.team.yeogibeoryeo.domain.item.model.DisposalItemGuide
import com.team.yeogibeoryeo.domain.item.repository.DisposalItemGuideRepository
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test

class ItemUseCasesTest {
    @Test
    fun `SearchDisposalItemGuidesUseCase는 검색어를 저장소에 전달한다`() =
        runBlocking {
            val captured = mutableListOf<String>()
            val expected = listOf(sampleGuide("종이"))
            val repository =
                FakeRepository(
                    onSearch = { query ->
                        captured += query
                        expected
                    },
                )

            val result = SearchDisposalItemGuidesUseCase(repository).invoke("종이")

            assertEquals(listOf("종이"), captured)
            assertEquals(expected, result)
        }

    @Test
    fun `GetDisposalCategoryGuidesUseCase는 카테고리를 저장소에 전달한다`() =
        runBlocking {
            val captured = mutableListOf<DisposalCategory>()
            val expected = listOf(sampleGuide("신문지"))
            val repository =
                FakeRepository(
                    onCategory = { category ->
                        captured += category
                        expected
                    },
                )

            val result = GetDisposalCategoryGuidesUseCase(repository).invoke(DisposalCategory.PAPER)

            assertEquals(listOf(DisposalCategory.PAPER), captured)
            assertEquals(expected, result)
        }

    @Test
    fun `GetDisposalCategoriesUseCase는 저장소의 카테고리 목록을 반환한다`() {
        val expected = DisposalCategory.entries.toList()
        val repository = FakeRepository(onCategories = { expected })

        val result = GetDisposalCategoriesUseCase(repository).invoke()

        assertEquals(expected, result)
    }

    private fun sampleGuide(name: String): DisposalItemGuide =
        DisposalItemGuide(
            id = name,
            name = name,
            category = DisposalCategory.PAPER,
            subCategory = null,
            instructions = listOf(DisposalInstruction(method = "재활용폐기물")),
            steps = emptyList(),
            cautions = emptyList(),
            tip = null,
            isRecyclable = true,
            relatedSpotTypes = null,
        )

    private class FakeRepository(
        private val onSearch: suspend (String) -> List<DisposalItemGuide> = { emptyList() },
        private val onCategory: suspend (DisposalCategory) -> List<DisposalItemGuide> = { emptyList() },
        private val onCategories: () -> List<DisposalCategory> = { emptyList() },
    ) : DisposalItemGuideRepository {
        override suspend fun searchItemGuides(query: String): List<DisposalItemGuide> = onSearch(query)

        override suspend fun getCategoryGuides(category: DisposalCategory): List<DisposalItemGuide> = onCategory(category)

        override fun getCategories(): List<DisposalCategory> = onCategories()
    }
}
