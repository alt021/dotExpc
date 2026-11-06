package com.dotexpc.app.ui.component

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.BlurOn
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ConfirmationNumber
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.FormatQuote
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.ConfirmationNumber
import androidx.compose.material.icons.outlined.DarkMode
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.FormatQuote
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.QrCode
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Star
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.unit.dp

/**
 * 应用图标统一取自 Material Design；能成对的地方都给了填充与框线两套。
 * 淘宝、拼多多没有 Material 对应图形，保留手绘品牌图形。
 */
internal object AppIcons {
    // 底栏：未选中用框线、选中用填充。
    val HomeFilled = Icons.Filled.Home
    val HomeOutline = Icons.Outlined.Home
    val PickupFilled = Icons.Filled.ConfirmationNumber
    val PickupOutline = Icons.Outlined.ConfirmationNumber
    val IdentityFilled = Icons.Filled.QrCode
    val IdentityOutline = Icons.Outlined.QrCode
    val SettingsFilled = Icons.Filled.Settings
    val SettingsOutline = Icons.Outlined.Settings

    // 设置页：主题、液态玻璃、开源许可是单枚图标，不随样式变化。
    val Theme = Icons.Outlined.DarkMode
    val StyleFilled = Icons.Filled.Star
    val StyleOutline = Icons.Outlined.Star
    val BackgroundFilled = Icons.Filled.Image
    val BackgroundOutline = Icons.Outlined.Image
    val QuoteFilled = Icons.Filled.FormatQuote
    val QuoteOutline = Icons.Outlined.FormatQuote
    val LiquidGlass = Icons.Filled.BlurOn
    val TrashFilled = Icons.Filled.Delete
    val TrashOutline = Icons.Outlined.Delete
    val AboutFilled = Icons.Filled.Info
    val AboutOutline = Icons.Outlined.Info
    val License = Icons.Outlined.Description

    // 各页动作图标。
    val Add = Icons.Filled.Add
    val Back = Icons.AutoMirrored.Filled.ArrowBack
    val ChevronForward = Icons.Filled.ChevronRight
    val Refresh = Icons.Filled.Refresh
    val Edit = Icons.Filled.Edit
    val Undo = Icons.AutoMirrored.Filled.Undo
    val Check = Icons.Filled.Check
    val Delete = Icons.Filled.Delete
    val Close = Icons.Filled.Close
    val ExpandLess = Icons.Filled.ExpandLess
    val ExpandMore = Icons.Filled.ExpandMore
    val FullScreen = Icons.Filled.Fullscreen

    /** 账户分组：拼多多两种图标样式共用一枚；淘宝按应用内图标样式切换填充/框线。 */
    val Pdd = icon("TabPdd", fill = listOf(PDD))
    val TaobaoFilled = icon("TabTaobaoFilled", fill = listOf(TAOBAO_FILLED))
    val TaobaoOutline = icon("TabTaobaoOutline", fill = listOf(TAOBAO_OUTLINE))
}

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

private fun icon(name: String, fill: List<String>): ImageVector =
    ImageVector.Builder(
        name = name,
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f,
    )
        .addPath(
            pathData = PathParser().parsePathString(fill.joinToString(" ")).toNodes(),
            pathFillType = PathFillType.EvenOdd,
            fill = SolidColor(Color.Black),
        )
        .build()
