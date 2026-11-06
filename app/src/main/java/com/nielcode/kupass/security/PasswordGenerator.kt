package com.nielcode.kupass.security

import java.security.SecureRandom

/**
 * Cryptographically secure password generator using [SecureRandom].
 *
 * Guarantees at least one character from each character category (uppercase, lowercase, digit, and
 * symbol) in every generated password.
 */
object PasswordGenerator {

    const val DEFAULT_LENGTH = 16
    const val MIN_LENGTH = 4

    const val UPPERCASE = "ABCDEFGHIJKLMNOPQRSTUVWXYZ"
    const val LOWERCASE = "abcdefghijklmnopqrstuvwxyz"
    const val DIGITS = "0123456789"
    const val SYMBOLS = "!@#$%^&*()-_=+[]{}|;:,.<>?"
    const val LOOKALIKE_CHARS = "O0Il1|"

    private val FILTERED_UPPERCASE = UPPERCASE.filterNot { it in LOOKALIKE_CHARS }
    private val FILTERED_LOWERCASE = LOWERCASE.filterNot { it in LOOKALIKE_CHARS }
    private val FILTERED_DIGITS = DIGITS.filterNot { it in LOOKALIKE_CHARS }
    private val FILTERED_SYMBOLS = SYMBOLS.filterNot { it in LOOKALIKE_CHARS }
    private val FILTERED_ALL_CHARS =
        FILTERED_UPPERCASE + FILTERED_LOWERCASE + FILTERED_DIGITS + FILTERED_SYMBOLS

    private const val ALL_CHARS = UPPERCASE + LOWERCASE + DIGITS + SYMBOLS
    private val random = SecureRandom()

    /**
     * Generates a cryptographically strong random password of the given [length].
     *
     * @param length The total character length of the generated password, minimum 4.
     * @param excludeLookalike When true, excludes confusing characters defined in
     *   [LOOKALIKE_CHARS].
     * @throws IllegalArgumentException if [length] is less than [MIN_LENGTH].
     */
    fun generate(length: Int = DEFAULT_LENGTH, excludeLookalike: Boolean = false): String {
        require(length >= MIN_LENGTH) {
            "Password length must be at least $MIN_LENGTH, was $length"
        }

        val uppercase = if (excludeLookalike) FILTERED_UPPERCASE else UPPERCASE
        val lowercase = if (excludeLookalike) FILTERED_LOWERCASE else LOWERCASE
        val digits = if (excludeLookalike) FILTERED_DIGITS else DIGITS
        val symbols = if (excludeLookalike) FILTERED_SYMBOLS else SYMBOLS
        val allChars = if (excludeLookalike) FILTERED_ALL_CHARS else ALL_CHARS

        val chars = ArrayList<Char>(length)
        chars.add(uppercase[random.nextInt(uppercase.length)])
        chars.add(lowercase[random.nextInt(lowercase.length)])
        chars.add(digits[random.nextInt(digits.length)])
        chars.add(symbols[random.nextInt(symbols.length)])

        val remaining = length - MIN_LENGTH
        repeat(remaining) {
            chars.add(allChars[random.nextInt(allChars.length)])
        }

        // Fisher-Yates shuffle using SecureRandom
        for (i in chars.size - 1 downTo 1) {
            val j = random.nextInt(i + 1)
            val temp = chars[i]
            chars[i] = chars[j]
            chars[j] = temp
        }

        return chars.joinToString(separator = "")
    }
}
