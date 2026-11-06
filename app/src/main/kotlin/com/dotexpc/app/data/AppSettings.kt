package com.dotexpc.app.data

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/** 深浅色偏好：跟随系统、始终浅色、始终深色。 */
enum class ThemeMode(val label: String) {
    System("跟随系统"),
    Light("浅色"),
    Dark("深色"),
}

/** 底栏图标风格：实心图形或线条描边。 */
enum class TabIconStyle(val label: String) {
    Filled("填充"),
    Outline("框线"),
}

/** 外观设置，只保存在本机；改动即时生效并写入 SharedPreferences。 */
class AppSettings(context: Context) {

    private val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    var themeMode by mutableStateOf(
        ThemeMode.entries.firstOrNull { it.name == prefs.getString(KEY_THEME, null) } ?: ThemeMode.System,
    )
        private set

    var tabIconStyle by mutableStateOf(
        TabIconStyle.entries.firstOrNull { it.name == prefs.getString(KEY_ICONS, null) }
            ?: TabIconStyle.Filled,
    )
        private set

    var liquidTabBar by mutableStateOf(prefs.getBoolean(KEY_GLASS, false))
        private set

    var termsAccepted by mutableStateOf(prefs.getBoolean(KEY_TERMS, false))
        private set

    fun selectThemeMode(mode: ThemeMode) {
        themeMode = mode
        prefs.edit().putString(KEY_THEME, mode.name).apply()
    }

    fun selectTabIconStyle(style: TabIconStyle) {
        tabIconStyle = style
        prefs.edit().putString(KEY_ICONS, style.name).apply()
    }

    fun toggleLiquidTabBar() {
        liquidTabBar = !liquidTabBar
        prefs.edit().putBoolean(KEY_GLASS, liquidTabBar).apply()
    }

    fun acceptTerms() {
        termsAccepted = true
        prefs.edit().putBoolean(KEY_TERMS, true).apply()
    }

    fun resetTerms() {
        termsAccepted = false
        prefs.edit().putBoolean(KEY_TERMS, false).apply()
    }

    private companion object {
        const val PREFS = "settings"
        const val KEY_THEME = "theme_mode"
        const val KEY_ICONS = "tab_icon_style"
        const val KEY_GLASS = "liquid_tab_bar"
        const val KEY_TERMS = "terms_accepted"
    }
}
