package com.team.yeogibeoryeo.data.spot.di

import com.team.yeogibeoryeo.data.spot.geocoder.AndroidSpotGeocoder
import com.team.yeogibeoryeo.data.spot.geocoder.SpotGeocoder
import com.team.yeogibeoryeo.data.spot.repository.CollectionSpotRepositoryImpl
import com.team.yeogibeoryeo.domain.spot.repository.CollectionSpotRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class SpotDataModule {

    @Binds
    @Singleton
    abstract fun bindCollectionSpotRepository(
        collectionSpotRepositoryImpl: CollectionSpotRepositoryImpl,
    ): CollectionSpotRepository

    @Binds
    @Singleton
    abstract fun bindSpotGeocoder(
        androidSpotGeocoder: AndroidSpotGeocoder,
    ): SpotGeocoder
}