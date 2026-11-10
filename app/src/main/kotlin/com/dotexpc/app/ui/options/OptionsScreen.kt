package com.dotexpc.app.ui.options

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.dotexpc.app.data.AppSessions
import com.dotexpc.app.data.AppSettings
import com.dotexpc.app.data.CacheStore
import com.dotexpc.app.data.HitokotoStore
import com.dotexpc.app.data.HomeBackground
import com.dotexpc.app.data.PddAccessMode
import com.dotexpc.app.data.Platform
import com.dotexpc.app.data.TabIconStyle
import com.dotexpc.app.data.ThemeMode
import com.dotexpc.app.ui.component.AppButton
import com.dotexpc.app.ui.component.AppButtonKind
import com.dotexpc.app.ui.component.AppCard
import com.dotexpc.app.ui.component.AppDialog
import com.dotexpc.app.ui.component.AppIcons
import com.dotexpc.app.ui.component.AppSectionTitle
import com.dotexpc.app.ui.component.AppSwitch
import com.dotexpc.app.ui.component.AppTextButton
import com.dotexpc.app.ui.component.AppTopBar
import com.dotexpc.app.ui.component.ClickAnchoredPositionProvider
import com.dotexpc.app.ui.component.appScrollBehavior
import com.dotexpc.app.ui.component.trackPressX
import top.yukonga.miuix.kmp.basic.BasicComponent
import top.yukonga.miuix.kmp.basic.DropdownImpl
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.ListPopupColumn
import top.yukonga.miuix.kmp.basic.ScrollBehavior
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import top.yukonga.miuix.kmp.overlay.OverlayListPopup
import top.yukonga.miuix.kmp.theme.MiuixTheme

