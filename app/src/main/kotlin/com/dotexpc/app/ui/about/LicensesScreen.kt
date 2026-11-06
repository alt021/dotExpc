package com.dotexpc.app.ui.about

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.unit.dp
import top.yukonga.miuix.kmp.basic.BasicComponent
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.SmallTitle
import top.yukonga.miuix.kmp.basic.TopAppBar
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Back
import top.yukonga.miuix.kmp.icon.extended.ChevronForward
import top.yukonga.miuix.kmp.theme.MiuixTheme

private const val PROJECT_URL = "https://github.com/alt021/dotExpc"
private const val APACHE_LICENSE_URL = "https://www.apache.org/licenses/LICENSE-2.0"

/** 开放源代码许可：本项目的仓库与许可证，以及应用打包的第三方库。每行都可跳到对应仓库。 */
@Composable
fun LicensesScreen(
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            // 和设置页同一层：浅色淡灰、深色纯黑，卡片才浮得出来。
            .background(MiuixTheme.colorScheme.surface),
    ) {
        TopAppBar(
            title = "开放源代码许可",
            navigationIcon = {
                IconButton(onClick = onClose) {
                    Icon(imageVector = MiuixIcons.Back, contentDescription = "返回")
                }
            },
        )
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .navigationBarsPadding()
                .padding(top = 8.dp, bottom = 24.dp),
        ) {
            SmallTitle("当前项目")
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
            ) {
                LicenseRow(
                    title = "dotExpc",
                    summary = "github.com/alt021/dotExpc",
                    url = PROJECT_URL,
                )
                LicenseRow(
                    title = "License",
                    summary = "Apache License 2.0",
                    url = APACHE_LICENSE_URL,
                )
            }
            SmallTitle("开源组件")
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
            ) {
                LicenseRow("AndroidX · Jetpack Compose", url = "https://github.com/androidx/androidx")
                LicenseRow("Kotlin", url = "https://github.com/JetBrains/kotlin")
                LicenseRow("kotlinx.coroutines", url = "https://github.com/Kotlin/kotlinx.coroutines")
                LicenseRow("Miuix", url = "https://github.com/compose-miuix-ui/miuix")
                LicenseRow("AndroidLiquidGlass", url = "https://github.com/Kyant0/AndroidLiquidGlass")
            }
        }
    }
}

/** 设置页同款行：标题 + 注释，点击打开对应仓库或许可证原文。 */
@Composable
private fun LicenseRow(
    title: String,
    url: String,
    summary: String = "Apache License 2.0",
) {
    val uriHandler = LocalUriHandler.current
    BasicComponent(
        title = title,
        summary = summary,
        onClick = { runCatching { uriHandler.openUri(url) } },
        endActions = {
            Icon(
                imageVector = MiuixIcons.ChevronForward,
                contentDescription = null,
                tint = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                modifier = Modifier.size(16.dp),
            )
        },
    )
}
