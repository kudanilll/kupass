package com.nielcode.kupass.ui.screens.detail

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.nielcode.kupass.R
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PasswordDetailScreen(
    passwordId: Long,
    onNavigateBack: () -> Unit,
    onNavigateToEdit: (Long) -> Unit,
    viewModel: PasswordDetailViewModel = viewModel()
) {
    val context = LocalContext.current
    val deleteSuccessText = stringResource(R.string.toast_success_delete)
    val password by viewModel.password.collectAsState()
    val deleteState by viewModel.deleteState.collectAsState()

    var showDeleteDialog by remember { mutableStateOf(false) }
    var passwordVisible by remember { mutableStateOf(false) }

    // Load password on first composition
    LaunchedEffect(passwordId) { viewModel.loadPassword(passwordId) }

    // Navigate back on successful delete
    LaunchedEffect(deleteState) {
        if (deleteState is DeleteState.Success) {
            viewModel.resetDeleteState()
            Toast.makeText(context, deleteSuccessText, Toast.LENGTH_SHORT).show()
            onNavigateBack()
        }
    }

    // Delete confirmation dialog
    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text(stringResource(R.string.dialog_title_delete)) },
            text = {
                Text("${stringResource(R.string.dialog_message_delete)} \"${password?.siteName}\"?")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteDialog = false
                        viewModel.deletePassword()
                    }
                ) {
                    Text(
                        stringResource(R.string.button_delete),
                        color = MaterialTheme.colorScheme.error
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text(stringResource(R.string.button_cancel))
                }
            }
        )
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.password_detail),
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    FilledTonalIconButton(
                        onClick = onNavigateBack,
                        modifier = Modifier.padding(start = 8.dp)
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
                    }
                },
                actions = {
                    IconButton(onClick = { password?.let { onNavigateToEdit(it.id) } }) {
                        Icon(
                            Icons.Default.Edit,
                            contentDescription = stringResource(R.string.button_edit)
                        )
                    }
                    IconButton(onClick = { showDeleteDialog = true }) {
                        Icon(
                            Icons.Default.Delete,
                            contentDescription = stringResource(R.string.button_delete),
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                }
            )
        }
    ) { innerPadding ->
        val currentPassword = password
        if (currentPassword != null) {
            Column(
                modifier =
                    Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                        .padding(horizontal = 20.dp)
                        .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Site Name
                DetailField(
                    label = stringResource(R.string.site_or_app_hint),
                    value = currentPassword.siteName,
                    onCopy = { copyToClipboard(context, "Site", currentPassword.siteName) }
                )

                // Username
                if (currentPassword.username.isNotBlank()) {
                    DetailField(
                        label = stringResource(R.string.username_hint),
                        value = currentPassword.username,
                        onCopy = { copyToClipboard(context, "Username", currentPassword.username) }
                    )
                }

                // Password
                DetailField(
                    label = stringResource(R.string.password_hint),
                    value = if (passwordVisible) currentPassword.password else "••••••••",
                    onCopy = { copyToClipboard(context, "Password", currentPassword.password) },
                    trailingAction = {
                        IconButton(onClick = { passwordVisible = !passwordVisible }) {
                            Icon(
                                imageVector =
                                    if (passwordVisible) Icons.Default.Visibility
                                    else Icons.Default.VisibilityOff,
                                contentDescription = stringResource(R.string.show_password)
                            )
                        }
                    }
                )

                // URL
                if (currentPassword.url.isNotBlank()) {
                    DetailField(
                        label = stringResource(R.string.url_label),
                        value = currentPassword.url,
                        onCopy = { copyToClipboard(context, "URL", currentPassword.url) }
                    )
                }

                // Notes
                if (currentPassword.notes.isNotBlank()) {
                    DetailField(
                        label = stringResource(R.string.notes_label),
                        value = currentPassword.notes,
                        onCopy = { copyToClipboard(context, "Notes", currentPassword.notes) }
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Timestamps
                val dateFormat = remember {
                    SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault())
                }
                Text(
                    text =
                        "${stringResource(R.string.created_at)}: ${
                            dateFormat.format(
                                Date(
                                    currentPassword.createdAt
                                )
                            )
                        }",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text =
                        "${stringResource(R.string.updated_at)}: ${
                            dateFormat.format(
                                Date(
                                    currentPassword.updatedAt
                                )
                            )
                        }",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}

@Composable
private fun DetailField(
    label: String,
    value: String,
    onCopy: () -> Unit,
    trailingAction: @Composable (() -> Unit)? = null
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors =
            CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp, top = 12.dp, end = 4.dp, bottom = 4.dp)
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = value,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f),
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis
                )
                if (trailingAction != null) {
                    trailingAction()
                }
                IconButton(onClick = onCopy) {
                    Icon(
                        Icons.Default.ContentCopy,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

private fun copyToClipboard(context: Context, label: String, text: String) {
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    val clip = ClipData.newPlainText(label, text)
    clipboard.setPrimaryClip(clip)
    Toast.makeText(context, context.getString(R.string.copied), Toast.LENGTH_SHORT).show()
}
