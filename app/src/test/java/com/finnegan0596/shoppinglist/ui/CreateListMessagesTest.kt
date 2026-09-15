package com.finnegan0596.shoppinglist.ui

import org.junit.Assert.assertEquals
import org.junit.Test

class CreateListMessagesTest {

    @Test
    fun `successful create message includes guid`() {
        assertEquals(
            "Created shared list 1234-abcd",
            createListSuccessMessage("1234-abcd")
        )
    }

    @Test
    fun `create error message falls back when exception has no message`() {
        val throwable = RuntimeException()

        assertEquals(
            "Could not create list. Check connection and try again.",
            createListErrorMessage(throwable)
        )
    }

    @Test
    fun `create error message includes exception message when available`() {
        val throwable = RuntimeException("backend rejected request")

        assertEquals(
            "Could not create list: backend rejected request",
            createListErrorMessage(throwable)
        )
    }
}
