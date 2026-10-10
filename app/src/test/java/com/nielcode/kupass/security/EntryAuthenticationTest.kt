package com.nielcode.kupass.security

import com.nielcode.kupass.security.EntryAuthenticationViewModel.Action
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class EntryAuthenticationTest {
    private var now = 1_000L
    private val timeout = 30_000L
    private val auth = EntryAuthenticationViewModel({ timeout }, { now })

    @Test
    fun `public launch has no grant or pending native action`() {
        home()
        assertNull(auth.state.value.pending)
        assertNull(auth.beginNativePrompt())
        assertTrue(auth.state.value.grants.isEmpty())
    }

    @Test
    fun `every home open including the same id needs a new native request`() {
        home()
        openAccount("detail-first")
        assertTrue(auth.canRead("detail-first", ACCOUNT))
        auth.setDestination(HOME, null)
        assertFalse(auth.canRead("detail-first", ACCOUNT))
        auth.request(Action.OpenAccount, HOME, ACCOUNT)
        val request = requireNotNull(auth.beginNativePrompt())
        assertFalse(auth.canRead("detail-second", ACCOUNT))
        auth.nativeResult(request.token, true)
        auth.openApprovedAccount(request.token) {
            auth.setDestination("detail-second", it)
            "detail-second"
        }
        assertTrue(auth.canRead("detail-second", ACCOUNT))
        assertFalse(auth.canRead("detail-first", ACCOUNT))
        assertFalse(auth.canRead("detail-second", ACCOUNT + 1))
    }

    @Test
    fun `cancel and late success cannot navigate`() {
        home()
        val request = begin(Action.OpenAccount)
        auth.cancelPending()
        auth.nativeResult(request.token, true)
        var navigated = false
        auth.openApprovedAccount(request.token) {
            navigated = true
            "detail"
        }
        assertFalse(navigated)
        assertTrue(auth.state.value.grants.isEmpty())
    }

    @Test
    fun `native failure grants neither entry access nor export consent`() {
        home()
        val request = begin(Action.OpenAccount)
        auth.nativeResult(request.token, false)
        assertNull(auth.state.value.approvedOpen)
        assertFalse(auth.canRead("detail", ACCOUNT))
        val export = begin(Action.Export)
        auth.nativeResult(export.token, false)
        assertNull(auth.state.value.export)
    }

    @Test
    fun `no credential leaves public page accessible with action-local guidance`() {
        auth.setDestination(HOME, null)
        auth.onForeground(false)
        auth.request(Action.Export, HOME)
        assertNotNull(auth.state.value.pending)
        assertNull(auth.beginNativePrompt())
        assertNull(auth.state.value.export)
        auth.cancelPending()
        assertNull(auth.state.value.pending)
    }

    @Test
    fun `rotation retains pending token but does not start a duplicate prompt`() {
        home()
        val request = begin(Action.OpenAccount)
        auth.onBackground(true)
        now += 60_000
        auth.onForeground(true)
        auth.setDestination(HOME, null)
        assertEquals(request, auth.state.value.nativeRequest)
        assertNull(auth.beginNativePrompt())
        auth.nativeResult(request.token, true)
        assertEquals(request, auth.state.value.approvedOpen)
    }

    @Test
    fun `serialize prompts until cancelled native session finishes and reject its stale callback`() {
        home()
        val old = begin(Action.OpenAccount)
        auth.cancelPending()
        auth.request(Action.Export, HOME)
        assertNull(auth.state.value.pending)
        auth.nativeResult(old.token, false)
        val fresh = begin(Action.Export)
        auth.nativeResult(old.token, true)
        assertEquals(fresh, auth.state.value.pending)
        assertNull(auth.state.value.export)
        auth.nativeResult(fresh.token, true)
        assertNotNull(auth.state.value.export)
    }

    @Test
    fun `leaving the requesting navigation entry rejects success`() {
        home()
        val request = begin(Action.OpenAccount)
        auth.setDestination("new-entry", null)
        auth.nativeResult(request.token, true)
        assertNull(auth.state.value.approvedOpen)
        assertTrue(auth.state.value.grants.isEmpty())
    }

    @Test
    fun `restored detail and positive editor have zero grants despite another authorized controller`() {
        home()
        openAccount("restored-detail")
        assertTrue(auth.canRead("restored-detail", ACCOUNT))
        val restored = EntryAuthenticationViewModel({ timeout }, { now })
        restored.onForeground(true)
        restored.setDestination("restored-detail", ACCOUNT)
        assertFalse(restored.canRead("restored-detail", ACCOUNT))
        restored.setDestination("restored-editor", ACCOUNT)
        assertFalse(restored.canRead("restored-editor", ACCOUNT))
        restored.request(Action.UnlockDestination, "restored-editor", ACCOUNT)
        assertNotNull(restored.beginNativePrompt())
    }

    @Test
    fun `detail to editor and back retains only the authenticated navigation chain`() {
        home()
        openAccount("detail")
        auth.openEditor("detail", ACCOUNT) {
            auth.setDestination("editor", ACCOUNT)
            "editor"
        }
        assertTrue(auth.canRead("editor", ACCOUNT))
        auth.setDestination("detail", ACCOUNT)
        assertTrue(auth.canRead("detail", ACCOUNT))
        assertNull(auth.state.value.pending)
        var navigated = false
        auth.openEditor("detail", ACCOUNT + 1) {
            navigated = true
            "wrong-editor"
        }
        assertFalse(navigated)
        auth.setDestination(HOME, null)
        assertFalse(auth.canRead("editor", ACCOUNT))
    }

    @Test
    fun `background timeout revokes detail and editor and a late request`() {
        home()
        openAccount("detail")
        auth.openEditor("detail", ACCOUNT) {
            auth.setDestination("editor", ACCOUNT)
            "editor"
        }
        auth.onBackground(false)
        now += timeout
        auth.onForeground(true)
        assertFalse(auth.canRead("detail", ACCOUNT))
        assertFalse(auth.canRead("editor", ACCOUNT))
        val request = begin(Action.UnlockDestination, "editor")
        auth.onBackground(false)
        now += AppLock.EXEMPT_TRIP_GRACE_MILLIS
        auth.nativeResult(request.token, true) // Can arrive before Activity.onStart.
        assertFalse(auth.canRead("editor", ACCOUNT))
    }

    @Test
    fun `short background preserves entry grants but revokes pending ordinary actions`() {
        home()
        openAccount("detail")
        auth.onBackground(false)
        now += timeout - 1
        auth.onForeground(true)
        assertTrue(auth.canRead("detail", ACCOUNT))
        auth.setDestination(HOME, null)
        auth.request(Action.OpenAccount, HOME, ACCOUNT)
        auth.onBackground(false)
        assertNull(auth.state.value.pending)
    }

    @Test
    fun `another controller starting then cancelling native auth cannot reset expired entry access`() {
        home()
        openAccount("activity-a-detail")
        auth.onBackground(false)
        now += timeout
        val other = EntryAuthenticationViewModel({ timeout }, { now })
        other.setDestination("activity-b-home", null)
        other.onForeground(true)
        other.request(Action.Export, "activity-b-home")
        val request = requireNotNull(other.beginNativePrompt())
        other.nativeResult(request.token, false)
        auth.onForeground(true)
        assertFalse(auth.canRead("activity-a-detail", ACCOUNT))
        assertTrue(auth.state.value.grants.isEmpty())
    }

    @Test
    fun `another controller successfully authenticating cannot reset expired entry access`() {
        home()
        openAccount("activity-a-detail")
        auth.onBackground(false)
        now += timeout
        val other = EntryAuthenticationViewModel({ timeout }, { now })
        other.setDestination("activity-b-home", null)
        other.onForeground(true)
        other.request(Action.Export, "activity-b-home")
        val request = requireNotNull(other.beginNativePrompt())
        other.nativeResult(request.token, true)
        assertNotNull(other.state.value.export)
        auth.onForeground(true)
        assertFalse(auth.canRead("activity-a-detail", ACCOUNT))
        assertTrue(auth.state.value.grants.isEmpty())
    }

    @Test
    fun `background allowance is owner scoped and its own grace remains bounded`() {
        home()
        openAccount("detail")
        val other = EntryAuthenticationViewModel({ timeout }, { now })
        other.allowNextBackground()
        auth.onBackground(false)
        now += timeout
        auth.onForeground(true)
        assertFalse(auth.canRead("detail", ACCOUNT))
        val request = begin(Action.UnlockDestination, "detail")
        auth.nativeResult(request.token, true)
        auth.allowNextBackground()
        auth.onBackground(false)
        now += AppLock.EXEMPT_TRIP_GRACE_MILLIS - 1
        auth.onForeground(true)
        assertTrue(auth.canRead("detail", ACCOUNT))
        auth.allowNextBackground()
        auth.onBackground(false)
        now += AppLock.EXEMPT_TRIP_GRACE_MILLIS
        auth.onForeground(true)
        assertFalse(auth.canRead("detail", ACCOUNT))
    }

    @Test
    fun `export is single use after fresh native success and an authorized picker round trip`() {
        home()
        assertFalse(auth.completeExportPicker(true))
        assertFalse(auth.consumeExportForWrite())
        val request = begin(Action.Export)
        assertNull(auth.state.value.export) // Password dialog cannot be shown yet.
        auth.nativeResult(request.token, true)
        assertFalse(auth.consumeExportForWrite()) // Dialog consent alone is not write permission.
        val fresh = begin(Action.Export)
        auth.nativeResult(fresh.token, true)
        assertTrue(auth.startExportPicker(fresh.token))
        auth.allowNextBackground()
        auth.onBackground(false)
        now += 1_000
        auth.onForeground(true)
        assertTrue(auth.completeExportPicker(true))
        assertTrue(auth.consumeExportForWrite(fresh.token))
        assertFalse(auth.consumeExportForWrite())
    }

    @Test
    fun `picker cancellation revocation and expiry never authorize export`() {
        home()
        val cancelled = begin(Action.Export)
        auth.nativeResult(cancelled.token, true)
        assertTrue(auth.startExportPicker(cancelled.token))
        assertFalse(auth.completeExportPicker(false))
        assertFalse(auth.consumeExportForWrite())
        val expired = begin(Action.Export)
        auth.nativeResult(expired.token, true)
        assertTrue(auth.startExportPicker(expired.token))
        auth.allowNextBackground()
        auth.onBackground(false)
        now += AppLock.EXEMPT_TRIP_GRACE_MILLIS
        assertFalse(auth.completeExportPicker(true))
        assertFalse(auth.consumeExportForWrite())
    }

    @Test
    fun `revoked outstanding picker blocks another request until its late result drains`() {
        home()
        val request = begin(Action.Export)
        auth.nativeResult(request.token, true)
        assertTrue(auth.startExportPicker(request.token))
        auth.revoke()
        auth.request(Action.Export, HOME)
        assertNull(auth.state.value.pending)
        assertFalse(auth.completeExportPicker(true))
        assertFalse(auth.consumeExportForWrite())
        auth.request(Action.Export, HOME)
        assertNotNull(auth.state.value.pending)
    }

    @Test
    fun `credential removal invalidates existing grants and export consent`() {
        home()
        openAccount("detail")
        auth.onForeground(false)
        assertFalse(auth.canRead("detail", ACCOUNT))
        auth.setDestination(HOME, null)
        auth.onForeground(true)
        val request = begin(Action.Export)
        auth.onForeground(false)
        auth.nativeResult(request.token, true)
        assertNull(auth.state.value.export)
    }

    @Test
    fun `restored picker result and mismatched write token never authorize export`() {
        home()
        val request = begin(Action.Export)
        auth.nativeResult(request.token, true)
        assertTrue(auth.startExportPicker(request.token))
        val restored = EntryAuthenticationViewModel({ timeout }, { now })
        restored.setDestination(HOME, null)
        restored.onForeground(true)
        assertFalse(restored.completeExportPicker(true))
        assertFalse(restored.consumeExportForWrite(request.token))
        assertTrue(auth.completeExportPicker(true))
        assertFalse(auth.consumeExportForWrite(request.token + 1))
        assertFalse(auth.consumeExportForWrite(request.token))
    }

    private fun home() {
        auth.setDestination(HOME, null)
        auth.onForeground(true)
    }

    private fun begin(
        action: Action,
        entryId: String = HOME,
    ): EntryAuthenticationViewModel.Request {
        auth.request(action, entryId, ACCOUNT)
        return requireNotNull(auth.beginNativePrompt())
    }

    private fun openAccount(entry: String) {
        val request = begin(Action.OpenAccount)
        auth.nativeResult(request.token, true)
        auth.openApprovedAccount(request.token) { id ->
            auth.setDestination(entry, id)
            entry
        }
    }

    private companion object {
        const val HOME = "home-entry"
        const val ACCOUNT = 42L
    }
}
