package com.dotexpc.app.ui.pickup

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.CutCornerShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import top.yukonga.miuix.kmp.basic.FloatingActionButton
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.Text
import com.dotexpc.app.ui.component.AppIcons
import com.dotexpc.app.ui.theme.Ef
import com.dotexpc.app.ui.theme.LocalEfColors
import com.dotexpc.app.ui.theme.LocalEndfield
import top.yukonga.miuix.kmp.theme.MiuixTheme

private val MenuWidth = 200.dp
private val MenuCornerRadius = 16.dp
private val MenuGap = 10.dp

/** 悬浮按钮挂在主页面层，展开状态和“手动添加”入口由取件码页提供。 */
@Stable
class PickupAddMenuState {
    var expanded by mutableStateOf(false)

    /** 取件码页在组合时挂入，右侧悬浮按钮的“手动添加”会调用它。 */
    internal var openManualAdd: (() -> Unit)? = null
}

/**
 * 加号按钮（Miuix FloatingActionButton，样式对齐 KernelSU 管理器），以及和它彼此独立的动作菜单。
 *
 * 菜单固定在按钮上方 [MenuGap]、右缘与按钮对齐；按钮本身不位移，展开后加号转 45° 变成 X。
 * 菜单沿用原右上角 ListPopup 的度量：200dp 宽、16dp 圆角、行内 20dp 水平留白，
 * 首末行 20dp、中间行 12dp 垂直留白。
 */
@Composable
fun AddFabMenu(
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    onManualAdd: () -> Unit,
    onImportTaobao: () -> Unit,
    pddActionLabel: String,
    onPddAction: (() -> Unit)?,
    modifier: Modifier = Modifier,
) {
    val colors = MiuixTheme.colorScheme
    val endfield = LocalEndfield.current
    val ef = LocalEfColors.current
    val fabShape = if (endfield) CutCornerShape(bottomEnd = Ef.cutButton) else CircleShape
    // 展开后加号转 45° 变成 X。
    val iconRotation by animateFloatAsState(
        targetValue = if (expanded) 45f else 0f,
        animationSpec = spring(dampingRatio = 0.7f, stiffness = Spring.StiffnessMediumLow),
        label = "fabIcon",
    )
    val actions = buildList {
        add("手动添加" to onManualAdd)
        add("从菜鸟导入" to onImportTaobao)
        // 官方应用模式下这一项改为直接打开拼多多的取件码页面。
        onPddAction?.let { add(pddActionLabel to it) }
    }

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.End,
    ) {
        AnimatedVisibility(
            visible = expanded,
            // 从按钮所在的右下角张开：面板放大淡入，行内容再依次上移补上。
            enter = fadeIn(tween(160)) + scaleIn(
                initialScale = 0.88f,
                transformOrigin = TransformOrigin(1f, 1f),
                animationSpec = spring(dampingRatio = 0.8f, stiffness = Spring.StiffnessMediumLow),
            ),
            exit = fadeOut(tween(110)) + scaleOut(
                targetScale = 0.94f,
                transformOrigin = TransformOrigin(1f, 1f),
                animationSpec = tween(110),
            ),
        ) {
            // 间距放进动画内容里：收起过程中菜单不会先下移 10dp 再消失。
            Column(modifier = Modifier.padding(bottom = MenuGap)) {
                // 终末地皮肤里菜单是直角面板 + 细线框，投影换成轮廓线。
                val menuShape = if (endfield) CutCornerShape(bottomEnd = Ef.cutPanel) else RoundedCornerShape(MenuCornerRadius)
                Box(
                    modifier = Modifier
                        .width(MenuWidth)
                        .then(
                            if (endfield) {
                                Modifier.border(Ef.hairline, ef.rule, menuShape)
                            } else {
                                Modifier.shadow(
                                    elevation = 16.dp,
                                    shape = menuShape,
                                    ambientColor = Color.Black.copy(alpha = 0.28f),
                                    spotColor = Color.Black.copy(alpha = 0.32f),
                                )
                            },
                        )
                        .clip(menuShape)
                        .background(if (endfield) ef.panel else colors.surfaceContainer),
                ) {
                    Column {
                        actions.forEachIndexed { index, (label, action) ->
                            MenuRow(
                                label = label,
                                topPadding = if (index == 0) 20.dp else 12.dp,
                                bottomPadding = if (index == actions.lastIndex) 20.dp else 12.dp,
                                index = index,
                                onClick = {
                                    onExpandedChange(false)
                                    action()
                                },
                            )
                        }
                    }
                }
            }
        }

        FloatingActionButton(
            onClick = { onExpandedChange(!expanded) },
            modifier = Modifier.border(
                width = if (endfield) Ef.hairline else 0.05.dp,
                color = if (endfield) ef.rule else colors.outline.copy(alpha = 0.5f),
                shape = fabShape,
            ),
            shape = fabShape,
            containerColor = if (endfield) ef.signal else colors.primary,
            shadowElevation = 0.dp,
        ) {
            Icon(
                imageVector = AppIcons.Add,
                contentDescription = if (expanded) "关闭" else "添加取件码",
                tint = if (endfield) ef.onSignal else colors.onPrimary,
                modifier = Modifier
                    .size(32.dp)
                    .graphicsLayer { rotationZ = iconRotation },
            )
        }
    }
}

@Composable
private fun MenuRow(
    label: String,
    topPadding: Dp,
    bottomPadding: Dp,
    index: Int,
    onClick: () -> Unit,
) {
    val colors = MiuixTheme.colorScheme

    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { visible = true }
    // 行依次补位，避免整块菜单同时亮起。
    val rowProgress by animateFloatAsState(
        targetValue = if (visible) 1f else 0f,
        animationSpec = tween(durationMillis = 180, delayMillis = index * 30),
        label = "menuRow",
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .graphicsLayer {
                alpha = rowProgress
                translationY = (1f - rowProgress) * 10.dp.toPx()
            }
            .clickable(onClick = onClick)
            .padding(start = 20.dp, end = 20.dp, top = topPadding, bottom = bottomPadding),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            fontSize = MiuixTheme.textStyles.body1.fontSize,
            fontWeight = FontWeight.Medium,
            color = colors.onSurface,
            maxLines = 1,
            modifier = Modifier.heightIn(min = 20.dp),
        )
    }
}
