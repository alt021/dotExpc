package com.dotexpc.app.ui.identity

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.view.View
import android.widget.FrameLayout
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.view.WindowCompat
import com.dotexpc.app.web.PlatformSession
import com.dotexpc.app.ui.component.AppIcons
import top.yukonga.miuix.kmp.basic.Icon

/**
 * 身份码全屏页：把会话里的 WebView 挂到横屏尺寸的容器里再整体旋转 90°，
 * 屏幕方向仍是竖屏；退出时 WebView 自动还回身份码卡片，登录态不变。
 */
@Composable
fun IdentityFullscreenScreen(
    session: PlatformSession,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val view = LocalView.current

    // 页面是纯白的，系统栏保持显示，这里只把图标切成深色，退出时还原。
    DisposableEffect(Unit) {
        val controller = view.context.findActivity()?.window?.let {
            WindowCompat.getInsetsController(it, view)
        }
        val lightStatus = controller?.isAppearanceLightStatusBars
        val lightNav = controller?.isAppearanceLightNavigationBars
        controller?.isAppearanceLightStatusBars = true
        controller?.isAppearanceLightNavigationBars = true
        onDispose {
            if (controller != null && lightStatus != null) {
                controller.isAppearanceLightStatusBars = lightStatus
            }
            if (controller != null && lightNav != null) {
                controller.isAppearanceLightNavigationBars = lightNav
            }
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.White),
    ) {
        // 内容和关闭按钮都躲开状态栏与导航栏，白色底衬仍铺到屏幕边缘。
        Box(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.systemBars),
        ) {
            AndroidView(
                factory = { context ->
                    RotatedFrameLayout(context).also { frame ->
                        session.attach(frame)
                        session.evaluateJavascript("window.DexpMode&&DexpMode.fullscreen(true)")
                    }
                },
                onRelease = { frame ->
                    session.evaluateJavascript("window.DexpMode&&DexpMode.fullscreen(false)")
                    session.detach(frame)
                },
                modifier = Modifier.fillMaxSize(),
            )

            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(16.dp)
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(Color.Black.copy(alpha = 0.45f))
                    .clickable(onClick = onClose),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = AppIcons.Close,
                    contentDescription = "退出全屏",
                    tint = Color.White,
                )
            }
        }
    }
}

/**
 * 全屏容器本身按竖屏铺满，唯一的子 View 按「宽 = 屏高、高 = 屏宽」布局后旋转 90°，
 * 旋转以子 View 中心为轴，正好铺满竖屏。View 自带的旋转会一并映射触摸坐标。
 */
private class RotatedFrameLayout(context: Context) : FrameLayout(context) {

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val width = MeasureSpec.getSize(widthMeasureSpec)
        val height = MeasureSpec.getSize(heightMeasureSpec)
        getChildAt(0)?.measure(
            MeasureSpec.makeMeasureSpec(height, MeasureSpec.EXACTLY),
            MeasureSpec.makeMeasureSpec(width, MeasureSpec.EXACTLY),
        )
        setMeasuredDimension(width, height)
    }

    override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
        val child: View = getChildAt(0) ?: return
        val width = right - left
        val height = bottom - top
        child.rotation = 90f
        val childLeft = (width - child.measuredWidth) / 2
        val childTop = (height - child.measuredHeight) / 2
        child.layout(
            childLeft,
            childTop,
            childLeft + child.measuredWidth,
            childTop + child.measuredHeight,
        )
    }

    /** 旋转写在子 View 自己身上，换回卡片容器前必须还原，否则卡片内容也会是横的。 */
    override fun onViewRemoved(child: View) {
        super.onViewRemoved(child)
        child.rotation = 0f
    }
}

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}
