package com.dotexpc.app.ui.options

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.dotexpc.app.data.AppSessions
import com.dotexpc.app.data.AppSettings
import com.dotexpc.app.data.CacheStore
import com.dotexpc.app.data.Platform
import com.dotexpc.app.data.TabIconStyle
import com.dotexpc.app.data.ThemeMode
import com.dotexpc.app.ui.component.ClickAnchoredPositionProvider
import com.dotexpc.app.ui.component.TabIcons
import com.dotexpc.app.ui.component.trackPressX
import top.yukonga.miuix.kmp.basic.BasicComponent
import top.yukonga.miuix.kmp.basic.Button
import top.yukonga.miuix.kmp.basic.ButtonDefaults
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.DropdownImpl
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.ListPopupColumn
import top.yukonga.miuix.kmp.basic.MiuixScrollBehavior
import top.yukonga.miuix.kmp.basic.SmallTitle
import top.yukonga.miuix.kmp.basic.Switch
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TextButton
import top.yukonga.miuix.kmp.basic.TopAppBar
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.ChevronForward
import top.yukonga.miuix.kmp.overlay.OverlayDialog
import top.yukonga.miuix.kmp.overlay.OverlayListPopup
import top.yukonga.miuix.kmp.theme.MiuixTheme

/** 选项页：账户与外观两个分区。 */
@Composable
fun OptionsScreen(
    settings: AppSettings,
    sessions: AppSessions,
    onLogin: (Platform) -> Unit,
    onOpenAbout: () -> Unit,
    onOpenLicenses: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = MiuixTheme.colorScheme
    val scrollBehavior = MiuixScrollBehavior()
    var showThemeMenu by remember { mutableStateOf(false) }
    var showIconMenu by remember { mutableStateOf(false) }
    var unbindTarget by remember { mutableStateOf<Platform?>(null) }
    var showUnbind by remember { mutableStateOf(false) }
    val themeTapX = remember { mutableFloatStateOf(0f) }
    val iconTapX = remember { mutableFloatStateOf(0f) }
    val themeAnchor = remember { ClickAnchoredPositionProvider(themeTapX) }
    val iconAnchor = remember { ClickAnchoredPositionProvider(iconTapX) }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var cacheSize by remember { mutableStateOf<Long?>(null) }

    LaunchedEffect(Unit) {
        cacheSize = withContext(Dispatchers.IO) { CacheStore.size(context) }
    }

    Column(modifier = modifier.fillMaxSize()) {
        TopAppBar(title = "选项", scrollBehavior = scrollBehavior)

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .nestedScroll(scrollBehavior.nestedScrollConnection)
                .verticalScroll(rememberScrollState())
                .padding(top = 8.dp, bottom = 100.dp),
        ) {
            SmallTitle("账户")
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
            ) {
                Platform.entries.forEach { platform ->
                    AccountRow(
                        platform = platform,
                        icon = when (platform) {
                            Platform.PDD -> TabIcons.Pdd
                            Platform.TAOBAO -> if (settings.tabIconStyle == TabIconStyle.Filled) {
                                TabIcons.TaobaoFilled
                            } else {
                                TabIcons.TaobaoOutline
                            }
                        },
                        bound = sessions.get(platform).state.bound,
                        onBind = { onLogin(platform) },
                        onUnbind = {
                            unbindTarget = platform
                            showUnbind = true
                        },
                    )
                }
            }

            SmallTitle("外观")
            Card(
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
                        icon = TabIcons.Theme,
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
                            TabIcons.StyleFilled
                        } else {
                            TabIcons.StyleOutline
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

                OptionRow(
                    title = "液态玻璃底栏",
                    summary = "启用类似 iOS 的液态玻璃底栏",
                    icon = TabIcons.LiquidGlass,
                    trailing = {
                        Switch(
                            checked = settings.liquidTabBar,
                            onCheckedChange = { settings.toggleLiquidTabBar() },
                        )
                    },
                    onClick = { settings.toggleLiquidTabBar() },
                )
            }

            SmallTitle("存储")
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
            ) {
                OptionRow(
                    title = "清除缓存",
                    summary = cacheSize?.let(CacheStore::format) ?: "…",
                    icon = if (settings.tabIconStyle == TabIconStyle.Filled) {
                        TabIcons.TrashFilled
                    } else {
                        TabIcons.TrashOutline
                    },
                    onClick = {
                        scope.launch {
                            sessions.clearCaches()
                            cacheSize = withContext(Dispatchers.IO) { CacheStore.size(context) }
                        }
                    },
                )
            }

            SmallTitle("关于")
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
            ) {
                OptionRow(
                    title = "关于此应用",
                    icon = if (settings.tabIconStyle == TabIconStyle.Filled) {
                        TabIcons.AboutFilled
                    } else {
                        TabIcons.AboutOutline
                    },
                    onClick = onOpenAbout,
                )
                OptionRow(
                    title = "开放源代码许可",
                    icon = TabIcons.License,
                    onClick = onOpenLicenses,
                )
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
}

/** 账户行：右侧按钮直接绑定/解绑，注释区显示当前状态。 */
@Composable
private fun AccountRow(
    platform: Platform,
    icon: ImageVector,
    bound: Boolean,
    onBind: () -> Unit,
    onUnbind: () -> Unit,
) {
    OptionRow(
        title = platform.title,
        summary = if (bound) "已绑定" else "未绑定",
        icon = icon,
        trailing = {
            TextButton(
                text = if (bound) "解绑" else "绑定",
                onClick = if (bound) onUnbind else onBind,
                minWidth = 0.dp,
                minHeight = 32.dp,
                cornerRadius = 16.dp,
                insideMargin = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                textStyle = MiuixTheme.textStyles.body2,
                colors = if (bound) {
                    ButtonDefaults.textButtonColors(
                        color = MiuixTheme.colorScheme.error,
                        textColor = Color.White,
                    )
                } else {
                    ButtonDefaults.textButtonColorsPrimary()
                },
            )
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
                    imageVector = MiuixIcons.ChevronForward,
                    contentDescription = null,
                    tint = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                    modifier = Modifier.size(16.dp),
                )
            }
        },
    )
}
