package com.nielcode.kupass.ui.components

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.rememberScrollableState
import androidx.compose.foundation.rememberOverscrollEffect
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.scrollableArea
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/** Native overscroll also needs a gesture receiver when the content fits its viewport. */
@Composable
internal fun Modifier.pageVerticalScroll(state: ScrollState = rememberScrollState()): Modifier {
    val scrolls = state.maxValue > 0
    val effect = rememberOverscrollEffect()
    val stretch = rememberScrollableState { 0f }
    return scrollableArea(
            state = stretch,
            orientation = Orientation.Vertical,
            enabled = !scrolls,
            overscrollEffect = if (scrolls) null else effect,
        )
        .verticalScroll(
            state = state,
            enabled = scrolls,
            overscrollEffect = if (scrolls) effect else null,
        )
}

@Composable
internal fun Modifier.stretchWhenContentFits(scrolls: Boolean): Modifier {
    val effect = rememberOverscrollEffect()
    val stretch = rememberScrollableState { 0f }
    return scrollableArea(
        state = stretch,
        orientation = Orientation.Vertical,
        enabled = !scrolls,
        overscrollEffect = if (scrolls) null else effect,
    )
}
