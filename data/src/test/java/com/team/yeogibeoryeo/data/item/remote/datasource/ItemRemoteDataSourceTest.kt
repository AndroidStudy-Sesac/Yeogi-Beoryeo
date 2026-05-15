package com.team.yeogibeoryeo.data.item.remote.datasource

import com.team.yeogibeoryeo.data.item.remote.ItemApiService
import com.team.yeogibeoryeo.data.item.remote.dto.ItemGuideBodyDto
import com.team.yeogibeoryeo.data.item.remote.dto.ItemGuideDto
import com.team.yeogibeoryeo.data.item.remote.dto.ItemGuideHeaderDto
import com.team.yeogibeoryeo.data.item.remote.dto.ItemGuideItemsDto
import com.team.yeogibeoryeo.data.item.remote.dto.ItemGuideResponseBodyDto
import com.team.yeogibeoryeo.data.item.remote.dto.ItemGuideResponseDto
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ItemRemoteDataSourceTest {
    @Test
    fun `응답 코드 00이면 품목 목록을 반환한다`() =
        runBlocking {
            val dataSource =
                ItemRemoteDataSource(
                    fakeService(
                        response(
                            resultCode = "00",
                            resultMsg = "NORMAL SERVICE.",
                            items = listOf(ItemGuideDto(itemNm = "유리병", dschgMthd = "재활용폐기물")),
                        ),
                    ),
                )

            val result = dataSource.searchItems(serviceKey = "key", itemNm = "유리")

            assertEquals(1, result.size)
            assertEquals("유리병", result.first().itemNm)
        }

    @Test
    fun `응답 코드 200이면 품목 목록을 반환한다`() =
        runBlocking {
            val dataSource =
                ItemRemoteDataSource(
                    fakeService(
                        response(
                            resultCode = "200",
                            resultMsg = "성공",
                            items = listOf(ItemGuideDto(itemNm = "골판지", dschgMthd = "재활용폐기물")),
                        ),
                    ),
                )

            val result = dataSource.searchItems(serviceKey = "key", itemNm = "골판지")

            assertEquals(1, result.size)
            assertEquals("골판지", result.first().itemNm)
        }

    @Test
    fun `응답 코드 03이면 빈 목록을 반환한다`() =
        runBlocking {
            val dataSource =
                ItemRemoteDataSource(
                    fakeService(
                        response(
                            resultCode = "03",
                            resultMsg = "NODATA_ERROR",
                            items = emptyList(),
                            totalCount = 0,
                        ),
                    ),
                )

            val result = dataSource.searchItems(serviceKey = "key", itemNm = "없는품목")

            assertTrue(result.isEmpty())
        }

    @Test
    fun `응답 코드 3이면 빈 목록을 반환한다`() =
        runBlocking {
            val dataSource =
                ItemRemoteDataSource(
                    fakeService(
                        response(
                            resultCode = "3",
                            resultMsg = "NODATA_ERROR",
                            items = emptyList(),
                            totalCount = 0,
                        ),
                    ),
                )

            val result = dataSource.searchItems(serviceKey = "key", itemNm = "없는품목")

            assertTrue(result.isEmpty())
        }

    @Test
    fun `응답 코드 10이면 잘못된 요청 예외를 던진다`() =
        runBlocking {
            assertApiError(resultCode = "10", resultMsg = "INVALID_REQUEST_PARAMETER_ERROR")
        }

    @Test
    fun `응답 코드 11이면 필수 파라미터 누락 예외를 던진다`() =
        runBlocking {
            assertApiError(resultCode = "11", resultMsg = "NO_MANDATORY_REQUEST_PARAMETER_ERROR")
        }

    @Test
    fun `응답 코드 99이면 기타 오류 예외를 던진다`() =
        runBlocking {
            assertApiError(resultCode = "99", resultMsg = "ETC_ERROR")
        }

    private suspend fun assertApiError(
        resultCode: String,
        resultMsg: String,
    ) {
        val dataSource =
            ItemRemoteDataSource(
                fakeService(
                    response(
                        resultCode = resultCode,
                        resultMsg = resultMsg,
                        items = emptyList(),
                    ),
                ),
            )

        try {
            dataSource.searchItems(serviceKey = "key", itemNm = "유리")
            throw AssertionError("ItemApiException이 발생해야 합니다")
        } catch (exception: ItemApiException) {
            assertEquals(resultCode, exception.code)
            assertEquals(resultMsg, exception.message)
        }
    }

    private fun fakeService(response: ItemGuideResponseDto): ItemApiService =
        object : ItemApiService {
            override suspend fun getItem(
                serviceKey: String,
                pageNo: Int,
                numOfRows: Int,
                itemNm: String,
            ): ItemGuideResponseDto = response
        }

    private fun response(
        resultCode: String,
        resultMsg: String,
        items: List<ItemGuideDto>,
        totalCount: Int = items.size,
    ): ItemGuideResponseDto =
        ItemGuideResponseDto(
            response =
                ItemGuideResponseBodyDto(
                    header =
                        ItemGuideHeaderDto(
                            resultCode = resultCode,
                            resultMsg = resultMsg,
                        ),
                    body =
                        ItemGuideBodyDto(
                            items = ItemGuideItemsDto(item = items),
                            numOfRows = 100,
                            pageNo = 1,
                            totalCount = totalCount,
                        ),
                ),
        )
}