/** 选项页：账户与外观两个分区。 */
@Composable
fun OptionsScreen(
    settings: AppSettings,
    sessions: AppSessions,
    hitokoto: HitokotoStore,
    onLogin: (Platform) -> Unit,
    onOpenAbout: () -> Unit,
    onOpenLicenses: () -> Unit,
    scrollState: ScrollState,
    scrollBehavior: ScrollBehavior,
    modifier: Modifier = Modifier,
) {
    var showThemeMenu by remember { mutableStateOf(false) }
    var showIconMenu by remember { mutableStateOf(false) }
    var showBackgroundMenu by remember { mutableStateOf(false) }
    var showPddMenu by remember { mutableStateOf(false) }
    var showRefreshHitokoto by remember { mutableStateOf(false) }
    var unbindTarget by remember { mutableStateOf<Platform?>(null) }
    var showUnbind by remember { mutableStateOf(false) }
    val themeTapX = remember { mutableFloatStateOf(0f) }
    val iconTapX = remember { mutableFloatStateOf(0f) }
    val backgroundTapX = remember { mutableFloatStateOf(0f) }
    val pddTapX = remember { mutableFloatStateOf(0f) }
    val themeAnchor = remember { ClickAnchoredPositionProvider(themeTapX) }
    val iconAnchor = remember { ClickAnchoredPositionProvider(iconTapX) }
    val backgroundAnchor = remember { ClickAnchoredPositionProvider(backgroundTapX) }
    val pddAnchor = remember { ClickAnchoredPositionProvider(pddTapX) }
    val pddInAppLogin = settings.pddAccessMode == PddAccessMode.InApp
    val glassLocked = settings.endfieldTheme
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var cacheSize by remember { mutableStateOf<Long?>(null) }

    LaunchedEffect(Unit) {
        cacheSize = withContext(Dispatchers.IO) { CacheStore.size(context) }
    }

    Column(modifier = modifier.fillMaxSize()) {
        AppTopBar(
            title = "选项",
            en = "SETTINGS",
            scrollBehavior = scrollBehavior,
        )

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .appScrollBehavior(scrollBehavior)
                .verticalScroll(scrollState)
                .padding(top = 8.dp, bottom = 100.dp),
        ) {
            AppSectionTitle("账户")
            AppCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
            ) {
                Platform.entries.forEach { platform ->
                    val row: @Composable () -> Unit = {
                        AccountRow(
                            platform = platform,
                            icon = when (platform) {
                                Platform.PDD -> AppIcons.Pdd
                                Platform.TAOBAO -> if (settings.tabIconStyle == TabIconStyle.Filled) {
                                    AppIcons.TaobaoFilled
                                } else {
                                    AppIcons.TaobaoOutline
                                }
                            },
                            bound = sessions.get(platform).state.bound,
                            inAppLogin = platform != Platform.PDD || pddInAppLogin,
                            onBind = { onLogin(platform) },
                            onUnbind = {
                                unbindTarget = platform
                                showUnbind = true
                            },
                            onConfigure = if (platform == Platform.PDD) {
                                { showPddMenu = true }
                            } else {
                                null
                            },
                        )
                    }
                    if (platform == Platform.PDD) {
                        // 弹层锚点必须和触发它的行在同一个布局节点里。
                        Box(
                            Modifier
                                .fillMaxWidth()
                                .trackPressX(pddTapX),
                        ) {
                            row()
                            OverlayListPopup(
                                show = showPddMenu,
                                popupPositionProvider = pddAnchor,
                                onDismissRequest = { showPddMenu = false },
                            ) {
                                ListPopupColumn {
                                    PddAccessMode.entries.forEachIndexed { index, mode ->
                                        DropdownImpl(
                                            text = mode.label,
                                            optionSize = PddAccessMode.entries.size,
                                            isSelected = mode == settings.pddAccessMode,
                                            index = index,
                                            onSelectedIndexChange = {
                                                // 只切换入口，不动应用内会话，切回来仍是登录态。
                                                settings.selectPddAccessMode(mode)
                                                showPddMenu = false
                                            },
                                        )
                                    }
                                }
                            }
                        }
                    } else {
                        row()
                    }
                }
            }

            AppSectionTitle("外观")
            AppCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
            ) {
                // 弹层锚点必须和触发它的行在同一个布局节点里。
                Box(
                    Modifier
                        .fillMaxWidth()
                        .trackPressX(themeTapX),
                ) {
                    OptionRow(
                        title = "主题",
                        summary = settings.themeMode.label,
                        icon = AppIcons.Theme,
                        onClick = { showThemeMenu = true },
                    )
                    OverlayListPopup(
                        show = showThemeMenu,
                        popupPositionProvider = themeAnchor,
                        onDismissRequest = { showThemeMenu = false },
                    ) {
                        ListPopupColumn {
                            ThemeMode.entries.forEachIndexed { index, mode ->
                                DropdownImpl(
                                    text = mode.label,
                                    optionSize = ThemeMode.entries.size,
                                    isSelected = mode == settings.themeMode,
                                    index = index,
                                    onSelectedIndexChange = {
                                        settings.selectThemeMode(mode)
                                        showThemeMenu = false
                                    },
                                )
                            }
                        }
                    }
                }

                Box(
                    Modifier
                        .fillMaxWidth()
                        .trackPressX(iconTapX),
                ) {
                    OptionRow(
                        title = "应用内图标样式",
                        summary = settings.tabIconStyle.label,
                        icon = if (settings.tabIconStyle == TabIconStyle.Filled) {
                            AppIcons.StyleFilled
                        } else {
                            AppIcons.StyleOutline
                        },
                        onClick = { showIconMenu = true },
                    )
                    OverlayListPopup(
                        show = showIconMenu,
                        popupPositionProvider = iconAnchor,
                        onDismissRequest = { showIconMenu = false },
                    ) {
                        ListPopupColumn {
                            TabIconStyle.entries.forEachIndexed { index, style ->
                                DropdownImpl(
                                    text = style.label,
                                    optionSize = TabIconStyle.entries.size,
                                    isSelected = style == settings.tabIconStyle,
                                    index = index,
                                    onSelectedIndexChange = {
                                        settings.selectTabIconStyle(style)
                                        showIconMenu = false
                                    },
                                )
                            }
                        }
                    }
                }

                Box(
                    Modifier
                        .fillMaxWidth()
                        .trackPressX(backgroundTapX),
                ) {
                    OptionRow(
                        title = "主页背景",
                        summary = settings.homeBackground.label,
                        icon = if (settings.tabIconStyle == TabIconStyle.Filled) {
                            AppIcons.BackgroundFilled
                        } else {
                            AppIcons.BackgroundOutline
                        },
                        onClick = { showBackgroundMenu = true },
                    )
                    OverlayListPopup(
                        show = showBackgroundMenu,
                        popupPositionProvider = backgroundAnchor,
                        onDismissRequest = { showBackgroundMenu = false },
                    ) {
                        ListPopupColumn {
                            HomeBackground.entries.forEachIndexed { index, background ->
                                DropdownImpl(
                                    text = background.label,
                                    optionSize = HomeBackground.entries.size,
                                    isSelected = background == settings.homeBackground,
                                    index = index,
                                    onSelectedIndexChange = {
                                        settings.selectHomeBackground(background)
                                        showBackgroundMenu = false
                                    },
                                )
                            }
                        }
                    }
                }

                OptionRow(
                    title = "主页一言",
                    summary = "在主页显示每日一言",
                    icon = if (settings.tabIconStyle == TabIconStyle.Filled) {
                        AppIcons.QuoteFilled
                    } else {
                        AppIcons.QuoteOutline
                    },
                    trailing = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(
                                onClick = { showRefreshHitokoto = true },
                                modifier = Modifier.padding(end = 10.dp),
                                minWidth = 36.dp,
                                minHeight = 36.dp,
                            ) {
                                Icon(
                                    imageVector = AppIcons.Refresh,
                                    contentDescription = "刷新一言",
                                    tint = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                                )
                            }
                            AppSwitch(
                                checked = settings.hitokotoEnabled,
                                onCheckedChange = { settings.toggleHitokoto() },
                            )
                        }
                    },
                    onClick = { settings.toggleHitokoto() },
                )

                OptionRow(
                    title = "液态玻璃底栏",
                    summary = if (glassLocked) {
                        "无法与终末地主题同时使用"
                    } else {
                        "启用类似 iOS 的液态玻璃底栏"
                    },
                    icon = AppIcons.LiquidGlass,
                    trailing = {
                        // 终末地底栏是平面细线风格，玻璃会把它糊掉：只把开关置灰。
                        AppSwitch(
                            checked = settings.liquidTabBar && !glassLocked,
                            onCheckedChange = { settings.toggleLiquidTabBar() },
                            enabled = !glassLocked,
                        )
                    },
                    onClick = if (glassLocked) null else ({ settings.toggleLiquidTabBar() }),
                )
            }

            AppSectionTitle("存储")
            AppCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
            ) {
                OptionRow(
                    title = "清除缓存",
                    summary = cacheSize?.let(CacheStore::format) ?: "…",
                    icon = if (settings.tabIconStyle == TabIconStyle.Filled) {
                        AppIcons.TrashFilled
                    } else {
                        AppIcons.TrashOutline
                    },
                    onClick = {
                        scope.launch {
                            sessions.clearCaches()
                            cacheSize = withContext(Dispatchers.IO) { CacheStore.size(context) }
                        }
                    },
                )
            }

            AppSectionTitle("关于")
            AppCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
            ) {
                OptionRow(
                    title = "关于此应用",
                    icon = if (settings.tabIconStyle == TabIconStyle.Filled) {
                        AppIcons.AboutFilled
                    } else {
                        AppIcons.AboutOutline
                    },
                    onClick = onOpenAbout,
                )
                OptionRow(
                    title = "开放源代码许可",
                    icon = AppIcons.License,
                    onClick = onOpenLicenses,
                )
            }
        }

        AppDialog(
            show = showUnbind,
            title = "解除${unbindTarget?.title ?: ""}绑定？",
            summary = "将清除本机保存的登录状态，之后需要重新登录官方页面才能显示身份码。",
            onDismissRequest = { showUnbind = false },
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                AppTextButton(
                    label = "取消",
                    onClick = { showUnbind = false },
                    modifier = Modifier.weight(1f),
                )
                AppButton(
                    label = "解除绑定",
                    onClick = {
                        unbindTarget?.let { sessions.get(it).unbind() }
                        showUnbind = false
                    },
                    modifier = Modifier.weight(1f),
                    kind = AppButtonKind.Danger,
                )
            }
        }

        AppDialog(
            show = showRefreshHitokoto,
            title = "刷新一言",
            summary = "为避免请求过多，请慎用刷新功能",
            onDismissRequest = { showRefreshHitokoto = false },
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                AppButton(
                    label = "取消",
                    onClick = { showRefreshHitokoto = false },
                    modifier = Modifier.weight(1f),
                    kind = AppButtonKind.Primary,
                )
                AppButton(
                    label = "刷新",
                    onClick = {
                        showRefreshHitokoto = false
                        scope.launch { hitokoto.refreshNow() }
                    },
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

/** 账户行：右侧按钮直接绑定/解绑，注释区显示当前状态。 */
@Composable
private fun AccountRow(
    platform: Platform,
    icon: ImageVector,
    bound: Boolean,
    inAppLogin: Boolean,
    onBind: () -> Unit,
    onUnbind: () -> Unit,
    onConfigure: (() -> Unit)? = null,
) {
    OptionRow(
        title = platform.title,
        summary = when {
            !inAppLogin -> "无需登录"
            bound -> "已绑定"
            else -> "未绑定"
        },
        icon = icon,
        trailing = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                if (onConfigure != null) {
                    IconButton(
                        onClick = onConfigure,
                        minWidth = 36.dp,
                        minHeight = 36.dp,
                    ) {
                        Icon(
                            imageVector = AppIcons.SettingsFilled,
                            contentDescription = "拼多多登录方式",
                            tint = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                        )
                    }
                }
                if (inAppLogin) {
                    AppTextButton(
                        label = if (bound) "解绑" else "绑定",
                        onClick = if (bound) onUnbind else onBind,
                        kind = if (bound) AppButtonKind.Danger else AppButtonKind.Primary,
                        minWidth = 0.dp,
                        minHeight = 32.dp,
                        cornerRadius = 16.dp,
                        insideMargin = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                        textStyle = MiuixTheme.textStyles.body2,
                    )
                }
            }
        },
    )
}

@Composable
private fun OptionRow(
    title: String,
    icon: ImageVector,
    summary: String? = null,
    trailing: (@Composable () -> Unit)? = null,
    onClick: (() -> Unit)? = null,
) {
    BasicComponent(
        title = title,
        summary = summary,
        startAction = {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MiuixTheme.colorScheme.onSurface,
                modifier = Modifier.padding(end = 8.dp),
            )
        },
        onClick = onClick,
        endActions = {
            if (trailing != null) {
                trailing()
            } else {
                Icon(
                    imageVector = AppIcons.ChevronForward,
                    contentDescription = null,
                    tint = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                    modifier = Modifier.size(16.dp),
                )
            }
        },
    )
}
