package com.nielcode.kupass.ui.activities

import android.content.ClipData
import android.content.ClipboardManager
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.harrytmthy.safebox.SafeBox
import com.nielcode.kupass.R
import com.nielcode.kupass.databinding.ActivityShowPasswordBinding
import com.nielcode.kupass.model.SiteAccount
import com.nielcode.kupass.model.UserCredential
import com.nielcode.kupass.ui.adapters.CredentialsAdapter
import kotlinx.coroutines.launch
import java.util.Locale
import java.util.concurrent.Executor

class ShowPasswordActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_SITE_ACCOUNT = "extra_site_account"
        private const val AUTH_VALID_MS = 5 * 60 * 1000L
        private const val SAFEBOX_NAME = "kupass_secure_prefs"
        private const val KEY_LAST_AUTH_AT = "last_auth_epoch_ms"
    }

    private val tag = "ShowPasswordActivity"
    private lateinit var binding: ActivityShowPasswordBinding
    private lateinit var executor: Executor
    private lateinit var adapter: CredentialsAdapter

    private val repo by lazy {
        val passphrase = com.nielcode.kupass.core.crypto.SqlCipherKey.getOrCreate(this)
        val db = com.nielcode.kupass.data.local.AppDatabase.get(this, passphrase)
        val crypto = com.nielcode.kupass.core.crypto.CryptoManager(this)
        com.nielcode.kupass.data.repository.PasswordRepositoryImpl(db.siteDao(), crypto)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        binding = ActivityShowPasswordBinding.inflate(layoutInflater)
        setContentView(binding.root)

        executor = ContextCompat.getMainExecutor(this)

        val account = intent.getParcelableExtra<SiteAccount>(EXTRA_SITE_ACCOUNT)
        if (account == null) {
            finish(); return
        }

        // Toolbar title according to site
        binding.toolbar.title =
            account.site.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.ROOT) else it.toString() }
        binding.toolbar.setNavigationOnClickListener { finish() }

        // Setup list credential (card)
        adapter = CredentialsAdapter(
            siteNote = account.note,
            onEdit = { /* TODO: navigate to edit */ },
            onCopy = { label, value -> copyToClipboard(label, value) },
            onDelete = { credential -> showDeleteConfirmationDialog(account, credential) }
        )

        binding.rvCredentials.layoutManager = LinearLayoutManager(this)
        binding.rvCredentials.adapter = adapter

        // Postpone data entry until authentication is complete
        ensureAuthenticated(
            onSuccess = {
                adapter.submitList(account.credentials)
            },
            onFailure = {
                finish()
            }
        )
    }

    private fun ensureAuthenticated(onSuccess: () -> Unit, onFailure: () -> Unit) {
        val prefs = SafeBox.create(this, SAFEBOX_NAME)
        val now = System.currentTimeMillis()
        val last = prefs.getLong(KEY_LAST_AUTH_AT, 0L)
        if (now - last < AUTH_VALID_MS) {
            onSuccess(); return
        }

        val authenticators =
            BiometricManager.Authenticators.BIOMETRIC_STRONG or
                    BiometricManager.Authenticators.DEVICE_CREDENTIAL

        val bm = BiometricManager.from(this)
        val can = bm.canAuthenticate(authenticators)

        if (can != BiometricManager.BIOMETRIC_SUCCESS &&
            can != BiometricManager.BIOMETRIC_STATUS_UNKNOWN // Some unusual vendors report unknown
        ) {
            // If biometrics are not available → bypass immediately
            // Toast.makeText(this, R.string.biometric_unavailable, Toast.LENGTH_SHORT).show()
            Log.i(tag, getString(R.string.biometric_unavailable))
            prefs.edit().putLong(KEY_LAST_AUTH_AT, System.currentTimeMillis()).apply()
            onSuccess()
            return
        }

        val promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle(getString(R.string.app_name))
            .setSubtitle(getString(R.string.show_password))
            .setAllowedAuthenticators(authenticators)
            .build()

        val prompt = BiometricPrompt(
            this, executor,
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    prefs.edit().putLong(KEY_LAST_AUTH_AT, System.currentTimeMillis()).apply()
                    onSuccess()
                }

                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    onFailure()
                }

                override fun onAuthenticationFailed() {
                    // Users can retry → not immediately fail
                }
            })
        prompt.authenticate(promptInfo)
    }


    private fun copyToClipboard(label: String, value: String) {
        val cm = getSystemService(CLIPBOARD_SERVICE) as ClipboardManager
        cm.setPrimaryClip(ClipData.newPlainText(label, value))
        Toast.makeText(this, R.string.copied, Toast.LENGTH_SHORT).show()
    }

    private fun showDeleteConfirmationDialog(siteAccount: SiteAccount, credential: UserCredential) {
        com.google.android.material.dialog.MaterialAlertDialogBuilder(this)
            .setTitle(getString(R.string.dialog_title_delete))
            .setMessage("${getString(R.string.dialog_message_delete)} ${credential.username}?")
            .setNegativeButton(getString(R.string.button_cancel), null)
            .setPositiveButton(getString(R.string.button_delete)) { _, _ ->
                lifecycleScope.launch {
                    // delete from database
                    repo.deleteCredential(siteAccount.id, credential.username)

                    // Local UI update (visible immediately) — observe Flow is not on this page
                    val newList = adapter.currentList.toMutableList().apply { remove(credential) }
                    adapter.submitList(newList)

                    Toast.makeText(
                        this@ShowPasswordActivity,
                        getString(R.string.toast_success_delete),
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
            .show()
    }
}
