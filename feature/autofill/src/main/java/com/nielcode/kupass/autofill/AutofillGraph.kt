package com.nielcode.kupass.autofill

import android.content.Context
import com.nielcode.kupass.data.CoreContainer
import com.nielcode.kupass.data.CoreContainerProvider
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

/** Wiring for the autofill service and activities, which share one process-memory session. */
internal object AutofillGraph {

    /** Process lifetime, like the app's own background scope; fill jobs cancel individually. */
    val scope: CoroutineScope = processScope()

    private fun processScope(dispatcher: CoroutineDispatcher = Dispatchers.Default) =
        CoroutineScope(SupervisorJob() + dispatcher)

    @Volatile private var session: AutofillSession? = null

    fun core(context: Context): CoreContainer =
        (context.applicationContext as CoreContainerProvider).coreContainer

    fun session(context: Context): AutofillSession {
        // The session lives as long as the process, so it must not keep the caller's activity.
        val app = context.applicationContext
        return session
            ?: synchronized(this) {
                session
                    ?: AutofillSession(
                            timeoutMillis = {
                                core(app).preferenceManager.autoLockSeconds * MILLIS_PER_SECOND
                            }
                        )
                        .also { session = it }
            }
    }

    private const val MILLIS_PER_SECOND = 1_000L
}
