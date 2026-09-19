package com.nielcode.kupass.ui.screens.home

import android.content.Context
import com.nielcode.kupass.R

/** One-shot results from [HomeViewModel] that the UI shows once (e.g. as a toast). */
sealed interface VaultEvent {
    data object ExportSucceeded : VaultEvent

    data object ExportFailed : VaultEvent

    data object NothingToExport : VaultEvent

    /** [skipped] counts duplicates and invalid rows that were not inserted. */
    data class ImportSucceeded(val imported: Int, val skipped: Int) : VaultEvent

    data object ImportFailed : VaultEvent

    data object ImportTooLarge : VaultEvent

    data object BackupFromOtherDevice : VaultEvent

    data object BackupUnsupported : VaultEvent

    data object VaultReadFailed : VaultEvent
}

/** User-facing text for [event]. */
fun VaultEvent.message(context: Context): String =
    when (this) {
        VaultEvent.ExportSucceeded -> context.getString(R.string.toast_success_export)
        VaultEvent.ExportFailed -> context.getString(R.string.toast_failed_export)
        VaultEvent.NothingToExport -> context.getString(R.string.toast_no_data)
        is VaultEvent.ImportSucceeded ->
            if (skipped == 0) {
                context.resources.getQuantityString(
                    R.plurals.toast_import_result,
                    imported,
                    imported,
                )
            } else {
                context.resources.getQuantityString(
                    R.plurals.toast_import_result,
                    imported,
                    imported,
                ) +
                    " " +
                    context.resources.getQuantityString(
                        R.plurals.toast_import_skipped,
                        skipped,
                        skipped,
                    )
            }
        VaultEvent.ImportFailed -> context.getString(R.string.toast_failed_import)
        VaultEvent.ImportTooLarge -> context.getString(R.string.toast_import_too_large)
        VaultEvent.BackupFromOtherDevice -> context.getString(R.string.toast_backup_foreign_device)
        VaultEvent.BackupUnsupported -> context.getString(R.string.toast_backup_unsupported)
        VaultEvent.VaultReadFailed -> context.getString(R.string.toast_vault_read_failed)
    }
