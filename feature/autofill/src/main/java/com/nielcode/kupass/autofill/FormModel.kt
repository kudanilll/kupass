package com.nielcode.kupass.autofill

/**
 * A plain snapshot of one `AssistStructure.ViewNode`, so form heuristics run without framework
 * types. [value] holds what the user typed (a secret); Android leaves it empty in fill requests.
 */
data class FormField(
    val index: Int,
    val autofillHints: List<String> = emptyList(),
    /** Lower-case HTML attribute names: `type`, `autocomplete`, `name`, `id`. */
    val htmlAttributes: Map<String, String> = emptyMap(),
    val inputType: Int = 0,
    val idEntry: String? = null,
    val hint: String? = null,
    val contentDescription: String? = null,
    val value: String? = null,
    val webDomain: String? = null,
    val isFocused: Boolean = false,
    /** Visible, enabled, text-autofillable, and not marked `importantForAutofill = no`. */
    val isFillable: Boolean = true,
)

/** The credential fields of one screen, by [FormField.index]. */
data class ParsedForm(
    val packageName: String,
    val webDomain: String?,
    val usernameField: Int?,
    val passwordFields: List<Int>,
    val isSignUp: Boolean,
    val usernameValue: String?,
    val passwordValue: String?,
) {
    val isEmpty: Boolean
        get() = usernameField == null && passwordFields.isEmpty()
}
