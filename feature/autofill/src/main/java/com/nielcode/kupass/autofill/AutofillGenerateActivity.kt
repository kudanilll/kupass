package com.nielcode.kupass.autofill

import android.app.assist.AssistStructure
import android.content.Intent
import android.os.Bundle
import android.view.autofill.AutofillManager
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.core.content.IntentCompat
import androidx.lifecycle.lifecycleScope
import com.nielcode.kupass.feature.autofill.R
import kotlinx.coroutines.launch

/**
 * No UI: generates a strong password, saves it with the typed username, and only then fills every
 * password field. Needs no verification because it reveals nothing from the vault.
 */
class AutofillGenerateActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // The target comes from the structure the system attached, not from our own extras.
        val read =
            IntentCompat.getParcelableExtra(
                    intent,
                    AutofillManager.EXTRA_ASSIST_STRUCTURE,
                    AssistStructure::class.java,
                )
                ?.let(StructureReader::read)
        val form =
            read
                ?.let { FormParser.parse(it.packageName, it.fields) }
                ?.takeIf { it.passwordFields.isNotEmpty() }
        val target = form?.let { TargetResolver.resolve(it.packageName, it.webDomain, packageName) }
        if (read == null || form == null || target == null) {
            finishCanceled()
            return
        }
        lifecycleScope.launch {
            val core = AutofillGraph.core(this@AutofillGenerateActivity)
            val password =
                core.passwordRepository.generateAndSave(
                    core.preferenceManager.excludeLookalike,
                    target,
                    form,
                    appLabelOf(target),
                )
            val dataset = password?.let {
                FillResponseBuilder.dataset(
                    this@AutofillGenerateActivity,
                    ResponsePlanner.generatedDataset(form, it),
                    read.ids,
                )
            }
            if (dataset == null) {
                Toast.makeText(
                        this@AutofillGenerateActivity,
                        R.string.autofill_generate_failed,
                        Toast.LENGTH_LONG,
                    )
                    .show()
                finishCanceled()
            } else {
                setResult(
                    RESULT_OK,
                    Intent().putExtra(AutofillManager.EXTRA_AUTHENTICATION_RESULT, dataset),
                )
                finish()
            }
        }
    }

    private fun finishCanceled() {
        setResult(RESULT_CANCELED)
        finish()
    }
}
