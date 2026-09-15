package com.finnegan0596.shoppinglist.data

import android.content.Context
import android.net.Uri
import androidx.core.content.FileProvider
import com.finnegan0596.shoppinglist.BuildConfig
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

/**
 * Persists [AppData] as a single human-readable JSON file in the app's private
 * storage. It also supports optional loading and syncing with the shared-list
 * Worker API when a GUID-backed route is opened.
 */
class ShoppingListRepository(private val context: Context) {

    companion object {
        @Volatile
        var remoteApiBaseUrl: String = BuildConfig.REMOTE_API_BASE_URL
    }

    private val json = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    private val storageFile: File
        get() = File(context.filesDir, "shopping_list.json")

    private val _data = MutableStateFlow(loadFromDisk())
    val data: StateFlow<AppData> = _data.asStateFlow()

    private fun loadFromDisk(): AppData {
        return try {
            if (storageFile.exists()) {
                json.decodeFromString(AppData.serializer(), storageFile.readText())
            } else {
                AppData()
            }
        } catch (e: Exception) {
            AppData()
        }
    }

    private fun persist(newData: AppData) {
        _data.value = newData
        storageFile.writeText(json.encodeToString(AppData.serializer(), newData))
    }

    private fun remoteListToAppData(remoteList: RemoteListState): AppData {
        return AppData(
            shops = emptyList(),
            items = remoteList.items.map { item ->
                Item(
                    id = item.id.toString(),
                    name = item.text,
                    shopIds = emptyList(),
                    inCart = true,
                    purchased = item.checked
                )
            }
        )
    }

    private fun requestJson(method: String, path: String, body: String? = null): String {
        val url = URL("${remoteApiBaseUrl.trimEnd('/')}$path")
        val connection = url.openConnection() as HttpURLConnection
        connection.requestMethod = method
        connection.connectTimeout = 10000
        connection.readTimeout = 10000
        connection.setRequestProperty("Accept", "application/json")
        connection.setRequestProperty("Content-Type", "application/json; charset=UTF-8")

        if (body != null) {
            connection.doOutput = true
            connection.outputStream.use { stream ->
                stream.write(body.toByteArray(Charsets.UTF_8))
            }
        }

        val responseCode = connection.responseCode
        val payload = if (responseCode in 200..299) {
            connection.inputStream?.bufferedReader()?.use { it.readText() }
        } else {
            connection.errorStream?.bufferedReader()?.use { it.readText() }
        }
        connection.disconnect()

        if (responseCode !in 200..299) {
            throw IllegalStateException(payload ?: "Remote request failed: $responseCode")
        }

        return payload ?: "{}"
    }

    fun openRemoteList(guid: String): RemoteListState {
        val payload = requestJson("GET", "/api/lists/$guid")
        val remoteList = json.decodeFromString(RemoteListState.serializer(), payload)
        persist(remoteListToAppData(remoteList))
        return remoteList
    }

    fun createRemoteList(guid: String? = null, name: String? = null): RemoteListState {
        val body = buildJsonObject {
            if (!guid.isNullOrBlank()) put("guid", guid)
            if (!name.isNullOrBlank()) put("name", name)
        }.toString()

        val payload = requestJson("POST", "/api/lists", body)
        val remoteList = json.decodeFromString(RemoteListState.serializer(), payload)
        persist(remoteListToAppData(remoteList))
        return remoteList
    }

    fun addRemoteItem(guid: String, name: String, revision: Int): RemoteListState {
        val body = buildJsonObject {
            put("text", name.trim())
            put("qty", null)
            put("checked", false)
            put("sortOrder", 0)
            put("revision", revision)
        }.toString()
        val payload = requestJson("POST", "/api/lists/$guid/items", body)
        return json.decodeFromString(RemoteListState.serializer(), payload)
    }

    fun updateRemoteItem(guid: String, itemId: Int, revision: Int, checked: Boolean): RemoteListState {
        val body = buildJsonObject {
            put("checked", checked)
            put("revision", revision)
        }.toString()
        val payload = requestJson("PATCH", "/api/lists/$guid/items/$itemId", body)
        return json.decodeFromString(RemoteListState.serializer(), payload)
    }

    fun deleteRemoteItem(guid: String, itemId: Int, revision: Int): RemoteListState {
        val body = buildJsonObject {
            put("revision", revision)
        }.toString()
        val payload = requestJson("DELETE", "/api/lists/$guid/items/$itemId", body)
        return json.decodeFromString(RemoteListState.serializer(), payload)
    }

    // ----- Shops -----

