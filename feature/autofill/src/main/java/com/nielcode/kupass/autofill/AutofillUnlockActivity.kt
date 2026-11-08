package com.nielcode.kupass.autofill

import android.app.KeyguardManager
import android.app.assist.AssistStructure
import android.content.Intent
import android.os.Bundle
import android.view.autofill.AutofillManager
import android.widget.Toast
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.biometric.BiometricPrompt
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.core.content.ContextCompat
import androidx.core.content.IntentCompat
import androidx.lifecycle.lifecycleScope
import com.nielcode.kupass.data.local.db.PasswordEntity
import com.nielcode.kupass.feature.autofill.R
import com.nielcode.kupass.security.kupassAuthenticators
import com.nielcode.kupass.ui.theme.KupassTheme
import com.nielcode.kupass.utils.AppConfig
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * Verifies the user (biometrics or device credential), starts the autofill session, then fills the
 * matching account or lets the user pick one. Nothing about the vault is shown before verification.
 */
class AutofillUnlockActivity : AppCompatActivity() {

    private class Request(
        val read: ReadStructure,
        val form: ParsedForm,
        val target: AutofillTarget,
        val mode: String,
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val request = request()
        val session = AutofillGraph.session(this)
        when {
            request == null -> finishCanceled()
            request.mode == AuthKind.Search.name && session.isActive() -> open(request)
            !getSystemService(KeyguardManager::class.java).isDeviceSecure -> {
                Toast.makeText(this, R.string.autofill_no_lock_screen, Toast.LENGTH_LONG).show()
                finishCanceled()
            }
            else -> authenticate(request)
        }
    }

    /** The target comes from the structure the system attached, not from our own extras. */
    private fun request(): Request? {
        val mode = intent.getStringExtra(EXTRA_MODE)?.takeIf { it in UNLOCK_MODES }
        val structure =
            IntentCompat.getParcelableExtra(
                intent,
                AutofillManager.EXTRA_ASSIST_STRUCTURE,
                AssistStructure::class.java,
            )
        return structure?.let(StructureReader::read)?.let { read ->
            FormParser.parse(read.packageName, read.fields)
                .takeUnless { it.isEmpty }
                ?.let { form ->
                    TargetResolver.resolve(form.packageName, form.webDomain, packageName)?.let {
                        target ->
                        mode?.let { Request(read, form, target, it) }
                    }
                }
        }
    }

    private fun authenticate(request: Request) {
        val callback =
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(
                    result: BiometricPrompt.AuthenticationResult
                ) {
                    AutofillGraph.session(this@AutofillUnlockActivity).start()
                    open(request)
                }

                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    finishCanceled()
                }
            }
        BiometricPrompt(this, ContextCompat.getMainExecutor(this), callback)
            .authenticate(
                BiometricPrompt.PromptInfo.Builder()
                    .setTitle(getString(R.string.autofill_prompt_title))
                    .setSubtitle(getString(R.string.autofill_prompt_subtitle))
                    .setAllowedAuthenticators(kupassAuthenticators())
                    .build()
            )
    }

    private fun open(request: Request) {
        lifecycleScope.launch {
            val entries =
                AutofillGraph.core(this@AutofillUnlockActivity)
                    .passwordRepository
                    .getAllPasswords()
                    .first()
            when (
                val outcome =
                    pickOutcome(CredentialMatcher.matches(entries, request.target), request.mode)
            ) {
                is PickOutcome.Fill -> fill(request, outcome.entry)
                is PickOutcome.Choose -> showPicker(request, outcome.entries, search = false)
                PickOutcome.Search -> showPicker(request, entries, search = true)
            }
        }
    }

    private fun showPicker(request: Request, entries: List<PasswordEntity>, search: Boolean) {
        val core = AutofillGraph.core(this)
        val dynamicColor =
            core.preferenceManager.dynamicColor == AppConfig.DynamicColors.Code.ENABLE
        setContent {
            KupassTheme(dynamicColor = dynamicColor) {
                var query by rememberSaveable { mutableStateOf("") }
                val results by
                    remember(query) { core.passwordRepository.searchPasswords(query) }
                        .collectAsState(initial = entries)
                AutofillPicker(
                    entries = if (search) results else entries,
                    query = query.takeIf { search },
                    onQueryChange = { query = it },
                    onPick = { fill(request, it) },
                    onDismiss = ::finishCanceled,
                )
            }
        }
    }

    private fun fill(request: Request, entry: PasswordEntity) {
        val dataset =
            FillResponseBuilder.dataset(
                this,
                ResponsePlanner.entryDataset(request.form, entry, autofillStrings()),
                request.read.ids,
            )
        if (dataset == null) {
            finishCanceled()
            return
        }
        AutofillGraph.session(this).touch()
        setResult(
            RESULT_OK,
            Intent().putExtra(AutofillManager.EXTRA_AUTHENTICATION_RESULT, dataset),
        )
        finish()
    }

    private fun finishCanceled() {
        setResult(RESULT_CANCELED)
        finish()
    }

    private companion object {
        val UNLOCK_MODES = setOf(AuthKind.Unlock.name, AuthKind.Search.name)
    }
}
