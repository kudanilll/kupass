package com.nielcode.kupass.ui.screens.home.components

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.unit.dp
import com.nielcode.kupass.R

/** Title and subtitle widths per placeholder row, so the rows don't look identical. */
private val RowWidths =
    listOf(0.55f to 0.35f, 0.4f to 0.5f, 0.65f to 0.3f, 0.5f to 0.45f, 0.6f to 0.4f, 0.45f to 0.55f)

/**
 * Placeholder rows shaped like [PasswordListItem], shown while the vault is read. Screen readers
 * hear one "loading" label instead of empty rows.
 */
@Composable
fun VaultListSkeleton(modifier: Modifier = Modifier) {
    val description = stringResource(R.string.vault_loading)
    val pulse by
        rememberInfiniteTransition(label = "vaultSkeleton")
            .animateFloat(
                initialValue = 1f,
                targetValue = 0.45f,
                animationSpec = infiniteRepeatable(tween(durationMillis = 800), RepeatMode.Reverse),
                label = "pulse",
            )
    val color = MaterialTheme.colorScheme.surfaceContainerHighest
    Column(
        modifier =
            modifier
                .fillMaxWidth()
                .clearAndSetSemantics { contentDescription = description }
                // Read in the draw phase only: the pulse never recomposes the rows.
                .graphicsLayer { alpha = pulse }
    ) {
        RowWidths.forEach { (title, subtitle) -> SkeletonRow(title, subtitle, color) }
    }
}

@Composable
private fun SkeletonRow(titleWidth: Float, subtitleWidth: Float, color: Color) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp).padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(48.dp).background(color, CircleShape))
        Column(
            modifier = Modifier.weight(1f).padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Bar(titleWidth, height = 16, color)
            Bar(subtitleWidth, height = 12, color)
        }
    }
}

@Composable
private fun Bar(widthFraction: Float, height: Int, color: Color) {
    Box(
        Modifier.fillMaxWidth(widthFraction)
            .height(height.dp)
            .background(color, RoundedCornerShape(percent = 50))
    )
}
