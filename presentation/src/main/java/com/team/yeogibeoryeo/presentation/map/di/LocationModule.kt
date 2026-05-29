package com.team.yeogibeoryeo.presentation.map.di

import android.content.Context
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.team.yeogibeoryeo.presentation.map.location.AndroidCurrentLocationProvider
import com.team.yeogibeoryeo.presentation.map.location.AndroidLocationPermissionChecker
import com.team.yeogibeoryeo.presentation.map.location.CurrentLocationProvider
import com.team.yeogibeoryeo.presentation.map.location.LocationPermissionChecker
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class LocationBindModule {

    @Binds
    @Singleton
    abstract fun bindCurrentLocationProvider(
        androidCurrentLocationProvider: AndroidCurrentLocationProvider,
    ): CurrentLocationProvider

    @Binds
    @Singleton
    abstract fun bindLocationPermissionChecker(
        androidLocationPermissionChecker: AndroidLocationPermissionChecker,
    ): LocationPermissionChecker
}

@Module
@InstallIn(SingletonComponent::class)
object LocationProvideModule {

    @Provides
    @Singleton
    fun provideFusedLocationProviderClient(
        @ApplicationContext context: Context,
    ): FusedLocationProviderClient {
        return LocationServices.getFusedLocationProviderClient(context)
    }
}
