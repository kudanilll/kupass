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

/** Flattens an [AssistStructure] into its fillable fields (plus nodes that carry a web domain). */
internal object StructureReader {

    private val HTML_KEYS = setOf("type", "autocomplete", "name", "id")

    fun read(structure: AssistStructure): ReadStructure {
        val nodes = mutableListOf<Pair<ViewNode, String?>>()
        for (window in 0 until structure.windowNodeCount) {
            collect(structure.getWindowNodeAt(window).rootViewNode, null, nodes)
        }
        return ReadStructure(
            packageName = structure.activityComponent.packageName,
            fields = nodes.mapIndexed { index, (node, domain) -> node.toField(index, domain) },
            ids = nodes.map { (node, _) -> node.autofillId },
        )
    }

    /**
     * Each node takes its own web domain or the nearest one above it. Chrome sets it on every field
     * (an iframe's fields carry the iframe's domain); a browser that sets it only on a frame's root
     * node gets the same result.
     */
    private fun collect(
        node: ViewNode,
        frame: String?,
        into: MutableList<Pair<ViewNode, String?>>,
    ) {
        val domain = node.webDomain ?: frame
        if (node.isFillable() || node.webDomain != null) into += node to domain
        for (child in 0 until node.childCount) collect(node.getChildAt(child), domain, into)
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

    private fun ViewNode.toField(index: Int, domain: String?): FormField {
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
            value = autofillValue?.takeIf { fillable && it.isText }?.textValue?.toString(),
            webDomain = domain,
            isFocused = isFocused,
            isFillable = fillable,
        )
    }
}
