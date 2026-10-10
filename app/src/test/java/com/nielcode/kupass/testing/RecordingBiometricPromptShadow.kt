package com.nielcode.kupass.testing

import androidx.biometric.BiometricPrompt
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.ViewModelStore
import java.util.concurrent.Executor
import org.robolectric.annotation.Implementation
import org.robolectric.annotation.Implements
import org.robolectric.util.ReflectionHelpers
import org.robolectric.util.ReflectionHelpers.ClassParameter

/** Models a retained OS session and AndroidX's current client callback per ViewModelStore. */
@Implements(BiometricPrompt::class, isInAndroidSdk = false)
class RecordingBiometricPromptShadow {
    private lateinit var client: Client

    @Implementation
    // Robolectric's constructor interception requires this exact JVM method name.
    @Suppress("FunctionNaming")
    fun __constructor__(
        activity: FragmentActivity,
        executor: Executor,
        callback: BiometricPrompt.AuthenticationCallback,
    ) {
        client = clients.getOrPut(activity.viewModelStore) { Client() }
        client.activity = activity
        client.executor = executor
        client.callback = callback
        attachments += activity
    }

    @Implementation
    fun authenticate(info: BiometricPrompt.PromptInfo) {
        check(client.session == null) { "The previous native session has not terminated" }
        val session = Session(client)
        client.session = session
        sessions += session
        prompts += info
    }

    @Implementation
    fun cancelAuthentication() {
        cancellations++
        client.session?.cancellationRequested = true
        // Cancellation may race success; tests explicitly deliver the retained session's result.
    }

    internal class Client {
        lateinit var activity: FragmentActivity
        lateinit var executor: Executor
        lateinit var callback: BiometricPrompt.AuthenticationCallback
        var session: Session? = null
    }

    class Session internal constructor(private val client: Client) {
        var cancellationRequested = false
        var deliveredTo: FragmentActivity? = null
            private set

        var deliveries = 0
            private set

        var ignoredEvents = 0
            private set

        fun succeed() {
            val result =
                ReflectionHelpers.callConstructor(
                    BiometricPrompt.AuthenticationResult::class.java,
                    ClassParameter.from(BiometricPrompt.CryptoObject::class.java, null),
                    ClassParameter.from(
                        Integer.TYPE,
                        BiometricPrompt.AUTHENTICATION_RESULT_TYPE_DEVICE_CREDENTIAL,
                    ),
                )
            dispatch(terminal = true) { it.onAuthenticationSucceeded(result) }
        }

        fun cancel() {
            dispatch(terminal = true) {
                it.onAuthenticationError(BiometricPrompt.ERROR_USER_CANCELED, "cancelled")
            }
        }

        fun fail() {
            dispatch(terminal = false) { it.onAuthenticationFailed() }
        }

        private fun dispatch(
            terminal: Boolean,
            event: (BiometricPrompt.AuthenticationCallback) -> Unit,
        ) {
            if (client.session !== this) {
                ignoredEvents++
                return
            }
            if (terminal) client.session = null
            client.executor.execute {
                // Resolve the client at delivery, not when authenticate or the constructor ran.
                deliveredTo = client.activity
                deliveries++
                event(client.callback)
            }
        }
    }

    companion object {
        private val clients = mutableMapOf<ViewModelStore, Client>()
        val attachments = mutableListOf<FragmentActivity>()
        val sessions = mutableListOf<Session>()
        val prompts = mutableListOf<BiometricPrompt.PromptInfo>()
        var cancellations = 0

        fun reset() {
            clients.clear()
            attachments.clear()
            sessions.clear()
            prompts.clear()
            cancellations = 0
        }
    }
}
