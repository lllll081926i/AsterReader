package com.example.ui.components

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChanged

/** 观察中央点击，不抢占列表拖动；抬手即响应，不等待双击超时。 */
@Composable
internal fun Modifier.readerCenterTap(onTap: () -> Unit): Modifier {
    val tap by rememberUpdatedState(onTap)
    return pointerInput(Unit) {
        awaitEachGesture {
            val down = awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
            if (down.position.x !in size.width * 0.25f..size.width * 0.75f) return@awaitEachGesture
            var eligible = true
            do {
                // 系统取消会合成已消费的抬手；在子节点处理前识别，避免误切换。
                val initial = awaitPointerEvent(PointerEventPass.Initial)
                if (initial.changes.any { !it.pressed && it.isConsumed }) eligible = false
                val event = awaitPointerEvent(PointerEventPass.Final)
                val pointer = event.changes.firstOrNull { it.id == down.id } ?: break
                if (event.changes.size != 1 || (pointer.position - down.position).getDistance() > viewConfiguration.touchSlop ||
                    event.changes.any { it.isConsumed && it.positionChanged() }) eligible = false
                if (!pointer.pressed) {
                    if (eligible && pointer.previousPressed &&
                        pointer.uptimeMillis - down.uptimeMillis < viewConfiguration.longPressTimeoutMillis) tap()
                    break
                }
            } while (event.changes.any { it.pressed })
        }
    }
}
