package com.team.yeogibeoryeo.data.spot.cache

import com.team.yeogibeoryeo.domain.spot.model.CollectionSpot
import com.team.yeogibeoryeo.domain.spot.model.CollectionSpotType
import com.team.yeogibeoryeo.domain.spot.model.Coordinate
import com.team.yeogibeoryeo.domain.spot.model.RecentCurrentLocationSpotCacheEntry

internal fun RecentCurrentLocationSpotCacheEntry.toDto(): RecentCurrentLocationSpotCacheDto {
    return RecentCurrentLocationSpotCacheDto(
        spots = spots.map(CollectionSpot::toDto),
        searchCoordinate = searchCoordinate.toDto(),
        savedAtMillis = savedAtMillis,
    )
}

internal fun RecentCurrentLocationSpotCacheDto.toDomain(): RecentCurrentLocationSpotCacheEntry {
    return RecentCurrentLocationSpotCacheEntry(
        spots = spots.map(CollectionSpotCacheDto::toDomain),
        searchCoordinate = requireNotNull(searchCoordinate) {
            "최근 현재 위치 캐시에 검색 기준 좌표가 없습니다"
        }.toDomain(),
        savedAtMillis = savedAtMillis,
    )
}

private fun CollectionSpot.toDto(): CollectionSpotCacheDto {
    return CollectionSpotCacheDto(
        id = id,
        name = name,
        type = type.name,
        address = address,
        detailLocation = detailLocation,
        coordinate = coordinate?.toDto(),
        distanceMeter = distanceMeter,
        isBookmarked = isBookmarked,
    )
}

private fun CollectionSpotCacheDto.toDomain(): CollectionSpot {
    return CollectionSpot(
        id = id,
        name = name,
        type = CollectionSpotType.valueOf(type),
        address = address,
        detailLocation = detailLocation,
        coordinate = coordinate?.toDomain(),
        distanceMeter = distanceMeter,
        isBookmarked = isBookmarked,
    )
}

private fun Coordinate.toDto(): CoordinateCacheDto {
    return CoordinateCacheDto(
        latitude = latitude,
        longitude = longitude,
    )
}

private fun CoordinateCacheDto.toDomain(): Coordinate {
    return Coordinate(
        latitude = latitude,
        longitude = longitude,
    )
}
