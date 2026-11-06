package com.nielcode.kupass.autofill

import java.util.Locale

/** Finds username and password fields and tells login screens from sign-up screens. */
object FormParser {

    private enum class Kind {
        Username,
        Password,
        NewPassword,
        Ignored,
        Text,
    }

    fun parse(packageName: String, fields: List<FormField>): ParsedForm {
        val kinds = fields.filter { it.isFillable }.associate { it.index to kindOf(it) }
        val passwords =
            kinds.filterValues { it == Kind.Password || it == Kind.NewPassword }.keys.sorted()
        val usernameField = usernameField(kinds, passwords.firstOrNull())
        val byIndex = fields.associateBy { it.index }
        return ParsedForm(
            packageName = packageName,
            webDomain = fields.firstNotNullOfOrNull { it.webDomain },
            usernameField = usernameField,
            passwordFields = passwords,
            isSignUp =
                Kind.NewPassword in kinds.values ||
                    passwords.size >= 2 ||
                    fields.any { field -> SIGN_UP_WORDS.any { it in labelText(field) } },
            usernameValue = usernameField?.let { byIndex[it]?.value },
            passwordValue = passwords.firstOrNull()?.let { byIndex[it]?.value },
        )
    }

    /** A hinted username, preferably the nearest one before the first password field. */
    private fun usernameField(kinds: Map<Int, Kind>, firstPassword: Int?): Int? {
        val hinted = kinds.filterValues { it == Kind.Username }.keys.sorted()
        if (firstPassword == null) return hinted.firstOrNull()
        return hinted.lastOrNull { it < firstPassword }
            ?: hinted.firstOrNull()
            ?: kinds
                .filter { (index, kind) -> kind == Kind.Text && index < firstPassword }
                .keys
                .maxOrNull()
    }

    // Signals in order of trust: autofill hints, HTML attributes, input type, then words.
    private fun kindOf(field: FormField): Kind =
        hintKind(field) ?: htmlKind(field) ?: inputTypeKind(field.inputType) ?: wordKind(field)

    private fun hintKind(field: FormField): Kind? {
        val hints = field.autofillHints.map { it.lowercase(Locale.ROOT) }
        return when {
            hints.any { "otp" in it || "onetimecode" in it } -> Kind.Ignored
            "newpassword" in hints -> Kind.NewPassword
            "password" in hints -> Kind.Password
            hints.any { it in USERNAME_HINTS } -> Kind.Username
            else -> null
        }
    }

    private fun htmlKind(field: FormField): Kind? {
        val tokens =
            field.htmlAttributes["autocomplete"].orEmpty().lowercase(Locale.ROOT).split(' ').toSet()
        val type = field.htmlAttributes["type"]?.lowercase(Locale.ROOT)
        return when {
            "one-time-code" in tokens -> Kind.Ignored
            "new-password" in tokens -> Kind.NewPassword
            "current-password" in tokens || type == "password" -> Kind.Password
            "username" in tokens || "email" in tokens || type == "email" -> Kind.Username
            type == "search" -> Kind.Ignored
            else -> null
        }
    }

    private fun inputTypeKind(inputType: Int): Kind? {
        val variation = inputType and TYPE_MASK_VARIATION
        return when (inputType and TYPE_MASK_CLASS) {
            TYPE_CLASS_TEXT ->
                when (variation) {
                    in TEXT_PASSWORD_VARIATIONS -> Kind.Password
                    in TEXT_EMAIL_VARIATIONS -> Kind.Username
                    else -> null
                }
            TYPE_CLASS_NUMBER -> if (variation == NUMBER_VARIATION_PASSWORD) Kind.Password else null
            else -> null
        }
    }

    private fun wordKind(field: FormField): Kind {
        val text = labelText(field)
        return when {
            SEARCH_WORDS.any { it in text } -> Kind.Ignored
            PASSWORD_WORDS.any { it in text } -> Kind.Password
            USERNAME_WORDS.any { it in text } -> Kind.Username
            else -> Kind.Text
        }
    }

    /** Describing text only. A fillable field's [FormField.value] is user input, never a label. */
    private fun labelText(field: FormField): String =
        listOfNotNull(
                field.idEntry,
                field.hint,
                field.contentDescription,
                field.htmlAttributes["name"],
                field.htmlAttributes["id"],
                field.value.takeUnless { field.isFillable },
            )
            .joinToString(" ")
            .lowercase(Locale.ROOT)

    private val USERNAME_HINTS = setOf("username", "emailaddress", "newusername", "email")
    private val PASSWORD_WORDS = listOf("password", "passwd", "pwd", "sandi")
    private val USERNAME_WORDS = listOf("user", "email", "e-mail", "login", "pengguna", "akun")
    private val SEARCH_WORDS = listOf("search", "cari")
    private val SIGN_UP_WORDS =
        listOf(
            "sign up",
            "signup",
            "sign_up",
            "register",
            "create account",
            "create_account",
            "daftar",
            "buat akun",
            "confirm",
            "konfirmasi",
        )

    // android.text.InputType values, kept local so the parser stays plain Kotlin.
    private const val TYPE_MASK_CLASS = 0x0000000f
    private const val TYPE_MASK_VARIATION = 0x00000ff0
    private const val TYPE_CLASS_TEXT = 0x00000001
    private const val TYPE_CLASS_NUMBER = 0x00000002
    private const val NUMBER_VARIATION_PASSWORD = 0x00000010
    private val TEXT_PASSWORD_VARIATIONS = setOf(0x80, 0x90, 0xe0)
    private val TEXT_EMAIL_VARIATIONS = setOf(0x20, 0xd0)
}
