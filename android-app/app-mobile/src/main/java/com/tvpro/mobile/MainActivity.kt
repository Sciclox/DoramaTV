package com.tvpro.mobile

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.tvpro.mobile.navigation.TvProMobileAppContent
import com.tvpro.mobile.ui.theme.TvProMobileTheme

class MainActivity : ComponentActivity() {

    private var currentActivityIntent by mutableStateOf<Intent?>(null)

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        currentActivityIntent = intent
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        currentActivityIntent = intent

        setContent {
            TvProMobileTheme {
                TvProMobileAppContent(activityIntent = currentActivityIntent)
            }
        }
    }
}
