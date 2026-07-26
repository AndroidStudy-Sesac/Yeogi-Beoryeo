package com.team.yeogibeoryeo.presentation.regionalguide.log

import android.util.Log
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

interface RegionalGuideErrorLogger {
    fun log(
        operation: String,
        throwable: Throwable,
    )
}

object LogcatRegionalGuideErrorLogger : RegionalGuideErrorLogger {
    override fun log(
        operation: String,
        throwable: Throwable,
    ) {
        Log.e(REGIONAL_GUIDE_ERROR_TAG, "$operation 중 오류가 발생했습니다.", throwable)
    }
}

@Module
@InstallIn(SingletonComponent::class)
object RegionalGuideErrorLoggerModule {
    @Provides
    @Singleton
    fun provideRegionalGuideErrorLogger(): RegionalGuideErrorLogger =
        LogcatRegionalGuideErrorLogger
}

private const val REGIONAL_GUIDE_ERROR_TAG = "RegionalGuide"
