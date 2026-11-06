package com.nielcode.kupass.autofill

import android.app.assist.AssistStructure
import android.app.assist.AssistStructure.ViewNode
import android.os.Build
import android.view.View
import android.view.autofill.AutofillId
import java.util.Locale

/** One screen as plain [FormField]s; [ids] holds each field's AutofillId at the same index. */
internal data class ReadStructure(
    val packageName: String,
    val fields: List<FormField>,
    val ids: List<AutofillId?>,
)

/** Flattens an [AssistStructure] into fillable fields plus short labels (for sign-up words). */
internal object StructureReader {

    private const val MAX_LABEL_LENGTH = 64
    private val HTML_KEYS = setOf("type", "autocomplete", "name", "id")

    fun read(structure: AssistStructure): ReadStructure {
        val nodes = mutableListOf<ViewNode>()
        for (window in 0 until structure.windowNodeCount) {
            collect(structure.getWindowNodeAt(window).rootViewNode, nodes)
        }
        return ReadStructure(
            packageName = structure.activityComponent.packageName,
            fields = nodes.mapIndexed { index, node -> node.toField(index) },
            ids = nodes.map { it.autofillId },
        )
    }

    private fun collect(node: ViewNode, into: MutableList<ViewNode>) {
        val label = node.text?.length?.let { it in 1..MAX_LABEL_LENGTH } == true
        if (node.isFillable() || label || node.webDomain != null) into += node
        for (child in 0 until node.childCount) collect(node.getChildAt(child), into)
    }

    private fun ViewNode.isFillable(): Boolean =
        autofillId != null &&
            autofillType == View.AUTOFILL_TYPE_TEXT &&
            visibility == View.VISIBLE &&
            isEnabled &&
            isImportantForAutofill()

    // getImportantForAutofill exists from API 28; on API 27 every text field counts.
    private fun ViewNode.isImportantForAutofill(): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.P ||
            importantForAutofill !in
                setOf(
                    View.IMPORTANT_FOR_AUTOFILL_NO,
                    View.IMPORTANT_FOR_AUTOFILL_NO_EXCLUDE_DESCENDANTS,
                )

    private fun ViewNode.toField(index: Int): FormField {
        val fillable = isFillable()
        return FormField(
            index = index,
            autofillHints = autofillHints?.toList().orEmpty(),
            htmlAttributes =
                htmlInfo
                    ?.attributes
                    ?.associate { it.first.lowercase(Locale.ROOT) to it.second }
                    ?.filterKeys { it in HTML_KEYS }
                    .orEmpty(),
            inputType = inputType,
            idEntry = idEntry,
            hint = hint,
            contentDescription = contentDescription?.toString(),
            value =
                if (fillable) autofillValue?.takeIf { it.isText }?.textValue?.toString()
                else text?.toString(),
            webDomain = webDomain,
            isFocused = isFocused,
            isFillable = fillable,
        )
    }
}