    fun addShop(name: String): Shop {
        val trimmed = name.trim()
        val existing = _data.value.shops.firstOrNull { it.name.equals(trimmed, ignoreCase = true) }
        if (existing != null) return existing
        val shop = Shop(id = UUID.randomUUID().toString(), name = trimmed)
        persist(_data.value.copy(shops = _data.value.shops + shop))
        return shop
    }

    fun renameShop(shopId: String, newName: String) {
        val updated = _data.value.shops.map {
            if (it.id == shopId) it.copy(name = newName.trim()) else it
        }
        persist(_data.value.copy(shops = updated))
    }

    fun deleteShop(shopId: String) {
        val updatedShops = _data.value.shops.filterNot { it.id == shopId }
        val updatedItems = _data.value.items.map { it.copy(shopIds = it.shopIds - shopId) }
        persist(_data.value.copy(shops = updatedShops, items = updatedItems))
    }

    // ----- Items -----

    /** Adds an item to the active list, reusing a previously remembered item of the same name if one exists. */
    fun addOrRestoreItem(name: String, shopIds: List<String>): Item {
        val trimmed = name.trim()
        val existing = _data.value.items.firstOrNull { it.name.equals(trimmed, ignoreCase = true) }
        val result: Item
        val updatedItems = if (existing != null) {
            result = existing.copy(inCart = true, purchased = false, shopIds = shopIds)
            _data.value.items.map { if (it.id == existing.id) result else it }
        } else {
            result = Item(id = UUID.randomUUID().toString(), name = trimmed, shopIds = shopIds, inCart = true, purchased = false)
            _data.value.items + result
        }
        persist(_data.value.copy(items = updatedItems))
        return result
    }

    fun updateItemShops(itemId: String, shopIds: List<String>) {
        val updated = _data.value.items.map { if (it.id == itemId) it.copy(shopIds = shopIds) else it }
        persist(_data.value.copy(items = updated))
    }

    fun setPurchased(itemId: String, purchased: Boolean) {
        val updated = _data.value.items.map { if (it.id == itemId) it.copy(purchased = purchased) else it }
        persist(_data.value.copy(items = updated))
    }

    /** Removes an item from the active shopping list but keeps it remembered for later ("history"). */
    fun removeFromCart(itemId: String) {
        val updated = _data.value.items.map { if (it.id == itemId) it.copy(inCart = false, purchased = false) else it }
        persist(_data.value.copy(items = updated))
    }

    /** Permanently forgets an item, removing it from history entirely. */
    fun deleteItemPermanently(itemId: String) {
        persist(_data.value.copy(items = _data.value.items.filterNot { it.id == itemId }))
    }

    fun clearPurchased() {
        val updated = _data.value.items.map {
            if (it.inCart && it.purchased) it.copy(inCart = false, purchased = false) else it
        }
        persist(_data.value.copy(items = updated))
    }

    // ----- Export / Import -----

    /** Writes the current data to a shareable file in the cache dir and returns a content:// Uri for it. */
    fun createExportUri(): Uri {
        val exportsDir = File(context.cacheDir, "exports").apply { mkdirs() }
        val stamp = SimpleDateFormat("yyyyMMdd-HHmmss", Locale.US).format(Date())
        val file = File(exportsDir, "shopping-list-$stamp.json")
        file.writeText(json.encodeToString(AppData.serializer(), _data.value))
        return FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
    }

    /** Replaces all local data with the contents of the given JSON text. */
    fun importFrom(jsonText: String) {
        val imported = json.decodeFromString(AppData.serializer(), jsonText)
        persist(imported)
    }

    /** Merges the given JSON text into the existing data (shops/items are combined by name, no duplicates). */
    fun mergeFrom(jsonText: String) {
        val imported = json.decodeFromString(AppData.serializer(), jsonText)
        var shops = _data.value.shops
        val shopIdRemap = mutableMapOf<String, String>()
        for (shop in imported.shops) {
            val existing = shops.firstOrNull { it.name.equals(shop.name, ignoreCase = true) }
            if (existing != null) {
                shopIdRemap[shop.id] = existing.id
            } else {
                shops = shops + shop
                shopIdRemap[shop.id] = shop.id
            }
        }
        var items = _data.value.items
        for (item in imported.items) {
            val remappedShopIds = item.shopIds.mapNotNull { shopIdRemap[it] }
            val existing = items.firstOrNull { it.name.equals(item.name, ignoreCase = true) }
            items = if (existing != null) {
                items.map {
                    if (it.id == existing.id) {
                        it.copy(
                            shopIds = (it.shopIds + remappedShopIds).distinct(),
                            inCart = it.inCart || item.inCart,
                            purchased = if (item.inCart) it.purchased else it.purchased
                        )
                    } else it
                }
            } else {
                items + item.copy(shopIds = remappedShopIds)
            }
        }
        persist(AppData(shops = shops, items = items))
    }
}
