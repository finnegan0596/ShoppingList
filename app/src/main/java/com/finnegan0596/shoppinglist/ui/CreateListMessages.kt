package com.finnegan0596.shoppinglist.ui

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull

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
    val trimmed = message.trim()
    val parsed = runCatching { Json.parseToJsonElement(trimmed) }.getOrNull() ?: return trimmed
    return when (parsed) {
        is JsonObject -> (parsed["error"] as? JsonPrimitive)?.takeIf { it.isString }?.contentOrNull
        is JsonArray -> null
        is JsonPrimitive -> if (parsed.isString) trimmed else null
        else -> null
    }
}

private fun sanitizeErrorDetail(detail: String): String? {
    val trimmed = detail.trim()
    val decoded = decodeQuotedJsonString(trimmed)
    val normalized = when {
        decoded != null -> decodeEscapedDisplaySequences(decoded).trim()
        trimmed.startsWith("\"") && trimmed.endsWith("\"") -> {
            val inner = trimmed.removeSurrounding("\"").trim()
            if (inner in setOf("{", "[", "}", "]")) return null
            inner
        }
        else -> trimmed
    }
    return normalized.takeUnless {
        it.isEmpty() || it.equals("null", ignoreCase = true) || it == "{}" || it == "[]"
    }
}

private fun decodeQuotedJsonString(message: String): String? {
    if (!(message.startsWith("\"") && message.endsWith("\""))) return null
    val parsed = runCatching { Json.parseToJsonElement(message) }.getOrNull() as? JsonPrimitive ?: return null
    return parsed.takeIf { it.isString }?.contentOrNull
}

private fun decodeEscapedDisplaySequences(message: String): String {
    return message
        .replace("\\n", "\n")
        .replace("\\t", "\t")
        .replace("\\r", "\r")
        .replace("\\\"", "\"")
}
