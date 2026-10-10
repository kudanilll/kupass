package com.nielcode.kupass.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.dropShadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.shadow.Shadow
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import com.nielcode.kupass.R
import com.nielcode.kupass.ui.theme.KupassTheme

/** Spring used by every bottom navigation animation, so they feel consistent and interruptible. */
internal fun <T> navSpring() =
    spring<T>(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessMediumLow)

/** Height the floating navigation occupies (FAB plus vertical padding); pages pad by this. */
val BottomNavHeight = 80.dp

private val NavShadow =
    Shadow(
        radius = 8.dp,
        spread = 1.dp,
        color = Color.Black.copy(alpha = 0.18f),
        offset = DpOffset.Zero,
    )
private val PillToAddSpacing = 12.dp

/** Floating pill navigation with an "add" button that only shows on the Home tab. */
@Composable
fun BottomNav(
    currentTab: MainTab,
    onTabClick: (MainTab) -> Unit,
    onAddClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val addVisible = currentTab == MainTab.Home

    Row(
        modifier = modifier.fillMaxWidth().padding(vertical = 12.dp),
        horizontalArrangement =
            Arrangement.spacedBy(PillToAddSpacing, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        TabPill(
            currentTab = currentTab,
            onTabClick = onTabClick,
        )
        AnimatedVisibility(
            visible = addVisible,
            enter = scaleIn(navSpring()) + fadeIn(navSpring()),
            exit = scaleOut(navSpring()) + fadeOut(navSpring()),
        ) {
            FloatingActionButton(
                onClick = onAddClick,
                shape = RoundedCornerShape(18.dp),
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.dropShadow(RoundedCornerShape(18.dp), NavShadow),
                elevation =
                    FloatingActionButtonDefaults.elevation(
                        defaultElevation = 0.dp,
                        pressedElevation = 0.dp,
                        focusedElevation = 0.dp,
                        hoveredElevation = 0.dp,
                    ),
            ) {
                Icon(Icons.Default.Add, contentDescription = stringResource(R.string.fab_open))
            }
        }
    }
}

@Composable
private fun TabPill(
    currentTab: MainTab,
    onTabClick: (MainTab) -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.dropShadow(CircleShape, NavShadow),
        shape = CircleShape,
        color = MaterialTheme.colorScheme.surfaceContainer,
        shadowElevation = 0.dp,
    ) {
        Row(
            modifier = Modifier.padding(6.dp).selectableGroup(),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            MainTab.entries.forEach { tab ->
                NavItem(
                    title = stringResource(tab.title),
                    icon = tab.icon,
                    selected = tab == currentTab,
                    onClick = { onTabClick(tab) },
                )
            }
        }
    }
}

/** Shared scrim for content scrolling behind the floating navigation. */
@Composable
fun BottomFade(modifier: Modifier = Modifier) {
    val surface = MaterialTheme.colorScheme.surface
    Box(
        modifier =
            modifier
                .fillMaxWidth()
                .height(BottomNavHeight)
                .background(
                    Brush.verticalGradient(
                        0f to Color.Transparent,
                        0.6f to surface.copy(alpha = 0.7f),
                        1f to surface,
                    )
                )
    )
}

@Composable
private fun NavItem(
    title: String,
    icon: ImageVector,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.colorScheme
    val background by
        animateColorAsState(
            targetValue = if (selected) colors.secondaryContainer else Color.Transparent,
            animationSpec = navSpring(),
            label = "nav_item_background",
        )
    val content by
        animateColorAsState(
            targetValue = if (selected) colors.onSecondaryContainer else colors.onSurfaceVariant,
            animationSpec = navSpring(),
            label = "nav_item_content",
        )

    Row(
        modifier =
            modifier
                .clip(CircleShape)
                .background(background)
                .selectable(selected = selected, onClick = onClick, role = Role.Tab)
                .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = title,
            modifier = Modifier.size(24.dp),
            tint = content,
        )
        AnimatedVisibility(
            visible = selected,
            enter = expandHorizontally(navSpring()) + fadeIn(navSpring()),
            exit = shrinkHorizontally(navSpring()) + fadeOut(navSpring()),
        ) {
            Text(
                text = title,
                color = content,
                style = MaterialTheme.typography.labelLarge,
                maxLines = 1,
                modifier = Modifier.padding(start = 8.dp),
            )
        }
    }
}

@Preview
@Composable
private fun BottomNavPreview() {
    KupassTheme(dynamicColor = false) {
        BottomNav(currentTab = MainTab.Home, onTabClick = {}, onAddClick = {})
    }
}
