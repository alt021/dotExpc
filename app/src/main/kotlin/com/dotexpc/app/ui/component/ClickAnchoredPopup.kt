package com.dotexpc.app.ui.component

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.FloatState
import androidx.compose.runtime.MutableFloatState
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import top.yukonga.miuix.kmp.basic.PopupPositionProvider

/** 在 Initial 阶段旁听按下事件，只记录位置，不消费点击。 */
fun Modifier.trackPressX(target: MutableFloatState): Modifier = pointerInput(target) {
    awaitPointerEventScope {
        while (true) {
            val change = awaitPointerEvent(PointerEventPass.Initial).changes.firstOrNull()
            if (change != null && change.pressed && !change.previousPressed) {
                target.floatValue = change.position.x
            }
        }
    }
}

/**
 * 让弹层落在按下点附近：水平方向以点击位置为中心，
 * 垂直方向优先贴在行下方，不够放时翻到行上方，两边都放不下才居中；
 * 无论怎样都至少与屏幕边缘保留 [EDGE_MARGIN_PX] 像素。
 */
class ClickAnchoredPositionProvider(
    private val clickX: FloatState,
) : PopupPositionProvider {

    override fun calculatePosition(
        anchorBounds: IntRect,
        windowBounds: IntRect,
        layoutDirection: LayoutDirection,
        popupContentSize: IntSize,
        popupMargin: IntRect,
        alignment: PopupPositionProvider.Align,
    ): IntOffset {
        val minLeft = windowBounds.left + EDGE_MARGIN_PX
        val maxLeft = (windowBounds.right - popupContentSize.width - EDGE_MARGIN_PX)
            .coerceAtLeast(minLeft)
        val centeredLeft = anchorBounds.left + clickX.floatValue.toInt() - popupContentSize.width / 2
        val left = centeredLeft.coerceIn(minLeft, maxLeft)

        val minTop = windowBounds.top + EDGE_MARGIN_PX
        val maxTop = (windowBounds.bottom - popupContentSize.height - EDGE_MARGIN_PX)
            .coerceAtLeast(minTop)
        val below = anchorBounds.bottom + popupMargin.bottom
        val above = anchorBounds.top - popupContentSize.height - popupMargin.top
        val top = when {
            below <= maxTop -> below
            above >= minTop -> above
            else -> anchorBounds.top + anchorBounds.height / 2 - popupContentSize.height / 2
        }.coerceIn(minTop, maxTop)
        return IntOffset(left, top)
    }

    override fun getMargins(): PaddingValues = PaddingValues(vertical = 8.dp)

    private companion object {
        /** 弹层与窗口任意一侧的最小距离（像素）。 */
        const val EDGE_MARGIN_PX = 20
    }
}
