package com.nielcode.kupass.autofill

import android.app.assist.AssistStructure
import android.content.Context
import android.os.CancellationSignal
import android.service.autofill.AutofillService
import android.service.autofill.FillCallback
import android.service.autofill.FillRequest
import android.service.autofill.FillResponse
import android.service.autofill.SaveCallback
import android.service.autofill.SaveRequest
import com.nielcode.kupass.feature.autofill.R
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * Kupass's Android autofill service. Never logs form or vault content, never touches the network,
 * and answers every request (with `null` on any error) so the host app is never left waiting.
 */
class KupassAutofillService : AutofillService() {

    override fun onFillRequest(
        request: FillRequest,
        cancellationSignal: CancellationSignal,
        callback: FillCallback,
    ) {
        val structure = request.fillContexts.lastOrNull()?.structure
        if (structure == null) {
            callback.onSuccess(null)
            return
        }
        val job =
            AutofillGraph.scope.launch {
                val response = runCatching { fillResponse(structure) }.getOrNull()
                if (isActive) callback.onSuccess(response)
            }
        cancellationSignal.setOnCancelListener { job.cancel() }
    }

    override fun onSaveRequest(request: SaveRequest, callback: SaveCallback) {
        val structure = request.fillContexts.lastOrNull()?.structure
        if (structure == null) {
            callback.onSuccess()
            return
        }
        // Process-lifetime scope: a confirmed save must finish even if the service is unbound.
        AutofillGraph.scope.launch {
            if (runCatching { save(structure) }.isSuccess) {
                callback.onSuccess()
            } else {
                callback.onFailure(getString(R.string.autofill_save_failed))
            }
        }
    }

    private suspend fun fillResponse(structure: AssistStructure): FillResponse? {
        val read = StructureReader.read(structure)
        val form = FormParser.parse(read.packageName, read.fields)
        val target =
            TargetResolver.resolve(read.packageName, form.webDomain, packageName).takeUnless {
                form.isEmpty
            } ?: return null
        val core = AutofillGraph.core(this)
        val matches =
            CredentialMatcher.matches(core.passwordRepository.getAllPasswords().first(), target)
        val attachSave =
            shouldAttachSaveInfo(
                form,
                matches.isNotEmpty(),
                core.preferenceManager.offerToSaveLogins,
            )
        val strings = autofillStrings()
        val plan =
            if (AutofillGraph.session(this).isActive()) {
                ResponsePlanner.unlocked(form, matches, strings, attachSave)
            } else {
                ResponsePlanner.locked(form, strings, attachSave)
            }
        return FillResponseBuilder.response(this, plan, read.ids)
    }

    private suspend fun save(structure: AssistStructure) {
        val read = StructureReader.read(structure)
        val form = FormParser.parse(read.packageName, read.fields)
        val target = TargetResolver.resolve(read.packageName, form.webDomain, packageName) ?: return
        val repository = AutofillGraph.core(this).passwordRepository
        val decision =
            resolveSave(
                repository.getAllPasswords().first(),
                target,
                form.usernameValue.orEmpty(),
                form.passwordValue.orEmpty(),
                appLabelOf(target),
            )
        when (decision) {
            SaveDecision.None -> Unit
            is SaveDecision.Insert -> repository.insertPassword(decision.entry)
            is SaveDecision.Update -> repository.updatePassword(decision.entry)
        }
    }
}

internal fun Context.autofillStrings() =
    AutofillStrings(
        unlockTitle = getString(R.string.autofill_unlock_title),
        unlockSubtitle = getString(R.string.autofill_unlock_subtitle),
        generateTitle = getString(R.string.autofill_generate_title),
        generateSubtitle = getString(R.string.autofill_generate_subtitle),
        searchTitle = getString(R.string.autofill_search_title),
        noUsername = getString(R.string.autofill_no_username),
    )
