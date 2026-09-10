package com.finnegan0596.shoppinglist.ui

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.finnegan0596.shoppinglist.data.AppData
import com.finnegan0596.shoppinglist.data.Item
import com.finnegan0596.shoppinglist.data.Shop
import com.finnegan0596.shoppinglist.data.ShoppingListRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class ShoppingListViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = ShoppingListRepository(application)

    val data: StateFlow<AppData> = repository.data

    private val _selectedShopFilter = MutableStateFlow<String?>(null)
    val selectedShopFilter: StateFlow<String?> = _selectedShopFilter.asStateFlow()

    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message.asStateFlow()

    fun setShopFilter(shopId: String?) {
        _selectedShopFilter.value = shopId
    }

    fun addShop(name: String) {
        if (name.isBlank()) return
        repository.addShop(name)
    }

    fun renameShop(shopId: String, newName: String) {
        if (newName.isBlank()) return
        repository.renameShop(shopId, newName)
    }

    fun deleteShop(shopId: String) {
        repository.deleteShop(shopId)
    }

    fun addOrRestoreItem(name: String, shopIds: List<String>) {
        if (name.isBlank()) return
        repository.addOrRestoreItem(name, shopIds)
    }

    fun updateItemShops(itemId: String, shopIds: List<String>) {
        repository.updateItemShops(itemId, shopIds)
    }

    fun setPurchased(itemId: String, purchased: Boolean) {
        repository.setPurchased(itemId, purchased)
    }

    fun removeFromCart(itemId: String) {
        repository.removeFromCart(itemId)
    }

    fun deleteItemPermanently(itemId: String) {
        repository.deleteItemPermanently(itemId)
    }

    fun clearPurchased() {
        repository.clearPurchased()
    }

    fun createExportUri(): Uri = repository.createExportUri()

    fun importReplacing(jsonText: String) {
        viewModelScope.launch {
            try {
                repository.importFrom(jsonText)
                _message.value = "Import complete (replaced local data)."
            } catch (e: Exception) {
                _message.value = "Import failed: ${e.message}"
            }
        }
    }

    fun importMerging(jsonText: String) {
        viewModelScope.launch {
            try {
                repository.mergeFrom(jsonText)
                _message.value = "Import complete (merged with local data)."
            } catch (e: Exception) {
                _message.value = "Import failed: ${e.message}"
            }
        }
    }

    fun clearMessage() {
        _message.value = null
    }

    /** All item names ever used, for autocomplete suggestions when adding a new item. */
    fun historyNames(): List<String> = data.value.items.map { it.name }.distinct().sorted()

    fun shopsById(): Map<String, Shop> = data.value.shops.associateBy { it.id }
}
