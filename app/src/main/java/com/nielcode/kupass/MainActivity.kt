package com.nielcode.kupass

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.runtime.remember
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.nielcode.kupass.data.local.prefs.PreferenceManager
import com.nielcode.kupass.ui.screens.MainAppScreen
import com.nielcode.kupass.ui.theme.KupassTheme
import com.nielcode.kupass.utils.AppConfig

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)

        enableEdgeToEdge()
        setContent {
            val prefs = remember { PreferenceManager(this) }
            val isDynamicEnabled = prefs.dynamicColor == AppConfig.DynamicColors.Code.ENABLE
            KupassTheme(dynamicColor = isDynamicEnabled) { MainAppScreen() }
        }
    }
}
