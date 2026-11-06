package com.sylveroptimizer.android

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.ui.graphics.toArgb
import androidx.core.view.WindowCompat
import com.sylveroptimizer.android.services.AppSettingsService
import com.sylveroptimizer.android.services.OptimizerService
import com.sylveroptimizer.android.services.SystemMonitorService
import com.sylveroptimizer.android.ui.SylverOptimizerScreen

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val settingsService = AppSettingsService(applicationContext)
        val monitorService = SystemMonitorService(applicationContext)
        val optimizerService = OptimizerService(applicationContext)

        setContent {
            SylverOptimizerScreen(
                settingsService = settingsService,
                monitorService = monitorService,
                optimizerService = optimizerService,
                onThemeChanged = { theme ->
                    window.statusBarColor = theme.windowBg.toArgb()
                    window.navigationBarColor = theme.windowBg.toArgb()
                    val isLightTheme = theme.id.equals("NordicLight", ignoreCase = true) ||
                        theme.id.equals("LightGray", ignoreCase = true)
                    val controller = WindowCompat.getInsetsController(window, window.decorView)
                    controller.isAppearanceLightStatusBars = isLightTheme
                    controller.isAppearanceLightNavigationBars = isLightTheme
                }
            )
        }
    }
}

