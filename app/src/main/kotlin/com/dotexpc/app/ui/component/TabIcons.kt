package com.dotexpc.app.ui.component

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.unit.dp
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Favorites
import top.yukonga.miuix.kmp.icon.extended.FavoritesFill

/**
 * 底栏图标：未选中用描边、选中用填充。
 * Miuix 没有 Home / 标签 / 码卡 的填充版本，这里按同一套几何手绘成对图标，
 * 描边统一 1.7（与 Miuix 图标同一视觉重量），填充版本用 EvenOdd 在实心形状上挖出细节。
 */
internal object TabIcons {
    val HomeOutline = icon("TabHomeOutline", stroke = listOf(HOUSE, DOOR_OPEN))
    val HomeFilled = icon("TabHomeFilled", fill = listOf("$HOUSE $DOOR_CLOSED"))
    val TagOutline = icon("TabTagOutline", stroke = listOf(TAG, TAG_HOLE))
    val TagFilled = icon("TabTagFilled", fill = listOf("$TAG $TAG_HOLE"))
    val CodeOutline = icon("TabCodeOutline", stroke = listOf(CODE_CARD, CODE_BARS))
    val CodeFilled = icon("TabCodeFilled", fill = listOf("$CODE_CARD $CODE_HOLES"))
    val SettingsOutline = icon("TabSettingsOutline", stroke = listOf(GEAR, GEAR_HOLE))
    val SettingsFilled = icon("TabSettingsFilled", fill = listOf("$GEAR $GEAR_HOLE"))

    /** 设置页「清除缓存」：垃圾桶，随图标样式切换实心/框线。 */
    val TrashOutline = icon("TabTrashOutline", stroke = listOf(TRASH_HANDLE, TRASH_LID, TRASH_BODY, TRASH_BARS))
    val TrashFilled = icon(
        "TabTrashFilled",
        fill = listOf(TRASH_SOLID, TRASH_SLOT_LEFT, TRASH_SLOT_RIGHT),
    )

    /** 设置页「主题」：半明半暗的圆，两种图标样式下都用同一枚。 */
    val Theme = icon("TabTheme", fill = listOf(THEME_CIRCLE, THEME_HALF))

    /** 设置页「应用内图标样式」直接借 Miuix 成对的星形示意填充/框线。 */
    val StyleOutline = MiuixIcons.Favorites
    val StyleFilled = MiuixIcons.FavoritesFill

    /** 设置页「液态玻璃底栏」：四角括号，不随图标样式变化。 */
    val LiquidGlass = icon("TabLiquidGlass", fill = listOf(GLASS_CORNERS))

    /** 账户分组：拼多多两种图标样式共用一枚；淘宝按应用内图标样式切换填充/框线。 */
    val Pdd = icon("TabPdd", fill = listOf(PDD))
    val TaobaoFilled = icon("TabTaobaoFilled", fill = listOf(TAOBAO_FILLED))
    val TaobaoOutline = icon("TabTaobaoOutline", fill = listOf(TAOBAO_OUTLINE))

    /** 设置页「关于」：信息圆环随图标样式切换；开源许可两种样式共用一枚。 */
    val AboutOutline = icon("TabAboutOutline", fill = listOf(ABOUT_OUTLINE))
    val AboutFilled = icon("TabAboutFilled", fill = listOf(ABOUT_FILLED))
    val License = icon("TabLicense", fill = listOf(LICENSE))
}

private const val STROKE = 1.7f

private const val HOUSE = "M3.4 10.1 L12 3.3 L20.6 10.1 V18.1 A2.9 2.9 0 0 1 17.7 21 H6.3 A2.9 2.9 0 0 1 3.4 18.1 Z"
private const val DOOR_OPEN = "M9.6 21 V15.6 A1.3 1.3 0 0 1 10.9 14.3 H13.1 A1.3 1.3 0 0 1 14.4 15.6 V21"
private const val DOOR_CLOSED = "M9.6 21 V15.6 A1.3 1.3 0 0 1 10.9 14.3 H13.1 A1.3 1.3 0 0 1 14.4 15.6 V21 Z"

private const val TAG = "M3.6 12.5 V5.6 A2 2 0 0 1 5.6 3.6 H12.5 A2 2 0 0 1 13.9 4.2 L20.3 10.6 A2 2 0 0 1 20.3 13.4 L13.4 20.3 A2 2 0 0 1 10.6 20.3 L4.2 13.9 A2 2 0 0 1 3.6 12.5 Z"
private const val TAG_HOLE = "M9.05 7.9 A1.15 1.15 0 1 0 6.75 7.9 A1.15 1.15 0 1 0 9.05 7.9 Z"

