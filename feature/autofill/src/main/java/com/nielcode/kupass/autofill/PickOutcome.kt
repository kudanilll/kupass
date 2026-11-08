package com.nielcode.kupass.autofill

import com.nielcode.kupass.data.local.db.PasswordEntity

/** What the unlock screen does once the vault is open. */
internal sealed interface PickOutcome {
    data class Fill(val entry: PasswordEntity) : PickOutcome

    data class Choose(val entries: List<PasswordEntity>) : PickOutcome

    data object Search : PickOutcome
}

/** One match fills at once, several ask which, none (or the Search row) searches the vault. */
internal fun pickOutcome(matches: List<PasswordEntity>, mode: String): PickOutcome =
    when {
        mode == AuthKind.Search.name || matches.isEmpty() -> PickOutcome.Search
        matches.size == 1 -> PickOutcome.Fill(matches.single())
        else -> PickOutcome.Choose(matches)
    }
