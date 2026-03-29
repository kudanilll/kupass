package com.nielcode.kupass.ui.screens.home.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Composable
fun BottomNav(currentRoute: String, onNavigate: (String) -> Unit) {
    Card(
        shape = RoundedCornerShape(20.dp),
        colors =
            CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        modifier =
            Modifier.fillMaxWidth()
                .height(64.dp)
                .padding(start = 16.dp, end = 88.dp, bottom = 24.dp),
    ) {
        NavigationBar(containerColor = Color.Transparent, tonalElevation = 0.dp) {
            NavigationBarItem(
                selected = currentRoute == "home",
                onClick = { onNavigate("home") },
                icon = { Icon(Icons.Default.Home, contentDescription = "Home") },
            )
            NavigationBarItem(
                selected = currentRoute == "settings",
                onClick = { onNavigate("settings") },
                icon = { Icon(Icons.Default.Settings, contentDescription = "Settings") },
            )
        }
    }
}
