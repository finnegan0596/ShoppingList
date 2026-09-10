package com.finnegan0596.shoppinglist.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
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
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import com.finnegan0596.shoppinglist.data.Item
import com.finnegan0596.shoppinglist.ui.ShoppingListViewModel

@Composable
fun ShoppingListScreen(viewModel: ShoppingListViewModel) {
    val appData by viewModel.data.collectAsState()
    val selectedShopFilter by viewModel.selectedShopFilter.collectAsState()
    var showAddDialog by remember { mutableStateOf(false) }
    var itemToEdit by remember { mutableStateOf<Item?>(null) }

    val shops = appData.shops
    val cartItems = appData.items
        .filter { it.inCart }
        .filter { selectedShopFilter == null || it.shopIds.contains(selectedShopFilter) }
        .sortedBy { it.purchased }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(onClick = { showAddDialog = true }) {
                Icon(Icons.Filled.Add, contentDescription = "Add item")
            }
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            if (shops.isNotEmpty()) {
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    item {
                        FilterChip(
                            selected = selectedShopFilter == null,
                            onClick = { viewModel.setShopFilter(null) },
                            label = { Text("All shops") }
                        )
                    }
                    items(shops, key = { it.id }) { shop ->
                        FilterChip(
                            selected = selectedShopFilter == shop.id,
                            onClick = {
                                viewModel.setShopFilter(if (selectedShopFilter == shop.id) null else shop.id)
                            },
                            label = { Text(shop.name) }
                        )
                    }
                }
            }

            if (cartItems.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Your shopping list is empty. Tap + to add an item.")
                }
            } else {
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    items(cartItems, key = { it.id }) { item ->
                        ShoppingListRow(
                            item = item,
                            shopNames = item.shopIds.mapNotNull { id -> shops.firstOrNull { it.id == id }?.name },
                            onTogglePurchased = { viewModel.setPurchased(item.id, !item.purchased) },
                            onEdit = { itemToEdit = item },
                            onRemove = { viewModel.removeFromCart(item.id) }
                        )
                    }
                }
                if (cartItems.any { it.purchased }) {
                    TextButton(
                        onClick = { viewModel.clearPurchased() },
                        modifier = Modifier.padding(8.dp)
                    ) {
                        Text("Clear purchased items")
                    }
                }
            }
        }
    }

    if (showAddDialog) {
        AddItemDialog(
            viewModel = viewModel,
            onDismiss = { showAddDialog = false }
        )
    }

    itemToEdit?.let { item ->
        EditItemShopsDialog(
            viewModel = viewModel,
            item = item,
            onDismiss = { itemToEdit = null }
        )
    }
}

@Composable
private fun ShoppingListRow(
    item: Item,
    shopNames: List<String>,
    onTogglePurchased: () -> Unit,
    onEdit: () -> Unit,
    onRemove: () -> Unit
) {
    ListItem(
        leadingContent = {
            Checkbox(checked = item.purchased, onCheckedChange = { onTogglePurchased() })
        },
        headlineContent = {
            Text(
                text = item.name,
                textDecoration = if (item.purchased) TextDecoration.LineThrough else TextDecoration.None
            )
        },
        supportingContent = if (shopNames.isNotEmpty()) {
            { Text(shopNames.joinToString(", ")) }
        } else null,
        trailingContent = {
            Row {
                IconButton(onClick = onEdit) {
                    Icon(Icons.Filled.Edit, contentDescription = "Edit shops for this item")
                }
                IconButton(onClick = onRemove) {
                    Icon(Icons.Filled.Delete, contentDescription = "Remove from list")
                }
            }
        }
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ShopChipSelector(
    shops: List<com.finnegan0596.shoppinglist.data.Shop>,
    selectedShopIds: Set<String>,
    onToggle: (String) -> Unit
) {
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        shops.forEach { shop ->
            FilterChip(
                selected = selectedShopIds.contains(shop.id),
                onClick = { onToggle(shop.id) },
                label = { Text(shop.name) }
            )
        }
    }
}

@Composable
private fun EditItemShopsDialog(
    viewModel: ShoppingListViewModel,
    item: Item,
    onDismiss: () -> Unit
) {
    val appData by viewModel.data.collectAsState()
    val selectedShopIds = remember(item.id) { mutableStateOf(item.shopIds.toSet()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit \"${item.name}\"") },
        text = {
            Column {
                if (appData.shops.isNotEmpty()) {
                    Text("Available at:", modifier = Modifier.padding(bottom = 4.dp))
                    ShopChipSelector(
                        shops = appData.shops,
                        selectedShopIds = selectedShopIds.value,
                        onToggle = { shopId ->
                            selectedShopIds.value = if (selectedShopIds.value.contains(shopId)) {
                                selectedShopIds.value - shopId
                            } else {
                                selectedShopIds.value + shopId
                            }
                        }
                    )
                } else {
                    Text("No shops added yet. Add shops from the Shops tab to tag this item.")
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    viewModel.updateItemShops(item.id, selectedShopIds.value.toList())
                    onDismiss()
                }
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
private fun AddItemDialog(
    viewModel: ShoppingListViewModel,
    onDismiss: () -> Unit
) {
    val appData by viewModel.data.collectAsState()
    var name by remember { mutableStateOf("") }
    var expanded by remember { mutableStateOf(false) }
    val selectedShopIds = remember { mutableStateOf(setOf<String>()) }

    val history = remember(appData.items) { appData.items.map { it.name }.distinct().sorted() }
    val suggestions = remember(name, history) {
        if (name.isBlank()) history else history.filter { it.contains(name, ignoreCase = true) }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add item") },
        text = {
            Column {
                OutlinedTextField(
                    value = name,
                    onValueChange = {
                        name = it
                        expanded = true
                    },
                    label = { Text("Item name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                // Suggestions are rendered inline (not as a DropdownMenu/Popup) because a Popup
                // nested inside this AlertDialog's own window can be torn down out of order when
                // the dialog is dismissed (e.g. tapping "Add" while suggestions are showing),
                // which crashes the app with a WindowManager.BadTokenException.
                if (expanded && suggestions.isNotEmpty()) {
                    Surface(
                        tonalElevation = 3.dp,
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 160.dp)
                    ) {
                        Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                            suggestions.take(8).forEach { suggestion ->
                                Text(
                                    text = suggestion,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            name = suggestion
                                            expanded = false
                                            val existing = appData.items.firstOrNull { it.name == suggestion }
                                            if (existing != null) {
                                                selectedShopIds.value = existing.shopIds.toSet()
                                            }
                                        }
                                        .padding(horizontal = 16.dp, vertical = 12.dp)
                                )
                            }
                        }
                    }
                }

                if (appData.shops.isNotEmpty()) {
                    Text("Available at:", modifier = Modifier.padding(top = 12.dp, bottom = 4.dp))
                    ShopChipSelector(
                        shops = appData.shops,
                        selectedShopIds = selectedShopIds.value,
                        onToggle = { shopId ->
                            selectedShopIds.value = if (selectedShopIds.value.contains(shopId)) {
                                selectedShopIds.value - shopId
                            } else {
                                selectedShopIds.value + shopId
                            }
                        }
                    )
                } else {
                    Text(
                        "No shops added yet. Add shops from the Shops tab to filter your list by store.",
                        modifier = Modifier.padding(top = 12.dp)
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (name.isNotBlank()) {
                        viewModel.addOrRestoreItem(name, selectedShopIds.value.toList())
                    }
                    onDismiss()
                }
            ) {
                Text("Add")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
