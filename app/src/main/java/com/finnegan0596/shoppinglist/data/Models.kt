package com.finnegan0596.shoppinglist.data

import kotlinx.serialization.Serializable

/**
 * A shop that items can be associated with (e.g. "SuperValu", "Lidl", "Aldi").
 */
@Serializable
data class Shop(
    val id: String,
    val name: String
)

/**
 * A single item. Items persist even after they are removed from the active
 * shopping list so that their name and shop associations can be reused later
 * (this is the "remembered item" history).
 *
 * @property inCart true while the item is currently on the active shopping list.
 * @property purchased true once the item has been checked off while shopping.
 */
@Serializable
data class Item(
    val id: String,
    val name: String,
    val shopIds: List<String> = emptyList(),
    val inCart: Boolean = true,
    val purchased: Boolean = false
)

/**
 * The full app data set. This is exactly what gets persisted to disk and what
 * gets exported/imported as JSON.
 */
@Serializable
data class AppData(
    val shops: List<Shop> = emptyList(),
    val items: List<Item> = emptyList()
)

@Serializable
data class RemoteListItem(
    val id: Int,
    val text: String,
    val qty: String? = null,
    val checked: Boolean = false,
    val sortOrder: Int = 0,
    val updatedAt: String? = null
)

@Serializable
data class RemoteListState(
    val guid: String,
    val name: String? = null,
    val revision: Int = 0,
    val createdAt: String? = null,
    val updatedAt: String? = null,
    val items: List<RemoteListItem> = emptyList()
)
