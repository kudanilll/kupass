package com.nielcode.kupass.ui.screens.home

import com.nielcode.kupass.data.local.db.PasswordEntity
import com.nielcode.kupass.data.repository.filterByQuery
import java.util.Locale
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertSame
import org.junit.Test

class VaultGroupingTest {
    @Test
    fun `web host ignores www case userinfo port path query and fragment`() {
        val urls =
            listOf(
                " https://user:pass@WWW.Example.COM:8443/path?q=1#part ",
                "http://example.com/other",
                "example.com:443/login",
                "//EXAMPLE.COM./",
            )
        urls.forEach {
            assertEquals(VaultGroupKey(VaultGroupKey.Namespace.WebHost, "example.com"), key(it))
        }
        assertNotEquals(key("example.com"), key("accounts.example.com"))
        assertNotEquals(key("accounts.example.com"), key("mail.example.com"))
    }

    @Test
    fun `private hosts IPs and local names group locally`() {
        listOf("localhost", "vault.local", "vault.internal", "intranet", "192.168.1.2", "[::1]")
            .forEach { host ->
                assertEquals(
                    VaultGroupKey(VaultGroupKey.Namespace.WebHost, host),
                    key("http://$host:8080/path"),
                )
                assertEquals(key("https://$host/a"), key("http://$host/b"))
                assertEquals(key("https://$host/a"), key("$host:8080/b"))
            }
    }

    @Test
    fun `android facets use package and ignore signing certificate`() {
        assertEquals(
            key("android://cert-one@com.example.app/"),
            key("android://cert-two@com.example.app/"),
        )
        assertEquals(
            VaultGroupKey(VaultGroupKey.Namespace.AndroidPackage, "com.example.app"),
            key("android://cert@com.example.app/"),
        )
        assertNotEquals(
            key("android://cert@com.example.app/"),
            key("android://cert@com.example.other/"),
        )
        assertEquals(VaultGroupKey.Namespace.SiteName, key("android://cert@bad-package/").namespace)
    }

    @Test
    fun `missing bad and nonweb URLs fall back to trimmed name without com guessing`() {
        listOf(
                "",
                "  ",
                "not a url",
                "https://",
                "https://bad host/x",
                "https://example.com:bad",
                "https://example.com:99999",
                "ftp://example.com",
                "mailto:user@example.com",
            )
            .forEach { url ->
                assertEquals(
                    VaultGroupKey(VaultGroupKey.Namespace.SiteName, "github"),
                    key(url, " GitHub "),
                )
            }
        assertNotEquals(key("", "GitHub"), key("github.com", "GitHub"))
        assertEquals("github", key("", "GitHub").value)
    }

    @Test
    fun `name web and package namespaces cannot collide even with equal labels`() {
        val name = key("", "com.example.app")
        val web = key("https://com.example.app")
        val app = key("android://cert@com.example.app/")
        assertEquals(3, setOf(name, web, app).size)
        assertEquals(3, setOf(name.lazyKey, web.lazyKey, app.lazyKey).size)
        assertNotEquals("account:42", name.lazyKey)
    }

    @Test
    fun `ROOT normalization does not depend on Turkish device locale`() {
        val original = Locale.getDefault()
        try {
            Locale.setDefault(Locale.forLanguageTag("tr-TR"))
            assertEquals("intranet", key("https://INTRANET").value)
            assertEquals("identity", key("", " IDENTITY ").value)
        } finally {
            Locale.setDefault(original)
        }
    }

    @Test
    fun `alphabetic ordering retains every original account and ID deterministically`() {
        val accounts =
            listOf(
                account(71, "Zulu", "https://z.example", "same"),
                account(42, "Alpha", "https://a.example", "same"),
                account(12, "Alpha", "https://a.example", "same"),
                account(99, "Alpha", "https://a.example", "other"),
            )
        val groups = groupVault(accounts)
        assertEquals(listOf("a.example", "z.example"), groups.map { it.key.value })
        assertEquals(listOf(99L, 12L, 42L, 71L), groups.flatMap { it.accounts }.map { it.id })
        assertEquals(groups, groupVault(accounts.reversed()))
        accounts.forEach { original ->
            assertSame(original, groups.flatMap { it.accounts }.single { it.id == original.id })
        }
    }

    @Test
    fun `notes query groups only matching accounts and never searches password`() {
        val first =
            account(1, "Example", "example.com", "one")
                .copy(notes = "recovery phrase", password = "secret-only")
        val second = account(2, "Example", "example.com", "two")
        assertEquals(
            listOf(first),
            groupVault(listOf(first, second).filterByQuery("RECOVERY")).single().accounts,
        )
        assertEquals(
            emptyList<PasswordEntity>(),
            listOf(first, second).filterByQuery("secret-only"),
        )
    }

    private fun key(url: String, name: String = "Fallback") = vaultGroupKey(account(42, name, url))

    private fun account(id: Long, name: String, url: String, username: String = "") =
        PasswordEntity(
            id = id,
            siteName = name,
            username = username,
            password = "unused",
            url = url,
        )
}
