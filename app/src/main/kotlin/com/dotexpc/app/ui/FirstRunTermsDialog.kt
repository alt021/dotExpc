package com.dotexpc.app.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import top.yukonga.miuix.kmp.basic.Button
import top.yukonga.miuix.kmp.basic.ButtonDefaults
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.overlay.OverlayDialog
import top.yukonga.miuix.kmp.theme.MiuixTheme

private const val COUNTDOWN_SECONDS = 10

private val TERMS = listOf(
    "1. 本软件为免费开源软件，您可以免费使用所有功能并在 Apache License 的许可范围内使用源代码",
    "2. 我们尊重您的隐私，您的登录数据和快递信息不会上传到任何第三方 API 端点，您的所有数据只在您的设备和您选择的服务商之间传输",
    "3. 本软件为第三方自制软件，与淘宝、拼多多官方无任何关联或从属关系",
    "4. 本软件仅为您提供官方页面和站点的快捷方式和网页数据匹配功能，不涉及任何对于商业软件接口和数据源的破解和利用",
    "5. 本软件使用了 GitHub 的代码托管和发行能力，软件内附带的检查更新会连接至 GitHub 获取数据。如果您有安全顾虑请勿使用相关功能",
)

/** 首次启动的使用须知：正文可滚动，倒计时结束后才能同意；未同意则每次启动都弹出。 */
@Composable
fun FirstRunTermsDialog(
    show: Boolean,
    onAccept: () -> Unit,
) {
    var remaining by remember(show) { mutableIntStateOf(COUNTDOWN_SECONDS) }

    LaunchedEffect(show) {
        if (!show) return@LaunchedEffect
        while (remaining > 0) {
            delay(1000)
            remaining--
        }
    }

    OverlayDialog(
        show = show,
        title = "使用须知",
        // 只能通过「同意并继续」关闭：点外部和返回键都不生效。
        onDismissRequest = null,
        content = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp),
            ) {
                TERMS.forEach { line ->
                    Text(
                        text = line,
                        fontSize = MiuixTheme.textStyles.body1.fontSize,
                        color = MiuixTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(bottom = 12.dp),
                    )
                }
            }
            Button(
                onClick = onAccept,
                enabled = remaining <= 0,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColorsPrimary(),
            ) {
                Text(
                    text = if (remaining > 0) "同意并继续（${remaining}s）" else "同意并继续",
                )
            }
        },
    )
}
