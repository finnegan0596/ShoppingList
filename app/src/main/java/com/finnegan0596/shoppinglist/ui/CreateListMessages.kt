package com.finnegan0596.shoppinglist.ui

internal fun createListSuccessMessage(guid: String): String = "Created shared list $guid"

internal fun createListErrorMessage(throwable: Throwable): String {
    val detail = throwable.message?.trim().takeUnless { it.isNullOrEmpty() }
    return if (detail != null) {
        "Could not create list: $detail"
    } else {
        "Could not create list. Check connection and try again."
    }
}
