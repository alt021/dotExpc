package com.dotexpc.app.ui.about

import android.os.SystemClock
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.TextStyle
import com.dotexpc.app.R
import com.dotexpc.app.web.openExternalLink
import com.kyant.backdrop.backdrops.LayerBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.lens
import com.kyant.backdrop.effects.vibrancy
import com.kyant.backdrop.highlight.Highlight
import com.kyant.backdrop.isRenderEffectSupported
import com.kyant.backdrop.shadow.Shadow as GlassShadow
import kotlinx.coroutines.launch
import com.dotexpc.app.ui.component.AppButton
import com.dotexpc.app.ui.component.AppButtonKind
import com.dotexpc.app.ui.component.AppIcons
import com.dotexpc.app.ui.theme.EfType
import com.dotexpc.app.ui.theme.LocalEfColors
import com.dotexpc.app.ui.theme.LocalEndfield
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.theme.MiuixTheme

private const val RELEASES_URL = "https://github.com/alt021/dotExpc/releases"

/** 关于此应用：动态渐变底，图标、名称与版本居中，底部是检查更新与署名。 */
@Composable
fun AboutScreen(
    onClose: () -> Unit,
    onOpenDeveloper: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val versionName = remember(context) {
        context.packageManager.getPackageInfo(context.packageName, 0).versionName ?: ""
    }
    val backdrop = rememberLayerBackdrop()
    val blurEnabled = remember { isRenderEffectSupported() }
    val endfield = LocalEndfield.current

    // 连点署名 5 次进开发者选项；间隔超过 2 秒重新计数，过程中不给任何反馈。
    var tapCount by remember { mutableIntStateOf(0) }
    var lastTapAt by remember { mutableLongStateOf(0L) }
    val signatureInteraction = remember { MutableInteractionSource() }

    DynamicGradientBackground(
        modifier = modifier.fillMaxSize(),
        backdrop = backdrop,
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier.align(Alignment.Center),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                AppIcon()
                Text(
                    text = "dotExpc",
                    modifier = Modifier.padding(top = 14.dp, bottom = 4.dp),
                    color = MiuixTheme.colorScheme.onBackground,
                    fontFamily = if (endfield) EfType.label else null,
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = "v$versionName",
                    color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                    style = if (endfield) EfType.Field.copy(fontSize = 14.sp) else TextStyle(fontSize = 14.sp),
                )
            }

            Column(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .navigationBarsPadding()
                    .padding(bottom = 28.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                if (endfield) {
                    AppButton(
                        label = "检查更新",
                        onClick = { openExternalLink(context, RELEASES_URL) },
                        kind = AppButtonKind.Primary,
                    )
                } else {
                    LiquidButton(
                        backdrop = backdrop,
                        blurEnabled = blurEnabled,
                        onClick = { openExternalLink(context, RELEASES_URL) },
                    ) {
                        Text(
                            text = "检查更新",
                            color = MiuixTheme.colorScheme.onBackground,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Medium,
                        )
                    }
                }
                Text(
                    text = "Produced by AmeXE2",
                    modifier = Modifier
                        .padding(top = 18.dp)
                        .clickable(
                            interactionSource = signatureInteraction,
                            indication = null,
                        ) {
                            val now = SystemClock.uptimeMillis()
                            tapCount = if (now - lastTapAt < 2000L) tapCount + 1 else 1
                            lastTapAt = now
                            if (tapCount >= 5) {
                                tapCount = 0
                                onOpenDeveloper()
                            }
                        },
                    color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                    style = if (endfield) EfType.Field.copy(fontSize = 14.sp) else TextStyle(fontSize = 16.sp),
                )
            }

            IconButton(
                onClick = onClose,
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .statusBarsPadding()
                    .padding(12.dp),
            ) {
                Icon(
                    imageVector = AppIcons.Back,
                    contentDescription = "返回",
                    tint = MiuixTheme.colorScheme.onBackground,
                )
            }
        }
    }
}

