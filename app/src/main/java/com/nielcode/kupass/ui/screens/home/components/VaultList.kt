package com.nielcode.kupass.ui.screens.home.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.nielcode.kupass.R

@Composable
fun VaultList(modifier: Modifier = Modifier, isEmpty: Boolean) {
    if (isEmpty) {
        // Empty State
        Column(
            modifier = modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Image(
                painter = painterResource(id = R.drawable.undraw_no_data),
                contentDescription = "Empty Vault",
                modifier = Modifier.size(196.dp),
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "Vault is empty",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
        }
    } else {
        // RecyclerView ekuivalen
        LazyColumn(modifier = modifier.fillMaxSize()) {
            items(20) { index -> // Dummy data
                // Panggil komponen ItemAccount kamu di sini
                Text("Password Item $index", modifier = Modifier.padding(16.dp))
            }
        }
    }
}
