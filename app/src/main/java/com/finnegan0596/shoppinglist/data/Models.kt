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
