package com.nielcode.kupass.autofill

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class FormParserTest {

    @Test
    fun `username and password login`() {
        val form =
            parse(
                FormField(0, autofillHints = listOf("username")),
                FormField(1, autofillHints = listOf("password")),
            )
        assertShape(form, username = 0, passwords = listOf(1), signUp = false)
    }

    @Test
    fun `email login by inputType`() {
        val form =
            parse(FormField(0, inputType = EMAIL_INPUT), FormField(1, inputType = PASSWORD_INPUT))
        assertShape(form, username = 0, passwords = listOf(1), signUp = false)
    }

    @Test
    fun `sign-up with confirmation`() {
        val form =
            parse(
                FormField(0, idEntry = "email"),
                FormField(1, inputType = PASSWORD_INPUT, idEntry = "password"),
                FormField(2, inputType = PASSWORD_INPUT, idEntry = "confirm_password"),
            )
        assertShape(form, username = 0, passwords = listOf(1, 2), signUp = true)
    }

    @Test
    fun `new-password hint`() {
        val form =
            parse(
                FormField(0, autofillHints = listOf("username")),
                FormField(1, autofillHints = listOf("newPassword")),
            )
        assertTrue(form.isSignUp)
        assertEquals(listOf(1), form.passwordFields)
    }

    @Test
    fun `web login via html info`() {
        val form =
            parse(
                FormField(
                    0,
                    htmlAttributes = mapOf("type" to "email", "name" to "login"),
                    webDomain = "github.com",
                ),
                FormField(
                    1,
                    htmlAttributes =
                        mapOf("type" to "password", "autocomplete" to "current-password"),
                ),
            )
        assertShape(form, username = 0, passwords = listOf(1), signUp = false)
        assertEquals("github.com", form.webDomain)
    }

    @Test
    fun `username-only step`() {
        val form = parse(FormField(0, autofillHints = listOf("emailAddress")))
        assertShape(form, username = 0, passwords = emptyList(), signUp = false)
    }

    @Test
    fun `password-only step`() {
        val form = parse(FormField(0, inputType = PASSWORD_INPUT))
        assertShape(form, username = null, passwords = listOf(0), signUp = false)
    }

    @Test
    fun `indonesian labels`() {
        val form =
            parse(
                FormField(0, hint = "Email atau nama pengguna"),
                FormField(1, hint = "Kata sandi"),
            )
        assertShape(form, username = 0, passwords = listOf(1), signUp = false)
    }

    @Test
    fun `one-time code ignored`() {
        val form =
            parse(
                FormField(0, autofillHints = listOf("username")),
                FormField(1, autofillHints = listOf("password")),
                FormField(2, htmlAttributes = mapOf("autocomplete" to "one-time-code")),
                FormField(3, autofillHints = listOf("smsOTPCode")),
            )
        assertShape(form, username = 0, passwords = listOf(1), signUp = false)
    }

    @Test
    fun `search field ignored`() {
        val form =
            parse(
                FormField(0, htmlAttributes = mapOf("type" to "search")),
                FormField(1, hint = "Cari akun"),
            )
        assertTrue(form.isEmpty)
    }

    @Test
    fun `values are read from the chosen fields`() {
        val form =
            parse(
                FormField(0, autofillHints = listOf("username"), value = "dan"),
                FormField(1, autofillHints = listOf("password"), value = "pw"),
            )
        assertEquals("dan", form.usernameValue)
        assertEquals("pw", form.passwordValue)
    }

    @Test
    fun `unhinted text field before the password is the username`() {
        val form =
            parse(FormField(0, inputType = TEXT_INPUT), FormField(1, inputType = PASSWORD_INPUT))
        assertShape(form, username = 0, passwords = listOf(1), signUp = false)
    }

    @Test
    fun `non-fillable fields are skipped but their labels can mark a sign-up`() {
        val form =
            parse(
                FormField(0, autofillHints = listOf("username")),
                FormField(1, autofillHints = listOf("password")),
                FormField(2, value = "Daftar", isFillable = false),
                FormField(3, autofillHints = listOf("password"), isFillable = false),
            )
        assertShape(form, username = 0, passwords = listOf(1), signUp = true)
    }

    @Test
    fun `typed values never decide the form kind`() {
        val form =
            parse(
                FormField(0, autofillHints = listOf("username"), value = "register"),
                FormField(1, autofillHints = listOf("password"), value = "confirm-sign-up"),
            )
        assertFalse(form.isSignUp)
    }

    private fun parse(vararg fields: FormField) =
        FormParser.parse("com.example.app", fields.toList())

    private fun assertShape(
        form: ParsedForm,
        username: Int?,
        passwords: List<Int>,
        signUp: Boolean,
    ) {
        if (username == null) assertNull(form.usernameField)
        else assertEquals(username, form.usernameField)
        assertEquals(passwords, form.passwordFields)
        assertEquals(signUp, form.isSignUp)
    }

    private companion object {
        const val TEXT_INPUT = 0x01
        const val PASSWORD_INPUT = 0x81 // TYPE_CLASS_TEXT | TYPE_TEXT_VARIATION_PASSWORD
        const val EMAIL_INPUT = 0x21 // TYPE_CLASS_TEXT | TYPE_TEXT_VARIATION_EMAIL_ADDRESS
    }
}
