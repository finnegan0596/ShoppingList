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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ShoppingListViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = ShoppingListRepository(application)

    val data: StateFlow<AppData> = repository.data

    private val _selectedShopFilter = MutableStateFlow<String?>(null)
    val selectedShopFilter: StateFlow<String?> = _selectedShopFilter.asStateFlow()

    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message.asStateFlow()

    private val _activeListGuid = MutableStateFlow<String?>(null)
    val activeListGuid: StateFlow<String?> = _activeListGuid.asStateFlow()

    private val _remoteRevision = MutableStateFlow<Int?>(null)
    val remoteRevision: StateFlow<Int?> = _remoteRevision.asStateFlow()

    fun setRemoteApiBaseUrl(baseUrl: String) {
        ShoppingListRepository.remoteApiBaseUrl = baseUrl.trimEnd('/')
    }

    fun setShopFilter(shopId: String?) {
        _selectedShopFilter.value = shopId
    }

    fun openRemoteList(guid: String) {
        if (guid.isBlank()) return
        viewModelScope.launch {
            try {
                val remoteList = repository.openRemoteList(guid)
                _activeListGuid.value = remoteList.guid
                _remoteRevision.value = remoteList.revision
                _message.value = "Opened shared list ${remoteList.guid}"
            } catch (e: Exception) {
                _message.value = "Could not open list: ${e.message}"
            }
        }
    }

    fun createRemoteList(name: String? = null) {
        viewModelScope.launch {
            try {
                val remoteList = withContext(Dispatchers.IO) {
                    repository.createRemoteList(name)
                }
                _activeListGuid.value = remoteList.guid
                _remoteRevision.value = remoteList.revision
                _message.value = createListSuccessMessage(remoteList.guid)
            } catch (e: Exception) {
                _message.value = createListErrorMessage(e)
            }
        }
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
        val guid = _activeListGuid.value
        if (guid != null) {
            viewModelScope.launch {
                try {
                    val remoteList = repository.addRemoteItem(guid, name, _remoteRevision.value ?: 0)
                    _remoteRevision.value = remoteList.revision
                    _message.value = "Synced item to shared list $guid"
                } catch (e: Exception) {
                    _message.value = "Could not sync item: ${e.message}"
                }
            }
            return
        }
        repository.addOrRestoreItem(name, shopIds)
    }

    fun updateItemShops(itemId: String, shopIds: List<String>) {
        repository.updateItemShops(itemId, shopIds)
    }

    fun setPurchased(itemId: String, purchased: Boolean) {
        val guid = _activeListGuid.value
        if (guid != null) {
            viewModelScope.launch {
                try {
                    val itemIdInt = itemId.toIntOrNull() ?: return@launch
                    val remoteList = repository.updateRemoteItem(guid, itemIdInt, _remoteRevision.value ?: 0, purchased)
                    _remoteRevision.value = remoteList.revision
                    _message.value = "Updated shared list $guid"
                } catch (e: Exception) {
                    _message.value = "Could not update item: ${e.message}"
                }
            }
            return
        }
        repository.setPurchased(itemId, purchased)
    }

    fun removeFromCart(itemId: String) {
        val guid = _activeListGuid.value
        if (guid != null) {
            viewModelScope.launch {
                try {
                    val itemIdInt = itemId.toIntOrNull() ?: return@launch
                    val remoteList = repository.deleteRemoteItem(guid, itemIdInt, _remoteRevision.value ?: 0)
                    _remoteRevision.value = remoteList.revision
                    _message.value = "Removed item from shared list $guid"
                } catch (e: Exception) {
                    _message.value = "Could not remove item: ${e.message}"
                }
            }
            return
        }
        repository.removeFromCart(itemId)
    }

    fun deleteItemPermanently(itemId: String) {
        val guid = _activeListGuid.value
        if (guid != null) {
            viewModelScope.launch {
                try {
                    val itemIdInt = itemId.toIntOrNull() ?: return@launch
                    val remoteList = repository.deleteRemoteItem(guid, itemIdInt, _remoteRevision.value ?: 0)
                    _remoteRevision.value = remoteList.revision
                    _message.value = "Deleted item from shared list $guid"
                } catch (e: Exception) {
                    _message.value = "Could not delete item: ${e.message}"
                }
            }
            return
        }
        repository.deleteItemPermanently(itemId)
    }

    fun clearPurchased() {
        val guid = _activeListGuid.value
        if (guid != null) {
            viewModelScope.launch {
                try {
                    val remoteList = repository.openRemoteList(guid)
                    remoteList.items.filter { it.checked }.forEach { item ->
                        repository.deleteRemoteItem(guid, item.id, _remoteRevision.value ?: 0)
                    }
                    val refreshed = repository.openRemoteList(guid)
                    _remoteRevision.value = refreshed.revision
                    _message.value = "Cleared purchased items in shared list $guid"
                } catch (e: Exception) {
                    _message.value = "Could not clear purchased items: ${e.message}"
                }
            }
            return
        }
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
