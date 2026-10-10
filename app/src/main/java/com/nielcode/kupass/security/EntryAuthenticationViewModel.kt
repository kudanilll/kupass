package com.nielcode.kupass.security

import android.os.SystemClock
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.nielcode.kupass.di.appContainer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Activity-retained UI authorization. Nothing here is written to saved state or preferences. */
// Request, navigation and native-session transitions stay together so every callback fails closed.
@Suppress("TooManyFunctions")
class EntryAuthenticationViewModel(
    timeoutMillis: () -> Long,
    private val clock: () -> Long = SystemClock::elapsedRealtime,
) : ViewModel() {
    // The absence timer and grants must have the same owner, including with multiple activities.
    private val appLock = AppLock(timeoutMillis, clock)

    enum class Action {
        OpenAccount,
        UnlockDestination,
        Export,
    }

    data class Request(
        val token: Long,
        val action: Action,
        val entryId: String,
        val accountId: Long,
    )

    data class State(
        val deviceSecure: Boolean = false,
        val pending: Request? = null,
        val nativeRequest: Request? = null,
        val approvedOpen: Request? = null,
        val grants: Map<String, Long> = emptyMap(),
        val export: ExportConsent? = null,
    )

    private val _state = MutableStateFlow(State())
    val state = _state.asStateFlow()
    private var nextToken = 0L
    private var destination: String? = null
    private var destinationAccount: Long? = null
    private var pickerToken: Long? = null

    /** Called synchronously by the navigation listener, including for restored destinations. */
    fun setDestination(entryId: String, accountId: Long?) {
        if (destination == entryId && destinationAccount == accountId) return
        destination = entryId
        destinationAccount = accountId
        val grants = _state.value.grants
        _state.value =
            _state.value.copy(
                pending = null,
                approvedOpen = null,
                export = null,
                grants =
                    if (accountId != null && grants[entryId] == accountId) grants else emptyMap(),
            )
    }

    fun request(action: Action, entryId: String, accountId: Long = -1L) {
        val current = _state.value
        val pendingAction =
            current.nativeRequest != null || current.pending != null || current.approvedOpen != null
        val pendingExport = current.export != null || pickerToken != null
        if (entryId != destination || pendingAction || pendingExport) return
        val validTarget =
            when (action) {
                Action.OpenAccount -> accountId > 0 && destinationAccount == null
                Action.UnlockDestination -> accountId > 0 && destinationAccount == accountId
                Action.Export -> destinationAccount == null
            }
        if (validTarget) {
            _state.value = current.copy(pending = Request(++nextToken, action, entryId, accountId))
        }
    }

    /** Only one native session may exist, even while a cancelled session is draining callbacks. */
    fun beginNativePrompt(): Request? {
        val current = _state.value
        val request = current.pending
        if (request != null && current.deviceSecure && current.nativeRequest == null) {
            appLock.unlock() // Arms the existing absence timer; this does not grant access.
            appLock.allowNextBackground() // Older Android versions open a credential Activity.
            _state.value = current.copy(nativeRequest = request)
            return request
        }
        return null
    }

    fun nativeResult(
        token: Long,
        succeeded: Boolean,
        credentialAvailable: Boolean = _state.value.deviceSecure,
    ) {
        val current = _state.value
        if (current.nativeRequest?.token != token) return
        val request = current.pending?.takeIf { it.token == token }
        _state.value =
            current.copy(
                nativeRequest = null,
                pending = null,
                deviceSecure = current.deviceSecure && credentialAvailable,
            )
        appLock.checkTimeout()
        if (!_state.value.deviceSecure) revoke()
        val validRequest = request?.takeIf {
            _state.value.deviceSecure && destination == it.entryId
        }
        if (!succeeded || validRequest == null || appLock.locked.value) return
        appLock.unlock()
        _state.value =
            when (validRequest.action) {
                Action.OpenAccount -> _state.value.copy(approvedOpen = validRequest)
                Action.UnlockDestination ->
                    _state.value.copy(
                        grants = mapOf(validRequest.entryId to validRequest.accountId)
                    )
                Action.Export ->
                    _state.value.copy(
                        export =
                            ExportConsent(
                                validRequest.token,
                                validRequest.entryId,
                                clock() + AppLock.EXEMPT_TRIP_GRACE_MILLIS,
                            )
                    )
            }
    }

    /** Closures are executed synchronously and never retained by authorization state. */
    fun openApprovedAccount(token: Long, navigate: (Long) -> String?) {
        val request = _state.value.approvedOpen
        if (request != null && request.token == token && isCurrent(request.entryId)) {
            _state.value = _state.value.copy(approvedOpen = null)
            navigate(request.accountId)?.let { entry ->
                if (destination == entry && destinationAccount == request.accountId) {
                    _state.value = _state.value.copy(grants = mapOf(entry to request.accountId))
                }
            }
        }
    }

    fun openEditor(entryId: String, accountId: Long, navigate: () -> String?) {
        if (!canRead(entryId, accountId) || destination != entryId) return
        val entry = navigate() ?: return
        if (destination == entry && destinationAccount == accountId) {
            _state.value =
                _state.value.copy(grants = mapOf(entryId to accountId, entry to accountId))
        }
    }

    fun canRead(entryId: String, accountId: Long): Boolean =
        accountId > 0 &&
            _state.value.deviceSecure &&
            !appLock.locked.value &&
            _state.value.grants[entryId] == accountId

    fun onForeground(deviceSecure: Boolean) {
        appLock.onForeground()
        _state.value = _state.value.copy(deviceSecure = deviceSecure)
        if (!deviceSecure || appLock.locked.value) revoke()
    }

    fun onBackground(isChangingConfigurations: Boolean) {
        val exempt = appLock.onBackground(isChangingConfigurations)
        if (!isChangingConfigurations && !exempt) cancelPending()
    }

    /** Exempt only this activity's next external trip, with AppLock's existing bounded grace. */
    fun allowNextBackground() {
        appLock.allowNextBackground()
    }

    fun cancelPending() {
        _state.value = _state.value.copy(pending = null, approvedOpen = null, export = null)
    }

    fun revoke() {
        cancelPending()
        _state.value = _state.value.copy(grants = emptyMap())
    }

    fun startExportPicker(token: Long): Boolean {
        val consent =
            _state.value.export?.takeIf {
                it.token == token && it.phase == ExportConsent.Phase.Password && pickerToken == null
            } ?: return false
        return if (isValidConsent(consent)) {
            pickerToken = token
            _state.value =
                _state.value.copy(export = consent.copy(phase = ExportConsent.Phase.Picker))
            true
        } else {
            false
        }
    }

    /**
     * Retains the outstanding picker token after revocation so a late result cannot start a new
     * export.
     */
    fun completeExportPicker(chosen: Boolean): Boolean {
        val token = pickerToken
        pickerToken = null
        val consent = _state.value.export
        val allowed =
            chosen &&
                token != null &&
                consent != null &&
                consent.token == token &&
                consent.phase == ExportConsent.Phase.Picker &&
                isValidConsent(consent)
        _state.value =
            _state.value.copy(
                export = if (allowed) consent.copy(phase = ExportConsent.Phase.Write) else null
            )
        return allowed
    }

    /** Called at the export API boundary, before taking a vault snapshot or opening any stream. */
    fun consumeExportForWrite(token: Long? = null): Boolean {
        val consent = _state.value.export
        val allowed =
            consent != null &&
                token == consent.token &&
                consent.phase == ExportConsent.Phase.Write &&
                isValidConsent(consent)
        _state.value = _state.value.copy(export = null)
        return allowed
    }

    private fun isCurrent(entryId: String): Boolean {
        appLock.checkTimeout()
        return destination == entryId && _state.value.deviceSecure && !appLock.locked.value
    }

    private fun isValidConsent(consent: ExportConsent): Boolean =
        clock() < consent.expiresAt && isCurrent(consent.entryId)

    override fun onCleared() {
        revoke()
    }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val preferences = appContainer().preferenceManager
                EntryAuthenticationViewModel({ preferences.autoLockSeconds * 1000L })
            }
        }
    }
}

/** A single export request, valid only on its originating navigation entry. */
data class ExportConsent(
    val token: Long,
    val entryId: String,
    val expiresAt: Long,
    val phase: Phase = Phase.Password,
) {
    enum class Phase {
        Password,
        Picker,
        Write,
    }
}
