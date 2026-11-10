package com.dotexpc.app.ui.component

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CutCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.state.ToggleableState
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dotexpc.app.ui.theme.Ef
import com.dotexpc.app.ui.theme.EfType
import com.dotexpc.app.ui.theme.LocalEfColors
import com.dotexpc.app.ui.theme.LocalEndfield
import top.yukonga.miuix.kmp.basic.Button
import top.yukonga.miuix.kmp.basic.ButtonDefaults
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.CardDefaults
import top.yukonga.miuix.kmp.basic.Checkbox
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.ScrollBehavior
import top.yukonga.miuix.kmp.basic.SmallTitle
import top.yukonga.miuix.kmp.basic.Switch
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TextButton
import top.yukonga.miuix.kmp.basic.TopAppBar
import top.yukonga.miuix.kmp.overlay.OverlayBottomSheet
import top.yukonga.miuix.kmp.overlay.OverlayDialog
import top.yukonga.miuix.kmp.theme.MiuixTheme

/** 按钮语义：默认（安静）、主操作（信号黄）、删除（红，只给删除）。 */
enum class AppButtonKind { Default, Primary, Danger }

/**
 * 应用按钮：默认主题走 Miuix 原样，终末地皮肤换成右下切角的方块，
 * 按下时信号黄从左擦入（主操作擦入墨色），没有涟漪。
 */
