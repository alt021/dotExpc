package com.dotexpc.app.ui.theme

import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.dotexpc.app.R
import top.yukonga.miuix.kmp.theme.TextStyles
import top.yukonga.miuix.kmp.theme.defaultTextStyles

/**
 * 终末地字体：拉丁字母和数字用 Saira 可变字体（宽体、几何、大写），
 * 中文没有对应字形，自动落到系统中文字体，正好是 skill 要的「Saira + 中文黑体」。
 *
 * 可变轴只在 API 26+ 生效；更低版本会退化成默认字重，不影响排版。
 */
@OptIn(ExperimentalTextApi::class)
private fun saira(weight: Int, width: Float = 100f): FontFamily = FontFamily(
    Font(
        resId = R.font.saira,
        weight = FontWeight(weight),
        variationSettings = FontVariation.Settings(
            FontVariation.weight(weight),
            FontVariation.width(width),
        ),
    ),
)

object EfType {
    /** 正文：拉丁不拉宽，中文同系统黑体。 */
    val body: FontFamily = saira(400)

    /** 按钮与小标题：拉宽到 112%，接近 skill 的界面字宽。 */
    val label: FontFamily = saira(700, 112f)

    /** 英文眉标：最宽的一档，全大写后接近 skill 的字标。 */
    val eyebrow: FontFamily = saira(600, 125f)

    val Eyebrow = TextStyle(
        fontFamily = eyebrow,
        fontSize = 11.sp,
        fontWeight = FontWeight.SemiBold,
        letterSpacing = 1.54.sp,
    )

    val Label = TextStyle(
        fontFamily = label,
        fontSize = 15.sp,
        fontWeight = FontWeight.Bold,
    )

    /** 编号、日期、计数：等宽数字，竖排字段不会跳动。 */
    val Field = TextStyle(
        fontFamily = saira(500, 112f),
        fontSize = 13.sp,
        fontWeight = FontWeight.Medium,
        fontFeatureSettings = "tnum",
    )

    val Title = TextStyle(
        fontFamily = label,
        fontSize = 20.sp,
        lineHeight = 26.sp,
        fontWeight = FontWeight.Bold,
    )
}

/** 把 Miuix 的全部文字角色换成 Saira，字号沿用原值，避免布局位移。 */
internal fun endfieldTextStyles(): TextStyles {
    val base = defaultTextStyles()
    val tnum = "tnum"
    return TextStyles(
        main = base.main.copy(fontFamily = EfType.body, lineHeight = 26.sp, fontFeatureSettings = tnum),
        paragraph = base.paragraph.copy(fontFamily = EfType.body, lineHeight = 31.sp),
        body1 = base.body1.copy(fontFamily = EfType.body, fontFeatureSettings = tnum),
        body2 = base.body2.copy(fontFamily = EfType.body, fontFeatureSettings = tnum),
        button = base.button.copy(fontFamily = EfType.label, fontSize = 15.sp),
        footnote1 = base.footnote1.copy(fontFamily = EfType.body, fontFeatureSettings = tnum),
        footnote2 = base.footnote2.copy(fontFamily = EfType.body, fontFeatureSettings = tnum),
        headline1 = base.headline1.copy(fontFamily = EfType.label),
        headline2 = base.headline2.copy(fontFamily = EfType.label),
        subtitle = base.subtitle.copy(fontFamily = EfType.label),
        title1 = base.title1.copy(fontFamily = EfType.label),
        title2 = base.title2.copy(fontFamily = EfType.label),
        title3 = base.title3.copy(fontFamily = EfType.label),
        title4 = base.title4.copy(fontFamily = EfType.label),
    )
}
