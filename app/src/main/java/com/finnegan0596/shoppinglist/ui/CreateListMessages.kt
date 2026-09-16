package com.finnegan0596.shoppinglist.ui

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject
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

private fun extractRemoteErrorDetail(message: String): String {
    return runCatching {
        Json.parseToJsonElement(message)
            .jsonObject["error"]
            ?.jsonPrimitive
            ?.contentOrNull
            ?.trim()
            ?.takeUnless { it.isEmpty() || it.equals("null", ignoreCase = true) }
    }.getOrNull() ?: message
}