@Composable
fun AppButton(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    kind: AppButtonKind = AppButtonKind.Default,
) {
    if (!LocalEndfield.current) {
        Button(
            onClick = onClick,
            modifier = modifier,
            enabled = enabled,
            colors = when (kind) {
                AppButtonKind.Default -> ButtonDefaults.buttonColors()
                AppButtonKind.Primary -> ButtonDefaults.buttonColorsPrimary()
                AppButtonKind.Danger -> ButtonDefaults.buttonColors(
                    color = MiuixTheme.colorScheme.error,
                    contentColor = MiuixTheme.colorScheme.onError,
                )
            },
        ) {
            Text(label)
        }
        return
    }

    val c = LocalEfColors.current
    val source = remember { MutableInteractionSource() }
    val pressed by source.collectIsPressedAsState()
    val wipe by animateFloatAsState(
        targetValue = if (pressed) 1f else 0f,
        animationSpec = tween(
            durationMillis = if (pressed) Ef.PRESS_MS else Ef.WIPE_MS,
            easing = Ef.Mech,
        ),
        label = "efWipe",
    )
    val rest = when {
        !enabled -> c.chip.copy(alpha = 0.6f)
        kind == AppButtonKind.Primary -> c.signal
        else -> c.chip
    }
    val wipeColor = when (kind) {
        AppButtonKind.Primary -> c.ink.copy(alpha = 0.12f)
        AppButtonKind.Danger -> c.danger
        AppButtonKind.Default -> c.signal
    }
    val foreground = when {
        !enabled -> c.ink2
        kind == AppButtonKind.Danger -> if (pressed) c.panel else c.danger
        pressed || kind == AppButtonKind.Primary -> c.onSignal
        else -> c.ink
    }
    val haptics = LocalHapticFeedback.current

    androidx.compose.foundation.layout.Box(
        modifier = modifier
            .heightIn(min = Ef.touch)
            .clip(CutCornerShape(bottomEnd = Ef.cutButton))
            .background(rest)
            .drawBehind {
                if (wipe > 0f) {
                    clipRect {
                        drawRect(wipeColor, size = size.copy(width = size.width * wipe))
                    }
                }
            }
            .clickable(
                interactionSource = source,
                indication = null,
                enabled = enabled,
                role = Role.Button,
                onClick = {
                    haptics.performHapticFeedback(HapticFeedbackType.KeyboardTap)
                    onClick()
                },
            )
            .padding(horizontal = 20.dp, vertical = 12.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(text = label, style = EfType.Label, color = foreground)
    }
}

/**
 * 应用内联按钮：默认主题原样转发 Miuix TextButton（含自定义度量和配色），
 * 终末地缩成小号切角块：默认细线描边、主操作信号黄实心、删除红字。
 */
@Composable
fun AppTextButton(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    kind: AppButtonKind = AppButtonKind.Default,
    minWidth: Dp = ButtonDefaults.MinWidth,
    minHeight: Dp = ButtonDefaults.MinHeight,
    cornerRadius: Dp = ButtonDefaults.CornerRadius,
    insideMargin: PaddingValues = ButtonDefaults.InsideMargin,
    textStyle: TextStyle? = null,
) {
    if (!LocalEndfield.current) {
        val scheme = MiuixTheme.colorScheme
        TextButton(
            text = label,
            onClick = onClick,
            modifier = modifier,
            enabled = enabled,
            cornerRadius = cornerRadius,
            minWidth = minWidth,
            minHeight = minHeight,
            colors = when (kind) {
                AppButtonKind.Default -> ButtonDefaults.textButtonColors()
                AppButtonKind.Primary -> ButtonDefaults.textButtonColorsPrimary()
                AppButtonKind.Danger -> ButtonDefaults.textButtonColors(
                    color = scheme.error,
                    textColor = scheme.onError,
                )
            },
            insideMargin = insideMargin,
            textStyle = textStyle,
        )
        return
    }

    val c = LocalEfColors.current
    val source = remember { MutableInteractionSource() }
    val pressed by source.collectIsPressedAsState()
    val wipe by animateFloatAsState(
        targetValue = if (pressed) 1f else 0f,
        animationSpec = tween(
            durationMillis = if (pressed) Ef.PRESS_MS else Ef.WIPE_MS,
            easing = Ef.Mech,
        ),
        label = "efTextWipe",
    )
    val rest = when {
        !enabled -> c.chip.copy(alpha = 0.6f)
        kind == AppButtonKind.Primary -> c.signal
        else -> c.panel
    }
    val wipeColor = if (kind == AppButtonKind.Danger) c.danger else c.signal
    val borderColor = when (kind) {
        AppButtonKind.Default -> c.rule
        AppButtonKind.Primary -> c.signal
        AppButtonKind.Danger -> c.danger.copy(alpha = 0.5f)
    }
    val foreground = when {
        !enabled -> c.ink2
        kind == AppButtonKind.Danger -> if (pressed) c.panel else c.danger
        pressed || kind == AppButtonKind.Primary -> c.onSignal
        else -> c.ink
    }
    val haptics = LocalHapticFeedback.current
    val shape = CutCornerShape(bottomEnd = Ef.cutSmall)

    androidx.compose.foundation.layout.Box(
        modifier = modifier
            .heightIn(min = 40.dp)
            .clip(shape)
            .background(rest)
            .drawBehind {
                if (wipe > 0f) {
                    clipRect {
                        drawRect(wipeColor, size = size.copy(width = size.width * wipe))
                    }
                }
            }
            .border(Ef.hairline, borderColor, shape)
            .clickable(
                interactionSource = source,
                indication = null,
                enabled = enabled,
                role = Role.Button,
                onClick = {
                    haptics.performHapticFeedback(HapticFeedbackType.KeyboardTap)
                    onClick()
                },
            )
            .padding(horizontal = 14.dp, vertical = 6.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            style = textStyle ?: EfType.Field.copy(fontWeight = FontWeight.Bold),
            color = foreground,
        )
    }
}

/** 方形开关：开 = 黄轨道墨方块，关 = 灰轨道面板色方块。 */
@Composable
fun AppSwitch(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    if (!LocalEndfield.current) {
        Switch(
            checked = checked,
            onCheckedChange = { onCheckedChange(it) },
            modifier = modifier,
            enabled = enabled,
        )
        return
    }

    val c = LocalEfColors.current
    val track by animateColorAsState(
        targetValue = if (checked) c.signal else c.chip,
        animationSpec = tween(Ef.WIPE_MS, easing = Ef.Mech),
        label = "efTrack",
    )
    val knobX by animateDpAsState(
        targetValue = if (checked) 22.dp else 0.dp,
        animationSpec = tween(Ef.WIPE_MS, easing = Ef.Mech),
        label = "efKnob",
    )
    androidx.compose.foundation.layout.Box(
        modifier = modifier
            .size(width = 48.dp, height = 26.dp)
            .alpha(if (enabled) 1f else 0.38f)
            .background(track)
            .toggleable(
                value = checked,
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                enabled = enabled,
                role = Role.Switch,
                onValueChange = onCheckedChange,
            )
            .padding(3.dp),
    ) {
        androidx.compose.foundation.layout.Box(
            modifier = Modifier
                .offset(x = knobX)
                .size(20.dp)
                .background(if (checked) c.onSignal else c.panel),
        )
    }
}

/** 方框勾选：默认 Miuix Checkbox；终末地是直角细线方框，选中后墨块 + 纸色对勾。 */
@Composable
fun AppCheckbox(
    state: ToggleableState,
    onClick: (() -> Unit)?,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    if (!LocalEndfield.current) {
        Checkbox(
            state = state,
            onClick = onClick,
            modifier = modifier,
            enabled = enabled,
        )
        return
    }

    val c = LocalEfColors.current
    val checked = state == ToggleableState.On
    val fill by animateColorAsState(
        targetValue = if (checked) c.ink else Color.Transparent,
        animationSpec = tween(Ef.WIPE_MS, easing = Ef.Mech),
        label = "efCheckFill",
    )
    androidx.compose.foundation.layout.Box(
        modifier = modifier
            .size(20.dp)
            .background(fill)
            .border(Ef.hairline, if (checked) c.ink else c.ink2),
        contentAlignment = Alignment.Center,
    ) {
        if (checked) {
            Icon(
                imageVector = AppIcons.Check,
                contentDescription = null,
                tint = c.paper,
                modifier = Modifier.size(14.dp),
            )
        }
        if (onClick != null) {
            androidx.compose.foundation.layout.Box(
                modifier = Modifier
                    .matchParentSize()
                    .toggleable(
                        value = checked,
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        enabled = enabled,
                        role = Role.Checkbox,
                        onValueChange = { onClick() },
                    ),
            )
        }
    }
}

/** 卡片：默认走 Miuix 圆角卡片；终末地换成直角面板 + 1px 细线框，可点的卡片切右下角。 */
@Composable
fun AppCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    cornerRadius: Dp = CardDefaults.CornerRadius,
    insideMargin: PaddingValues = CardDefaults.InsideMargin,
    content: @Composable ColumnScope.() -> Unit,
) {
    if (!LocalEndfield.current) {
        if (onClick == null) {
            Card(
                modifier = modifier,
                cornerRadius = cornerRadius,
                insideMargin = insideMargin,
                content = content,
            )
        } else {
            Card(
                modifier = modifier,
                cornerRadius = cornerRadius,
                insideMargin = insideMargin,
                onClick = onClick,
                content = content,
            )
        }
        return
    }

    val c = LocalEfColors.current
    val source = remember { MutableInteractionSource() }
    val pressed by source.collectIsPressedAsState()
    val wipe by animateFloatAsState(
        targetValue = if (pressed) 1f else 0f,
        animationSpec = tween(
            durationMillis = if (pressed) Ef.PRESS_MS else Ef.WIPE_MS,
            easing = Ef.Mech,
        ),
        label = "efCardWipe",
    )
    val shape = if (onClick != null) CutCornerShape(bottomEnd = Ef.cutSmall) else RectangleShape
    Column(
        modifier = modifier
            .clip(shape)
            .background(c.panel)
            .drawBehind {
                if (wipe > 0f) {
                    clipRect {
                        drawRect(
                            color = c.signal.copy(alpha = 0.16f),
                            size = size.copy(width = size.width * wipe),
                        )
                    }
                }
            }
            .border(Ef.hairline, c.rule, shape)
            .then(
                if (onClick != null) {
                    Modifier.clickable(
                        interactionSource = source,
                        indication = null,
                        role = Role.Button,
                        onClick = onClick,
                    )
                } else {
                    Modifier
                },
            )
            .padding(insideMargin),
        content = content,
    )
}

