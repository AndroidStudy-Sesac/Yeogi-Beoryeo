package com.team.yeogibeoryeo.data.item.remote.datasource

import com.team.yeogibeoryeo.data.item.remote.ItemApiService
import com.team.yeogibeoryeo.data.item.remote.dto.ItemGuideDto
import com.team.yeogibeoryeo.data.item.remote.dto.ItemGuideResponseDto
import javax.inject.Inject

class ItemRemoteDataSource
    @Inject
    constructor(
    private val itemApiService: ItemApiService,
    ) {
    suspend fun searchItems(
        serviceKey: String,
        itemNm: String,
        pageNo: Int = 1,
        numOfRows: Int = DEFAULT_PAGE_SIZE,
    ): List<ItemGuideDto> =
        getPagedItems(
            serviceKey = serviceKey,
            itemNm = itemNm,
            pageNo = pageNo,
            numOfRows = numOfRows,
        )

    private suspend fun getPagedItems(
        serviceKey: String,
        itemNm: String,
        pageNo: Int,
        numOfRows: Int,
    ): List<ItemGuideDto> {
        val items = mutableListOf<ItemGuideDto>()
        var currentPage = pageNo
        var totalCount: Int

        do {
            val response =
                itemApiService
                    .getItem(
                        serviceKey = serviceKey,
                        pageNo = currentPage,
                        numOfRows = numOfRows,
                        itemNm = itemNm,
                    )
            if (response.isNoData()) return emptyList()
            response.throwIfError()

            val body = response.response.body
            val pageItems = body.items?.item.orEmpty()

            items += pageItems
            totalCount = body.totalCount
            currentPage += 1
        } while (items.size < totalCount && pageItems.isNotEmpty())

        return items.distinctBy { it.itemNm }
    }

    private companion object {
        const val DEFAULT_PAGE_SIZE = 100
        val SUCCESS_CODES = setOf("00", "200")
        // API 문서에는 NODATA_ERROR가 "3"으로 정의되어 있지만, 실제 /getItem 응답에서는 "03"으로 내려올 수 있습니다.
        val NO_DATA_CODES = setOf("3", "03")
        const val INVALID_REQUEST_PARAMETER = "10"
        const val NO_MANDATORY_REQUEST_PARAMETER = "11"
        const val ETC_ERROR = "99"
    }

    private fun ItemGuideResponseDto.isNoData(): Boolean = response.header.resultCode in NO_DATA_CODES

    private fun ItemGuideResponseDto.throwIfError() {
        val header = response.header
        when (header.resultCode) {
            in SUCCESS_CODES -> return
            INVALID_REQUEST_PARAMETER, NO_MANDATORY_REQUEST_PARAMETER, ETC_ERROR ->
                throw ItemApiException(header.resultCode, header.resultMsg)
            else -> throw ItemApiException(header.resultCode, header.resultMsg)
        }
    }
}
