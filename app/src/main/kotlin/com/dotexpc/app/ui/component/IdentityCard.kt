package com.dotexpc.app.ui.component

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.dotexpc.app.data.Platform
import com.dotexpc.app.web.PlatformSession
import com.dotexpc.app.web.openPddIdentity
import top.yukonga.miuix.kmp.basic.Button
import top.yukonga.miuix.kmp.basic.ButtonDefaults
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.theme.MiuixTheme

private val ContentHeight = 330.dp
private val ContentShape = RoundedCornerShape(bottomStart = 22.dp, bottomEnd = 22.dp)

/**
 * 身份码卡片：卡头常驻，展开后才挂载官方页面。
 * 收起时 WebView 会被 detach，但实例与登录态保留，再次展开无需重新登录。
 */
@Composable
fun IdentityCard(
    platform: Platform,
    session: PlatformSession,
    expanded: Boolean,
    onToggle: () -> Unit,
    onLogin: () -> Unit,
    onFullscreen: () -> Unit,
    officialApp: Boolean,
    modifier: Modifier = Modifier,
) {
    val state = session.state
    val colors = MiuixTheme.colorScheme
    val context = LocalContext.current

    Card(
        modifier = modifier.fillMaxWidth(),
        cornerRadius = 22.dp,
        insideMargin = PaddingValues(0.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onToggle)
                .padding(start = 16.dp, end = 8.dp, top = 12.dp, bottom = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            PlatformAvatar(platform, size = 38)
            Text(
                text = platform.title,
                style = MiuixTheme.textStyles.main,
                modifier = Modifier.weight(1f),
            )
            if (!officialApp) {
                IconButton(onClick = { session.refresh() }) {
                    Icon(
                        imageVector = AppIcons.Refresh,
                        contentDescription = "刷新",
                        tint = colors.onSurface,
                    )
                }
                // 只有身份码已经出现才提供全屏，避免打开空白横屏页。
                if (state.bound && state.identityReady && !state.isLoginRequired) {
                    IconButton(onClick = onFullscreen) {
                        Icon(
                            imageVector = AppIcons.FullScreen,
                            contentDescription = "全屏显示",
                            tint = colors.onSurface,
                        )
                    }
                }
            }
            IconButton(onClick = onToggle) {
                Icon(
                    imageVector = if (expanded) AppIcons.ExpandLess else AppIcons.ExpandMore,
                    contentDescription = if (expanded) "收起" else "展开",
                    tint = colors.onSurfaceVariantSummary,
                )
            }
        }

        AnimatedVisibility(
            visible = expanded,
            enter = expandVertically(animationSpec = tween(durationMillis = 220)),
            exit = shrinkVertically(animationSpec = tween(durationMillis = 180)),
        ) {
            Column {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .height(0.5.dp)
                        .background(colors.dividerLine.copy(alpha = 0.5f)),
                )
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(ContentHeight)
                        .clip(ContentShape)
                        .background(Color.White),
                ) {
                    if (officialApp) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(24.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            Button(
                                onClick = { openPddIdentity(context) },
                                colors = ButtonDefaults.buttonColors(
                                    color = Color(0xFFF0F0F0),
                                    contentColor = Color(0xFF666666),
                                ),
                            ) {
                                Text(text = "点此前往拼多多")
                            }
                        }
                    } else {
                        SessionWeb(
                            session = session,
                            modifier = Modifier.fillMaxSize(),
                        )

                        if (!state.bound && state.isLoading) {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center,
                            ) {
                                Text(
                                    text = "正在加载官方页面…",
                                    style = MiuixTheme.textStyles.footnote1,
                                    color = colors.onSurfaceVariantSummary,
                                )
                            }
                        }

                        when {
                            state.phase == "error" -> CardCover(
                                title = "页面没有加载成功",
                                action = "重试",
                                onAction = { session.refresh() },
                            )

                            state.isLoginRequired -> CardCover(
                                title = "登录${platform.title}后显示",
                                action = "去登录",
                                onAction = onLogin,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CardCover(
    title: String,
    action: String,
    onAction: () -> Unit,
) {
    val colors = MiuixTheme.colorScheme
    val interaction = remember { MutableInteractionSource() }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.surface.copy(alpha = 0.96f))
            .clickable(interactionSource = interaction, indication = null) { }
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = title,
            style = MiuixTheme.textStyles.title3,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(16.dp))
        Button(onClick = onAction) {
            Text(text = action)
        }
    }
}
