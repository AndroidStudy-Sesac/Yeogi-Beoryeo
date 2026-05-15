package com.team.yeogibeoryeo.core.key

import com.team.yeogibeoryeo.BuildConfig
import com.team.yeogibeoryeo.data.core.key.PublicDataKeyProvider
import javax.inject.Inject

class BuildConfigPublicDataKeyProvider @Inject constructor() : PublicDataKeyProvider {

    override val serviceKey: String
        get() = BuildConfig.PUBLIC_DATA_SERVICE_KEY
}