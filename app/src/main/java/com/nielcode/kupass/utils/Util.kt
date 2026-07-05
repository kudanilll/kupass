package com.nielcode.kupass.utils

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.core.net.toUri
import com.nielcode.kupass.R

fun openUrl(context: Context, url: String) {
    try {
        val intent = Intent(Intent.ACTION_VIEW, url.toUri())
        context.startActivity(intent)
    } catch (_: ActivityNotFoundException) {
        // Avoid crashing if no browser or activity can handle the intent.
        Toast.makeText(context, context.getString(R.string.toast_url_failed), Toast.LENGTH_SHORT)
            .show()
    }
}
