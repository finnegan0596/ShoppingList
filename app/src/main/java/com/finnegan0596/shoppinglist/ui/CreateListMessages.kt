package com.finnegan0596.shoppinglist.ui

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
    val errorPrefix = "\"error\":"
    val errorIndex = message.indexOf(errorPrefix)
    if (errorIndex == -1) return message

    val valueStart = message.indexOf('"', errorIndex + errorPrefix.length)
    if (valueStart == -1) return message
    val valueEnd = message.indexOf('"', valueStart + 1)
    if (valueEnd == -1 || valueEnd <= valueStart + 1) return message
    return message.substring(valueStart + 1, valueEnd)
}
