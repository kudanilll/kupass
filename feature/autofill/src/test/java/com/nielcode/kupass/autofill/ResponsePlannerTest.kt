package com.nielcode.kupass.autofill

import com.nielcode.kupass.data.local.db.PasswordEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ResponsePlannerTest {

    private val strings =
        AutofillStrings(
            unlockTitle = "Kupass",
            unlockSubtitle = "Unlock to fill",
            generateTitle = "Suggest strong password",
            generateSubtitle = "Saved to Kupass automatically",
            searchTitle = "Search Kupass",
            noUsername = "(no username)",
        )
    private val vault =
        listOf(
            entry(1, site = "GitHub", username = "kudanilll", password = "Gh-Secret-1"),
            entry(2, site = "GitHub", username = "", password = "Gh-Secret-2"),
        )

    @Test
    fun `locked plan reveals nothing`() {
        val plan = ResponsePlanner.locked(login(), strings, attachSave = false)
        val shown = plan.datasets.flatMap { listOfNotNull(it.title, it.subtitle) }
        vault.forEach { account ->
            assertFalse(shown.any { account.username.isNotEmpty() && account.username in it })
            assertFalse(shown.any { account.siteName in it })
        }
        assertTrue(plan.datasets.all { dataset -> dataset.values.values.all { it == null } })
        assertEquals(listOf(AuthKind.Unlock), plan.datasets.map { it.auth })
    }

    @Test
    fun `sign-up adds generate row when locked and unlocked`() {
        val signUp = login().copy(isSignUp = true)
        assertEquals(
            listOf(AuthKind.Unlock, AuthKind.Generate),
            ResponsePlanner.locked(signUp, strings, attachSave = true).datasets.map { it.auth },
        )
        assertEquals(
            listOf(null, null, AuthKind.Search, AuthKind.Generate),
            ResponsePlanner.unlocked(signUp, vault, strings, attachSave = true).datasets.map {
                it.auth
            },
        )
    }

    @Test
    fun `unlocked lists matches then search`() {
        val plan = ResponsePlanner.unlocked(login(), vault, strings, attachSave = false)
        assertEquals(
            listOf("kudanilll", "(no username)", "Search Kupass"),
            plan.datasets.map { it.title },
        )
        assertEquals(mapOf(0 to "kudanilll", 1 to "Gh-Secret-1"), plan.datasets[0].values)
    }

    @Test
    fun `presentations never contain the password`() {
        val plans =
            listOf(
                ResponsePlanner.locked(login(), strings, attachSave = true),
                ResponsePlanner.unlocked(login().copy(isSignUp = true), vault, strings, true),
            )
        val shown = plans.flatMap { plan ->
            plan.datasets.flatMap { listOfNotNull(it.title, it.subtitle) }
        }
        vault.forEach { account -> assertFalse(shown.any { account.password in it }) }
    }

    @Test
    fun `username-only form fills only the username`() {
        val usernameOnly = login().copy(passwordFields = emptyList())
        val dataset = ResponsePlanner.entryDataset(usernameOnly, vault[0], strings)
        assertEquals(mapOf(0 to "kudanilll"), dataset.values)
    }

    @Test
    fun `entry without username never clears the typed username`() {
        val dataset = ResponsePlanner.entryDataset(login(), vault[1], strings)
        assertEquals(mapOf(1 to "Gh-Secret-2"), dataset.values)
    }

    @Test
    fun `generated password fills every password field`() {
        val form = login().copy(passwordFields = listOf(1, 2), isSignUp = true)
        val dataset = ResponsePlanner.generatedDataset(form, "Gen-1")
        assertEquals(mapOf(1 to "Gen-1", 2 to "Gen-1"), dataset.values)
        assertNull(dataset.auth)
    }

    @Test
    fun `generated password never fills the current password field`() {
        val dataset = ResponsePlanner.generatedDataset(changePassword(), "Gen-1")
        assertEquals(mapOf(2 to "Gen-1", 3 to "Gen-1"), dataset.values)
    }

    @Test
    fun `generate row is not offered on the current password field`() {
        val generate =
            ResponsePlanner.locked(changePassword(), strings, attachSave = true).datasets.single {
                it.auth == AuthKind.Generate
            }
        assertEquals(setOf(2, 3), generate.values.keys)
    }

    @Test
    fun `account fills only the current password field of a change-password form`() {
        val dataset = ResponsePlanner.entryDataset(changePassword(), vault[0], strings)
        assertEquals(mapOf(0 to "kudanilll", 1 to "Gh-Secret-1"), dataset.values)
    }

    @Test
    fun `save fields follow attachSave`() {
        assertEquals(
            SaveFields(required = listOf(1), optional = listOf(0)),
            ResponsePlanner.locked(login(), strings, attachSave = true).saveFields,
        )
        assertNull(ResponsePlanner.locked(login(), strings, attachSave = false).saveFields)
    }

    @Test
    fun `at most ten account rows`() {
        val many =
            (1L..15L).map { entry(it, site = "GitHub", username = "user$it", password = "p$it") }
        val plan = ResponsePlanner.unlocked(login(), many, strings, attachSave = false)
        assertEquals(10, plan.datasets.count { it.auth == null })
        assertEquals(AuthKind.Search, plan.datasets.last().auth)
    }

    private fun login() =
        ParsedForm(
            packageName = "com.android.chrome",
            webDomain = "github.com",
            usernameField = 0,
            passwordFields = listOf(1),
            isSignUp = false,
            usernameValue = null,
            passwordValue = null,
        )

    private fun changePassword() =
        login()
            .copy(
                passwordFields = listOf(1, 2, 3),
                currentPasswordFields = listOf(1),
                isSignUp = true,
            )

    private fun entry(id: Long, site: String, username: String, password: String) =
        PasswordEntity(
            id = id,
            siteName = site,
            username = username,
            password = password,
            url = "https://github.com",
        )
}
