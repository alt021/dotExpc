package com.dotexpc.app.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dotexpc.app.data.Platform
import top.yukonga.miuix.kmp.basic.Text

val Platform.accent: Color
    get() = when (this) {
        Platform.TAOBAO -> Color(0xFFFF5000)
        Platform.PDD -> Color(0xFFE02E24)
    }

val Platform.initial: String
    get() = when (this) {
        Platform.TAOBAO -> "淘"
        Platform.PDD -> "拼"
    }

@Composable
fun PlatformAvatar(platform: Platform, size: Int = 40) {
    Box(
        modifier = Modifier
            .size(size.dp)
            .clip(CircleShape)
            .background(platform.accent.copy(alpha = 0.12f)),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = platform.initial,
            color = platform.accent,
            fontSize = (size * 0.42f).sp,
        )
    }
}