private const val CODE_CARD = "M5.6 3.6 H18.4 A2 2 0 0 1 20.4 5.6 V18.4 A2 2 0 0 1 18.4 20.4 H5.6 A2 2 0 0 1 3.6 18.4 V5.6 A2 2 0 0 1 5.6 3.6 Z"
private const val CODE_BARS = "M8 8 V16 M12 8 V16 M16 8 V16"
private const val CODE_HOLES = "M7.45 8 h1.1 v8 h-1.1 Z M11.45 8 h1.1 v8 h-1.1 Z M15.45 8 h1.1 v8 h-1.1 Z"

/** 垃圾桶：框线用三条开口轮廓，实心用同一边界围成的整体再挖两条提手槽。 */
private const val TRASH_HANDLE = "M8.4 5.55 V4.4 A1.6 1.6 0 0 1 10 2.8 H14 A1.6 1.6 0 0 1 15.6 4.4 V5.55"
private const val TRASH_LID = "M3.9 5.55 H20.1"
private const val TRASH_BODY = "M18.7 5.55 L17.8 19.2 A2 2 0 0 1 15.8 21 H8.2 A2 2 0 0 1 6.2 19.2 L5.3 5.55"
private const val TRASH_BARS = "M9.8 9.4 V17.8 M14.2 9.4 V17.8"
private const val TRASH_SOLID =
    "M3.4 6.45 V5.65 A1 1 0 0 1 4.4 4.65 H8.4 V4.15 A1.2 1.2 0 0 1 9.6 2.95 " +
        "H14.4 A1.2 1.2 0 0 1 15.6 4.15 V4.65 H19.6 A1 1 0 0 1 20.6 5.65 V6.45 " +
        "H18.7 L17.8 19.2 A2 2 0 0 1 15.8 21 H8.2 A2 2 0 0 1 6.2 19.2 L5.3 6.45 Z"
private const val TRASH_SLOT_LEFT =
    "M8.95 10.25 A0.85 0.85 0 0 1 10.65 10.25 V16.95 A0.85 0.85 0 0 1 8.95 16.95 Z"
private const val TRASH_SLOT_RIGHT =
    "M13.35 10.25 A0.85 0.85 0 0 1 15.05 10.25 V16.95 A0.85 0.85 0 0 1 13.35 16.95 Z"

/** 齿轮轮廓沿用 Material Icons 的 settings（Apache 2.0），填充与框线共用同一套几何。 */
private const val GEAR =
    "M19.14 12.94c.04-.3.06-.61.06-.94 0-.32-.02-.64-.07-.94l2.03-1.58c.18-.14.23-.41.12-.61" +
        "l-1.92-3.32c-.12-.22-.37-.29-.59-.22l-2.39.96c-.5-.38-1.03-.7-1.62-.94l-.36-2.54" +
        "c-.04-.24-.24-.41-.48-.41h-3.84c-.24 0-.43.17-.47.41l-.36 2.54c-.59.24-1.13.57-1.62.94" +
        "l-2.39-.96c-.22-.08-.47 0-.59.22L2.74 8.87c-.12.21-.08.47.12.61l2.03 1.58" +
        "c-.05.3-.09.63-.09.94s.02.64.07.94l-2.03 1.58c-.18.14-.23.41-.12.61l1.92 3.32" +
        "c.12.22.37.29.59.22l2.39-.96c.5.38 1.03.7 1.62.94l.36 2.54c.05.24.24.41.48.41h3.84" +
        "c.24 0 .44-.17.47-.41l.36-2.54c.59-.24 1.13-.56 1.62-.94l2.39.96c.22.08.47 0 .59-.22" +
        "l1.92-3.32c.12-.22.07-.47-.12-.61l-2.01-1.58Z"
private const val GEAR_HOLE = "M12 15.6c-1.98 0-3.6-1.62-3.6-3.6s1.62-3.6 3.6-3.6 3.6 1.62 3.6 3.6-1.62 3.6-3.6 3.6Z"

/** 主题图标：外圆 r10、内半圆 r8，等比缩自 1024 视图的原始路径。 */
private const val THEME_CIRCLE = "M12 22 A10 10 0 1 0 12 2 A10 10 0 1 0 12 22 Z"
private const val THEME_HALF = "M12 20 V4 A8 8 0 1 1 12 20 Z"

/** 液态玻璃：四个 L 形角括号，边宽 2 单位。 */
private const val GLASS_CORNERS =
    "M3 16 C3 18.8 5.2 21 8 21 H10 V19 H8 C6.3 19 5 17.7 5 16 V14 H3 Z " +
        "M21 8 C21 5.2 18.8 3 16 3 H14 V5 H16 C17.7 5 19 6.3 19 8 V10 H21 Z " +
        "M16 21 C18.8 21 21 18.8 21 16 V14 H19 V16 C19 17.7 17.7 19 16 19 H14 V21 Z " +
        "M8 3 C5.2 3 3 5.2 3 8 V10 H5 V8 C5 6.3 6.3 5 8 5 H10 V3 Z"

