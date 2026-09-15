package com.finnegan0596.shoppinglist.ui.screens

import android.content.Intent
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.finnegan0596.shoppinglist.ui.ShoppingListViewModel
import kotlinx.coroutines.launch

@Composable
fun SettingsScreen(viewModel: ShoppingListViewModel) {
    val context = LocalContext.current
    val message by viewModel.message.collectAsState()
    val activeGuid by viewModel.activeListGuid.collectAsState()
    val scope = rememberCoroutineScope()
    var pendingImportText by remember { mutableStateOf<String?>(null) }
    var sharedGuid by remember { mutableStateOf(activeGuid ?: "") }
    var sharedListName by remember { mutableStateOf("") }

    LaunchedEffect(activeGuid) {
        sharedGuid = activeGuid ?: sharedGuid
    }

    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            scope.launch {
                try {
                    val text = context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }
                    if (text != null) {
                        pendingImportText = text
                    } else {
                        Toast.makeText(context, "Could not read file", Toast.LENGTH_SHORT).show()
                    }
                } catch (e: Exception) {
                    Toast.makeText(context, "Could not read file: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    LaunchedEffect(message) {
        if (message != null) {
            Toast.makeText(context, message, Toast.LENGTH_LONG).show()
            viewModel.clearMessage()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("Local export/import stays available. Shared lists can also be opened by GUID when the Worker API is configured.")

        Text(if (activeGuid != null) "Active shared list: $activeGuid" else "No shared list is open.")

        OutlinedTextField(
            value = sharedGuid,
            onValueChange = { sharedGuid = it },
            label = { Text("Shared list GUID") },
            modifier = Modifier.fillMaxWidth()
        )

        OutlinedTextField(
            value = sharedListName,
            onValueChange = { sharedListName = it },
            label = { Text("New list name (optional)") },
            modifier = Modifier.fillMaxWidth()
        )

        Button(
            onClick = { viewModel.openRemoteList(sharedGuid) },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Open shared list")
        }

        OutlinedButton(
            onClick = { viewModel.createRemoteList(guid = sharedGuid.ifBlank { null }, name = sharedListName.ifBlank { null }) },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Create shared list")
        }

        Button(
            onClick = {
                val uri = viewModel.createExportUri()
                val sendIntent = Intent(Intent.ACTION_SEND).apply {
                    type = "application/json"
                    putExtra(Intent.EXTRA_STREAM, uri)
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                context.startActivity(Intent.createChooser(sendIntent, "Share shopping list"))
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Export / Share shopping list")
        }

        OutlinedButton(
            onClick = { importLauncher.launch("*/*") },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Import shopping list")
        }
    }

    pendingImportText?.let { text ->
        AlertDialog(
            onDismissRequest = { pendingImportText = null },
            title = { Text("Import shopping list") },
            text = { Text("Merge with your current list, or replace it entirely?") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.importMerging(text)
                    pendingImportText = null
                }) { Text("Merge") }
            },
            dismissButton = {
                TextButton(onClick = {
                    viewModel.importReplacing(text)
                    pendingImportText = null
                }) { Text("Replace") }
            }
        )
    }
}
