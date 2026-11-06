package com.nielcode.kupass.autofill

import android.content.Context
import android.content.pm.PackageManager
import com.nielcode.kupass.data.repository.PasswordRepository
import com.nielcode.kupass.security.PasswordGenerator
import kotlinx.coroutines.CancellationException

/**
 * Generates a password and stores it with the typed username **before** anything is filled. Returns
 * the password only after the insert succeeded, so a filled password is always saved. Each call is
 * a new entry: an earlier suggestion may already be an account's real password.
 */
internal suspend fun PasswordRepository.generateAndSave(
    excludeLookalike: Boolean,
    target: AutofillTarget,
    form: ParsedForm,
    appLabel: String?,
    generate: (Int, Boolean) -> String = PasswordGenerator::generate,
): String? {
    val password = generate(PasswordGenerator.DEFAULT_LENGTH, excludeLookalike)
    val entry = newEntry(target, appLabel, form.usernameValue.orEmpty(), password)
    return runCatching { insertPassword(entry) }
        .onFailure { if (it is CancellationException) throw it }
        .map { password }
        .getOrNull()
}

/** The installed app's name, or `null` when it is not visible to Kupass (or for websites). */
internal fun Context.appLabelOf(target: AutofillTarget): String? =
    (target as? AutofillTarget.App)?.let { app ->
        try {
            packageManager
                .getApplicationLabel(packageManager.getApplicationInfo(app.packageName, 0))
                .toString()
        } catch (_: PackageManager.NameNotFoundException) {
            null
        }
    }
