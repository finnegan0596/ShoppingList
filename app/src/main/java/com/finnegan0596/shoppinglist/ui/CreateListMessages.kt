package com.finnegan0596.shoppinglist.ui

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonPrimitive

internal fun createListSuccessMessage(guid: String): String = "Created shared list $guid"

internal fun createListErrorMessage(throwable: Throwable): String {
    val detail = throwable.message
        ?.trim()
        ?.removeSurrounding("\"")
        ?.takeUnless { it.isEmpty() || it.equals("null", ignoreCase = true) || it == "{}" }
        ?.let { extractRemoteErrorDetail(it) }
    return if (detail != null) {
        "Could not create list: $detail"
    } else {
        "Could not create list. Check connection and try again."
    }
}

private fun extractRemoteErrorDetail(message: String): String? {
    val parsed = runCatching { Json.parseToJsonElement(message) }.getOrNull() ?: return message
    val parsedObject = parsed as? JsonObject ?: return null
    return parsedObject["error"]
        ?.jsonPrimitive
        ?.contentOrNull
        ?.trim()
        ?.takeUnless { it.isEmpty() || it.equals("null", ignoreCase = true) }
}