/** 分组标题：默认是 Miuix 小标题；终末地是 `// 分组` 的宽体眉标。 */
@Composable
fun AppSectionTitle(text: String) {
    if (!LocalEndfield.current) {
        SmallTitle(text)
        return
    }
    Text(
        text = "// $text",
        style = EfType.Eyebrow,
        color = LocalEfColors.current.ink2,
        modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 8.dp),
    )
}

/** 子页顶栏：默认 Miuix TopAppBar；终末地是左对齐的眉标 + 粗体标题 + 细线。 */
@Composable
fun AppTopBar(
    title: String,
    onBack: (() -> Unit)? = null,
    en: String = "",
    subtitle: String = "",
    scrollBehavior: ScrollBehavior? = null,
    actions: @Composable RowScope.() -> Unit = {},
) {
    if (!LocalEndfield.current) {
        val navigationIcon: @Composable () -> Unit = if (onBack != null) {
            {
                IconButton(onClick = onBack) {
                    Icon(imageVector = AppIcons.Back, contentDescription = "返回")
                }
            }
        } else {
            {}
        }
        TopAppBar(
            title = title,
            subtitle = subtitle,
            navigationIcon = navigationIcon,
            actions = actions,
            scrollBehavior = scrollBehavior,
        )
        return
    }

    val c = LocalEfColors.current
    Column(modifier = Modifier.fillMaxWidth()) {
        androidx.compose.foundation.layout.Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .height(if (subtitle.isEmpty()) 64.dp else 78.dp)
                .padding(horizontal = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (onBack != null) {
                IconButton(onClick = onBack) {
                    Icon(imageVector = AppIcons.Back, contentDescription = "返回", tint = c.ink)
                }
            }
            Column(modifier = Modifier.padding(start = if (onBack != null) 4.dp else 16.dp)) {
                if (en.isNotEmpty()) {
                    Text(
                        text = en.uppercase(),
                        style = EfType.Eyebrow,
                        color = c.ink2,
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                }
                Text(
                    text = title,
                    style = EfType.Title,
                    color = c.ink,
                )
                if (subtitle.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = subtitle,
                        style = MiuixTheme.textStyles.footnote2,
                        color = c.ink2,
                    )
                }
            }
            Spacer(modifier = Modifier.weight(1f))
            actions()
        }
        EfHairlineRow(c.rule)
    }
}

