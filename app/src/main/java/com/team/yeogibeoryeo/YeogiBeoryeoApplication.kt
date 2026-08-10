package com.team.yeogibeoryeo

import android.app.Application
import com.team.yeogibeoryeo.core.startup.OperationNoticeStartupInitializer
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp
class YeogiBeoryeoApplication : Application() {

    @Inject
    lateinit var operationNoticeStartupInitializer: OperationNoticeStartupInitializer

    override fun onCreate() {
        super.onCreate()
        operationNoticeStartupInitializer.initialize()
    }
}
