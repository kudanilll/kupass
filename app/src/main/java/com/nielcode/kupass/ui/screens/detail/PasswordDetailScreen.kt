package com.nielcode.kupass.ui.screens.detail

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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.nielcode.kupass.R
import com.nielcode.kupass.data.local.db.PasswordEntity
import com.nielcode.kupass.security.SecureClipboard
import com.nielcode.kupass.ui.components.DeletePasswordDialog
import com.nielcode.kupass.ui.components.PasswordVisibilityToggle
import com.nielcode.kupass.ui.components.VaultFieldShape
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private const val MASKED_PASSWORD = "••••••••"
private const val FIELD_MAX_LINES = 3

/** One vault entry: copy any field, reveal the password, edit, or delete. */
@Composable
fun PasswordDetailScreen(
    passwordId: Long,
    onNavigateBack: () -> Unit,
    onNavigateToEdit: (Long) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: PasswordDetailViewModel = viewModel(factory = PasswordDetailViewModel.Factory),
) {
    val context = LocalContext.current
    val password by viewModel.password.collectAsStateWithLifecycle()
    val deleteState by viewModel.deleteState.collectAsStateWithLifecycle()
    val currentOnNavigateBack by rememberUpdatedState(onNavigateBack)
    var showDeleteDialog by remember { mutableStateOf(false) }
    val deletedText = stringResource(R.string.toast_success_delete)
    val deleteFailedText = stringResource(R.string.toast_failed_delete)

    SideEffect(passwordId) { viewModel.loadPassword(passwordId) }
    SideEffect(deleteState) {
        when (deleteState) {
            DeleteState.Success -> {
                viewModel.resetDeleteState()
                Toast.makeText(context, deletedText, Toast.LENGTH_SHORT).show()
                currentOnNavigateBack()
            }
            DeleteState.Error -> {
                viewModel.resetDeleteState()
                Toast.makeText(context, deleteFailedText, Toast.LENGTH_LONG).show()
            }
            else -> Unit
        }
    }

    val current = password
    if (showDeleteDialog && current != null) {
        DeletePasswordDialog(
            siteName = current.siteName,
            onConfirm = {
                showDeleteDialog = false
                viewModel.deletePassword()
            },
            onDismiss = { showDeleteDialog = false },
        )
    }

    Scaffold(
        modifier = modifier,
        topBar = {
            DetailTopBar(
                onBack = onNavigateBack,
                onEdit = { current?.let { onNavigateToEdit(it.id) } },
                onDelete = { showDeleteDialog = true },
            )
        },
    ) { innerPadding ->
        if (current != null) {
            DetailContent(password = current, modifier = Modifier.padding(innerPadding))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DetailTopBar(
    onBack: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
) {
    CenterAlignedTopAppBar(
        modifier = modifier,
        title = { Text(stringResource(R.string.password_detail), fontWeight = FontWeight.Bold) },
        navigationIcon = {
            FilledTonalIconButton(onClick = onBack, modifier = Modifier.padding(start = 8.dp)) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
            }
        },
        actions = {
            IconButton(onClick = onEdit) {
                Icon(Icons.Default.Edit, contentDescription = stringResource(R.string.button_edit))
            }
            IconButton(onClick = onDelete) {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = stringResource(R.string.button_delete),
                    tint = MaterialTheme.colorScheme.error,
                )
            }
        },
    )
}

@Composable
private fun DetailContent(password: PasswordEntity, modifier: Modifier = Modifier) {
    val clipboard = rememberClipboardCopier()
    Column(
        modifier =
            modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp)
                .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        DetailField(
            label = stringResource(R.string.site_or_app_hint),
            value = password.siteName,
            onCopy = { clipboard.copy("Site", password.siteName) },
        )
        OptionalDetailField(
            R.string.username_hint,
            password.username,
            onCopy = { clipboard.copy("Username", password.username) },
        )
        PasswordField(
            password = password.password,
            onCopy = { clipboard.copy("Password", password.password, sensitive = true) },
        )
        OptionalDetailField(
            R.string.url_label,
            password.url,
            onCopy = { clipboard.copy("URL", password.url) },
        )
        OptionalDetailField(
            R.string.notes_label,
            password.notes,
            onCopy = { clipboard.copy("Notes", password.notes) },
        )
        Spacer(modifier = Modifier.height(8.dp))
        Timestamp(labelRes = R.string.created_at, epochMillis = password.createdAt)
        Timestamp(labelRes = R.string.updated_at, epochMillis = password.updatedAt)
        Spacer(modifier = Modifier.height(16.dp))
    }
}

/** The password, masked until the user reveals it. */
@Composable
private fun PasswordField(password: String, onCopy: () -> Unit, modifier: Modifier = Modifier) {
    var visible by remember { mutableStateOf(false) }
    DetailField(
        label = stringResource(R.string.password_hint),
        value = if (visible) password else MASKED_PASSWORD,
        onCopy = onCopy,
        modifier = modifier,
    ) {
        PasswordVisibilityToggle(visible = visible, onToggle = { visible = !visible })
    }
}

/** A [DetailField] that is omitted when [value] is blank. */
@Composable
private fun OptionalDetailField(
    labelRes: Int,
    value: String,
    onCopy: () -> Unit,
    modifier: Modifier = Modifier,
) {
    if (value.isNotBlank()) {
        DetailField(
            label = stringResource(labelRes),
            value = value,
            onCopy = onCopy,
            modifier = modifier,
        )
    }
}

@Composable
private fun Timestamp(labelRes: Int, epochMillis: Long, modifier: Modifier = Modifier) {
    val dateFormat = remember { SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault()) }
    Text(
        text = "${stringResource(labelRes)}: ${dateFormat.format(Date(epochMillis))}",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = modifier,
    )
}

/** A labeled value with a copy button and an optional extra action before it. */
@Composable
private fun DetailField(
    label: String,
    value: String,
    onCopy: () -> Unit,
    modifier: Modifier = Modifier,
    trailingAction: (@Composable () -> Unit)? = null,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = VaultFieldShape,
        colors =
            CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
    ) {
        Column(
            modifier =
                Modifier.fillMaxWidth()
                    .padding(start = 16.dp, top = 12.dp, end = 4.dp, bottom = 4.dp)
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary,
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = value,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f),
                    maxLines = FIELD_MAX_LINES,
                    overflow = TextOverflow.Ellipsis,
                )
                trailingAction?.invoke()
                IconButton(onClick = onCopy) {
                    Icon(
                        Icons.Default.ContentCopy,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

/** Copies vault values and confirms with a toast; sensitive values auto-clear. */
private class ClipboardCopier(private val context: Context, private val copiedText: String) {
    fun copy(label: String, text: String, sensitive: Boolean = false) {
        SecureClipboard.copy(context, label, text, sensitive = sensitive)
        Toast.makeText(context, copiedText, Toast.LENGTH_SHORT).show()
    }
}

@Composable
private fun rememberClipboardCopier(): ClipboardCopier {
    val context = LocalContext.current
    val copiedText = stringResource(R.string.copied)
    return remember(context, copiedText) { ClipboardCopier(context, copiedText) }
}
