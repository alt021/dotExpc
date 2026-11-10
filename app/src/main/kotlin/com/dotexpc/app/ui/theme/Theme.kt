package com.dotexpc.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import com.dotexpc.app.data.ThemeMode
import top.yukonga.miuix.kmp.theme.ColorSchemeMode
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.theme.ThemeController
import top.yukonga.miuix.kmp.theme.defaultTextStyles

/** 三档主题开关映射到 Miuix 配色模式；刻意不用 Monet* 分支，保持内置蓝白配色。 */
fun ThemeMode.toColorSchemeMode(): ColorSchemeMode = when (this) {
    ThemeMode.System -> ColorSchemeMode.System
    ThemeMode.Light -> ColorSchemeMode.Light
    ThemeMode.Dark -> ColorSchemeMode.Dark
}

/**
 * 应用主题：默认使用 Miuix 内置蓝白配色。
 *
 * 刻意不传 `keyColor`、也不使用 `Monet*` 模式 —— 那两条路都会落到 MD3 取色
 * （种子色推导 / 壁纸动态色），拿不到固定的 Miuix 主色 `#3482FF`。
 *
 * `endfield` 打开时换成隐藏的「终末地」皮肤：纸墨配色、Saira 宽体字，
 * 并且通过 [LocalEndfield] 让底栏、弹窗、按钮等组件切换成切角样式。
 *
 * `ThemeController.colorSchemeMode` 是不可变 `val`，切换模式只能重建 controller，
 * 因此 `remember` 以模式和皮肤为键。
 */
@Composable
fun DotExpcTheme(
    colorSchemeMode: ColorSchemeMode = ColorSchemeMode.System,
    endfield: Boolean = false,
    content: @Composable () -> Unit,
) {
    val controller = remember(colorSchemeMode, endfield) {
        if (endfield) {
            ThemeController(
                colorSchemeMode = colorSchemeMode,
                lightColors = endfieldLightColors(),
                darkColors = endfieldDarkColors(),
            )
        } else {
            ThemeController(colorSchemeMode = colorSchemeMode)
        }
    }

    val textStyles = remember(endfield) {
        if (endfield) endfieldTextStyles() else defaultTextStyles()
    }

    // 皮肤令牌要跟着实际生效的深浅色走，而不是只看系统设置。
    val systemDark = isSystemInDarkTheme()
    val dark = when (colorSchemeMode) {
        ColorSchemeMode.System -> systemDark
        ColorSchemeMode.Light -> false
        ColorSchemeMode.Dark -> true
        else -> systemDark
    }
    val palette = if (dark) EfDark else EfLight

    CompositionLocalProvider(
        LocalEndfield provides endfield,
        LocalEfColors provides palette,
    ) {
        MiuixTheme(
            controller = controller,
            textStyles = textStyles,
            content = content,
        )
    }
}
