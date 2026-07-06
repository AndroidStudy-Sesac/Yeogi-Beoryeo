package com.team.yeogibeoryeo.domain.region.usecase

import com.team.yeogibeoryeo.domain.region.model.Region
import com.team.yeogibeoryeo.domain.region.repository.RegionOptionsRepository
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test

class NormalizeRegionForRegionalGuideUseCaseTest {

    @Test
    fun `normalizes administrative region through repository`() = runBlocking {
        val normalizedRegion = Region(
            sido = "경기도",
            sigungu = "성남시",
            eupmyeondong = "중앙동"
        )
        val useCase = NormalizeRegionForRegionalGuideUseCase(
            repository = FakeRegionOptionsRepository(normalizedRegion)
        )

        val result = useCase(
            Region(
                sido = "경기도",
                sigungu = "성남시 중원구",
                eupmyeondong = "중앙동"
            )
        )

        assertEquals(normalizedRegion, result)
    }

    private class FakeRegionOptionsRepository(
        private val normalizedRegion: Region
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
        ): Region = normalizedRegion

        override suspend fun findAdminDongCandidatesForLegalDong(
            region: Region
        ): List<Region> = emptyList()
    }
}
