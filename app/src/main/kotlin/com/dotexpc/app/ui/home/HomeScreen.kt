package com.dotexpc.app.ui.home

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dotexpc.app.data.AppSessions
import com.dotexpc.app.data.AppSettings
import com.dotexpc.app.data.HitokotoStore
import com.dotexpc.app.data.HomeBackground
import com.dotexpc.app.data.PickupStore
import com.dotexpc.app.data.Platform
import com.dotexpc.app.ui.component.AppButton
import com.dotexpc.app.ui.component.AppButtonKind
import com.dotexpc.app.ui.component.AppCard
import com.dotexpc.app.ui.component.AppDialog
import com.dotexpc.app.ui.component.accent
import com.dotexpc.app.ui.theme.EfType
import com.dotexpc.app.ui.theme.LocalEfColors
import com.dotexpc.app.ui.theme.LocalEndfield
import java.util.Calendar
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.text.CharCategory
import kotlinx.coroutines.delay
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.theme.MiuixTheme

/** 主页：顶部一言 + 待取数量，下方两个平台的绑定状态。 */
@Composable
fun HomeScreen(
    sessions: AppSessions,
    settings: AppSettings,
    hitokoto: HitokotoStore,
    pickups: PickupStore,
    onLogin: (Platform) -> Unit,
    onOpenOptions: () -> Unit,
    pddOfficialMode: Boolean,
    modifier: Modifier = Modifier,
) {
    val colors = MiuixTheme.colorScheme
    val endfield = LocalEndfield.current
    val clipboard = LocalClipboardManager.current
    var showQuote by remember { mutableStateOf(false) }
    // 打开主页时拉一次，之后每小时检查一遍；store 内部按天去重，同一天不会重复请求。
    LaunchedEffect(settings.hitokotoEnabled) {
        if (!settings.hitokotoEnabled) return@LaunchedEffect
        while (true) {
            hitokoto.refreshIfNeeded()
            delay(60 * 60 * 1000L)
        }
    }
    val quote = if (settings.hitokotoEnabled) {
        // 原文只保留单行，展示用的换行由排版函数生成，复制和弹窗都拿这一份。
        hitokoto.sentence.replace(WHITESPACE, " ").trim()
    } else {
        ""
    }
    val author = if (settings.hitokotoEnabled) hitokoto.author else ""
    val greeting by produceState(Calendar.getInstance().get(Calendar.HOUR_OF_DAY)) {
        while (true) {
            delay(60_000L)
            value = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        }
    }
    val hero = quote.ifBlank { greetingFor(greeting) }
    val pending = pickups.pendingCount
    val textMeasurer = rememberTextMeasurer()
    val density = LocalDensity.current

    Box(modifier = modifier.fillMaxSize()) {
        when (settings.homeBackground) {
            HomeBackground.Blank -> Unit
            HomeBackground.Dots -> HomeDots(Modifier.fillMaxSize())
            // 等高线要画到页面外才能铺满，这里裁回本页，否则会漏到隔壁取件码页。
            HomeBackground.Contour -> HomeContour(Modifier.fillMaxSize().clipToBounds())
            HomeBackground.Gradient -> HomeGradient()
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding(),
        ) {
            // 文字区块固定在屏幕上部约 20% 处，句子再长也只向下扩展。
            Spacer(Modifier.fillMaxHeight(0.2f))

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 28.dp),
            ) {
                BoxWithConstraints(
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    val availableWidthPx = with(density) { maxWidth.toPx() }
                    val heroStyle = TextStyle(
                        fontFamily = if (endfield) EfType.body else null,
                        fontSize = 34.sp,
                        lineHeight = 45.sp,
                        fontWeight = if (quote.isBlank()) FontWeight.SemiBold else FontWeight.Normal,
                    )
                    val heroText = remember(hero, availableWidthPx, heroStyle, textMeasurer) {
                        formatHero(hero, textMeasurer, heroStyle, availableWidthPx)
                    }
                    Text(
                        text = heroText,
                        style = heroStyle,
                        color = colors.onBackground,
                        modifier = Modifier.clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            enabled = quote.isNotBlank(),
                        ) { showQuote = true },
                    )
                }
                Spacer(Modifier.height(12.dp))
                Text(
                    text = if (pending > 0) "剩余 $pending 件待取" else "暂无待取快递",
                    fontSize = 17.sp,
                    color = colors.onBackground.copy(alpha = 0.72f),
                )
            }

            Spacer(Modifier.weight(1f))

            Column(
                modifier = Modifier.padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Platform.entries.forEach { platform ->
                    val officialApp = pddOfficialMode && platform == Platform.PDD
                    val bound = !officialApp && sessions.get(platform).state.bound
                    AccountStatusCard(
                        platform = platform,
                        bound = bound,
                        officialApp = officialApp,
                        // 已绑定的卡片不再跳转，只有尚未绑定时点击才去登录。
                        onClick = if (bound || officialApp) null else ({ onLogin(platform) }),
                        // 绑定/解绑统一在选项页顶部处理。
                        onManage = onOpenOptions,
                    )
                }
            }

            Spacer(Modifier.height(116.dp))
        }

        AppDialog(
            show = showQuote,
            title = quote,
            onDismissRequest = { showQuote = false },
        ) {
            if (author.isNotBlank()) {
                Text(
                    text = author,
                    fontSize = 15.sp,
                    color = colors.onSurfaceVariantSummary,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(16.dp))
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                AppButton(
                    label = "关闭",
                    onClick = { showQuote = false },
                    modifier = Modifier.weight(1f),
                )
                AppButton(
                    label = "复制",
                    onClick = {
                        clipboard.setText(AnnotatedString(quote))
                        showQuote = false
                    },
                    modifier = Modifier.weight(1f),
                    kind = AppButtonKind.Primary,
                )
            }
        }
    }
}

