package com.team.yeogibeoryeo.data.regionalguide.availability

import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import com.team.yeogibeoryeo.data.region.local.RegionAssetContract
import com.team.yeogibeoryeo.data.region.local.dto.RegionalGuideAvailabilityDto
import com.team.yeogibeoryeo.data.regionalguide.remote.RegionalGuideApiService
import com.team.yeogibeoryeo.data.regionalguide.remote.dto.RegionalGuideItemDto
import com.team.yeogibeoryeo.data.regionalguide.remote.dto.RegionalGuideRootDto
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Test
import retrofit2.Response
import retrofit2.Retrofit
import java.io.File

class RegionalGuideAvailabilityApiContractTest {

    private val json = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
    }

    @Test
    fun `지역 가이드 제공 지역 차이를 보고서에 포함한다`() {
        val assetOnlyKey = RegionalGuideRegionKey(
            sidoName = "서울특별시",
            sigunguName = "종로구",
            managementZoneName = "청운효자동",
            targetRegionName = "청운동",
        )
        val apiOnlyKey = RegionalGuideRegionKey(
            sidoName = "서울특별시",
            sigunguName = "종로구",
            managementZoneName = "사직동",
            targetRegionName = "사직동",
        )

        val report = RegionalGuideAvailabilityReport(
            assetKeys = setOf(assetOnlyKey),
            apiRegions = ApiRegions(
                keys = setOf(apiOnlyKey),
                rowCount = 1,
            ),
            verificationTarget = VerificationTarget(
                isFull = true,
                sigunguNames = emptySet(),
            ),
        ).toMarkdown()

        assertTrue(report.contains("| /info API 응답 행 | 1 |"))
        assertTrue(report.contains("| asset에만 존재 | 1 |"))
        assertTrue(report.contains(assetOnlyKey.displayName()))
        assertTrue(report.contains(apiOnlyKey.displayName()))
    }

    @Test
    fun `변경 시군구 범위에서는 대상 지역만 보고서에 포함한다`() {
        val targetKey = RegionalGuideRegionKey(
            sidoName = "서울특별시",
            sigunguName = "종로구",
            managementZoneName = "청운효자동",
            targetRegionName = "청운동",
        )
        val verificationTarget = VerificationTarget(
            isFull = false,
            sigunguNames = setOf(targetKey.sigunguName),
        )

        val report = RegionalGuideAvailabilityReport(
            assetKeys = setOf(targetKey),
            apiRegions = ApiRegions(
                keys = setOf(targetKey),
                rowCount = 1,
            ),
            verificationTarget = verificationTarget,
        ).toMarkdown()

        assertTrue(verificationTarget.includes("종로구"))
        assertTrue(!verificationTarget.includes("중구"))
        assertTrue(report.contains("검증 범위: 변경 시군구 종로구"))
        assertTrue(report.contains("| availability asset 지역 | 1 |"))
    }

    @Test
    fun `지역 가이드 제공 가능 지역 자산과 API 제공 지역의 차이를 보고한다`() = runBlocking {
        assumeTrue(
            "지역 가이드 availability 검증 전용 CI에서만 실행합니다.",
            System.getenv(VERIFICATION_ENABLED_ENVIRONMENT) == "true"
        )

        try {
            val serviceKey = checkNotNull(System.getenv(SERVICE_KEY_ENVIRONMENT)?.takeIf(String::isNotBlank)) {
                "PUBLIC_DATA_SERVICE_KEY가 필요합니다."
            }
            val verificationTarget = verificationTarget()
            val assetKeys = loadAssetKeys(verificationTarget)
            val apiRegions = fetchApiRegions(
                serviceKey = serviceKey,
                verificationTarget = verificationTarget,
            )
            val report = RegionalGuideAvailabilityReport(
                assetKeys = assetKeys,
                apiRegions = apiRegions,
                verificationTarget = verificationTarget,
            ).toMarkdown()

            appendGitHubStepSummary(report)
            println(report)
        } catch (failure: Throwable) {
            val failureMessage = failure.message ?: "원인을 확인할 수 없습니다."
            println("지역 가이드 availability 검증 실패: $failureMessage")
            appendGitHubStepSummary(
                """
                ## 지역 가이드 availability 검증 실패

                - 원인: $failureMessage
                - API 인증, 통신 또는 응답 형식을 확인하세요.
                """.trimIndent()
            )
            throw failure
        }
    }

    private fun loadAssetKeys(verificationTarget: VerificationTarget): Set<RegionalGuideRegionKey> {
        val availabilityRegions = json.decodeFromString<List<RegionalGuideAvailabilityDto>>(
            File(assetFilePath()).readText(Charsets.UTF_8)
        )
        val keys = availabilityRegions.map { region -> region.toRegionKey() }

        assertEquals(
            "지역 가이드 제공 가능 지역 자산에 중복 지역이 있습니다.",
            keys.size,
            keys.toSet().size,
        )

        return keys
            .filter { verificationTarget.includes(it.sigunguName) }
            .toSet()
    }

    private suspend fun fetchApiRegions(
        serviceKey: String,
        verificationTarget: VerificationTarget,
    ): ApiRegions {
        val apiService = regionalGuideApiService()
        val items = if (verificationTarget.isFull) {
            fetchApiPages { pageNo ->
                apiService.getAllRegionalGuides(
                    serviceKey = serviceKey,
                    pageNo = pageNo,
                    numOfRows = PAGE_SIZE,
                )
            }
        } else {
            val targetItems = mutableListOf<RegionalGuideItemDto>()
            for (sigunguName in verificationTarget.sigunguNames) {
                targetItems += fetchApiPages { pageNo ->
                    apiService.getRegionalGuides(
                        serviceKey = serviceKey,
                        pageNo = pageNo,
                        numOfRows = PAGE_SIZE,
                        sigunguName = sigunguName,
                    )
                }
            }
            targetItems
        }
        val scopedItems = items.filter { item ->
            verificationTarget.includes(item.sigunguName)
        }

        return ApiRegions(
            keys = scopedItems.map { item -> item.toRegionKey() }.toSet(),
            rowCount = scopedItems.size,
        )
    }

    private suspend fun fetchApiPages(
        requestPage: suspend (pageNo: Int) -> Response<RegionalGuideRootDto>,
    ): List<RegionalGuideItemDto> {
        val firstPage = fetchApiPage(requestPage(FIRST_PAGE_NO))
        val items = firstPage.items.toMutableList()
        val totalPages = (firstPage.totalCount + PAGE_SIZE - 1) / PAGE_SIZE

        for (pageNo in (FIRST_PAGE_NO + 1)..totalPages) {
            val page = fetchApiPage(requestPage(pageNo))
            check(page.totalCount == firstPage.totalCount) {
                "/info API totalCount가 페이지마다 다릅니다."
            }
            items += page.items
        }

        check(items.size == firstPage.totalCount) {
            "/info API 페이지네이션 결과가 totalCount와 다릅니다. " +
                "expected=${firstPage.totalCount}, actual=${items.size}"
        }

        return items
    }

    private fun fetchApiPage(
        response: Response<RegionalGuideRootDto>,
    ): ApiPage {
        check(response.isSuccessful) {
            "/info API HTTP 오류: ${response.code()}"
        }

        val apiResponse = checkNotNull(response.body()?.response) {
            "/info API 응답이 비어 있습니다."
        }
        val header = checkNotNull(apiResponse.header) {
            "/info API 응답에 header가 없습니다."
        }
        check(header.resultCode == SUCCESS_RESULT_CODE) {
            "/info API 오류: 코드=${header.resultCode}, 메시지=${header.resultMessage}"
        }
        val body = checkNotNull(apiResponse.body) {
            "/info API 응답에 body가 없습니다."
        }

        return ApiPage(
            items = body.items?.item.orEmpty(),
            totalCount = checkNotNull(body.totalCount) {
                "/info API 응답에 totalCount가 없습니다."
            },
        )
    }

    private fun regionalGuideApiService(): RegionalGuideApiService {
        val retrofit = Retrofit.Builder()
            .baseUrl(API_BASE_URL)
            .client(OkHttpClient.Builder().build())
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()

        return retrofit.create(RegionalGuideApiService::class.java)
    }

    private fun assetFilePath(): String {
        return "src/main/assets/${RegionAssetContract.REGIONAL_GUIDE_AVAILABILITY_ASSET_PATH}"
    }

    private fun verificationTarget(): VerificationTarget {
        val sigunguNames = System.getenv(TARGET_SIGUNGU_NAMES_ENVIRONMENT)
            ?.lineSequence()
            ?.map(String::trim)
            ?.filter(String::isNotBlank)
            ?.toSet()
            .orEmpty()

        return VerificationTarget(
            isFull = System.getenv(TARGETED_VERIFICATION_ENVIRONMENT) != "true",
            sigunguNames = sigunguNames,
        )
    }

    private fun appendGitHubStepSummary(report: String) {
        val summaryPath = System.getenv(GITHUB_STEP_SUMMARY_ENVIRONMENT) ?: return

        File(summaryPath).appendText("$report\n", Charsets.UTF_8)
    }

    private fun RegionalGuideAvailabilityDto.toRegionKey(): RegionalGuideRegionKey {
        return RegionalGuideRegionKey(
            sidoName = sidoName,
            sigunguName = sigunguName,
            managementZoneName = managementZoneName,
            targetRegionName = targetRegionName,
        )
    }

    private fun RegionalGuideItemDto.toRegionKey(): RegionalGuideRegionKey {
        return RegionalGuideRegionKey(
            sidoName = requireRegionName(sidoName, "CTPV_NM"),
            sigunguName = requireRegionName(sigunguName, "SGG_NM"),
            managementZoneName = requireRegionName(managementZoneName, "MNG_ZONE_NM"),
            targetRegionName = requireRegionName(dongName, "MNG_ZONE_TRGT_RGN_NM"),
        )
    }

    private fun requireRegionName(value: String?, fieldName: String): String {
        return value?.takeIf(String::isNotBlank)
            ?: throw AssertionError("/info API 응답의 $fieldName 값이 비어 있습니다.")
    }

    private data class RegionalGuideRegionKey(
        val sidoName: String,
        val sigunguName: String,
        val managementZoneName: String,
        val targetRegionName: String,
    ) {
        fun displayName(): String {
            return listOf(sidoName, sigunguName, managementZoneName, targetRegionName)
                .joinToString(" > ")
        }
    }

    private data class RegionalGuideAvailabilityReport(
        val assetKeys: Set<RegionalGuideRegionKey>,
        val apiRegions: ApiRegions,
        val verificationTarget: VerificationTarget,
    ) {
        private val assetOnlyKeys = assetKeys - apiRegions.keys
        private val apiOnlyKeys = apiRegions.keys - assetKeys

        fun toMarkdown(): String {
            return buildList {
                add("## 지역 가이드 availability 검증 결과")
                add("")
                if (verificationTarget.isFull) {
                    add("- 검증 범위: 전체 지역")
                } else if (verificationTarget.sigunguNames.isEmpty()) {
                    add("- 검증 범위: 변경 지역 없음")
                } else {
                    add("- 검증 범위: 변경 시군구 ${verificationTarget.sigunguNames.sorted().joinToString()}")
                }
                add("")
                add("| 구분 | 건수 |")
                add("| --- | ---: |")
                add("| availability asset 지역 | ${assetKeys.size} |")
                add("| /info API 응답 행 | ${apiRegions.rowCount} |")
                add("| /info API 고유 지역 | ${apiRegions.keys.size} |")
                add("| asset에만 존재 | ${assetOnlyKeys.size} |")
                add("| API에만 존재 | ${apiOnlyKeys.size} |")
                addDifference("asset에만 존재", assetOnlyKeys)
                addDifference("API에만 존재", apiOnlyKeys)
            }.joinToString("\n")
        }

        private fun MutableList<String>.addDifference(
            title: String,
            keys: Set<RegionalGuideRegionKey>,
        ) {
            if (keys.isEmpty()) return

            add("")
            add("### $title (최대 ${MAX_REPORTED_DIFFERENCES}건)")
            keys
                .sortedBy(RegionalGuideRegionKey::displayName)
                .take(MAX_REPORTED_DIFFERENCES)
                .forEach { key -> add("- ${key.displayName()}") }
        }
    }

    private data class ApiPage(
        val items: List<RegionalGuideItemDto>,
        val totalCount: Int,
    )

    private data class ApiRegions(
        val keys: Set<RegionalGuideRegionKey>,
        val rowCount: Int,
    )

    private data class VerificationTarget(
        val isFull: Boolean,
        val sigunguNames: Set<String>,
    ) {
        fun includes(sigunguName: String?): Boolean {
            return isFull || sigunguName in sigunguNames
        }
    }

    private companion object {
        const val API_BASE_URL = "https://apis.data.go.kr/"
        const val FIRST_PAGE_NO = 1
        const val GITHUB_STEP_SUMMARY_ENVIRONMENT = "GITHUB_STEP_SUMMARY"
        const val MAX_REPORTED_DIFFERENCES = 20
        const val PAGE_SIZE = 100
        const val SERVICE_KEY_ENVIRONMENT = "PUBLIC_DATA_SERVICE_KEY"
        const val SUCCESS_RESULT_CODE = "00"
        const val TARGET_SIGUNGU_NAMES_ENVIRONMENT = "REGIONAL_GUIDE_AVAILABILITY_TARGET_SIGUNGU_NAMES"
        const val TARGETED_VERIFICATION_ENVIRONMENT = "REGIONAL_GUIDE_AVAILABILITY_TARGETED"
        const val VERIFICATION_ENABLED_ENVIRONMENT = "REGIONAL_GUIDE_AVAILABILITY_VERIFICATION"
    }
}
