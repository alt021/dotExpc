package com.dotexpc.app.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CutCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dotexpc.app.ui.theme.Ef
import com.dotexpc.app.ui.theme.EfType
import com.dotexpc.app.ui.theme.LocalEfColors
import top.yukonga.miuix.kmp.basic.Text

/**
 * 终末地底栏：白底 + 顶边细线，每格是「英文小字 + 中文」。
 * 当前项是一整块右下切角的信号黄，上面压墨色字；切换直接硬切，不做过渡动画。
 */
@Composable
fun EndfieldTabBar(
    selected: AppTab,
    onSelect: (AppTab) -> Unit,
    modifier: Modifier = Modifier,
) {
    val c = LocalEfColors.current
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(c.panel)
            .drawBehind {
                drawLine(
                    color = c.rule,
                    start = Offset(0f, 0f),
                    end = Offset(size.width, 0f),
                    strokeWidth = Ef.hairline.toPx(),
                )
            }
            .navigationBarsPadding()
            .padding(horizontal = 8.dp, vertical = 8.dp),
    ) {
        AppTab.entries.forEach { tab ->
            val current = tab == selected
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 3.dp)
                    .height(52.dp)
                    .clip(CutCornerShape(bottomEnd = Ef.cutSmall))
                    .background(if (current) c.signal else Color.Transparent)
                    .selectable(
                        selected = current,
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        role = Role.Tab,
                        onClick = { onSelect(tab) },
                    ),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text = tab.en,
                    style = EfType.Eyebrow.copy(fontSize = 9.sp),
                    color = if (current) c.onSignal else c.ink2,
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = tab.label,
                    style = TextStyle(
                        fontFamily = EfType.label,
                        fontSize = 14.sp,
                        fontWeight = if (current) FontWeight.Bold else FontWeight.Medium,
                    ),
                    color = if (current) c.onSignal else c.ink,
                )
            }
        }
    }
}
