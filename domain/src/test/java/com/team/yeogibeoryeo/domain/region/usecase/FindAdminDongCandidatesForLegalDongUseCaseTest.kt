package com.team.yeogibeoryeo.domain.region.usecase

import com.team.yeogibeoryeo.domain.region.model.Region
import com.team.yeogibeoryeo.domain.region.repository.RegionOptionsRepository
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test

class FindAdminDongCandidatesForLegalDongUseCaseTest {

    @Test
    fun `returns admin dong candidates from repository`() = runBlocking {
        val requestedRegion = Region(
            sido = "서울특별시",
            sigungu = "노원구",
            eupmyeondong = "하계동"
        )
        val adminDongCandidates = listOf(
            Region(sido = "서울특별시", sigungu = "노원구", eupmyeondong = "하계1동"),
            Region(sido = "서울특별시", sigungu = "노원구", eupmyeondong = "하계2동")
        )
        val useCase = FindAdminDongCandidatesForLegalDongUseCase(
            repository = FakeRegionOptionsRepository(adminDongCandidates)
        )

        val result = useCase(requestedRegion)

        assertEquals(adminDongCandidates, result)
    }

    private class FakeRegionOptionsRepository(
        private val adminDongCandidates: List<Region>
    ) : RegionOptionsRepository {
        override suspend fun getSidoOptions(): List<String> = emptyList()

        override suspend fun getSigunguOptions(sido: String): List<String> = emptyList()

        override suspend fun getEupmyeondongOptions(
            sido: String,
            sigungu: String
        ): List<String> = emptyList()

        override suspend fun findRegionsByEupmyeondongKeyword(
            keyword: String
        ): List<Region> = emptyList()

        override suspend fun findRegionsBySigunguKeyword(
            keyword: String
        ): List<Region> = emptyList()

        override suspend fun normalizeRegionForRegionalGuide(
            region: Region
        ): Region = region

        override suspend fun findAdminDongCandidatesForLegalDong(
            region: Region
        ): List<Region> = adminDongCandidates
    }
}