/**
 * 关于页图标用主题色现画，而不是读启动器位图：默认主题跟随主色，
 * 终末地用信号黄配黑盒子，与当前外观始终一致。
 */
@Composable
private fun AppIcon(modifier: Modifier = Modifier) {
    val endfield = LocalEndfield.current
    val ef = LocalEfColors.current
    val colors = MiuixTheme.colorScheme
    Box(
        modifier = modifier
            .size(88.dp)
            .background(
                color = if (endfield) ef.signal else colors.primary,
                shape = RoundedCornerShape(20.dp),
            ),
        contentAlignment = Alignment.Center,
    ) {
        Image(
            painter = painterResource(R.drawable.ic_launcher_foreground),
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            colorFilter = ColorFilter.tint(if (endfield) ef.onSignal else colors.onPrimary),
        )
    }
}

/**
 * 液态玻璃按钮：胶囊形，按下出现折射、高光和内阴影，水平拖动时朝拖动方向拉伸、
 * 垂直方向压扁，松手后回弹。
 */
@Composable
private fun LiquidButton(
    backdrop: LayerBackdrop,
    blurEnabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val isInDark = MiuixTheme.colorScheme.background.luminance() < 0.5f
    val tint = if (isInDark) Color.White.copy(alpha = 0.1f) else Color.White.copy(alpha = 0.42f)

    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val pressProgress = remember { Animatable(0f) }
    val stretch = remember { Animatable(0f) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(isPressed) {
        pressProgress.animateTo(
            targetValue = if (isPressed) 1f else 0f,
            animationSpec = spring(dampingRatio = 1f, stiffness = 900f, visibilityThreshold = 0.001f),
        )
    }

    Box(
        modifier = modifier
            .height(44.dp)
            .graphicsLayer {
                val progress = pressProgress.value
                val drag = stretch.value
                // 按下轻微鼓起；拖动时沿拖动方向拉长、另一个方向压扁。
                scaleX = 1f + 0.03f * progress + drag * 0.35f
                scaleY = 1f + 0.03f * progress - drag * 0.2f
            }
            .then(
                if (blurEnabled) {
                    Modifier.drawBackdrop(
                        backdrop = backdrop,
                        shape = { CircleShape },
                        effects = {
                            vibrancy()
                            blur(20.dp.toPx())
                            val progress = pressProgress.value
                            lens(
                                refractionHeight = 8.dp.toPx() * progress,
                                refractionAmount = 12.dp.toPx() * progress,
                                chromaticAberration = true,
                            )
                        },
                        highlight = { Highlight.Default.copy(alpha = pressProgress.value) },
                        shadow = { GlassShadow(alpha = pressProgress.value) },
                        onDrawSurface = {
                            drawRect(tint)
                            drawRect(
                                color = if (isInDark) Color.White else Color.Black,
                                alpha = 0.05f * pressProgress.value,
                            )
                        },
                    )
                } else {
                    Modifier.background(tint, CircleShape)
                },
            )
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick,
            )
            // Initial 阶段旁听指针，不消费事件，不影响点击和返回手势。
            .pointerInput(Unit) {
                val width = size.width.toFloat()
                awaitPointerEventScope {
                    var startX = Float.NaN
                    while (true) {
                        val change = awaitPointerEvent(PointerEventPass.Initial).changes.firstOrNull()
                            ?: continue
                        if (change.pressed) {
                            if (startX.isNaN()) startX = change.position.x
                            val offset = (change.position.x - startX) / width
                            val target = offset.coerceIn(-0.25f, 0.25f)
                            scope.launch {
                                stretch.animateTo(target, spring(0.55f, 1200f, 0.001f))
                            }
                        } else if (!startX.isNaN()) {
                            startX = Float.NaN
                            scope.launch {
                                stretch.animateTo(0f, spring(0.5f, 600f, 0.001f))
                            }
                        }
                    }
                }
            }
            .padding(horizontal = 28.dp),
        contentAlignment = Alignment.Center,
    ) {
        content()
    }
}
