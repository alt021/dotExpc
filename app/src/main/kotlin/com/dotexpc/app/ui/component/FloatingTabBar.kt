// Copyright 2026, compose-miuix-ui contributors
// SPDX-License-Identifier: Apache-2.0
//
// Layout adapted from the compose-miuix-ui example (IosLiquidGlassNavigationBar, Apache 2.0).

package com.dotexpc.app.ui.component

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.EaseOut
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredWidth
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.dropShadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.shadow.Shadow
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.util.fastCoerceIn
import com.dotexpc.app.data.TabIconStyle
import com.kyant.backdrop.backdrops.LayerBackdrop
import com.kyant.backdrop.backdrops.layerBackdrop
import com.kyant.backdrop.backdrops.rememberCombinedBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.lens
import com.kyant.backdrop.effects.vibrancy
import com.kyant.backdrop.highlight.Highlight
import com.kyant.backdrop.shadow.InnerShadow as GlassInnerShadow
import com.kyant.backdrop.shadow.Shadow as GlassShadow
import kotlinx.coroutines.launch
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.theme.LocalContentColor
import top.yukonga.miuix.kmp.theme.MiuixTheme
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlin.math.sign

enum class AppTab(
    val label: String,
    val en: String,
    val filledIcon: ImageVector,
    val outlineIcon: ImageVector,
) {
    HOME("主页", "HOME", AppIcons.HomeFilled, AppIcons.HomeOutline),
    PICKUP("取件码", "PICKUPS", AppIcons.PickupFilled, AppIcons.PickupOutline),
    IDENTITY("身份码", "IDENTITY", AppIcons.IdentityFilled, AppIcons.IdentityOutline),
    OPTIONS("选项", "SETTINGS", AppIcons.SettingsFilled, AppIcons.SettingsOutline),
}

/**
 * 底栏胶囊下沿距屏幕底部的距离；其它需要与底栏对齐的悬浮控件也用它，
 * 手指导航条存在时贴着导航条，否则留出 28dp。
 */
@Composable
fun floatingTabBarBottomPadding(): Dp {
    val navInset = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    return if (navInset != 0.dp) 8.dp + navInset else 28.dp
}

/**
 * 悬浮底栏：胶囊容器 + 投影，选中项由一块跟随手指滑动的主题色底片标出。
 * 结构移植自 compose-miuix-ui 示例的 IosLiquidGlassNavigationBar 非模糊分支（Apache 2.0）；
 * 传入 backdrop 时换成 AndroidLiquidGlass 官方 LiquidBottomTabs 的做法：
 * 容器与指示块都画玻璃，指示块采样「胶囊玻璃 + 一层不可见的主题色图标」，
 * 这样选中区不会把页面内容原样透出来，图标也保持清晰。
 */@Composable
