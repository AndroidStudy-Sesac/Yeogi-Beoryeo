package com.team.yeogibeoryeo.presentation.map.location

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

interface LocationPermissionChecker {
    fun hasFineLocationPermission(): Boolean
}

class AndroidLocationPermissionChecker @Inject constructor(
    @param:ApplicationContext private val context: Context,
) : LocationPermissionChecker {

    override fun hasFineLocationPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION,
        ) == PackageManager.PERMISSION_GRANTED
    }
}
