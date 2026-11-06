package com.dotexpc.app.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import com.dotexpc.app.data.ThemeMode
import top.yukonga.miuix.kmp.theme.ColorSchemeMode
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.theme.ThemeController

/** 三档主题开关映射到 Miuix 配色模式；刻意不用 Monet* 分支，保持内置蓝白配色。 */
fun ThemeMode.toColorSchemeMode(): ColorSchemeMode = when (this) {
    ThemeMode.System -> ColorSchemeMode.System
    ThemeMode.Light -> ColorSchemeMode.Light
    ThemeMode.Dark -> ColorSchemeMode.Dark
}

/**
 * 应用主题：直接使用 Miuix 内置蓝白配色。
 *
 * 刻意不传 `keyColor`、也不使用 `Monet*` 模式 —— 那两条路都会落到 MD3 取色
 * （种子色推导 / 壁纸动态色），拿不到固定的 Miuix 主色 `#3482FF`。
 *
 * `ThemeController.colorSchemeMode` 是不可变 `val`，切换模式只能重建 controller，
 * 因此 `remember` 以模式为键。
 */
@Composable
fun DotExpcTheme(
    colorSchemeMode: ColorSchemeMode = ColorSchemeMode.System,
    content: @Composable () -> Unit,
) {
    val controller = remember(colorSchemeMode) {
        ThemeController(colorSchemeMode = colorSchemeMode)
    }

    MiuixTheme(
        controller = controller,
        content = content,
    )
}
