package com.team.yeogibeoryeo.data.spot.mapper

import com.team.yeogibeoryeo.data.spot.remote.dto.SpotItemDto
import com.team.yeogibeoryeo.domain.spot.model.CollectionSpot
import javax.inject.Inject

class SpotMapper @Inject constructor() {

    fun mapToDomain(
        dto: SpotItemDto,
    ): CollectionSpot {
        val name = dto.spotNm.orEmpty()
        val address = dto.addrBase.orEmpty()
        val detailLocation = dto.addrDtl?.takeIf { it.isNotBlank() }

        return CollectionSpot(
            id = createSpotId(
                name = name,
                address = address,
                detailLocation = detailLocation,
            ),
            name = name,
            type = SpotTypeMapper.mapToType(
                spotName = name,
                detailAddress = detailLocation,
            ),
            address = address,
            detailLocation = detailLocation,
            coordinate = null,
            distanceMeter = null,
            isBookmarked = false,
        )
    }

    fun mapToDomainList(
        dtoList: List<SpotItemDto>,
    ): List<CollectionSpot> {
        return dtoList.map { dto ->
            mapToDomain(dto)
        }
    }

    private fun createSpotId(
        name: String,
        address: String,
        detailLocation: String?,
    ): String {
        return listOfNotNull(
            name,
            address,
            detailLocation,
        ).joinToString(separator = "_")
    }
}