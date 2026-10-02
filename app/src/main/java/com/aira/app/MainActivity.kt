package com.aira.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.aira.app.ui.navigation.AiraNavHost
import com.aira.app.ui.theme.AiraTheme
import dagger.hilt.android.AndroidEntryPoint

/** The single activity of the app. All screens are Compose destinations. */
@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        // Must come before super.onCreate: swaps the splash theme for the normal app theme once the app is ready.
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            AiraTheme {
                AiraNavHost()
            }
        }
    }
}
