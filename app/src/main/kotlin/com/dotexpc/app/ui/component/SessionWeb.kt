package com.dotexpc.app.ui.component

import android.widget.FrameLayout
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import com.dotexpc.app.web.PlatformSession

/**
 * 把平台会话的 WebView 挂到当前容器的布局里。
 * 容器销毁时 detach，新容器再 attach —— WebView 本身不销毁，登录态与页面都保留。
 */
@Composable
fun SessionWeb(
    session: PlatformSession,
    modifier: Modifier = Modifier,
) {
    AndroidView(
        factory = { context ->
            FrameLayout(context).also { frame ->
                session.attach(frame)
            }
        },
        onRelease = { frame -> session.detach(frame) },
        modifier = modifier,
    )
}
