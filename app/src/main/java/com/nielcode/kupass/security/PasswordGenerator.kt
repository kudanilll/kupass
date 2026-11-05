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

    private const val ALL_CHARS = UPPERCASE + LOWERCASE + DIGITS + SYMBOLS
    private val random = SecureRandom()

    /**
     * Generates a cryptographically strong random password of the given [length].
     *
     * @param length The total character length of the generated password, minimum 4.
     * @throws IllegalArgumentException if [length] is less than [MIN_LENGTH].
     */
    fun generate(length: Int = DEFAULT_LENGTH): String {
        require(length >= MIN_LENGTH) {
            "Password length must be at least $MIN_LENGTH, was $length"
        }

        val chars = ArrayList<Char>(length)
        chars.add(UPPERCASE[random.nextInt(UPPERCASE.length)])
        chars.add(LOWERCASE[random.nextInt(LOWERCASE.length)])
        chars.add(DIGITS[random.nextInt(DIGITS.length)])
        chars.add(SYMBOLS[random.nextInt(SYMBOLS.length)])

        val remaining = length - MIN_LENGTH
        repeat(remaining) {
            chars.add(ALL_CHARS[random.nextInt(ALL_CHARS.length)])
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
