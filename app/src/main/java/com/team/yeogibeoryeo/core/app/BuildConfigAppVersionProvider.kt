package com.team.yeogibeoryeo.core.app

import com.team.yeogibeoryeo.BuildConfig
import com.team.yeogibeoryeo.domain.app.AppVersionProvider
import javax.inject.Inject

class BuildConfigAppVersionProvider @Inject constructor() : AppVersionProvider {
    override val versionCode: Int
        get() = BuildConfig.VERSION_CODE
}

