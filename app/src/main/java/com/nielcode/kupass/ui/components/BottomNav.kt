package com.nielcode.kupass.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Home
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.nielcode.kupass.R

private const val NAV_ANIMATION_MILLIS = 250
private const val ITEM_ANIMATION_MILLIS = 200

/** Floating pill navigation with an "add" button that only shows on the Home tab. */
@Composable
fun BottomNav(
    currentTab: MainTab,
    onTabClick: (MainTab) -> Unit,
    onAddClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val addVisible = currentTab == MainTab.Home
    val offsetX by
        animateDpAsState(
            // Shift the pill left to make room for the add button next to it.
            targetValue = if (addVisible) (-12).dp else 0.dp,
            animationSpec = tween(NAV_ANIMATION_MILLIS, easing = LinearEasing),
            label = "nav_offset",
        )

    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        TabPill(
            currentTab = currentTab,
            onTabClick = onTabClick,
            modifier = Modifier.offset { IntOffset(offsetX.roundToPx(), 0) },
        )
        AnimatedVisibility(
            visible = addVisible,
            enter =
                slideInHorizontally(tween(NAV_ANIMATION_MILLIS, easing = LinearEasing)) { it / 2 } +
                    fadeIn(tween(NAV_ANIMATION_MILLIS, easing = LinearEasing)),
            exit =
                slideOutHorizontally(tween(NAV_ANIMATION_MILLIS, easing = LinearEasing)) {
                    it / 2
                } + fadeOut(tween(NAV_ANIMATION_MILLIS, easing = LinearEasing)),
        ) {
            AddButton(onClick = onAddClick)
        }
    }
}

@Composable
private fun TabPill(
    currentTab: MainTab,
    onTabClick: (MainTab) -> Unit,
    modifier: Modifier = Modifier,
) {
    val background =
        if (isSystemInDarkTheme()) MaterialTheme.colorScheme.surfaceContainer
        else MaterialTheme.colorScheme.surfaceVariant
    Row(
        modifier = modifier.clip(RoundedCornerShape(50)).background(background).padding(8.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        MainTab.entries.forEach { tab ->
            NavItem(
                title = stringResource(tab.title),
                icon = tab.icon,
                isSelected = tab == currentTab,
                onClick = { onTabClick(tab) },
            )
        }
    }
}

@Composable
private fun AddButton(onClick: () -> Unit, modifier: Modifier = Modifier) {
    Surface(
        modifier =
            modifier
                .size(56.dp)
                .clip(RoundedCornerShape(16.dp))
                .clickable(role = Role.Button, onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.secondary,
        contentColor = MaterialTheme.colorScheme.onSecondary,
        tonalElevation = 4.dp,
    ) {
        Icon(
            imageVector = Icons.Default.Add,
            contentDescription = stringResource(R.string.fab_open),
            modifier = Modifier.padding(16.dp),
        )
    }
}

@Composable
private fun NavItem(
    title: String,
    icon: ImageVector,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val scale by
        animateFloatAsState(
            targetValue = if (isSelected) 1.1f else 1.0f,
            animationSpec = tween(ITEM_ANIMATION_MILLIS, easing = LinearEasing),
            label = "nav_item_scale",
        )
    val alpha by
        animateFloatAsState(
            targetValue = if (isSelected) 1.0f else 0.6f,
            animationSpec = tween(ITEM_ANIMATION_MILLIS, easing = LinearEasing),
            label = "nav_item_alpha",
        )

    Row(
        modifier =
            modifier
                .clip(RoundedCornerShape(50))
                .background(
                    if (isSelected) MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.2f)
                    else Color.Transparent
                )
                .clickable(onClick = onClick)
                .padding(horizontal = 12.dp, vertical = 8.dp)
                .animateContentSize(tween(ITEM_ANIMATION_MILLIS, easing = LinearEasing))
                .graphicsLayer {
                    scaleX = scale
                    scaleY = scale
                    this.alpha = alpha
                },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = title,
            modifier = Modifier.size(24.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = alpha),
        )
        if (isSelected) {
            Text(
                text = title,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.labelLarge,
                modifier = Modifier.padding(horizontal = 8.dp),
            )
        }
    }
}
