package com.team.yeogibeoryeo.presentation.map.log

import android.util.Log
import com.team.yeogibeoryeo.domain.spot.log.MapSearchTimingLogger
import com.team.yeogibeoryeo.presentation.BuildConfig
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

object LogcatMapSearchTimingLogger : MapSearchTimingLogger {
    override fun log(message: String) {
        if (BuildConfig.DEBUG) {
            Log.d(MAP_SEARCH_TIMING_TAG, message)
        }
    }
}

@Module
@InstallIn(SingletonComponent::class)
object MapSearchTimingLoggerModule {
    @Provides
    @Singleton
    fun provideMapSearchTimingLogger(): MapSearchTimingLogger =
        LogcatMapSearchTimingLogger
}

private const val MAP_SEARCH_TIMING_TAG = "MapSearchTiming"