/** 弹窗：默认 Miuix OverlayDialog；终末地换成直角面板、`//` 前缀标题与手写行距。 */
@Composable
fun AppDialog(
    show: Boolean,
    title: String? = null,
    summary: String? = null,
    onDismissRequest: (() -> Unit)? = null,
    content: @Composable () -> Unit,
) {
    if (!LocalEndfield.current) {
        OverlayDialog(
            show = show,
            title = title,
            summary = summary,
            onDismissRequest = onDismissRequest,
            content = content,
        )
        return
    }

    val c = LocalEfColors.current
    OverlayDialog(
        show = show,
        title = null,
        summary = null,
        backgroundColor = c.panel,
        cornerRadius = 0.dp,
        onDismissRequest = onDismissRequest,
        content = {
            Column(modifier = Modifier.fillMaxWidth()) {
                if (title != null) {
                    Text(
                        text = "// $title",
                        style = EfType.Title,
                        color = c.ink,
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }
                if (summary != null) {
                    Text(
                        text = summary,
                        style = MiuixTheme.textStyles.body2.copy(lineHeight = 22.sp),
                        color = c.ink2,
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                }
                content()
            }
        },
    )
}

/** 底部弹层：默认 Miuix OverlayBottomSheet；终末地直角面板、细线拖拽条与 `//` 标题。 */
@Composable
fun AppBottomSheet(
    show: Boolean,
    title: String? = null,
    onDismissRequest: (() -> Unit)? = null,
    content: @Composable () -> Unit,
) {
    if (!LocalEndfield.current) {
        OverlayBottomSheet(
            show = show,
            title = title,
            onDismissRequest = onDismissRequest,
            content = content,
        )
        return
    }

    val c = LocalEfColors.current
    OverlayBottomSheet(
        show = show,
        title = null,
        backgroundColor = c.panel,
        cornerRadius = 0.dp,
        dragHandleColor = c.rule,
        onDismissRequest = onDismissRequest,
        content = {
            Column(modifier = Modifier.fillMaxWidth()) {
                if (title != null) {
                    Text(
                        text = "// $title",
                        style = EfType.Title,
                        color = c.ink,
                        modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 4.dp, bottom = 16.dp),
                    )
                    EfHairlineRow(c.rule)
                }
                content()
            }
        },
    )
}

/**
 * 顶栏折叠连接：Miuix 的折叠上限由顶栏在布局时量出，终末地顶栏是固定栏、不参与折叠，
 * 继续挂接会让连接把上滑全部当成折叠量吃掉（页面就滚不动了），所以此时不挂。
 */
@Composable
fun Modifier.appScrollBehavior(scrollBehavior: ScrollBehavior): Modifier =
    if (LocalEndfield.current) {
        this
    } else {
        nestedScroll(scrollBehavior.nestedScrollConnection)
    }

/** 底栏顶边与分组之间的 1px 细线。 */
@Composable
internal fun EfHairlineRow(color: androidx.compose.ui.graphics.Color) {
    androidx.compose.foundation.layout.Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(Ef.hairline)
            .background(color),
    )
}

internal fun androidx.compose.ui.Modifier.efTopHairline(color: androidx.compose.ui.graphics.Color) =
    drawBehind {
        drawLine(
            color = color,
            start = Offset(0f, 0f),
            end = Offset(size.width, 0f),
            strokeWidth = Ef.hairline.toPx(),
        )
    }
