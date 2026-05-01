package com.moon.yeogi_beoryeo

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.moon.yeogi_beoryeo.spike.SpotTestScreen
import com.moon.yeogi_beoryeo.spike.SpotViewModel
import com.moon.yeogi_beoryeo.ui.theme.YeogiBeoryeoTheme

class SpotTestActivity : ComponentActivity() {
    private val viewModel: SpotViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            YeogiBeoryeoTheme {
                Surface(
                    modifier = Modifier
                        .fillMaxSize()
                        .windowInsetsPadding(WindowInsets.safeDrawing)
                        .padding(16.dp)
                ) {
                    SpotTestScreen(viewModel)
                }
            }
        }
    }
}