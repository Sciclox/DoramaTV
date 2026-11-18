package com.tvpro.app

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.view.KeyEvent
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.tvpro.app.navigation.TvNavHost

data class DeepLinkPayload(
    val uri: String,
    val title: String? = null
)

object DeepLinkBus {
    var payload by mutableStateOf<DeepLinkPayload?>(null)
}

object PlayerKeyDispatcher {
    var onPlayerKeyEvent: ((KeyEvent) -> Boolean)? = null
}

class MainActivity : ComponentActivity() {

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        val dataStr = intent.dataString
        android.util.Log.e("TVPRO_PLAY", "MainActivity onNewIntent: data=$dataStr, extraTitle=${intent.getStringExtra("title")}")
        if (dataStr != null) {
            DeepLinkBus.payload = DeepLinkPayload(uri = dataStr, title = intent.getStringExtra("title"))
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        val dataStr = intent?.dataString
        android.util.Log.e("TVPRO_PLAY", "MainActivity onCreate: data=$dataStr, extraTitle=${intent?.getStringExtra("title")}")
        if (dataStr != null) {
            DeepLinkBus.payload = DeepLinkPayload(uri = dataStr, title = intent?.getStringExtra("title"))
        }
        setContent {
            TvNavHost()
        }
    }

    @SuppressLint("RestrictedApi")
    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        val handler = PlayerKeyDispatcher.onPlayerKeyEvent
        if (handler != null && handler.invoke(event)) {
            return true
        }

        return try {
            super.dispatchKeyEvent(event)
        } catch (e: IllegalStateException) {
            if (e.message?.contains("focus system is invalidated") == true) {
                // Safeguard against Compose focus invalidation race conditions during rapid D-pad navigation
                true
            } else {
                throw e
            }
        }
    }
}
