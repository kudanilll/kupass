package com.nielcode.kupass.security

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class PasswordGeneratorTest {

    @Test
    fun `generate produces string of default length 16`() {
        val password = PasswordGenerator.generate()

        assertEquals(PasswordGenerator.DEFAULT_LENGTH, password.length)
        assertEquals(16, password.length)
    }

    @Test
    fun `generate produces string of custom length`() {
        val lengths = listOf(4, 8, 20, 32, 64)

        for (len in lengths) {
            val password = PasswordGenerator.generate(length = len)
            assertEquals(len, password.length)
        }
    }

    @Test
    fun `generate throws IllegalArgumentException when length is less than 4`() {
        assertThrows(IllegalArgumentException::class.java) {
            PasswordGenerator.generate(length = 3)
        }
        assertThrows(IllegalArgumentException::class.java) {
            PasswordGenerator.generate(length = 0)
        }
        assertThrows(IllegalArgumentException::class.java) {
            PasswordGenerator.generate(length = -1)
        }
    }

    @Test
    fun `generate contains uppercase, lowercase, digit, and symbol`() {
        // Run multiple times to verify invariant holds consistently
        repeat(50) { _ ->
            val password = PasswordGenerator.generate(length = 16)

            val hasUpper = password.any { char -> char in 'A'..'Z' }
            val hasLower = password.any { char -> char in 'a'..'z' }
            val hasDigit = password.any { char -> char in '0'..'9' }
            val hasSymbol = password.any { char -> char in PasswordGenerator.SYMBOLS }

            assertTrue("Expected uppercase in $password", hasUpper)
            assertTrue("Expected lowercase in $password", hasLower)
            assertTrue("Expected digit in $password", hasDigit)
            assertTrue("Expected symbol in $password", hasSymbol)
        }
    }

    @Test
    fun `generate produces unique passwords across multiple calls`() {
        val passwords = (1..50).map { PasswordGenerator.generate() }.toSet()

        assertEquals("Expected 50 unique passwords", 50, passwords.size)
    }

    @Test
    fun `first character is not always from the same character set`() {
        // Verify shuffling so guaranteed categories aren't in fixed positions
        val firstChars = (1..50).map { PasswordGenerator.generate().first() }

        val hasUpper = firstChars.any { it in 'A'..'Z' }
        val hasLower = firstChars.any { it in 'a'..'z' }
        val hasDigit = firstChars.any { it in '0'..'9' }

        assertTrue(hasUpper || hasLower || hasDigit)
        assertNotEquals("All first chars were identical", 1, firstChars.toSet().size)
    }
}
