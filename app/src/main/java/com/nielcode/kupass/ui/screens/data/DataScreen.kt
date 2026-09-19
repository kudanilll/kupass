package com.nielcode.kupass.ui.screens.data

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.nielcode.kupass.R
import com.nielcode.kupass.ui.components.SectionHeader
import com.nielcode.kupass.ui.components.SectionItem
import com.nielcode.kupass.ui.screens.pagerPageInsets

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DataScreen(
    onExportClick: () -> Unit,
    onImportClick: () -> Unit,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(),
) {
    Scaffold(modifier = modifier.fillMaxSize(), contentWindowInsets = pagerPageInsets()) {
        innerPadding ->
        Column(
            modifier =
                Modifier.fillMaxSize()
                    .padding(innerPadding)
                    .verticalScroll(rememberScrollState())
                    .padding(top = 16.dp, bottom = 16.dp + contentPadding.calculateBottomPadding())
        ) {
            SectionHeader(title = stringResource(R.string.data_export_import_title))
            SectionItem(
                icon = Icons.Default.FileUpload,
                title = stringResource(R.string.nav_export),
                subtitle = stringResource(R.string.data_export_subtitle),
                onClick = onExportClick,
            )

            SectionItem(
                icon = Icons.Default.FileDownload,
                title = stringResource(R.string.nav_import),
                subtitle = stringResource(R.string.data_import_subtitle),
                onClick = onImportClick,
            )
        }
    }
}