/** 拼多多：方形底板 + 斜向镂空。 */
private const val PDD =
    "M20 2 H4 C2.9 2 2 2.9 2 4 V20 C2 21.1 2.9 22 4 22 H20 C21.1 22 22 21.1 22 20 V4 " +
        "C22 2.9 21.1 2 20 2 Z " +
        "M18.59 4 L12 10.59 L5.41 4 Z " +
        "M13.41 12 L16.5 8.91 L19.59 12 L16.5 15.09 Z " +
        "M14.59 16 H9.41 L12 13.41 Z " +
        "M4 5.41 L10.59 12 L4 18.59 Z " +
        "M11.59 20 H5.41 L7.41 18 H13.59 Z " +
        "M20 20 H14.41 L20 14.41 Z " +
        "M20 9.59 L17.91 7.5 L20 5.41 Z"

/** 淘宝：实心提袋。 */
private const val TAOBAO_FILLED =
    "M12 13 A5 5 0 0 1 7 8 H9 A3 3 0 0 0 12 11 A3 3 0 0 0 15 8 H17 A5 5 0 0 1 12 13 Z " +
        "M12 3 A3 3 0 0 1 15 6 H9 A3 3 0 0 1 12 3 Z " +
        "M19 6 H17 A5 5 0 0 0 12 1 A5 5 0 0 0 7 6 H5 C3.89 6 3 6.89 3 8 V20 " +
        "A2 2 0 0 0 5 22 H19 A2 2 0 0 0 21 20 V8 A2 2 0 0 0 19 6 Z"

/** 淘宝：框线提袋。 */
private const val TAOBAO_OUTLINE =
    "M19 6 H17 C17 3.2 14.8 1 12 1 S7 3.2 7 6 H5 C3.9 6 3 6.9 3 8 V20 " +
        "C3 21.1 3.9 22 5 22 H19 C20.1 22 21 21.1 21 20 V8 C21 6.9 20.1 6 19 6 Z " +
        "M12 3 C13.7 3 15 4.3 15 6 H9 C9 4.3 10.3 3 12 3 Z " +
        "M19 20 H5 V8 H19 Z " +
        "M12 12 C10.3 12 9 10.7 9 9 H7 C7 11.8 9.2 14 12 14 S17 11.8 17 9 H15 " +
        "C15 10.7 13.7 12 12 12 Z"

/** 关于：信息圆环与两段竖条。 */
private const val ABOUT_OUTLINE =
    "M11 7 H13 V9 H11 Z " +
        "M11 11 H13 V17 H11 Z " +
        "M12 2 C6.48 2 2 6.48 2 12 S6.48 22 12 22 S22 17.52 22 12 S17.52 2 12 2 Z " +
        "M12 20 C7.59 20 4 16.41 4 12 S7.59 4 12 4 S20 7.59 20 12 S16.41 20 12 20 Z"

private const val ABOUT_FILLED =
    "M12 2 C6.48 2 2 6.48 2 12 S6.48 22 12 22 S22 17.52 22 12 S17.52 2 12 2 Z " +
        "M13 17 H11 V11 H13 Z " +
        "M13 9 H11 V7 H13 Z"

/** 开源许可：法槌。 */
private const val LICENSE =
    "M1 21 H13 V23 H1 Z " +
        "M5.24 8.07 L8.07 5.24 L22.21 19.38 L19.38 22.21 Z " +
        "M12.32 1 L17.98 6.66 L15.15 9.49 L9.49 3.83 Z " +
        "M3.83 9.48 L9.49 15.14 L6.66 17.97 L1 12.31 Z"

private fun icon(
    name: String,
    fill: List<String> = emptyList(),
    stroke: List<String> = emptyList(),
): ImageVector {
    val builder = ImageVector.Builder(
        name = name,
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f,
    )
    if (fill.isNotEmpty()) {
        builder.addPath(
            pathData = PathParser().parsePathString(fill.joinToString(" ")).toNodes(),
            pathFillType = PathFillType.EvenOdd,
            fill = SolidColor(Color.Black),
        )
    }
    if (stroke.isNotEmpty()) {
        builder.addPath(
            pathData = PathParser().parsePathString(stroke.joinToString(" ")).toNodes(),
            stroke = SolidColor(Color.Black),
            strokeLineWidth = STROKE,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round,
        )
    }
    return builder.build()
}
