package com.team.yeogibeoryeo

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.naver.maps.map.NaverMapSdk
import com.team.yeogibeoryeo.ui.theme.EwasteAppTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        NaverMapSdk.getInstance(this).client =
            NaverMapSdk.NcpKeyClient(BuildConfig.NAVER_CLIENT_ID)

        setContent {
            EwasteAppTheme {
                PublicWasteSpotMapScreen()
            }
        }
    }
}