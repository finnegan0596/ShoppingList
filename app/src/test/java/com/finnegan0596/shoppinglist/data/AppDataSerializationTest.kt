package com.finnegan0596.shoppinglist.data

import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Verifies that [AppData] round-trips through JSON exactly, since this is the
 * format used for both local storage and export/import.
 */
class AppDataSerializationTest {

    private val json = Json { prettyPrint = true }

    @Test
    fun `app data round trips through json`() {
        val original = AppData(
            shops = listOf(Shop(id = "s1", name = "SuperValu"), Shop(id = "s2", name = "Lidl")),
            items = listOf(
                Item(id = "i1", name = "Milk", shopIds = listOf("s1", "s2"), inCart = true, purchased = false),
                Item(id = "i2", name = "Bread", shopIds = listOf("s1"), inCart = false, purchased = false)
            )
        )

        val encoded = json.encodeToString(AppData.serializer(), original)
        val decoded = json.decodeFromString(AppData.serializer(), encoded)

        assertEquals(original, decoded)
    }

    @Test
    fun `empty app data round trips`() {
        val original = AppData()
        val encoded = json.encodeToString(AppData.serializer(), original)
        val decoded = json.decodeFromString(AppData.serializer(), encoded)
        assertEquals(original, decoded)
    }
}
