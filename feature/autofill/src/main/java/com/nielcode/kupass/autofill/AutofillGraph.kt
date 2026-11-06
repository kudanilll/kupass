package com.nielcode.kupass.autofill

import android.content.Context
import com.nielcode.kupass.data.CoreContainer
import com.nielcode.kupass.data.CoreContainerProvider

/** Wiring for the autofill service and activities, which share one process-memory session. */
internal object AutofillGraph {

    @Volatile private var session: AutofillSession? = null

    fun core(context: Context): CoreContainer =
        (context.applicationContext as CoreContainerProvider).coreContainer

    fun session(context: Context): AutofillSession =
        session
            ?: synchronized(this) {
                session
                    ?: AutofillSession(
                            timeoutMillis = {
                                core(context).preferenceManager.autoLockSeconds * MILLIS_PER_SECOND
                            }
                        )
                        .also { session = it }
            }

    private const val MILLIS_PER_SECOND = 1_000L
}
