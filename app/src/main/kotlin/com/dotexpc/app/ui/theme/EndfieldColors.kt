package com.dotexpc.app.ui.theme

import androidx.compose.ui.graphics.Color
import top.yukonga.miuix.kmp.theme.Colors
import top.yukonga.miuix.kmp.theme.darkColorScheme
import top.yukonga.miuix.kmp.theme.lightColorScheme

/**
 * 隐藏主题「终末地」的 Miuix 配色映射。
 *
 * 主色是墨不是黄：Miuix 用 primary 画文字、光标、滑轨这些细线，黄色在浅色纸上看不见。
 * 信号黄只放在实心填充上 —— 默认按钮（secondaryVariant）、选中项（tertiaryContainer），
 * 内容色统一用墨色。其余角色按面板 / 细线 / 次级文字对号入座。
 */
internal fun endfieldLightColors(): Colors = EfLight.toMiuixColors(isDark = false)

internal fun endfieldDarkColors(): Colors = EfDark.toMiuixColors(isDark = true)

private fun EfColors.toMiuixColors(isDark: Boolean): Colors {
    val scheme = if (isDark) darkColorScheme() else lightColorScheme()
    return scheme.copy(
        primary = ink,
        onPrimary = paper,
        primaryVariant = ink,
        onPrimaryVariant = paper,
        error = danger,
        onError = onSignal,
        disabledPrimary = ink2,
        disabledOnPrimary = chip,
        disabledPrimaryButton = signal.copy(alpha = 0.4f),
        disabledOnPrimaryButton = onSignal.copy(alpha = 0.38f),
        disabledPrimarySlider = ink2,
        primaryContainer = signal,
        onPrimaryContainer = onSignal,
        // 未选中开关的轨道；状态主要由滑块位置表达，轨道保持安静。
        secondary = if (isDark) rule else chip,
        onSecondary = panel,
        secondaryVariant = signal,
        onSecondaryVariant = onSignal,
        disabledSecondary = (if (isDark) rule else chip).copy(alpha = 0.6f),
        disabledOnSecondary = ink2,
        disabledSecondaryVariant = signal.copy(alpha = 0.4f),
        disabledOnSecondaryVariant = onSignal.copy(alpha = 0.38f),
        secondaryContainer = chip,
        onSecondaryContainer = ink2,
        secondaryContainerVariant = chip,
        onSecondaryContainerVariant = ink2,
        tertiaryContainer = signal,
        onTertiaryContainer = onSignal,
        tertiaryContainerVariant = chip,
        background = paper,
        onBackground = ink,
        onBackgroundVariant = ink2,
        surface = paper,
        onSurface = ink,
        surfaceVariant = panel,
        onSurfaceSecondary = ink.copy(alpha = 0.8f),
        onSurfaceVariantSummary = ink2,
        onSurfaceVariantActions = ink.copy(alpha = 0.4f),
        disabledOnSurface = ink2.copy(alpha = 0.6f),
        surfaceContainer = panel,
        onSurfaceContainer = ink,
        onSurfaceContainerVariant = ink2,
        surfaceContainerHigh = chip,
        onSurfaceContainerHigh = ink2,
        surfaceContainerHighest = chip,
        onSurfaceContainerHighest = ink,
        outline = rule,
        dividerLine = rule,
        windowDimming = scrim,
        sliderKeyPoint = ink.copy(alpha = 0.3f),
        sliderKeyPointForeground = if (isDark) signal else ink,
        sliderBackground = ink.copy(alpha = 0.06f),
    )
}
