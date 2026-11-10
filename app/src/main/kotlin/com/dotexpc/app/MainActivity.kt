package com.dotexpc.app

import android.content.ComponentName
import android.content.pm.PackageManager
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
    private lateinit var settings: AppSettings

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        sessions = AppSessions(this)
        sessions.onAppOpened()
        settings = AppSettings(this)

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

            DotExpcTheme(
                colorSchemeMode = settings.themeMode.toColorSchemeMode(),
                endfield = settings.endfieldTheme,
            ) {
                DotExpcApp(sessions = sessions, settings = settings)
            }
        }
    }

    override fun onStop() {
        super.onStop()
        // 图标只在离开应用时换：桌面此时才看得到图标，也避免前台禁用当前任务的启动入口。
        applyLauncherIcon(settings.endfieldTheme)
        sessions.persistCookies()
        sessions.onAppHidden()
    }

    override fun onDestroy() {
        sessions.destroy()
        super.onDestroy()
    }

    /**
     * 启动器图标跟随终末地主题：两个 activity-alias 只差图标，启用其中一个即可。
     * 只在状态不一致时写入，避免每次启动都让桌面重新加载图标。
     */
    private fun applyLauncherIcon(endfield: Boolean) {
        val active = ComponentName(this, "$packageName.Launcher${if (endfield) "Endfield" else "Miuix"}")
        val inactive = ComponentName(this, "$packageName.Launcher${if (endfield) "Miuix" else "Endfield"}")
        setComponentState(active, PackageManager.COMPONENT_ENABLED_STATE_ENABLED)
        setComponentState(inactive, PackageManager.COMPONENT_ENABLED_STATE_DISABLED)
    }

    private fun setComponentState(component: ComponentName, state: Int) {
        val manager = packageManager
        if (manager.getComponentEnabledSetting(component) == state) return
        manager.setComponentEnabledSetting(component, state, PackageManager.DONT_KILL_APP)
    }
}
