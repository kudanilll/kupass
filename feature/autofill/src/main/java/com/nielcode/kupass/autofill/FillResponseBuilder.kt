package com.nielcode.kupass.autofill

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.IntentSender
import android.os.Build
import android.service.autofill.Dataset
import android.service.autofill.Field
import android.service.autofill.FillResponse
import android.service.autofill.Presentations
import android.service.autofill.SaveInfo
import android.view.View
import android.view.autofill.AutofillId
import android.view.autofill.AutofillValue
import android.widget.RemoteViews
import com.nielcode.kupass.feature.autofill.R

/** Extra telling [AutofillUnlockActivity] whether it unlocks or searches. */
internal const val EXTRA_MODE = "com.nielcode.kupass.autofill.MODE"

/** Turns a [ResponsePlan] into framework objects. No decisions live here. */
internal object FillResponseBuilder {

    fun response(context: Context, plan: ResponsePlan, ids: List<AutofillId?>): FillResponse? {
        val datasets = plan.datasets.mapNotNull { dataset(context, it, ids) }
        if (datasets.isEmpty()) return null
        val builder = FillResponse.Builder()
        datasets.forEach(builder::addDataset)
        plan.saveFields?.let { saveInfo(it, ids) }?.let(builder::setSaveInfo)
        return builder.build()
    }

    fun dataset(context: Context, plan: DatasetPlan, ids: List<AutofillId?>): Dataset? {
        val values = plan.values.mapNotNull { (index, value) -> ids.getOrNull(index)?.to(value) }
        if (values.isEmpty()) return null
        val presentation = presentation(context, plan)
        val builder =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                Dataset.Builder(Presentations.Builder().setMenuPresentation(presentation).build())
            } else {
                @Suppress("DEPRECATION") // The Presentations constructor needs API 33.
                Dataset.Builder(presentation)
            }
        values.forEach { (id, value) -> builder.put(id, value?.let(AutofillValue::forText)) }
        plan.auth?.let { builder.setAuthentication(authIntent(context, it)) }
        return builder.build()
    }

    private fun Dataset.Builder.put(id: AutofillId, value: AutofillValue?) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            setField(id, Field.Builder().apply { value?.let(::setValue) }.build())
        } else {
            @Suppress("DEPRECATION") // setField needs API 33.
            setValue(id, value)
        }
    }

    private fun presentation(context: Context, plan: DatasetPlan) =
        RemoteViews(context.packageName, R.layout.kupass_autofill_item).apply {
            setTextViewText(R.id.autofill_title, plan.title)
            if (plan.subtitle == null) {
                setViewVisibility(R.id.autofill_subtitle, View.GONE)
            } else {
                setTextViewText(R.id.autofill_subtitle, plan.subtitle)
            }
        }

    private fun saveInfo(fields: SaveFields, ids: List<AutofillId?>): SaveInfo? {
        val required = fields.required.mapNotNull { ids.getOrNull(it) }
        if (required.isEmpty()) return null
        val optional = fields.optional.mapNotNull { ids.getOrNull(it) }
        return SaveInfo.Builder(
                SaveInfo.SAVE_DATA_TYPE_USERNAME or SaveInfo.SAVE_DATA_TYPE_PASSWORD,
                required.toTypedArray(),
            )
            .apply { if (optional.isNotEmpty()) setOptionalIds(optional.toTypedArray()) }
            .setFlags(SaveInfo.FLAG_SAVE_ON_ALL_VIEWS_INVISIBLE)
            .build()
    }

    /**
     * Explicit component, unique request code per row kind. Mutable on API 31+ because the platform
     * adds EXTRA_ASSIST_STRUCTURE to the intent (the docs say not to make it immutable).
     */
    private fun authIntent(context: Context, kind: AuthKind): IntentSender {
        val activity =
            if (kind == AuthKind.Generate) AutofillGenerateActivity::class.java
            else AutofillUnlockActivity::class.java
        val intent = Intent(context, activity).putExtra(EXTRA_MODE, kind.name)
        val mutable =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) PendingIntent.FLAG_MUTABLE else 0
        return PendingIntent.getActivity(
                context,
                kind.ordinal,
                intent,
                PendingIntent.FLAG_CANCEL_CURRENT or mutable,
            )
            .intentSender
    }
}
