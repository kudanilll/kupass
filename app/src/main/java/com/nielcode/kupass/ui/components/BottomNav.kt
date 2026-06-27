package com.nielcode.kupass.ui.components

import android.annotation.SuppressLint
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ExperimentalAnimationApi
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
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Storage
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.nielcode.kupass.R

@OptIn(ExperimentalAnimationApi::class)
@SuppressLint("ModifierParameter", "UseOfNonLambdaOffsetOverload")
@Composable
fun BottomNav(
    currentRoute: String,
    onNavigate: (String) -> Unit,
    onAddClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isDarkMode = isSystemInDarkTheme()
    val fabVisible = !(currentRoute == "settings" || currentRoute == "data")
    val spacing = 12.dp

    val offsetX by
        animateDpAsState(
            targetValue = if (fabVisible) -(spacing / 2 + spacing / 2) else 0.dp,
            animationSpec = tween(durationMillis = 250, easing = LinearEasing),
            label = "nav_offset"
        )

    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            modifier =
                Modifier.offset(x = offsetX)
                    .clip(RoundedCornerShape(50))
                    .background(
                        if (isDarkMode) MaterialTheme.colorScheme.surfaceContainer
                        else MaterialTheme.colorScheme.surfaceVariant
                    )
                    .padding(8.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            NavItem(
                title = "Home",
                icon = Icons.Default.Home,
                isSelected = currentRoute == "home",
                onClick = { onNavigate("home") }
            )

            NavItem(
                title = "Data",
                icon = Icons.Default.Storage,
                isSelected = currentRoute == "data",
                onClick = { onNavigate("data") }
            )

            NavItem(
                title = "Settings",
                icon = Icons.Default.Settings,
                isSelected = currentRoute == "settings",
                onClick = { onNavigate("settings") }
            )
        }

        AnimatedVisibility(
            visible = fabVisible,
            enter =
                slideInHorizontally(
                    initialOffsetX = { fullWidth -> fullWidth / 2 },
                    animationSpec = tween(durationMillis = 250, easing = LinearEasing)
                ) + fadeIn(animationSpec = tween(durationMillis = 250, easing = LinearEasing)),
            exit =
                slideOutHorizontally(
                    targetOffsetX = { fullWidth -> fullWidth / 2 },
                    animationSpec = tween(durationMillis = 250, easing = LinearEasing)
                ) + fadeOut(animationSpec = tween(durationMillis = 250, easing = LinearEasing))
        ) {
            Surface(
                modifier =
                    Modifier.size(56.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .clickable(role = Role.Button, onClick = onAddClick),
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.secondary,
                contentColor = MaterialTheme.colorScheme.onSecondary,
                tonalElevation = 4.dp
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = stringResource(R.string.fab_open),
                    modifier = Modifier.padding(16.dp)
                )
            }
        }
    }
}

@Composable
private fun NavItem(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val targetScale = if (isSelected) 1.1f else 1.0f
    val scale by
        animateFloatAsState(
            targetValue = targetScale,
            animationSpec = tween(durationMillis = 200, easing = LinearEasing)
        )

    val targetAlpha = if (isSelected) 1.0f else 0.6f
    val alpha by
        animateFloatAsState(
            targetValue = targetAlpha,
            animationSpec = tween(durationMillis = 200, easing = LinearEasing)
        )

    Row(
        modifier =
            Modifier.clip(RoundedCornerShape(50))
                .background(
                    if (isSelected) MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.2f)
                    else Color.Transparent
                )
                .clickable(onClick = onClick)
                .padding(horizontal = 12.dp, vertical = 8.dp)
                .animateContentSize(
                    animationSpec = tween(durationMillis = 200, easing = LinearEasing)
                )
                .graphicsLayer {
                    this.scaleX = scale
                    this.scaleY = scale
                    this.alpha = alpha
                },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = title,
            modifier = Modifier.size(24.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = alpha)
        )
        if (isSelected) {
            Text(
                text = title,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.labelLarge,
                modifier = Modifier.padding(horizontal = 8.dp)
            )
        }
    }
}
