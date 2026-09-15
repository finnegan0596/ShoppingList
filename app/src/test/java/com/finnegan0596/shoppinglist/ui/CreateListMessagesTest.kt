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
}
