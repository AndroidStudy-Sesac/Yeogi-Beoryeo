package com.team.yeogibeoryeo.data.regionalguide.availability

import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import com.team.yeogibeoryeo.data.network.di.HOUSEHOLD_WASTE_INFO_API_BASE_URL
import com.team.yeogibeoryeo.data.region.local.RegionAssetContract
import com.team.yeogibeoryeo.data.region.local.dto.AdministrativeRegionDto
import com.team.yeogibeoryeo.data.region.local.dto.RegionalGuideAvailabilityDto
import com.team.yeogibeoryeo.data.regionalguide.remote.RegionalGuideApiService
import com.team.yeogibeoryeo.data.regionalguide.remote.dto.RegionalGuideBodyDto
import com.team.yeogibeoryeo.data.regionalguide.remote.dto.RegionalGuideHeaderDto
import com.team.yeogibeoryeo.data.regionalguide.remote.dto.RegionalGuideItemDto
import com.team.yeogibeoryeo.data.regionalguide.remote.dto.RegionalGuideItemsDto
import com.team.yeogibeoryeo.data.regionalguide.remote.dto.RegionalGuideResponseDto
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
    fun `API 성공 코드는 앞자리 0 개수와 관계없이 0이면 성공으로 판단한다`() {
        assertTrue(RegionalGuideHeaderDto(resultCode = "0").isSuccessful())
        assertTrue(RegionalGuideHeaderDto(resultCode = "00").isSuccessful())
    }

    @Test
    fun `빈 시군구명을 세종 검증 대상명으로 변환한다`() {
        val scope = AdministrativeRegionDto(
            adminCode = "3611010100",
            sidoName = "세종특별자치시",
            sigunguName = "",
            eupmyeondongName = "조치원읍",
            fullName = "세종특별자치시 조치원읍",
        ).toVerificationScope()

        assertEquals(RegionalGuideVerificationScope("세종특별자치시", "없음"), scope)
    }

    @Test
    fun `변경 대상은 시도와 시군구 쌍으로 구분한다`() {
        val target = parseVerificationScope("서울특별시\t중구")

        assertEquals(RegionalGuideVerificationScope("서울특별시", "중구"), target)
        assertTrue(target != RegionalGuideVerificationScope("부산광역시", "중구"))
    }

    @Test
    fun `응답 페이지 번호가 요청과 다르면 페이지네이션 오류로 처리한다`() {
        val failure = runCatching {
            fetchApiPage("종로구", 1, successfulResponse(pageNo = 2))
        }.exceptionOrNull()

        assertTrue(failure?.message?.contains("시군구=종로구, 페이지=1") == true)
    }

    @Test
    fun `반복된 페이지 응답은 페이지네이션 오류로 처리한다`() = runBlocking {
        val firstPageItems = List(PAGE_SIZE) { index ->
            RegionalGuideItemDto(managementNumber = index.toString())
        }

        val failure = runCatching {
            fetchApiPages("종로구") { pageNo ->
                successfulResponse(
                    pageNo = pageNo,
                    totalCount = PAGE_SIZE + 1,
                    items = if (pageNo == FIRST_PAGE_NO) firstPageItems else listOf(firstPageItems.first()),
                )
            }
        }.exceptionOrNull()

        assertTrue(failure?.message?.contains("중복 행") == true)
        assertTrue(failure?.message?.contains("시군구=종로구, 페이지=2") == true)
    }

    @Test
    fun `시군구명이 비어 있는 API 응답은 범위 필터 전 오류로 처리한다`() {
        val failure = runCatching {
            RegionalGuideItemDto(
                sidoName = "서울특별시",
                sigunguName = "",
                managementZoneName = "청운효자동",
                dongName = "청운동",
            ).toRegionKey("시군구=종로구, 페이지=1")
        }.exceptionOrNull()

        assertTrue(failure?.message?.contains("SGG_NM") == true)
        assertTrue(failure?.message?.contains("시군구=종로구, 페이지=1") == true)
    }

    @Test
    fun `변경 지역이 없으면 API 호출 없이 정상 보고서를 만든다`() {
        val report = RegionalGuideAvailabilityReport(
            assetKeys = emptySet(),
            apiRegions = ApiRegions.empty(),
            verificationTarget = VerificationTarget(isFull = false, scopes = emptySet()),
        ).toMarkdown()

        assertTrue(report.contains("검증 범위: 변경 지역 없음"))
        assertTrue(report.contains("| /info API 응답 행 | 0 |"))
    }

    @Test
    fun `지역 가이드 제공 지역 차이를 보고서에 포함한다`() {
        val assetOnlyKey = RegionalGuideRegionKey("서울특별시", "종로구", "청운효자동", "청운동")
        val apiOnlyKey = RegionalGuideRegionKey("서울특별시", "종로구", "사직동", "사직동")

        val report = RegionalGuideAvailabilityReport(
            assetKeys = setOf(assetOnlyKey),
            apiRegions = ApiRegions(setOf(apiOnlyKey), rowCount = 1, queriedSigunguCount = 1),
            verificationTarget = VerificationTarget(isFull = true, scopes = emptySet()),
        ).toMarkdown()

        assertTrue(report.contains("| /info API 응답 행 | 1 |"))
        assertTrue(report.contains("| asset에만 존재 | 1 |"))
        assertTrue(report.contains(assetOnlyKey.displayName()))
        assertTrue(report.contains(apiOnlyKey.displayName()))
    }

    @Test
    fun `변경 시군구 범위에서는 같은 시도의 대상 지역만 보고서에 포함한다`() {
        val targetKey = RegionalGuideRegionKey("서울특별시", "중구", "소공동", "소공동")
        val target = VerificationTarget(isFull = false, scopes = setOf(targetKey.scope()))

        assertTrue(target.includes(targetKey))
        assertTrue(!target.includes(targetKey.copy(sidoName = "부산광역시")))
    }

    @Test
    fun `지역 가이드 제공 가능 지역 자산과 API 제공 지역의 차이를 보고한다`() = runBlocking {
        val verificationTarget = verificationTarget()
        assumeTrue(
            "지역 가이드 availability 검증 전용 CI에서만 실행합니다.",
            System.getenv(VERIFICATION_ENABLED_ENVIRONMENT) == "true",
        )

        try {
            val serviceKey = checkNotNull(System.getenv(SERVICE_KEY_ENVIRONMENT)?.takeIf(String::isNotBlank)) {
                "PUBLIC_DATA_SERVICE_KEY가 필요합니다."
            }
            val assetKeys = loadAssetKeys(verificationTarget)
            val apiRegions = if (verificationTarget.isTargeted && verificationTarget.scopes.isEmpty()) {
                ApiRegions.empty()
            } else {
                fetchApiRegions(serviceKey, verificationTarget)
            }
            val report = RegionalGuideAvailabilityReport(assetKeys, apiRegions, verificationTarget).toMarkdown()

            writeVerificationReport(report)
            println(report)
        } catch (failure: Throwable) {
            val failureMessage = failure.message ?: "원인을 확인할 수 없습니다."
            println("지역 가이드 availability 검증 실패: $failureMessage")
            writeVerificationReport(
                """
                ## 지역 가이드 availability 검증 실패

                - 원인: $failureMessage
                - API 인증, 통신 또는 응답 형식을 확인하세요.
                """.trimIndent(),
            )
            throw failure
        }
    }

    private fun loadAssetKeys(target: VerificationTarget): Set<RegionalGuideRegionKey> {
        val keys = json.decodeFromString<List<RegionalGuideAvailabilityDto>>(
            File(assetFilePath()).readText(Charsets.UTF_8),
        ).map { region -> region.toRegionKey() }

        assertEquals("지역 가이드 제공 가능 지역 자산에 중복 지역이 있습니다.", keys.size, keys.toSet().size)
        return keys.filter(target::includes).toSet()
    }

    private suspend fun fetchApiRegions(
        serviceKey: String,
        verificationTarget: VerificationTarget,
    ): ApiRegions {
        val scopes = if (verificationTarget.isFull) loadAllVerificationScopes() else verificationTarget.scopes
        check(scopes.isNotEmpty()) { "검증할 시군구명이 없습니다." }

        val apiService = regionalGuideApiService()
        val queriedItems = buildList<QueriedRegionalGuideItem> {
            scopes.map(RegionalGuideVerificationScope::sigunguName).toSet().forEach { sigunguName ->
                fetchApiPages(sigunguName) { pageNo ->
                    apiService.getRegionalGuides(serviceKey, pageNo, PAGE_SIZE, sigunguName = sigunguName)
                }.forEach { item ->
                    add(QueriedRegionalGuideItem(sigunguName, item))
                }
            }
        }
        val keys = queriedItems.map { queriedItem ->
            queriedItem.item.item.toRegionKey(requestContext(queriedItem.sigunguName, queriedItem.item.pageNo))
        }
        val scopedKeys = keys.filter(verificationTarget::includes)

        return ApiRegions(
            keys = scopedKeys.toSet(),
            rowCount = scopedKeys.size,
            queriedSigunguCount = scopes.map(RegionalGuideVerificationScope::sigunguName).toSet().size,
        )
    }

    private suspend fun fetchApiPages(
        sigunguName: String,
        requestPage: suspend (Int) -> Response<RegionalGuideRootDto>,
    ): List<ApiPageItem> {
        val firstPage = fetchApiPage(sigunguName, FIRST_PAGE_NO, requestPage(FIRST_PAGE_NO))
        val items = firstPage.items.map { item -> ApiPageItem(FIRST_PAGE_NO, item) }.toMutableList()
        val seenItems = firstPage.items.toMutableSet()
        check(seenItems.size == firstPage.items.size) {
            "/info API 페이지네이션 중복 행이 있습니다: ${requestContext(sigunguName, FIRST_PAGE_NO)}"
        }
        val totalPages = (firstPage.totalCount + PAGE_SIZE - 1) / PAGE_SIZE

        for (pageNo in (FIRST_PAGE_NO + 1)..totalPages) {
            val page = fetchApiPage(sigunguName, pageNo, requestPage(pageNo))
            check(page.totalCount == firstPage.totalCount) {
                "/info API totalCount가 페이지마다 다릅니다: ${requestContext(sigunguName, pageNo)}"
            }
            val duplicateCount = page.items.count { item -> !seenItems.add(item) }
            check(duplicateCount == 0) {
                "/info API 페이지네이션 중복 행이 있습니다: ${requestContext(sigunguName, pageNo)}, 중복 행=$duplicateCount"
            }
            items += page.items.map { item -> ApiPageItem(pageNo, item) }
        }

        check(items.size == firstPage.totalCount) {
            "/info API 페이지네이션 결과가 totalCount와 다릅니다: " +
                "${requestContext(sigunguName, totalPages)}, expected=${firstPage.totalCount}, actual=${items.size}"
        }
        return items
    }

    private fun fetchApiPage(
        sigunguName: String,
        requestedPageNo: Int,
        response: Response<RegionalGuideRootDto>,
    ): ApiPage {
        val context = requestContext(sigunguName, requestedPageNo)
        check(response.isSuccessful) { "/info API HTTP 오류: ${response.code()}, $context" }
        val apiResponse = checkNotNull(response.body()?.response) { "/info API 응답이 비어 있습니다: $context" }
        val header = checkNotNull(apiResponse.header) { "/info API 응답에 header가 없습니다: $context" }
        check(header.isSuccessful()) {
            "/info API 오류: 코드=${header.resultCode}, 메시지=${header.resultMessage}, $context"
        }
        val body = checkNotNull(apiResponse.body) { "/info API 응답에 body가 없습니다: $context" }
        check(body.pageNo == requestedPageNo) {
            "/info API 응답 페이지 번호가 요청과 다릅니다: $context, 응답 페이지=${body.pageNo}"
        }
        return ApiPage(
            items = body.items?.item.orEmpty(),
            totalCount = checkNotNull(body.totalCount) { "/info API 응답에 totalCount가 없습니다: $context" },
        )
    }

    private fun regionalGuideApiService(): RegionalGuideApiService {
        return Retrofit.Builder()
            .baseUrl(HOUSEHOLD_WASTE_INFO_API_BASE_URL)
            .client(OkHttpClient.Builder().build())
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
            .create(RegionalGuideApiService::class.java)
    }

    private fun assetFilePath(): String {
        return "src/main/assets/${RegionAssetContract.REGIONAL_GUIDE_AVAILABILITY_ASSET_PATH}"
    }

    private fun loadAllVerificationScopes(): Set<RegionalGuideVerificationScope> {
        return json.decodeFromString<List<AdministrativeRegionDto>>(
            File("src/main/assets/${RegionAssetContract.ADMINISTRATIVE_REGION_ASSET_PATH}").readText(Charsets.UTF_8),
        ).map { region -> region.toVerificationScope() }.toSet()
    }

    private fun verificationTarget(): VerificationTarget {
        val scopes = System.getenv(TARGET_REGIONS_ENVIRONMENT)
            ?.lineSequence()
            ?.map(String::trim)
            ?.filter(String::isNotBlank)
            ?.map(::parseVerificationScope)
            ?.toSet()
            .orEmpty()
        return VerificationTarget(
            isFull = System.getenv(TARGETED_VERIFICATION_ENVIRONMENT) != "true",
            scopes = scopes,
        )
    }

    private fun writeVerificationReport(report: String) {
        File(VERIFICATION_REPORT_PATH).apply {
            parentFile?.mkdirs()
            writeText("$report\n", Charsets.UTF_8)
        }
    }

    private fun parseVerificationScope(value: String): RegionalGuideVerificationScope {
        val parts = value.split(TARGET_REGION_SEPARATOR, limit = 2)
        check(parts.size == 2 && parts.all(String::isNotBlank)) { "검증 대상 지역 형식이 올바르지 않습니다: $value" }
        return RegionalGuideVerificationScope(parts[0], parts[1])
    }

    private fun requestContext(sigunguName: String, pageNo: Int): String {
        return "시군구=$sigunguName, 페이지=$pageNo"
    }

    private fun successfulResponse(
        pageNo: Int,
        totalCount: Int = 0,
        items: List<RegionalGuideItemDto> = emptyList(),
    ): Response<RegionalGuideRootDto> {
        return Response.success(
            RegionalGuideRootDto(
                response = RegionalGuideResponseDto(
                    header = RegionalGuideHeaderDto(resultCode = "0"),
                    body = RegionalGuideBodyDto(
                        pageNo = pageNo,
                        totalCount = totalCount,
                        items = RegionalGuideItemsDto(item = items),
                    ),
                ),
            ),
        )
    }

    private fun RegionalGuideHeaderDto.isSuccessful(): Boolean = resultCode?.toIntOrNull() == SUCCESS_RESULT_CODE

    private fun AdministrativeRegionDto.toVerificationScope(): RegionalGuideVerificationScope {
        return RegionalGuideVerificationScope(sidoName, sigunguName.ifBlank { SEJONG_SIGUNGU_NAME })
    }

    private fun RegionalGuideAvailabilityDto.toRegionKey(): RegionalGuideRegionKey {
        return RegionalGuideRegionKey(sidoName, sigunguName, managementZoneName, targetRegionName)
    }

    private fun RegionalGuideItemDto.toRegionKey(context: String): RegionalGuideRegionKey {
        return RegionalGuideRegionKey(
            sidoName = requireRegionName(sidoName, "CTPV_NM", context),
            sigunguName = requireRegionName(sigunguName, "SGG_NM", context),
            managementZoneName = requireRegionName(managementZoneName, "MNG_ZONE_NM", context),
            targetRegionName = requireRegionName(dongName, "MNG_ZONE_TRGT_RGN_NM", context),
        )
    }

    private fun requireRegionName(value: String?, fieldName: String, context: String): String {
        return value?.takeIf(String::isNotBlank)
            ?: throw AssertionError("/info API 응답의 $fieldName 값이 비어 있습니다: $context")
    }

    private data class RegionalGuideRegionKey(
        val sidoName: String,
        val sigunguName: String,
        val managementZoneName: String,
        val targetRegionName: String,
    ) {
        fun scope(): RegionalGuideVerificationScope = RegionalGuideVerificationScope(sidoName, sigunguName)

        fun displayName(): String = listOf(sidoName, sigunguName, managementZoneName, targetRegionName).joinToString(" > ")
    }

    private data class RegionalGuideVerificationScope(
        val sidoName: String,
        val sigunguName: String,
    ) {
        fun displayName(): String = "$sidoName $sigunguName"
    }

    private data class ApiPage(val items: List<RegionalGuideItemDto>, val totalCount: Int)

    private data class ApiPageItem(val pageNo: Int, val item: RegionalGuideItemDto)

    private data class QueriedRegionalGuideItem(val sigunguName: String, val item: ApiPageItem)

    private data class ApiRegions(
        val keys: Set<RegionalGuideRegionKey>,
        val rowCount: Int,
        val queriedSigunguCount: Int,
    ) {
        companion object {
            fun empty(): ApiRegions = ApiRegions(emptySet(), 0, 0)
        }
    }

    private data class VerificationTarget(
        val isFull: Boolean,
        val scopes: Set<RegionalGuideVerificationScope>,
    ) {
        val isTargeted: Boolean get() = !isFull

        fun includes(key: RegionalGuideRegionKey): Boolean = isFull || key.scope() in scopes
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
                when {
                    verificationTarget.isFull -> add("- 검증 범위: 전국 시군구 ${apiRegions.queriedSigunguCount}곳")
                    verificationTarget.scopes.isEmpty() -> add("- 검증 범위: 변경 지역 없음")
                    else -> add("- 검증 범위: 변경 시군구 " + verificationTarget.scopes.sortedWith(compareBy(RegionalGuideVerificationScope::sidoName, RegionalGuideVerificationScope::sigunguName)).joinToString { it.displayName() })
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

        private fun MutableList<String>.addDifference(title: String, keys: Set<RegionalGuideRegionKey>) {
            if (keys.isEmpty()) return
            add("")
            add("### $title (최대 ${MAX_REPORTED_DIFFERENCES}건)")
            keys.sortedBy(RegionalGuideRegionKey::displayName).take(MAX_REPORTED_DIFFERENCES)
                .forEach { key -> add("- ${key.displayName()}") }
        }
    }

    private companion object {
        const val FIRST_PAGE_NO = 1
        const val MAX_REPORTED_DIFFERENCES = 20
        const val PAGE_SIZE = 100
        const val SERVICE_KEY_ENVIRONMENT = "PUBLIC_DATA_SERVICE_KEY"
        const val SUCCESS_RESULT_CODE = 0
        const val SEJONG_SIGUNGU_NAME = "없음"
        const val TARGET_REGIONS_ENVIRONMENT = "REGIONAL_GUIDE_AVAILABILITY_TARGET_REGIONS"
        const val TARGET_REGION_SEPARATOR = "\t"
        const val TARGETED_VERIFICATION_ENVIRONMENT = "REGIONAL_GUIDE_AVAILABILITY_TARGETED"
        const val VERIFICATION_ENABLED_ENVIRONMENT = "REGIONAL_GUIDE_AVAILABILITY_VERIFICATION"
        const val VERIFICATION_REPORT_PATH = "build/reports/regional-guide-availability-summary.md"
    }
}
