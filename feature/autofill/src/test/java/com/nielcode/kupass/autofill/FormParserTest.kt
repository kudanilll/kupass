package com.nielcode.kupass.autofill

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class FormParserTest {

    @Test
    fun `a login form in a third-party frame uses that frame's domain`() {
        val form =
            parse(
                FormField(0, webDomain = "bank.com", isFillable = false),
                FormField(1, autofillHints = listOf("username"), webDomain = "ads.example"),
                FormField(2, autofillHints = listOf("password"), webDomain = "ads.example"),
            )
        assertEquals("ads.example", form.webDomain)
    }

    @Test
    fun `fields from another frame are left out`() {
        val form =
            parse(
                FormField(0, webDomain = "bank.com", isFillable = false),
                FormField(1, autofillHints = listOf("username"), webDomain = "bank.com"),
                FormField(
                    2,
                    autofillHints = listOf("password"),
                    webDomain = "bank.com",
                    isFocused = true,
                ),
                FormField(3, autofillHints = listOf("password"), webDomain = "ads.example"),
            )
        assertEquals("bank.com", form.webDomain)
        assertShape(form, username = 1, passwords = listOf(2), signUp = false)
    }

    @Test
    fun `the focused field picks the frame`() {
        val form =
            parse(
                FormField(1, autofillHints = listOf("password"), webDomain = "bank.com"),
                FormField(
                    2,
                    autofillHints = listOf("password"),
                    webDomain = "ads.example",
                    isFocused = true,
                ),
            )
        assertEquals("ads.example", form.webDomain)
        assertEquals(listOf(2), form.passwordFields)
    }

    @Test
    fun `without focus the first password field picks the frame`() {
        val form =
            parse(
                FormField(0, autofillHints = listOf("username"), webDomain = "ads.example"),
                FormField(1, autofillHints = listOf("username"), webDomain = "bank.com"),
                FormField(2, autofillHints = listOf("password"), webDomain = "bank.com"),
            )
        assertEquals("bank.com", form.webDomain)
        assertShape(form, username = 1, passwords = listOf(2), signUp = false)
    }

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
                    webDomain = "github.com",
                ),
            )
        assertShape(form, username = 0, passwords = listOf(1), signUp = false)
        assertEquals("github.com", form.webDomain)
    }

    @Test
    fun `the browser's own fields stay out of a web form`() {
        val form =
            parse(
                FormField(0, hint = "Address"),
                FormField(1, autofillHints = listOf("password"), webDomain = "github.com"),
            )
        assertShape(form, username = null, passwords = listOf(1), signUp = false)
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
    fun `a login page with a sign-up link stays a login`() {
        val form =
            parse(
                FormField(0, autofillHints = listOf("username")),
                FormField(1, autofillHints = listOf("password")),
                FormField(2, value = "Belum punya akun? Daftar", isFillable = false),
                FormField(3, value = "Sign up", isFillable = false),
                FormField(4, autofillHints = listOf("password"), isFillable = false),
            )
        assertShape(form, username = 0, passwords = listOf(1), signUp = false)
    }

    @Test
    fun `sign-up words on the credential field itself mark a sign-up`() {
        val form =
            parse(
                FormField(0, idEntry = "email"),
                FormField(1, inputType = PASSWORD_INPUT, idEntry = "register_password"),
            )
        assertShape(form, username = 0, passwords = listOf(1), signUp = true)
        assertTrue(
            parse(
                    FormField(
                        0,
                        htmlAttributes = mapOf("type" to "password", "name" to "new-password"),
                    )
                )
                .isSignUp
        )
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

    @Test
    fun `change-password form saves the new password, not the current one`() {
        val form =
            parse(
                FormField(0, autofillHints = listOf("username"), value = "dan"),
                FormField(
                    1,
                    htmlAttributes = mapOf("autocomplete" to "current-password"),
                    value = "old",
                ),
                FormField(
                    2,
                    htmlAttributes = mapOf("autocomplete" to "new-password"),
                    value = "new",
                ),
                FormField(
                    3,
                    htmlAttributes = mapOf("autocomplete" to "new-password"),
                    value = "new",
                ),
            )
        assertShape(
            form,
            username = 0,
            passwords = listOf(1, 2, 3),
            signUp = true,
            current = listOf(1),
        )
        assertEquals("new", form.passwordValue)
    }

    @Test
    fun `password hint next to newPassword hints is the current password`() {
        val form =
            parse(
                FormField(0, autofillHints = listOf("password"), value = "old"),
                FormField(1, autofillHints = listOf("newPassword"), value = "new"),
            )
        assertShape(
            form,
            username = null,
            passwords = listOf(0, 1),
            signUp = true,
            current = listOf(0),
        )
        assertEquals("new", form.passwordValue)
    }

    @Test
    fun `old-password label marks the current password`() {
        val form =
            parse(
                FormField(0, inputType = PASSWORD_INPUT, idEntry = "old_password", value = "old"),
                FormField(1, inputType = PASSWORD_INPUT, idEntry = "new_password", value = "new"),
            )
        assertShape(
            form,
            username = null,
            passwords = listOf(0, 1),
            signUp = true,
            current = listOf(0),
        )
        assertEquals("new", form.passwordValue)
    }

    @Test
    fun `first of three unhinted password fields is the current password`() {
        val form =
            parse(
                FormField(0, inputType = PASSWORD_INPUT, value = "old"),
                FormField(1, inputType = PASSWORD_INPUT, value = "new"),
                FormField(2, inputType = PASSWORD_INPUT, value = "new"),
            )
        assertShape(
            form,
            username = null,
            passwords = listOf(0, 1, 2),
            signUp = true,
            current = listOf(0),
        )
        assertEquals("new", form.passwordValue)
    }

    @Test
    fun `a lone current-password field is a login`() {
        val form =
            parse(
                FormField(0, htmlAttributes = mapOf("type" to "email")),
                FormField(
                    1,
                    htmlAttributes = mapOf("autocomplete" to "current-password"),
                    value = "pw",
                ),
            )
        assertShape(form, username = 0, passwords = listOf(1), signUp = false)
        assertEquals("pw", form.passwordValue)
    }

    private fun parse(vararg fields: FormField) =
        FormParser.parse("com.example.app", fields.toList())

    private fun assertShape(
        form: ParsedForm,
        username: Int?,
        passwords: List<Int>,
        signUp: Boolean,
        current: List<Int> = emptyList(),
    ) {
        if (username == null) assertNull(form.usernameField)
        else assertEquals(username, form.usernameField)
        assertEquals(passwords, form.passwordFields)
        assertEquals(signUp, form.isSignUp)
        assertEquals(current, form.currentPasswordFields)
    }

    private companion object {
        const val TEXT_INPUT = 0x01
        const val PASSWORD_INPUT = 0x81 // TYPE_CLASS_TEXT | TYPE_TEXT_VARIATION_PASSWORD
        const val EMAIL_INPUT = 0x21 // TYPE_CLASS_TEXT | TYPE_TEXT_VARIATION_EMAIL_ADDRESS
    }
}
