package com.nielcode.kupass.ui.screens.data

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.nielcode.kupass.R
import com.nielcode.kupass.ui.screens.settings.components.SettingItem

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DataScreen(
    onExportClick: () -> Unit,
    onImportClick: () -> Unit
) {
    Scaffold { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(vertical = 16.dp)
        ) {
            Text(
                text = "Export & Import",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 24.dp, bottom = 4.dp)
            )
            SettingItem(
                icon = Icons.Default.FileUpload,
                title = stringResource(R.string.nav_export),
                subtitle = "Backup passwords to a JSON file",
                onClick = onExportClick
            )

            SettingItem(
                icon = Icons.Default.FileDownload,
                title = stringResource(R.string.nav_import),
                subtitle = "Restore passwords from a JSON backup",
                onClick = onImportClick
            )
        }
    }
}
