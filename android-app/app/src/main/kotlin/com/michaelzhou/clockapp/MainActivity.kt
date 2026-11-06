package com.michaelzhou.clockapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.material3.windowsizeclass.calculateWindowSizeClass
import com.michaelzhou.clockapp.ui.ClockScreen
import com.michaelzhou.clockapp.ui.theme.ClockAppTheme

class MainActivity : ComponentActivity() {
    @OptIn(ExperimentalMaterial3WindowSizeClassApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val windowSizeClass = calculateWindowSizeClass(this)
            ClockAppTheme {
                ClockScreen(windowWidthSizeClass = windowSizeClass.widthSizeClass)
            }
        }
    }
}
