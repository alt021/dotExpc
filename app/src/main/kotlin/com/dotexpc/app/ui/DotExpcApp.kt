package com.dotexpc.app.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerDefaults
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.toMutableStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.dotexpc.app.data.AppSessions
import com.dotexpc.app.data.AppSettings
import com.dotexpc.app.data.PickupStore
import com.dotexpc.app.data.Platform
import com.dotexpc.app.ui.about.AboutScreen
import com.dotexpc.app.ui.about.LicensesScreen
import com.dotexpc.app.ui.component.AppTab
import com.dotexpc.app.ui.component.FloatingTabBar
import com.dotexpc.app.ui.component.floatingTabBarBottomPadding
import com.dotexpc.app.ui.developer.DeveloperScreen
import com.dotexpc.app.ui.home.HomeScreen
import com.dotexpc.app.ui.identity.IdentityScreen
import com.dotexpc.app.ui.login.LoginScreen
import com.dotexpc.app.ui.options.OptionsScreen
import com.dotexpc.app.ui.pickup.AddFabMenu
import com.dotexpc.app.ui.pickup.PickupAddMenuState
import com.dotexpc.app.ui.pickup.PickupScreen
import com.dotexpc.app.ui.pickup.WebImportScreen
import com.kyant.backdrop.backdrops.layerBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import com.kyant.backdrop.isRenderEffectSupported
import kotlinx.coroutines.launch
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.nav.core.NavBackStack
import top.yukonga.miuix.kmp.nav.core.NavDisplay
import top.yukonga.miuix.kmp.nav.core.NavDisplayEffects
import top.yukonga.miuix.kmp.nav.core.NavKey
import top.yukonga.miuix.kmp.nav.core.navBackStackOf
import top.yukonga.miuix.kmp.nav.core.rememberNavSystemCornerRadius
import top.yukonga.miuix.kmp.nav.transition.NavSwipeDirection
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.utils.PagerNavigationSpringSpec

/** 根级路由：标签页常驻栈底，登录页、导入页和关于页压在其上。 */
private sealed interface Route : NavKey {
    data object Main : Route
    data object About : Route
    data object Licenses : Route
    data object Developer : Route
    data class Login(val platform: Platform) : Route
    data class Import(val platform: Platform) : Route
}

// 导航栈只存路由标识，恢复时再映射回对象，避免依赖 kotlinx.serialization 插件。
private fun routeId(route: NavKey): String = when (route) {
    is Route.About -> "about"
    is Route.Licenses -> "licenses"
    is Route.Developer -> "developer"
    is Route.Login -> "login:${route.platform.id}"
    is Route.Import -> "import:${route.platform.id}"
    else -> "main"
}

private fun routeOf(id: String): NavKey? = when {
    id == "main" -> Route.Main
    id == "about" -> Route.About
    id == "licenses" -> Route.Licenses
    id == "developer" -> Route.Developer
    id.startsWith("login:") -> Platform.fromId(id.removePrefix("login:"))?.let(Route::Login)
    id.startsWith("import:") -> Platform.fromId(id.removePrefix("import:"))?.let(Route::Import)
    else -> null
}

private val BackStackSaver = listSaver<NavBackStack, String>(
    save = { stack -> stack.map(::routeId) },
    restore = { ids ->
        val restored = ids.mapNotNull(::routeOf).toMutableStateList()
        if (restored.firstOrNull() == Route.Main) restored else mutableStateListOf<NavKey>(Route.Main)
    },
)

private fun NavBackStack.popTop() {
    if (size > 1) removeAt(lastIndex)
}

/**
 * 应用根壳：标签页 + 悬浮底栏，登录页和导入页由 Miuix 导航栈压在其上。
 * 转场、预判返回、覆盖页视差和圆角裁剪全部交给 NavDisplay，与 KernelSU Manager 同源。
 */
@Composable
fun DotExpcApp(
    sessions: AppSessions,
    settings: AppSettings,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val pickups = remember(context) { PickupStore(context.applicationContext) }
    val backStack = rememberSaveable(saver = BackStackSaver) { navBackStackOf(Route.Main) }

    val rtl = LocalLayoutDirection.current == LayoutDirection.Rtl
    val swipeBack = if (rtl) NavSwipeDirection.RightToLeft else NavSwipeDirection.LeftToRight
    // 同意过就不再出现；只有点过「同意并继续」才会写入。
    var showTerms by rememberSaveable { mutableStateOf(!settings.termsAccepted) }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = MiuixTheme.colorScheme.surface,
        contentWindowInsets = WindowInsets(0),
    ) { _ ->
        NavDisplay(
            backStack = backStack,
            modifier = Modifier.fillMaxSize(),
            effects = NavDisplayEffects(
                cornerClipRadius = rememberNavSystemCornerRadius(),
                backdropColor = MiuixTheme.colorScheme.surface,
            ),
            onBack = { backStack.popTop() },
        ) {
            entry<Route.Main> {
                MainScreen(
                    sessions = sessions,
                    settings = settings,
                    pickups = pickups,
                    // 标签页始终留在栈底，只有它是最上层时才接管返回键。
                    isTopLevel = backStack.size == 1,
                    onLogin = { platform -> backStack.add(Route.Login(platform)) },
                    onOpenImport = { platform -> backStack.add(Route.Import(platform)) },
                    onOpenAbout = { backStack.add(Route.About) },
                    onOpenLicenses = { backStack.add(Route.Licenses) },
                )
            }

            entry<Route.About>(swipeDismiss = swipeBack) {
                AboutScreen(
                    onClose = { backStack.popTop() },
                    onOpenDeveloper = { backStack.add(Route.Developer) },
                )
            }

            entry<Route.Licenses>(swipeDismiss = swipeBack) {
                LicensesScreen(onClose = { backStack.popTop() })
            }

            entry<Route.Developer>(swipeDismiss = swipeBack) {
                DeveloperScreen(
                    onClose = { backStack.popTop() },
                    onReopenTerms = {
                        settings.resetTerms()
                        showTerms = true
                    },
                )
            }

            entry<Route.Login>(swipeDismiss = swipeBack) { route ->
                LoginScreen(
                    platform = route.platform,
                    session = sessions.get(route.platform),
                    onClose = { backStack.popTop() },
                )
            }

            entry<Route.Import>(swipeDismiss = swipeBack) { route ->
                WebImportScreen(
                    pickups = pickups,
                    platform = route.platform,
                    // 拼多多导入复用身份码页那个已登入的 WebView。
                    session = if (route.platform == Platform.PDD) sessions.pdd else null,
                    onClose = { backStack.popTop() },
                )
            }
        }

        // 声明式弹层要挂在 Scaffold 子树里，否则 OverlayDialog 不会显示。
        FirstRunTermsDialog(
            show = showTerms,
            onAccept = {
                settings.acceptTerms()
                showTerms = false
            },
        )
    }
}

