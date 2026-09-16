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

    @Test
    fun `create error message falls back when exception message is literal null`() {
        val throwable = RuntimeException("null")

        assertEquals(
            "Could not create list. Check connection and try again.",
            createListErrorMessage(throwable)
        )
    }

    @Test
    fun `create error message falls back when exception message is quoted null`() {
        val throwable = RuntimeException("\"null\"")

        assertEquals(
            "Could not create list. Check connection and try again.",
            createListErrorMessage(throwable)
        )
    }

    @Test
    fun `create error message extracts remote error field from json payload`() {
        val throwable = RuntimeException("""{"error":"List not found"}""")

        assertEquals(
            "Could not create list: List not found",
            createListErrorMessage(throwable)
        )
    }

    @Test
    fun `create error message extracts remote error field with whitespace`() {
        val throwable = RuntimeException("""{ "error" : "List not found" }""")

        assertEquals(
            "Could not create list: List not found",
            createListErrorMessage(throwable)
        )
    }

    @Test
    fun `create error message preserves escaped quotes in remote error payload`() {
        val throwable = RuntimeException("""{"error":"bad \"name\""}""")

        assertEquals(
            "Could not create list: bad \"name\"",
            createListErrorMessage(throwable)
        )
    }

    @Test
    fun `create error message falls back when json payload has no error field`() {
        val throwable = RuntimeException("""{"message":"unknown"}""")

        assertEquals(
            "Could not create list. Check connection and try again.",
            createListErrorMessage(throwable)
        )
    }

    @Test
    fun `create error message falls back when payload is json array`() {
        val throwable = RuntimeException("""["unexpected"]""")

        assertEquals(
            "Could not create list. Check connection and try again.",
            createListErrorMessage(throwable)
        )
    }

    @Test
    fun `create error message falls back when error field is nested object`() {
        val throwable = RuntimeException("""{"error":{"message":"nested"}}""")

        assertEquals(
            "Could not create list. Check connection and try again.",
            createListErrorMessage(throwable)
        )
    }

    @Test
    fun `create error message unwraps primitive json string payload`() {
        val throwable = RuntimeException("\"backend down\"")

        assertEquals(
            "Could not create list: backend down",
            createListErrorMessage(throwable)
        )
    }

    @Test
    fun `create error message decodes escaped primitive json string payload`() {
        val throwable = RuntimeException("\"backend\\\\n down\"")

        assertEquals(
            "Could not create list: backend\n down",
            createListErrorMessage(throwable)
        )
    }

    @Test
    fun `create error message falls back for quoted null with whitespace`() {
        val throwable = RuntimeException("\" null \"")

        assertEquals(
            "Could not create list. Check connection and try again.",
            createListErrorMessage(throwable)
        )
    }

    @Test
    fun `create error message falls back for numeric error field`() {
        val throwable = RuntimeException("""{"error":500}""")

        assertEquals(
            "Could not create list. Check connection and try again.",
            createListErrorMessage(throwable)
        )
    }

    @Test
    fun `create error message falls back for boolean error field`() {
        val throwable = RuntimeException("""{"error":true}""")

        assertEquals(
            "Could not create list. Check connection and try again.",
            createListErrorMessage(throwable)
        )
    }

    @Test
    fun `create error message decodes escaped quotes in primitive json payload`() {
        val throwable = RuntimeException("\"bad \\\\\"name\\\\\"\"")

        assertEquals(
            "Could not create list: bad \"name\"",
            createListErrorMessage(throwable)
        )
    }

    @Test
    fun `create error message falls back for malformed quoted payload`() {
        val throwable = RuntimeException("\"{\"")

        assertEquals(
            "Could not create list. Check connection and try again.",
            createListErrorMessage(throwable)
        )
    }

    @Test
    fun `create error message falls back for malformed quoted array payload`() {
        val throwable = RuntimeException("\"[\"")

        assertEquals(
            "Could not create list. Check connection and try again.",
            createListErrorMessage(throwable)
        )
    }

    @Test
    fun `create error message falls back for malformed quoted closing json delimiter`() {
        val throwable = RuntimeException("\"}\"")

        assertEquals(
            "Could not create list. Check connection and try again.",
            createListErrorMessage(throwable)
        )
    }
}
