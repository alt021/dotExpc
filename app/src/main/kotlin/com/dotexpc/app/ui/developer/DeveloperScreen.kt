package com.dotexpc.app.ui.developer

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.dotexpc.app.ui.component.AppCard
import com.dotexpc.app.ui.component.AppSectionTitle
import com.dotexpc.app.ui.component.AppSwitch
import com.dotexpc.app.ui.component.AppTopBar
import top.yukonga.miuix.kmp.basic.BasicComponent
import top.yukonga.miuix.kmp.theme.MiuixTheme

/** 开发者选项：连续点击关于页署名 5 次进入，不对外暴露。 */
@Composable
fun DeveloperScreen(
    onClose: () -> Unit,
    endfieldTheme: Boolean,
    onToggleEndfieldTheme: () -> Unit,
    onReopenTerms: () -> Unit,
    onResetPddNotice: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MiuixTheme.colorScheme.surface),
    ) {
        AppTopBar(
            title = "开发者选项",
            en = "DEVELOPER",
            onBack = onClose,
        )
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .navigationBarsPadding()
                .padding(bottom = 24.dp),
        ) {
            AppSectionTitle("外观")
            AppCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
            ) {
                BasicComponent(
                    title = "终末地主题",
                    endActions = {
                        AppSwitch(
                            checked = endfieldTheme,
                            onCheckedChange = { onToggleEndfieldTheme() },
                        )
                    },
                    onClick = onToggleEndfieldTheme,
                )
            }
            AppSectionTitle("应用")
            AppCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
            ) {
                BasicComponent(
                    title = "重新打开使用须知",
                    onClick = onReopenTerms,
                )
                BasicComponent(
                    title = "重置拼多多登录提示",
                    onClick = onResetPddNotice,
                )
            }
        }
    }
}
