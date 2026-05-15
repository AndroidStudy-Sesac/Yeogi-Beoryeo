package com.team.yeogibeoryeo.core.key

import com.team.yeogibeoryeo.BuildConfig
import com.team.yeogibeoryeo.data.core.key.AppKeyProvider
import javax.inject.Inject

class BuildConfigAppKeyProvider @Inject constructor() : AppKeyProvider {

    override val publicDataServiceKey: String
        get() = BuildConfig.PUBLIC_DATA_SERVICE_KEY

    override val naverClientId: String
        get() = BuildConfig.NAVER_CLIENT_ID
}
