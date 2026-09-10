package com.finnegan0596.shoppinglist.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.finnegan0596.shoppinglist.data.Shop
import com.finnegan0596.shoppinglist.ui.ShoppingListViewModel

@Composable
fun ShopsScreen(viewModel: ShoppingListViewModel) {
    val appData by viewModel.data.collectAsState()
    var showAddDialog by remember { mutableStateOf(false) }
    var shopToEdit by remember { mutableStateOf<Shop?>(null) }
    var shopToDelete by remember { mutableStateOf<Shop?>(null) }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(onClick = { showAddDialog = true }) {
                Icon(Icons.Filled.Add, contentDescription = "Add shop")
            }
        }
    ) { padding ->
        if (appData.shops.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Text("No shops yet. Tap + to add one, e.g. SuperValu, Lidl or Aldi.")
            }
        } else {
            LazyColumn(modifier = Modifier.fillMaxSize().padding(padding)) {
                items(appData.shops, key = { it.id }) { shop ->
                    ListItem(
                        headlineContent = { Text(shop.name) },
                        trailingContent = {
                            Row {
                                IconButton(onClick = { shopToEdit = shop }) {
                                    Icon(Icons.Filled.Edit, contentDescription = "Rename shop")
                                }
                                IconButton(onClick = { shopToDelete = shop }) {
                                    Icon(Icons.Filled.Delete, contentDescription = "Delete shop")
                                }
                            }
                        }
                    )
                }
            }
        }
    }

    if (showAddDialog) {
        ShopNameDialog(
            title = "Add shop",
            initialName = "",
            onConfirm = { viewModel.addShop(it) },
            onDismiss = { showAddDialog = false }
        )
    }

    shopToEdit?.let { shop ->
        ShopNameDialog(
            title = "Rename shop",
            initialName = shop.name,
            onConfirm = { viewModel.renameShop(shop.id, it) },
            onDismiss = { shopToEdit = null }
        )
    }

    shopToDelete?.let { shop ->
        AlertDialog(
            onDismissRequest = { shopToDelete = null },
            title = { Text("Delete ${shop.name}?") },
            text = { Text("This removes the shop. Items previously linked to it will no longer be filtered by it.") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.deleteShop(shop.id)
                    shopToDelete = null
                }) { Text("Delete") }
            },
            dismissButton = {
                TextButton(onClick = { shopToDelete = null }) { Text("Cancel") }
            }
        )
    }
}

@Composable
private fun ShopNameDialog(
    title: String,
    initialName: String,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var name by remember { mutableStateOf(initialName) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Shop name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (name.isNotBlank()) onConfirm(name)
                    onDismiss()
                }
            ) { Text("Save") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