/** 按时段给出问候语。minSdk 24 没有 java.time，用 Calendar 取小时即可。 */
private fun greetingFor(hour: Int): String = when (hour) {
    in 5..8 -> "早上好"
    in 9..11 -> "上午好"
    in 12..13 -> "中午好"
    in 14..17 -> "下午好"
    in 18..22 -> "晚上好"
    else -> "夜深了"
}

private val WHITESPACE = Regex("\\s+")

/** 单行最多 13 个字，标点也算一个；页面排版与折行都以此为上界。 */
private const val MAX_LINE_CHARS = 13

/** 可在其后断行的标点：句读和收尾类。开引号、开括号不在内，连续的「……」「！？」要等整段结束。 */
private fun isBreakPunctuation(char: Char): Boolean = when (char.category) {
    CharCategory.OTHER_PUNCTUATION,
    CharCategory.DASH_PUNCTUATION,
    CharCategory.END_PUNCTUATION,
    CharCategory.FINAL_QUOTE_PUNCTUATION -> true
    else -> false
}

private fun breaksAfter(text: String, index: Int): Boolean =
    isBreakPunctuation(text[index]) &&
        (index + 1 == text.length || !isBreakPunctuation(text[index + 1]))

/**
 * 主页大字排版：第一个标点能落在第一行时，按标点断行（标点留在上一行）；
 * 若第一个标点本身就要落到第二行，则不做任何强制断行，交给自然折行。
 * 断行后任一侧只剩一两个字时不换，避免出现「孤行」；每行最多 [MAX_LINE_CHARS] 个字，
 * 仍需折行时按实际测量宽度处理，尾行同样不留一两个字。
 * 只在展示文本上生效，弹窗和复制仍使用原始句子。
 */
private fun formatHero(
    text: String,
    measurer: TextMeasurer,
    style: TextStyle,
    maxWidthPx: Float,
): String {
    if (text.isEmpty() || maxWidthPx <= 0f) return text

    val firstBreak = text.indices.firstOrNull { breaksAfter(text, it) } ?: -1
    val firstBreakOnFirstLine = firstBreak >= 0 &&
        firstBreak + 1 <= MAX_LINE_CHARS &&
        measuredWidth(text.substring(0, firstBreak + 1), measurer, style) <= maxWidthPx

    val lines = if (!firstBreakOnFirstLine) {
        listOf(text)
    } else {
        val clauses = mutableListOf<String>()
        var start = 0
        text.indices.forEach { index ->
            if (breaksAfter(text, index)) {
                clauses += text.substring(start, index + 1)
                start = index + 1
            }
        }
        if (start < text.length) clauses += text.substring(start)

        val result = mutableListOf<String>()
        var current = ""
        clauses.forEach { clause ->
            if (current.isNotEmpty() && visibleLength(current) > 2 && visibleLength(clause) > 2) {
                result += current
                current = ""
            }
            current += clause
        }
        if (current.isNotEmpty()) result += current
        result
    }

    return lines
        .flatMap { wrapLine(it, measurer, style, maxWidthPx) }
        .joinToString("\n")
}

