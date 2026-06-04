package com.team.yeogibeoryeo.data.time

import com.team.yeogibeoryeo.domain.time.TimeProvider
import javax.inject.Inject

class SystemTimeProvider @Inject constructor() : TimeProvider {
    override fun currentTimeMillis(): Long {
        return System.currentTimeMillis()
    }
}
