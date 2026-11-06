package com.nielcode.kupass.autofill

import com.nielcode.kupass.data.androidPackageOf
import com.nielcode.kupass.data.androidUrlOf
import com.nielcode.kupass.data.local.db.PasswordEntity
import com.nielcode.kupass.data.webHostOf

/** Vault entries for a target. Strict: exact package or exact normalized host, never site names. */
object CredentialMatcher {
    fun matches(entries: List<PasswordEntity>, target: AutofillTarget): List<PasswordEntity> =
        entries.filter {
            it.belongsTo(target)
        }
}

private fun PasswordEntity.belongsTo(target: AutofillTarget): Boolean =
    when (target) {
        is AutofillTarget.App -> androidPackageOf(url) == target.packageName
        is AutofillTarget.Web ->
            !url.trim().startsWith("android://", ignoreCase = true) &&
                webHostOf(url.trim()) == target.host
    }

/** The entry autofill saves: app label (or package) for apps, the host for websites. */
fun newEntry(
    target: AutofillTarget,
    appLabel: String?,
    username: String,
    password: String,
): PasswordEntity =
    when (target) {
        is AutofillTarget.App ->
            PasswordEntity(
                siteName = appLabel?.trim()?.takeIf { it.isNotEmpty() } ?: target.packageName,
                username = username,
                password = password,
                url = androidUrlOf(target.packageName),
            )
        is AutofillTarget.Web ->
            PasswordEntity(
                siteName = target.host,
                username = username,
                password = password,
                url = "https://${target.host}",
            )
    }

sealed interface SaveDecision {
    data object None : SaveDecision

    data class Insert(val entry: PasswordEntity) : SaveDecision

    data class Update(val entry: PasswordEntity) : SaveDecision
}

/**
 * What a confirmed save request does. An empty password never saves. The same password for the same
 * target only gains a username it lacked (a generated entry finished at submit); anything else is a
 * new entry. Existing passwords are never overwritten.
 */
fun resolveSave(
    entries: List<PasswordEntity>,
    target: AutofillTarget,
    username: String,
    password: String,
    appLabel: String?,
): SaveDecision {
    val existing =
        CredentialMatcher.matches(entries, target)
            .filter { it.password == password }
            .maxByOrNull { it.updatedAt }
    return when {
        password.isEmpty() -> SaveDecision.None
        existing == null -> SaveDecision.Insert(newEntry(target, appLabel, username, password))
        username.isBlank() || existing.username == username -> SaveDecision.None
        else ->
            SaveDecision.Update(
                existing.copy(username = username, updatedAt = System.currentTimeMillis())
            )
    }
}

/** Sign-ups always offer saving; logins only when allowed and Kupass has nothing for the target. */
fun shouldAttachSaveInfo(
    form: ParsedForm,
    hasEntryForTarget: Boolean,
    offerToSave: Boolean,
): Boolean =
    form.passwordFields.isNotEmpty() && (form.isSignUp || (offerToSave && !hasEntryForTarget))