/** 底栏胶囊高度；加号悬在它上方 28dp。 */
private val TabBarHeight = 64.dp

@Composable
private fun MainScreen(
    sessions: AppSessions,
    settings: AppSettings,
    pickups: PickupStore,
    isTopLevel: Boolean,
    onLogin: (Platform) -> Unit,
    onOpenImport: (Platform) -> Unit,
    onOpenAbout: () -> Unit,
    onOpenLicenses: () -> Unit,
) {
    val tabs = AppTab.entries
    val scope = rememberCoroutineScope()
    val pagerState = rememberPagerState(pageCount = { tabs.size })
    val addMenu = remember { PickupAddMenuState() }
    val selectedTab = tabs[pagerState.settledPage.coerceIn(tabs.indices)]

    // 液态玻璃底栏采样的是页面内容；旧系统没有 RenderEffect，退回普通胶囊。
    val backdrop = rememberLayerBackdrop()
    val glassTabs = settings.liquidTabBar && isRenderEffectSupported()

    // 根页按返回时先退回第一个标签。
    BackHandler(enabled = isTopLevel && pagerState.settledPage != 0) {
        scope.launch { pagerState.scrollToPage(0) }
    }

    LaunchedEffect(selectedTab) {
        // 离开取件码页时收起加号菜单。
        if (selectedTab != AppTab.PICKUP) addMenu.expanded = false
    }

    Box(modifier = Modifier.fillMaxSize()) {
        HorizontalPager(
            state = pagerState,
            // 玻璃层只记录它之后的绘制，页面本身透明，所以表面色画在 layerBackdrop 里面。
            modifier = Modifier
                .fillMaxSize()
                .then(if (glassTabs) Modifier.layerBackdrop(backdrop) else Modifier)
                .background(MiuixTheme.colorScheme.surface),
            // 所有页面常驻：身份码页始终挂着两个 WebView，主页也能读到登录状态。
            beyondViewportPageCount = tabs.lastIndex,
            flingBehavior = PagerDefaults.flingBehavior(
                state = pagerState,
                snapAnimationSpec = PagerNavigationSpringSpec,
            ),
        ) { page ->
            key(tabs[page]) {
                when (tabs[page]) {
                    AppTab.HOME -> HomeScreen(
                        sessions = sessions,
                        pickups = pickups,
                        onLogin = onLogin,
                    )

                    AppTab.PICKUP -> PickupScreen(
                        pickups = pickups,
                        addMenu = addMenu,
                        onOpenImport = onOpenImport,
                    )

                    AppTab.IDENTITY -> IdentityScreen(
                        sessions = sessions,
                        onLogin = onLogin,
                    )

                    AppTab.OPTIONS -> OptionsScreen(
                        settings = settings,
                        sessions = sessions,
                        onLogin = onLogin,
                        onOpenAbout = onOpenAbout,
                        onOpenLicenses = onOpenLicenses,
                    )
                }
            }
        }

        FloatingTabBar(
            selected = selectedTab,
            iconStyle = settings.tabIconStyle,
            // 切页不做转场动画，直接跳到目标页。
            onSelect = { target -> scope.launch { pagerState.scrollToPage(target.ordinal) } },
            modifier = Modifier.align(Alignment.BottomCenter),
            backdrop = if (glassTabs) backdrop else null,
        )

        // 加号只属于取件码页；与 KernelSU 的 FAB 一样悬在底栏上方。
        if (selectedTab == AppTab.PICKUP) {
            AddFabMenu(
                expanded = addMenu.expanded,
                onExpandedChange = { addMenu.expanded = it },
                onManualAdd = { addMenu.openManualAdd?.invoke() },
                onImportTaobao = { onOpenImport(Platform.TAOBAO) },
                onImportPdd = { onOpenImport(Platform.PDD) },
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(
                        end = 20.dp,
                        bottom = floatingTabBarBottomPadding() + TabBarHeight + 28.dp,
                    ),
            )
        }
    }
}
