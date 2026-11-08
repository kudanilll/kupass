package com.nielcode.kupass.autofill

import com.nielcode.kupass.data.local.db.PasswordEntity
import org.junit.Assert.assertEquals
import org.junit.Test

class UnlockLogicTest {

    private val first = PasswordEntity(id = 1, siteName = "GitHub", username = "a", password = "1")
    private val second = PasswordEntity(id = 2, siteName = "GitHub", username = "b", password = "2")

    @Test
    fun `one match fills`() {
        assertEquals(PickOutcome.Fill(first), pickOutcome(listOf(first), AuthKind.Unlock.name))
    }

    @Test
    fun `several matches choose`() {
        assertEquals(
            PickOutcome.Choose(listOf(first, second)),
            pickOutcome(listOf(first, second), AuthKind.Unlock.name),
        )
    }

    @Test
    fun `no match searches`() {
        assertEquals(PickOutcome.Search, pickOutcome(emptyList(), AuthKind.Unlock.name))
    }

    @Test
    fun `search mode always searches`() {
        assertEquals(PickOutcome.Search, pickOutcome(listOf(first), AuthKind.Search.name))
    }
}
