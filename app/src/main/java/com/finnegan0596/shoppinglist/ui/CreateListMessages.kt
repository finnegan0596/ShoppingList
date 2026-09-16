package com.finnegan0596.shoppinglist.ui

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonPrimitive

internal fun createListSuccessMessage(guid: String): String = "Created shared list $guid"

internal fun createListErrorMessage(throwable: Throwable): String {
    val detail = throwable.message
        ?.let { extractRemoteErrorDetail(it) }
        ?.let { sanitizeErrorDetail(it) }
    return if (detail != null) {
        "Could not create list: $detail"
    } else {
        "Could not create list. Check connection and try again."
    }
}

private fun extractRemoteErrorDetail(message: String): String? {
    val parsed = runCatching { Json.parseToJsonElement(message) }.getOrNull() ?: return message
    return when (parsed) {
        is JsonObject -> parsed["error"]?.jsonPrimitive?.contentOrNull
        is JsonPrimitive -> parsed.contentOrNull
        else -> null
    }
}

private fun sanitizeErrorDetail(detail: String): String? {
    val normalized = detail.trim().removeSurrounding("\"")
    return normalized.takeUnless {
        it.isEmpty() || it.equals("null", ignoreCase = true) || it == "{}"
    }
}
