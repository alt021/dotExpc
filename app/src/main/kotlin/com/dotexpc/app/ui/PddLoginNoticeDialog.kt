package com.dotexpc.app.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.dotexpc.app.ui.component.AppButton
import com.dotexpc.app.ui.component.AppButtonKind
import com.dotexpc.app.ui.component.AppDialog
import com.dotexpc.app.ui.component.AppTextButton
import kotlinx.coroutines.delay

private const val COUNTDOWN_SECONDS = 5

/** 应用内登录拼多多的首次提示：顶号限制，继续按钮倒计时结束后才可点。 */
@Composable
fun PddLoginNoticeDialog(
    show: Boolean,
    onCancel: () -> Unit,
    onContinue: () -> Unit,
) {
    var remaining by remember(show) { mutableIntStateOf(COUNTDOWN_SECONDS) }

    LaunchedEffect(show) {
        if (!show) return@LaunchedEffect
        while (remaining > 0) {
            delay(1000)
            remaining--
        }
    }

    AppDialog(
        show = show,
        title = "登录提示",
        summary = "受拼多多限制，同一账号仅可同时登录一台设备。若遇到顶号问题，请使用“打开官方应用”模式",
        onDismissRequest = onCancel,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            AppTextButton(
                label = "取消",
                onClick = onCancel,
                modifier = Modifier.weight(1f),
            )
            AppButton(
                label = if (remaining > 0) "继续（${remaining}s）" else "继续",
                onClick = onContinue,
                enabled = remaining <= 0,
                modifier = Modifier.weight(1f),
                kind = AppButtonKind.Primary,
            )
        }
    }
}
