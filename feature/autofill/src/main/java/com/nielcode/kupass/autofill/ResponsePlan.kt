package com.nielcode.kupass.autofill

import com.nielcode.kupass.data.local.db.PasswordEntity

/** What a dropdown row opens before it can fill: unlock, search the vault, or generate. */
enum class AuthKind {
    Unlock,
    Search,
    Generate,
}

/** One dropdown row. [values] maps field index to the value it fills (`null` = filled later). */
data class DatasetPlan(
    val title: String,
    val subtitle: String?,
    val values: Map<Int, String?>,
    val auth: AuthKind?,
)

data class SaveFields(val required: List<Int>, val optional: List<Int>)

data class ResponsePlan(val datasets: List<DatasetPlan>, val saveFields: SaveFields?)

/** Localized row labels, passed in so the planner stays plain Kotlin. */
data class AutofillStrings(
    val unlockTitle: String,
    val unlockSubtitle: String,
    val generateTitle: String,
    val generateSubtitle: String,
    val searchTitle: String,
    val noUsername: String,
)

/**
 * Decides what the dropdown shows. Locked plans show only Kupass labels and carry no values; titles
 * and subtitles never contain a password.
 */
object ResponsePlanner {

    private const val MAX_ACCOUNTS = 10

    fun locked(form: ParsedForm, strings: AutofillStrings, attachSave: Boolean): ResponsePlan =
        ResponsePlan(
            datasets =
                listOf(
                    DatasetPlan(
                        title = strings.unlockTitle,
                        subtitle = strings.unlockSubtitle,
                        values = (listOfNotNull(form.usernameField) + form.passwordFields).empty(),
                        auth = AuthKind.Unlock,
                    )
                ) + listOfNotNull(generateRow(form, strings)),
            saveFields = saveFields(form, attachSave),
        )

    fun unlocked(
        form: ParsedForm,
        matches: List<PasswordEntity>,
        strings: AutofillStrings,
        attachSave: Boolean,
    ): ResponsePlan =
        ResponsePlan(
            datasets =
                matches.take(MAX_ACCOUNTS).map { entryDataset(form, it, strings) } +
                    DatasetPlan(
                        title = strings.searchTitle,
                        subtitle = null,
                        values = (listOfNotNull(form.usernameField) + form.passwordFields).empty(),
                        auth = AuthKind.Search,
                    ) +
                    listOfNotNull(generateRow(form, strings)),
            saveFields = saveFields(form, attachSave),
        )

    /** Fills an account. A blank stored username leaves whatever the user typed in place. */
    fun entryDataset(
        form: ParsedForm,
        entry: PasswordEntity,
        strings: AutofillStrings,
    ): DatasetPlan {
        val username = form.usernameField?.takeIf { entry.username.isNotBlank() }
        return DatasetPlan(
            title = entry.username.ifBlank { strings.noUsername },
            subtitle = entry.siteName,
            values =
                listOfNotNull(username?.let { it to entry.username }).toMap() +
                    form.passwordFields.associateWith { entry.password },
            auth = null,
        )
    }

    fun generatedDataset(form: ParsedForm, password: String): DatasetPlan =
        DatasetPlan(
            title = "",
            subtitle = null,
            values = form.passwordFields.associateWith { password },
            auth = null,
        )

    private fun generateRow(form: ParsedForm, strings: AutofillStrings): DatasetPlan? =
        if (form.isSignUp && form.passwordFields.isNotEmpty()) {
            DatasetPlan(
                title = strings.generateTitle,
                subtitle = strings.generateSubtitle,
                values = form.passwordFields.empty(),
                auth = AuthKind.Generate,
            )
        } else {
            null
        }

    private fun saveFields(form: ParsedForm, attachSave: Boolean): SaveFields? =
        if (attachSave) SaveFields(form.passwordFields, listOfNotNull(form.usernameField)) else null

    private fun List<Int>.empty(): Map<Int, String?> = associateWith { null }
}
