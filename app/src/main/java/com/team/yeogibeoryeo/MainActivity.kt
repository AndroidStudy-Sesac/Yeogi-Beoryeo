package com.team.yeogibeoryeo

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.google.firebase.Firebase
import com.google.firebase.remoteconfig.remoteConfig
import com.google.firebase.remoteconfig.remoteConfigSettings
import com.team.yeogibeoryeo.common.design.theme.YeogiBeoryeoTheme
import com.team.yeogibeoryeo.navigation.YeogiBeoryeoNavHost
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        testRemoteConfig()
        enableEdgeToEdge()
        setContent {
            YeogiBeoryeoTheme {
                YeogiBeoryeoNavHost()
            }
        }
    }

    private fun testRemoteConfig() {
        val remoteConfig = Firebase.remoteConfig
        val settings = remoteConfigSettings {
            minimumFetchIntervalInSeconds = if (BuildConfig.DEBUG) {
                0L
            } else {
                REMOTE_CONFIG_RELEASE_FETCH_INTERVAL_SECONDS
            }
        }

        remoteConfig.setConfigSettingsAsync(settings)
        remoteConfig.setDefaultsAsync(R.xml.remote_config_defaults)
            .continueWithTask {
                remoteConfig.fetchAndActivate()
            }
            .addOnCompleteListener { task ->
                val noticeJson = remoteConfig.getString(OPERATION_NOTICE_KEY)
                if (task.isSuccessful) {
                    Log.d(
                        REMOTE_CONFIG_TAG,
                        "updated=${task.result}, notice=$noticeJson",
                    )
                } else {
                    Log.e(
                        REMOTE_CONFIG_TAG,
                        "Remote Config fetch failed. fallback=$noticeJson",
                        task.exception,
                    )
                }
            }
    }

    private companion object {
        const val REMOTE_CONFIG_TAG = "RemoteConfigTest"
        const val OPERATION_NOTICE_KEY = "operation_notice"
        const val REMOTE_CONFIG_RELEASE_FETCH_INTERVAL_SECONDS = 3_600L
    }
}