fun FloatingTabBar(
    selected: AppTab,
    iconStyle: TabIconStyle,
    onSelect: (AppTab) -> Unit,
    modifier: Modifier = Modifier,
    backdrop: LayerBackdrop? = null,
) {
    val colors = MiuixTheme.colorScheme
    val isInDark = colors.background.luminance() < 0.5f
    val pillShape = remember { CircleShape }
    val accentColor = colors.primary
    val tabContentColor = colors.onSurface
    val containerColor = colors.surfaceContainer
    val tabs = AppTab.entries
    val tabsCount = tabs.size
    val selectedIndex = tabs.indexOf(selected)

    // 玻璃模式额外录两层：胶囊的玻璃表面、一层不可见的主题色图标。
    // 指示块采样这两层，既有磨砂底面，又不会把页面内容原样透出来。
    val barBackdrop = rememberLayerBackdrop()
    val tabsBackdrop = rememberLayerBackdrop()
    val indicatorBackdrop = rememberCombinedBackdrop(barBackdrop, tabsBackdrop)

    val density = LocalDensity.current
    val isLtr = LocalLayoutDirection.current == LayoutDirection.Ltr
    val scope = rememberCoroutineScope()

    var tabWidthPx by remember { mutableFloatStateOf(0f) }
    var totalWidthPx by remember { mutableFloatStateOf(0f) }

    // 拖动超出容器时，整块胶囊轻微跟手，松手回弹。
    val offsetAnimation = remember { Animatable(0f) }
    val rubberBandPx = with(density) { 4.dp.toPx() }
    val panelOffset by remember(rubberBandPx) {
        derivedStateOf {
            if (totalWidthPx == 0f) {
                0f
            } else {
                val fraction = (offsetAnimation.value / totalWidthPx).fastCoerceIn(-1f, 1f)
                rubberBandPx * fraction.sign * EaseOut.transform(abs(fraction))
            }
        }
    }

    var currentIndex by remember { mutableIntStateOf(selectedIndex) }
    val onSelectedUpdated by rememberUpdatedState(onSelect)

    fun indexAt(positionX: Float): Int {
        if (tabWidthPx == 0f) return currentIndex
        val horizontalPaddingPx = with(density) { 4.dp.toPx() }
        val logicalX = if (isLtr) positionX else totalWidthPx - positionX
        return ((logicalX - horizontalPaddingPx) / tabWidthPx)
            .toInt()
            .coerceIn(0, tabsCount - 1)
    }

    val drag = remember(scope, tabsCount, density, isLtr) {
        DampedDragAnimation(
            animationScope = scope,
            initialValue = selectedIndex.toFloat(),
            valueRange = 0f..(tabsCount - 1).toFloat(),
            visibilityThreshold = 0.001f,
            initialScale = 1f,
            pressedScale = 78f / 56f,
            canDrag = { offset -> offset.x in 0f..totalWidthPx },
            onDragStarted = { position ->
                updateValue(indexAt(position.x).toFloat())
            },
            onDragStopped = {
                val targetIndex = targetValue.roundToInt().coerceIn(0, tabsCount - 1)
                if (currentIndex != targetIndex) {
                    currentIndex = targetIndex
                    onSelectedUpdated(tabs[targetIndex])
                }
                updateValue(targetIndex.toFloat())
                scope.launch { offsetAnimation.animateTo(0f, spring(1f, 300f, 0.5f)) }
            },
            onDragCancelled = {
                updateValue(currentIndex.toFloat())
                scope.launch { offsetAnimation.animateTo(0f, spring(1f, 300f, 0.5f)) }
            },
            onDrag = { _, dragAmount ->
                if (tabWidthPx > 0f && dragAmount.x != 0f) {
                    updateValue(
                        (targetValue + dragAmount.x / tabWidthPx * if (isLtr) 1f else -1f)
                            .coerceIn(0f, (tabsCount - 1).toFloat()),
                    )
                    scope.launch { offsetAnimation.snapTo(offsetAnimation.value + dragAmount.x) }
                }
            },
        )
    }

    // 页面滑动或点击切换后，让指示块跟上外部选中的标签。
    LaunchedEffect(selectedIndex) {
        if (currentIndex != selectedIndex) {
            currentIndex = selectedIndex
            drag.animateToValue(selectedIndex.toFloat())
        }
    }

    fun activateTab(index: Int) {
        if (index !in 0 until tabsCount) return
        // 点击当前标签也要通知：主页面层据此判断用户是否选择了取件码。
        currentIndex = index
        onSelectedUpdated(tabs[index])
        drag.animateToValue(index.toFloat())
    }

    val bottomPadding = floatingTabBarBottomPadding()

    Box(
        modifier = modifier
            // 胶囊两侧和下方的留白也吞掉点击，避免误触到页面内容。
            .pointerInput(Unit) { detectTapGestures { } }
            .width(IntrinsicSize.Min)
            .padding(start = 28.dp, end = 28.dp, bottom = bottomPadding),
        contentAlignment = Alignment.CenterStart,
    ) {
        Row(
            Modifier
                .onGloballyPositioned { coords ->
                    totalWidthPx = coords.size.width.toFloat()
                    val contentWidthPx = totalWidthPx - with(density) { 8.dp.toPx() }
                    tabWidthPx = (contentWidthPx / tabsCount).coerceAtLeast(0f)
                }
                .selectableGroup()
                .graphicsLayer { translationX = panelOffset }
                .then(
                    if (backdrop != null) {
                        Modifier.drawBackdrop(
                            backdrop = backdrop,
                            shape = { pillShape },
                            effects = {
                                vibrancy()
                                blur(8.dp.toPx())
                                lens(24.dp.toPx(), 24.dp.toPx())
                            },
                            layerBlock = {
                                // 按下时整块胶囊轻微鼓起来。
                                val progress = drag.pressProgress
                                val scale = 1f + 16.dp.toPx() / size.width * progress
                                scaleX = scale
                                scaleY = scale
                            },
                            shadow = {
                                GlassShadow(
                                    radius = 10.dp,
                                    color = Color.Black,
                                    alpha = if (isInDark) 0.2f else 0.1f,
                                )
                            },
                            onDrawSurface = { drawRect(containerColor.copy(alpha = 0.4f)) },
                        )
                    } else {
                        Modifier
                            .dropShadow(
                                shape = pillShape,
                                shadow = Shadow(
                                    radius = 10.dp,
                                    color = Color.Black,
                                    alpha = if (isInDark) 0.2f else 0.1f,
                                ),
                            )
                            .background(containerColor, pillShape)
                    },
                )
                .then(drag.modifier)
                .height(64.dp)
                .padding(4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            CompositionLocalProvider(LocalContentColor provides tabContentColor) {
                TabItems(
                    activateTab = ::activateTab,
                    tabs = tabs,
                    currentIndex = currentIndex,
                    iconStyle = iconStyle,
                )
            }
        }

        // 仅记录胶囊的玻璃表面（不含图标），供指示块采样。
        if (backdrop != null) {
            Box(
                Modifier
                    .matchParentSize()
                    .clearAndSetSemantics {}
                    .alpha(0f)
                    .layerBackdrop(barBackdrop)
                    .graphicsLayer { translationX = panelOffset }
                    .drawBackdrop(
                        backdrop = backdrop,
                        shape = { pillShape },
                        effects = {
                            vibrancy()
                            blur(8.dp.toPx())
                            lens(24.dp.toPx(), 24.dp.toPx())
                        },
                        layerBlock = {
                            val progress = drag.pressProgress
                            val scale = 1f + 16.dp.toPx() / size.width * progress
                            scaleX = scale
                            scaleY = scale
                        },
                        shadow = {
                            GlassShadow(
                                radius = 10.dp,
                                color = Color.Black,
                                alpha = if (isInDark) 0.2f else 0.1f,
                            )
                        },
                        onDrawSurface = { drawRect(containerColor.copy(alpha = 0.4f)) },
                    ),
            )
        }

        // 仅记录进 tabsBackdrop，本身不可见，也不参与点击。
        if (backdrop != null) {
            Row(
                Modifier
                    .clearAndSetSemantics {}
                    .alpha(0f)
                    .layerBackdrop(tabsBackdrop)
                    .graphicsLayer { translationX = panelOffset }
                    .height(64.dp)
                    .padding(4.dp)
                    .graphicsLayer(
                        colorFilter = ColorFilter.tint(accentColor),
                    ),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                CompositionLocalProvider(LocalContentColor provides accentColor) {
                    TabItems(
                        activateTab = ::activateTab,
                        tabs = tabs,
                        currentIndex = currentIndex,
                        iconStyle = iconStyle,
                        interactive = false,
                    )
                }
            }
        }

        if (tabWidthPx > 0f) {
            val tabWidthDp = with(density) { tabWidthPx.toDp() }
            Box(
                Modifier
                    .padding(horizontal = 4.dp)
                    .graphicsLayer {
                        val progressOffset = drag.value * tabWidthPx
                        translationX = if (isLtr) progressOffset + panelOffset else -progressOffset + panelOffset
                    }
                    .then(
                        if (backdrop != null) {
                            Modifier.drawBackdrop(
                                backdrop = indicatorBackdrop,
                                shape = { pillShape },
                                effects = {
                                    // 底面已经是胶囊玻璃，这里只补按住时的折射；
                                    // 不再模糊图标层，否则选中的图标和文字会被糊成一团。
                                    val progress = drag.pressProgress
                                    lens(
                                        refractionHeight = 10.dp.toPx() * progress,
                                        refractionAmount = 14.dp.toPx() * progress,
                                        chromaticAberration = true,
                                    )
                                },
                                highlight = { Highlight.Default.copy(alpha = drag.pressProgress) },
                                shadow = { GlassShadow(alpha = drag.pressProgress) },
                                innerShadow = {
                                    GlassInnerShadow(
                                        radius = 8.dp * drag.pressProgress,
                                        alpha = drag.pressProgress,
                                    )
                                },
                                layerBlock = {
                                    scaleX = drag.scaleX
                                    scaleY = drag.scaleY
                                    // 拖得越快，指示块越扁、越被拉长。
                                    val velocity = drag.velocity / 10f
                                    scaleX /= 1f - (velocity * 0.75f).coerceIn(-0.2f, 0.2f)
                                    scaleY *= 1f - (velocity * 0.25f).coerceIn(-0.2f, 0.2f)
                                },
                                onDrawSurface = {
                                    val progress = drag.pressProgress
                                    drawRect(
                                        color = if (isInDark) Color.White else Color.Black,
                                        alpha = 0.1f * (1f - progress),
                                    )
                                    drawRect(Color.Black.copy(alpha = 0.03f * progress))
                                },
                            )
                        } else {
                            Modifier
                                .clip(pillShape)
                                .background(accentColor.copy(alpha = 0.15f), pillShape)
                        },
                    )
                    .height(56.dp)
                    .width(tabWidthDp),
                contentAlignment = Alignment.CenterStart,
            ) {
                if (backdrop == null) {
                    CompositionLocalProvider(LocalContentColor provides accentColor) {
                        Row(
                            Modifier
                                .clearAndSetSemantics {}
                                .wrapContentWidth(align = Alignment.Start, unbounded = true)
                                .requiredWidth(with(density) { (totalWidthPx - 8.dp.toPx()).toDp() })
                                .height(56.dp)
                                .graphicsLayer {
                                    val progressOffset = drag.value * tabWidthPx
                                    translationX = if (isLtr) -progressOffset else progressOffset
                                },
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            TabItems(
                                activateTab = ::activateTab,
                                tabs = tabs,
                                currentIndex = currentIndex,
                                iconStyle = iconStyle,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun RowScope.TabItems(
    activateTab: (Int) -> Unit,
    tabs: List<AppTab>,
    currentIndex: Int,
    iconStyle: TabIconStyle,
    interactive: Boolean = true,
) {
    tabs.forEachIndexed { index, tab ->
        FloatingTabBarItem(
            selected = currentIndex == index,
            onClick = { activateTab(index) },
            interactive = interactive,
            modifier = Modifier.defaultMinSize(minWidth = 76.dp),
        ) {
            Icon(
                imageVector = if (iconStyle == TabIconStyle.Filled) tab.filledIcon else tab.outlineIcon,
                contentDescription = tab.label,
            )
            Text(
                text = tab.label,
                fontSize = 11.sp,
                lineHeight = 14.sp,
                maxLines = 1,
                softWrap = false,
                overflow = TextOverflow.Visible,
            )
        }
    }
}

@Composable
private fun RowScope.FloatingTabBarItem(
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    interactive: Boolean = true,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier
            .then(
                if (interactive) {
                    Modifier
                        .semantics(mergeDescendants = true) {
                            this.selected = selected
                            role = Role.Tab
                            onClick {
                                onClick()
                                true
                            }
                        }
                        .onKeyEvent { event ->
                            val activationKey = event.key == Key.Enter ||
                                event.key == Key.NumPadEnter || event.key == Key.Spacebar
                            if (activationKey) {
                                if (event.type == KeyEventType.KeyUp) onClick()
                                true
                            } else {
                                false
                            }
                        }
                        .focusable()
                } else {
                    Modifier
                },
            )
            .fillMaxHeight()
            .weight(1f),
        verticalArrangement = Arrangement.spacedBy(1.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
        content = content,
    )
}
