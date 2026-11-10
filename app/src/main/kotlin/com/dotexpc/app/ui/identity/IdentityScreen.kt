package com.dotexpc.app.ui.identity

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.dotexpc.app.data.AppSessions
import com.dotexpc.app.data.Platform
import com.dotexpc.app.ui.component.AppTopBar
import com.dotexpc.app.ui.component.IdentityCard
import com.dotexpc.app.ui.component.appScrollBehavior
import top.yukonga.miuix.kmp.basic.MiuixScrollBehavior

/** 身份码页：每个平台一张可展开/收起的卡片。 */
@Composable
fun IdentityScreen(
    sessions: AppSessions,
    onLogin: (Platform) -> Unit,
    onFullscreen: (Platform) -> Unit,
    pddOfficialMode: Boolean,
    modifier: Modifier = Modifier,
) {
    var taobaoExpanded by rememberSaveable { mutableStateOf(true) }
    var pddExpanded by rememberSaveable { mutableStateOf(true) }
    val scrollBehavior = MiuixScrollBehavior()

    Column(
        modifier = modifier.fillMaxSize(),
    ) {
        AppTopBar(
            title = "身份码",
            en = "IDENTITY",
            scrollBehavior = scrollBehavior,
        )

        Column(
            modifier = Modifier
                .weight(1f)
                .appScrollBehavior(scrollBehavior)
                .verticalScroll(rememberScrollState())
                .padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 120.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Platform.entries.forEach { platform ->
                val expanded = when (platform) {
                    Platform.TAOBAO -> taobaoExpanded
                    Platform.PDD -> pddExpanded
                }
                IdentityCard(
                    platform = platform,
                    session = sessions.get(platform),
                    expanded = expanded,
                    onToggle = {
                        when (platform) {
                            Platform.TAOBAO -> taobaoExpanded = !taobaoExpanded
                            Platform.PDD -> pddExpanded = !pddExpanded
                        }
                    },
                    onLogin = { onLogin(platform) },
                    onFullscreen = { onFullscreen(platform) },
                    // 拼多多选择「打开官方应用」后，卡片不再加载官方身份码页面。
                    officialApp = pddOfficialMode && platform == Platform.PDD,
                )
            }
        }
    }
}
