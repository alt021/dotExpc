package com.dotexpc.app.ui.theme

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.foundation.shape.CutCornerShape
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * 「终末地」皮肤的令牌与常量，取自 endfield-ui skill 的 assets/tokens.json。
 *
 * Miuix 配色只能表达「主色 / 表面 / 描边」这一层；切角、信号黄、墨标签这些
 * 是 Miuix 没有的角色，放在这里供组件直接使用。
 */
@Immutable
data class EfColors(
    val paper: Color,
    val panel: Color,
    val ink: Color,
    val ink2: Color,
    val rule: Color,
    val chip: Color,
    val trace: Color,
    val tagBg: Color,
    val tagFg: Color,
    val danger: Color,
    val scrim: Color,
    val signal: Color,
    val onSignal: Color,
)

val EfLight = EfColors(
    paper = Color(0xFFF4F4F2),
    panel = Color(0xFFFFFFFF),
    ink = Color(0xFF161616),
    ink2 = Color(0xFF5C5C5C),
    rule = Color(0xFFD9D9D6),
    chip = Color(0xFFE4E4E0),
    trace = Color(0xFF161616),
    tagBg = Color(0xFF161616),
    tagFg = Color(0xFFFFFFFF),
    danger = Color(0xFFB3261E),
    scrim = Color(0x80161616),
    signal = Color(0xFFFFFA00),
    onSignal = Color(0xFF161616),
)

val EfDark = EfColors(
    paper = Color(0xFF191919),
    panel = Color(0xFF222222),
    ink = Color(0xFFEDEDED),
    ink2 = Color(0xFFA3A3A3),
    rule = Color(0xFF353535),
    chip = Color(0xFF2E2E2E),
    trace = Color(0xFFFFFA00),
    tagBg = Color(0xFF3A3A3A),
    tagFg = Color(0xFFEDEDED),
    danger = Color(0xFFFF8A7A),
    scrim = Color(0xFFA0000000),
    signal = Color(0xFFFFFA00),
    onSignal = Color(0xFF161616),
)

/** 当前是否处于终末地皮肤；组件据此在 Miuix 原生外观和终末地外观之间切换。 */
val LocalEndfield = staticCompositionLocalOf { false }

/** 当前皮肤令牌；默认给浅色一套，避免未开启主题时读取到未初始化值。 */
val LocalEfColors = staticCompositionLocalOf { EfLight }

/** 不随深浅色变化的形式与动效常量（单位 dp / ms）。 */
object Ef {
    val cutSmall: Dp = 7.dp
    val cutButton: Dp = 8.dp
    val cutPanel: Dp = 26.dp
    val pad: Dp = 16.dp
    val hairline: Dp = 1.dp
    val tab: Dp = 5.dp
    val traceBar: Dp = 4.dp
    val touch: Dp = 48.dp

    const val PRESS_MS = 100
    const val WIPE_MS = 280
    const val ROW_MS = 200

    /** 出现：先快后缓；退出 / 擦入 / 快门：先缓后硬。 */
    val Snap = CubicBezierEasing(0.16f, 1f, 0.3f, 1f)
    val Mech = CubicBezierEasing(0.7f, 0f, 0.2f, 1f)

    /** 右下切角的可点击块。 */
    val CutPressable = CutCornerShape(bottomEnd = cutButton)
}
