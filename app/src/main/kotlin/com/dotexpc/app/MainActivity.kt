package com.dotexpc.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import com.dotexpc.app.data.AppSessions
import com.dotexpc.app.data.AppSettings
import com.dotexpc.app.data.ThemeMode
import com.dotexpc.app.ui.DotExpcApp
import com.dotexpc.app.ui.theme.DotExpcTheme
import com.dotexpc.app.ui.theme.toColorSchemeMode

class MainActivity : ComponentActivity() {

    private lateinit var sessions: AppSessions

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        sessions = AppSessions(this)
        sessions.onAppOpened()
        val settings = AppSettings(this)

        // 内容延伸到状态栏/导航栏下方；各页自行处理 inset
        enableEdgeToEdge()

        setContent {
            val systemDark = isSystemInDarkTheme()
            // 应用内的深浅色选择优先于系统设置。
            val darkTheme = when (settings.themeMode) {
                ThemeMode.System -> systemDark
                ThemeMode.Light -> false
                ThemeMode.Dark -> true
            }

            // 系统栏图标跟随「实际生效的深浅色」，而不是只看系统设置。
            val view = LocalView.current
            SideEffect {
                WindowCompat.getInsetsController(window, view).apply {
                    isAppearanceLightStatusBars = !darkTheme
                    isAppearanceLightNavigationBars = !darkTheme
                }
            }

            DotExpcTheme(colorSchemeMode = settings.themeMode.toColorSchemeMode()) {
                DotExpcApp(sessions = sessions, settings = settings)
            }
        }
    }

    override fun onStop() {
        super.onStop()
        sessions.onAppHidden()
    }

    override fun onDestroy() {
        sessions.destroy()
        super.onDestroy()
    }
}
