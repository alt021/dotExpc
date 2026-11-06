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
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dotexpc.app.data.AppSessions
import com.dotexpc.app.data.PickupStore
import com.dotexpc.app.data.Platform
import com.dotexpc.app.ui.component.accent
import java.util.Calendar
import kotlin.math.sqrt
import kotlinx.coroutines.delay
import top.yukonga.miuix.kmp.basic.Button
import top.yukonga.miuix.kmp.basic.ButtonDefaults
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TextButton
import top.yukonga.miuix.kmp.overlay.OverlayDialog
import top.yukonga.miuix.kmp.theme.MiuixTheme

/** 主页：手动导入的取件码数量 + 两个平台的绑定状态。 */
@Composable
fun HomeScreen(
    sessions: AppSessions,
    pickups: PickupStore,
    onLogin: (Platform) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = MiuixTheme.colorScheme
    var unbindTarget by remember { mutableStateOf<Platform?>(null) }
    var showUnbind by remember { mutableStateOf(false) }
    val breathe by rememberInfiniteTransition(label = "homeDots").animateFloat(
        initialValue = 0.8f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 9000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "homeDotsAlpha",
    )
    val hour by produceState(Calendar.getInstance().get(Calendar.HOUR_OF_DAY)) {
        while (true) {
            delay(60_000L)
            value = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        }
    }
    val pending = pickups.pendingCount

    Box(modifier = modifier.fillMaxSize()) {
        // 点阵铺满整屏（含状态栏区域），顶边落在屏幕外，避免在标题上方出现一道生硬截断。
        HomeDots(
            progress = breathe,
            modifier = Modifier.fillMaxSize(),
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding(),
        ) {
            // 标题区块整体下移约 10% 屏高。
            Spacer(Modifier.fillMaxHeight(0.1f))

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(start = 24.dp, end = 24.dp, top = 40.dp),
            ) {
                Text(
                    text = greetingFor(hour),
                    fontSize = 38.sp,
                    fontWeight = FontWeight.SemiBold,
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    text = if (pending > 0) "当前有 $pending 件快递待取" else "目前没有快递待取",
                    style = MiuixTheme.textStyles.title4,
                    color = colors.onSurfaceVariantSummary,
                )
            }

            Column(
                modifier = Modifier.padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Platform.entries.forEach { platform ->
                    val bound = sessions.get(platform).state.bound
                    AccountStatusCard(
                        platform = platform,
                        bound = bound,
                        // 已绑定的卡片不再跳转，只有尚未绑定时点击才去登录。
                        onClick = if (bound) null else ({ onLogin(platform) }),
                        onBind = { onLogin(platform) },
                        onUnbind = {
                            unbindTarget = platform
                            showUnbind = true
                        },
                    )
                }
            }

            Spacer(Modifier.height(116.dp))
        }
    }

    OverlayDialog(
        show = showUnbind,
        title = "解除${unbindTarget?.title ?: ""}绑定？",
        summary = "将清除本机保存的登录状态，之后需要重新登录官方页面才能显示身份码。",
        onDismissRequest = { showUnbind = false },
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            TextButton(
                text = "取消",
                onClick = { showUnbind = false },
                modifier = Modifier.weight(1f),
            )
            Button(
                onClick = {
                    unbindTarget?.let { sessions.get(it).unbind() }
                    showUnbind = false
                },
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(
                    color = colors.error,
                    contentColor = Color.White,
                ),
            ) {
                Text(text = "解除绑定")
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

/**
 * 文字区域向外辐射的点阵：以问候语所在位置为圆心，点阵的浓度随距离平方衰减，
 * 越靠外越淡直至消失。`progress` 只做极缓慢的呼吸，不改变图案本身。
 *
 * 画布从屏幕最顶端开始铺（比标题区多出约 10% 屏高），所以顶部不会被裁成一条直线。
 */
@Composable
private fun HomeDots(
    progress: Float,
    modifier: Modifier = Modifier,
) {
    val colors = MiuixTheme.colorScheme
    val dotColor = colors.onSurface
    // 深色背景上白点比白底黑点更容易显脏，压得更淡一些；整体再压到文字之下。
    val maxAlpha = if (colors.background.luminance() < 0.5f) 0.11f else 0.13f
    val statusBarTop = WindowInsets.statusBars.getTop(LocalDensity.current).toFloat()

    Canvas(modifier = modifier) {
        val spacing = 17.dp.toPx()
        val baseRadius = 1.dp.toPx()
        // 标题区在去掉状态栏后的 Column 里下移 10% 再加 40dp，换算回整屏坐标。
        val origin = Offset(
            x = 24.dp.toPx() + 76.dp.toPx(),
            y = statusBarTop + (size.height - statusBarTop) * 0.1f + 40.dp.toPx() + 30.dp.toPx(),
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
                        color = dotColor.copy(alpha = maxAlpha * t * t * progress),
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

@Composable
private fun AccountStatusCard(
    platform: Platform,
    bound: Boolean,
    onClick: (() -> Unit)?,
    onBind: () -> Unit,
    onUnbind: () -> Unit,
) {
    val colors = MiuixTheme.colorScheme

    Card(
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
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(platform.accent),
            )
            Text(
                text = platform.title,
                style = MiuixTheme.textStyles.main,
                modifier = Modifier.weight(1f),
            )
            Text(
                text = if (bound) "解除绑定" else "绑定",
                style = MiuixTheme.textStyles.footnote1,
                color = if (bound) colors.error else colors.primary,
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .clickable { if (bound) onUnbind() else onBind() }
                    .padding(horizontal = 6.dp, vertical = 6.dp),
            )
        }
    }
}