private fun visibleLength(text: String): Int = text.count { !it.isWhitespace() }

private fun wrapLine(
    line: String,
    measurer: TextMeasurer,
    style: TextStyle,
    maxWidthPx: Float,
): List<String> {
    if (line.length <= MAX_LINE_CHARS && measuredWidth(line, measurer, style) <= maxWidthPx) {
        return listOf(line)
    }

    val result = mutableListOf<String>()
    var start = 0
    while (start < line.length) {
        var low = 1
        var high = minOf(line.length - start, MAX_LINE_CHARS)
        var fit = 1
        while (low <= high) {
            val mid = (low + high) / 2
            if (measuredWidth(line.substring(start, start + mid), measurer, style) <= maxWidthPx) {
                fit = mid
                low = mid + 1
            } else {
                high = mid - 1
            }
        }
        var end = start + fit
        val rest = line.length - end
        // 尾行只有一两个字时，从上一行借字，让它至少凑到三个字。
        if (rest in 1..2 && end - (3 - rest) > start) end -= 3 - rest
        result += line.substring(start, end)
        start = end
    }
    return result
}

private fun measuredWidth(text: String, measurer: TextMeasurer, style: TextStyle): Float =
    measurer.measure(
        text = AnnotatedString(text),
        style = style,
        softWrap = false,
    ).size.width.toFloat()

/** 顶部静态渐变：主色极淡地铺在顶部，向下淡出，不做任何动画。终末地改用墨色，浓度更低。 */
@Composable
private fun HomeGradient(modifier: Modifier = Modifier) {
    val colors = MiuixTheme.colorScheme
    val dark = colors.background.luminance() < 0.5f
    // 终末地主色就是墨色，按同样的方式铺一层，只是压到平面皮肤能接受的程度。
    val alpha = when {
        LocalEndfield.current -> if (dark) 0.08f else 0.06f
        dark -> 0.16f
        else -> 0.18f
    }
    val top = colors.primary.copy(alpha = alpha)
    Box(
        modifier = modifier
            .fillMaxWidth()
            .fillMaxHeight(0.46f)
            .background(
                Brush.verticalGradient(
                    listOf(top, colors.primary.copy(alpha = 0f)),
                ),
            ),
    )
}

/**
 * 文字区域向外辐射的点阵：以一言所在位置为圆心，点阵的浓度随距离平方衰减，
 * 越靠外越淡直至消失。呼吸只改透明度，不改变图案本身。
 *
 * 画布从屏幕最顶端开始铺（含状态栏区域），所以顶部不会被裁成一条直线。
 */
@Composable
private fun HomeDots(modifier: Modifier = Modifier) {
    val colors = MiuixTheme.colorScheme
    val dotColor = colors.onSurface
    // 深色背景上白点比白底黑点更容易显脏，压得更淡一些；整体再压到文字之下。
    val maxAlpha = if (colors.background.luminance() < 0.5f) 0.11f else 0.13f
    val statusBarTop = WindowInsets.statusBars.getTop(LocalDensity.current).toFloat()
    val breathe by rememberInfiniteTransition(label = "homeDots").animateFloat(
        initialValue = 0.8f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 9000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "homeDotsAlpha",
    )

    Canvas(modifier = modifier) {
        val spacing = 17.dp.toPx()
        val baseRadius = 1.dp.toPx()
        // 文字区在去掉状态栏后的 Column 里下移 20%，换算回整屏坐标。
        val origin = Offset(
            x = 28.dp.toPx() + 76.dp.toPx(),
            y = statusBarTop + (size.height - statusBarTop) * 0.2f + 30.dp.toPx(),
        )
        val falloff = size.minDimension * 1.01f

        var row = 0
        var y = spacing / 2f
        while (y < size.height) {
            var x = spacing / 2f + if (row % 2 == 0) 0f else spacing / 2f
            while (x < size.width + spacing) {
                val dx = x - origin.x
                val dy = y - origin.y
                val t = 1f - sqrt(dx * dx + dy * dy) / falloff
                if (t > 0f) {
                    drawCircle(
                        color = dotColor.copy(alpha = maxAlpha * t * t * breathe),
                        radius = baseRadius * (0.75f + 0.25f * t),
                        center = Offset(x, y),
                    )
                }
                x += spacing
            }
            y += spacing
            row++
        }
    }
}

