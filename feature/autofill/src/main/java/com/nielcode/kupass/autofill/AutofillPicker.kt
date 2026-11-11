package com.nielcode.kupass.autofill

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.input.TextFieldLineLimits
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.nielcode.kupass.data.local.db.PasswordEntity
import com.nielcode.kupass.data.repository.filterByQuery
import com.nielcode.kupass.feature.autofill.R

/** Account list for autofill: username and site only, never a password. */
@Composable
internal fun AutofillPicker(
    entries: List<PasswordEntity>,
    searchable: Boolean,
    onPick: (PasswordEntity) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val query = rememberTextFieldState()
    // Filters the already-decrypted list for the next frame. A database flow per keystroke showed
    // the whole vault until each new result arrived.
    val shown by
        remember(entries, searchable) {
            derivedStateOf {
                if (searchable) entries.filterByQuery(query.text.toString()) else entries
            }
        }
    Surface(modifier = modifier.fillMaxSize()) {
        Column(Modifier.safeDrawingPadding().padding(16.dp)) {
            if (searchable) {
                OutlinedTextField(
                    state = query,
                    modifier = Modifier.fillMaxWidth(),
                    lineLimits = TextFieldLineLimits.SingleLine,
                    placeholder = { Text(stringResource(R.string.autofill_search_placeholder)) },
                )
            }
            if (shown.isEmpty()) {
                Text(
                    text = stringResource(R.string.autofill_empty),
                    modifier = Modifier.padding(vertical = 24.dp),
                    style = MaterialTheme.typography.bodyLarge,
                )
            }
            LazyColumn(Modifier.weight(1f)) {
                items(shown, key = { it.id }) { entry ->
                    AccountRow(entry, onClick = { onPick(entry) })
                    HorizontalDivider()
                }
            }
            TextButton(onClick = onDismiss) { Text(stringResource(android.R.string.cancel)) }
        }
    }
}

@Composable
private fun AccountRow(entry: PasswordEntity, onClick: () -> Unit) {
    Column(Modifier.fillMaxWidth().clickable(onClick = onClick).padding(vertical = 12.dp)) {
        Text(
            text = entry.username.ifBlank { stringResource(R.string.autofill_no_username) },
            style = MaterialTheme.typography.titleMedium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            text = entry.siteName,
            style = MaterialTheme.typography.bodyMedium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}
