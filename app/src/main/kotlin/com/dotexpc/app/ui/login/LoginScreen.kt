package com.dotexpc.app.ui.login

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.dotexpc.app.data.Platform
import com.dotexpc.app.ui.component.AppTextButton
import com.dotexpc.app.ui.component.AppTopBar
import com.dotexpc.app.ui.component.SessionWeb
import com.dotexpc.app.web.PlatformSession
import top.yukonga.miuix.kmp.theme.MiuixTheme

/**
 * 全屏登录页：把官方页面完整交给用户操作。
 * 不使用弹层，避免 Miuix 浮层的 CompositionLocal 约束，也方便官方表单弹窗正常显示。
 */
@Composable
fun LoginScreen(
    platform: Platform,
    session: PlatformSession,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MiuixTheme.colorScheme.background),
    ) {
        AppTopBar(
            title = "登录${platform.title}",
            en = "LOGIN",
            subtitle = "登录在官方页面内完成，应用不保存账号或密码",
            onBack = onClose,
            actions = {
                AppTextButton(label = "完成", onClick = onClose)
            },
        )
        SessionWeb(
            session = session,
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .navigationBarsPadding(),
        )
    }
}
