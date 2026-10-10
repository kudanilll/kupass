package com.nielcode.kupass.ui.screens.home.components

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.AwaitPointerEventScope
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerInputChange
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import kotlin.math.abs

private const val DELETE_WIDTH_FRACTION = 0.35f

/**
 * Observe DOWN without consuming it. Claim only predominantly physical +X at touch slop, before the
 * pager's Main pass. Rejected gestures drain until all pointers lift, never reacquire.
 * AnchoredDraggable cannot do this: disabling a dismiss anchor still claims horizontal drags.
 */
internal fun Modifier.rightwardDeleteGesture(
    accountId: Long,
    onOffsetChange: (Float) -> Unit,
    onDelete: () -> Unit,
): Modifier =
    pointerInput(accountId) {
        awaitEachGesture {
            try {
                val down =
                    awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
                if (!down.isConsumed) {
                    val start = awaitRightwardSlop(down)
                    if (start != null) {
                        val initialOffset =
                            start.position.x - down.position.x - viewConfiguration.touchSlop
                        val releasedOffset =
                            trackRightwardDrag(start, initialOffset, onOffsetChange)
                        if (
                            releasedOffset != null &&
                                releasedOffset >= size.width * DELETE_WIDTH_FRACTION
                        ) {
                            onDelete()
                        }
                    }
                }
            } finally {
                onOffsetChange(0f)
            }
        }
    }

// Guard returns immediately abandon invalid/consumed input without entering another event pass.
@Suppress("ReturnCount")
private suspend fun AwaitPointerEventScope.awaitRightwardSlop(
    down: PointerInputChange
): PointerInputChange? {
    while (true) {
        val event = awaitPointerEvent(PointerEventPass.Initial)
        val change = event.changes.singleOrNull() ?: return null
        if (change.id != down.id || !change.pressed || change.isConsumed) return null
        val distance = change.position - down.position
        // Match Foundation's inclusive boundary before the pager can claim its Main pass.
        if (
            abs(distance.x) >= viewConfiguration.touchSlop ||
                abs(distance.y) >= viewConfiguration.touchSlop
        ) {
            if (distance.x <= 0f || distance.x <= abs(distance.y)) return null
            change.consume()
            return change
        }
        // The pager/list may claim the Main pass even before our directional slop is reached.
        if (awaitPointerEvent(PointerEventPass.Final).changes.any { it.isConsumed }) return null
    }
}

private suspend fun AwaitPointerEventScope.trackRightwardDrag(
    start: PointerInputChange,
    initialOffset: Float,
    onOffsetChange: (Float) -> Unit,
): Float? {
    var distance = initialOffset
    onOffsetChange(distance.coerceIn(0f, size.width.toFloat()))
    while (true) {
        val event = awaitPointerEvent(PointerEventPass.Initial)
        val change = event.changes.singleOrNull()
        if (change == null || change.id != start.id || change.isConsumed) return null
        distance += change.positionChange().x
        // Retain ownership through reversal; only the displayed offset is clamped.
        change.consume()
        if (!change.pressed) return distance.coerceAtLeast(0f)
        onOffsetChange(distance.coerceIn(0f, size.width.toFloat()))
    }
}