/**
 * 等高线：以一言所在位置为中心的一组闭合细线，半径按固定谐波起伏，
 * 像一张没有标注的地形图。各环共用同一轮廓只按半径缩放，所以永远不会相交；
 * 只画线、不填充、不做动画，每五条作为计曲线略重一些。
 */
@Composable
private fun HomeContour(modifier: Modifier = Modifier) {
    val colors = MiuixTheme.colorScheme
    val endfield = LocalEndfield.current
    val lineColor = if (endfield) LocalEfColors.current.rule else colors.onSurface
    val lineAlpha = if (endfield) {
        1f
    } else {
        // 线条比点阵更连续，透明度再压一档，避免抢过正文。
        if (colors.background.luminance() < 0.5f) 0.09f else 0.11f
    }
    val fadeStrength = if (endfield) 0.35f else 0.55f
    val statusBarTop = WindowInsets.statusBars.getTop(LocalDensity.current).toFloat()

    Canvas(modifier = modifier) {
        val stroke = 1.dp.toPx()
        val spacing = 30.dp.toPx()
        val origin = Offset(
            x = 28.dp.toPx() + 76.dp.toPx(),
            y = statusBarTop + (size.height - statusBarTop) * 0.2f + 30.dp.toPx(),
        )
        val reachX = maxOf(origin.x, size.width - origin.x)
        val reachY = maxOf(origin.y, size.height - origin.y)
        val rings = (sqrt(reachX * reachX + reachY * reachY) / spacing).toInt() + 1
        val segments = 240
        val path = Path()

        for (index in 1..rings) {
            val base = spacing * index
            path.reset()
            for (segment in 0..segments) {
                val angle = segment * (2f * PI.toFloat() / segments)
                val wobble = 1f +
                    0.070f * sin(angle * 3f + 0.6f) +
                    0.040f * sin(angle * 5f - 1.1f) +
                    0.020f * sin(angle * 9f + 2.3f)
                val radius = base * wobble
                val x = origin.x + cos(angle) * radius
                val y = origin.y + sin(angle) * radius
                if (segment == 0) path.moveTo(x, y) else path.lineTo(x, y)
            }
            path.close()
            val indexLine = index % 5 == 0
            // 越靠外的环越淡，边缘只留一点痕迹，不把整屏铺满。
            val fade = 1f - fadeStrength * index / (rings + 1)
            drawPath(
                path = path,
                color = lineColor.copy(alpha = lineAlpha * fade * if (indexLine) 1f else 0.7f),
                style = Stroke(width = stroke),
            )
        }
    }
}

@Composable
private fun AccountStatusCard(
    platform: Platform,
    bound: Boolean,
    officialApp: Boolean,
    onClick: (() -> Unit)?,
    onManage: () -> Unit,
) {
    val colors = MiuixTheme.colorScheme
    val endfield = LocalEndfield.current

    AppCard(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        cornerRadius = 18.dp,
        insideMargin = PaddingValues(horizontal = 18.dp, vertical = 14.dp),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(if (endfield) 7.dp else 8.dp)
                    .then(if (endfield) Modifier else Modifier.clip(CircleShape))
                    .background(if (endfield) LocalEfColors.current.signal else platform.accent),
            )
            Text(
                text = platform.title,
                style = MiuixTheme.textStyles.main,
                modifier = Modifier.weight(1f),
            )
            Text(
                text = when {
                    officialApp -> "无需登录"
                    bound -> "解除绑定"
                    else -> "绑定"
                },
                style = MiuixTheme.textStyles.footnote1,
                color = when {
                    officialApp -> colors.onSurfaceVariantSummary
                    bound -> colors.error
                    else -> colors.primary
                },
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .then(if (officialApp) Modifier else Modifier.clickable { onManage() })
                    .padding(horizontal = 6.dp, vertical = 6.dp),
            )
        }
    }
}
