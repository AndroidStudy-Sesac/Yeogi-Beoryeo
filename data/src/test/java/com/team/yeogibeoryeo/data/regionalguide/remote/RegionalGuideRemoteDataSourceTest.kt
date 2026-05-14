package com.team.yeogibeoryeo.data.regionalguide.remote

import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import retrofit2.Retrofit

/**
 * 실제 행정안전부 공공데이터 API와 통신하는 Integration Test.
 *
 * 검증 대상:
 * 1. /info API 200 OK 통신 여부
 * 2. JSON 응답이 DTO에 정상적으로 매핑되는지 여부
 *
 * 주의:
 * - 실제 네트워크 환경이 필요합니다.
 * - API Key 설정이 필요합니다.
 * - 외부 API 상태에 따라 테스트가 실패할 수 있습니다.
 */
class RegionalGuideRemoteDataSourceTest {

    private lateinit var dataSource: RegionalGuideRemoteDataSource

    @Before
    fun setUp() {
        val json = Json { ignoreUnknownKeys = true }

        val loggingInterceptor = HttpLoggingInterceptor { message ->
            println(message)
        }.apply {
            level = HttpLoggingInterceptor.Level.BODY
        }

        val okHttpClient = OkHttpClient.Builder()
            .addInterceptor(loggingInterceptor)
            .build()

        val retrofit = Retrofit.Builder()
            .baseUrl("https://apis.data.go.kr/")
            .client(okHttpClient)
            .addConverterFactory(
                json.asConverterFactory("application/json".toMediaType())
            )
            .build()

        val apiService = retrofit.create(RegionalGuideApiService::class.java)
        dataSource = RegionalGuideRemoteDataSource(apiService)
    }

    @Test
    fun `행안부 API 실제 통신 및 JSON DTO 파싱이 정상 수행된다`() = runBlocking {
        val sigunguName = "영등포구"
        val result = dataSource.fetchRegionalGuides(sigunguName)

        assertTrue("API 통신 실패: ${result.exceptionOrNull()?.message}", result.isSuccess)

        val items = result.getOrNull()

        println(
            """
            =========================================
            ✅ 파싱된 아이템 개수: ${items?.size}
            
            ✅ 첫 번째 데이터 샘플:
            ${items?.firstOrNull()}
            =========================================
            """.trimIndent()
        )

        assertTrue("데이터가 비어있습니다.", !items.isNullOrEmpty())
    }
}