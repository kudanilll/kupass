package com.nielcode.kupass.ui.activities

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.util.Patterns
import android.view.inputmethod.EditorInfo
import androidx.appcompat.app.AppCompatActivity
import androidx.core.widget.doOnTextChanged
import androidx.lifecycle.lifecycleScope
import com.google.android.material.color.DynamicColors
import com.nielcode.kupass.R
import com.nielcode.kupass.core.PreferenceManager
import com.nielcode.kupass.core.crypto.CryptoManager
import com.nielcode.kupass.core.crypto.SqlCipherKey
import com.nielcode.kupass.data.local.AppDatabase
import com.nielcode.kupass.data.repository.PasswordRepositoryImpl
import com.nielcode.kupass.databinding.ActivityCreatePasswordBinding
import com.nielcode.kupass.utils.AppConfig
import kotlinx.coroutines.launch
import java.util.Date
import kotlin.random.Random

class CreatePasswordActivity : AppCompatActivity() {

    private val tag = "CreatePasswordActivity"

    private val binding by lazy { ActivityCreatePasswordBinding.inflate(layoutInflater) }
    private val prefs by lazy { PreferenceManager(this) }

    private val repo by lazy {
        val passphrase = SqlCipherKey.getOrCreate(this)
        val db = AppDatabase.get(this, passphrase)
        val crypto = CryptoManager(this)
        PasswordRepositoryImpl(db.siteDao(), crypto)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (prefs.dynamicColor == AppConfig.DynamicColors.Code.ENABLE && DynamicColors.isDynamicColorAvailable()) {
            Log.d(tag, "Applying dynamic colors")
            DynamicColors.applyToActivityIfAvailable(this)
        }
        setContentView(binding.root)

        setupToolbar()
        setupInputs()
        setupActions()
    }

    private fun setupToolbar() {
        binding.toolbar.setNavigationOnClickListener { finish() }
    }

    private fun setupInputs() = with(binding) {
        etAccount.doOnTextChanged { _, _, _, _ -> updateSaveEnabled() }

        etUsername.doOnTextChanged { text, _, _, _ ->
            // If contains '@', validate as email; otherwise allow username.
            val hasAt = !text.isNullOrBlank() && text.contains("@")
            val isInvalid = hasAt && !Patterns.EMAIL_ADDRESS.matcher(text).matches()

            if (isInvalid) {
                binding.tilUsername.isErrorEnabled = true
                binding.tilUsername.error = getString(R.string.error_invalid_email)
            } else {
                // Clear error AND disable error state to remove extra space
                binding.tilUsername.error = null
                binding.tilUsername.isErrorEnabled = false
            }

            updateSaveEnabled()
        }

        etPassword.doOnTextChanged { _, _, _, _ ->
            updateSaveEnabled()
        }

        etPassword.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_DONE && binding.btnSave.isEnabled) {
                performSave()
                true
            } else false
        }
    }

    private fun setupActions() = with(binding) {
        btnGenerate.setOnClickListener {
            val generated = generatePassword()
            etPassword.setText(generated)
            etPassword.setSelection(generated.length)
        }
        btnSave.setOnClickListener { performSave() }
    }

    private fun performSave() {
        val account = binding.etAccount.text?.toString()?.trim().orEmpty()
        val username = binding.etUsername.text?.toString()?.trim().orEmpty()
        val password = binding.etPassword.text?.toString()?.trim().orEmpty()
        val notes = binding.etNotes.text?.toString()?.trim().orEmpty()

        if (account.isBlank()) {
            binding.tilAccount.error = getString(R.string.error_required); return
        }
        binding.tilAccount.error = null

        if (password.length < 8) {
            binding.tilPassword.error = getString(R.string.error_min_password); return
        }
        binding.tilPassword.error = null

        lifecycleScope.launch {
            val siteId = repo.addOrUpdateSite(site = account, note = notes.ifBlank { null })
            repo.addCredential(siteId, username, password)
            setResult(RESULT_OK, Intent())
            finish()
        }
    }

    private fun updateSaveEnabled() {
        val accountOk = !binding.etAccount.text.isNullOrBlank()
        val passOk = (binding.etPassword.text?.length ?: 0) >= 8
        val emailTxt = binding.etUsername.text?.toString().orEmpty()
        val emailOk = !emailTxt.contains("@") || Patterns.EMAIL_ADDRESS.matcher(emailTxt).matches()
        binding.btnSave.isEnabled = accountOk && passOk && emailOk
    }

    /** Simple secure generator */
    private fun generatePassword(length: Int = 16, includeSymbols: Boolean = true): String {
        val upper = "ABCDEFGHJKLMNPQRSTUVWXYZ"
        val lower = "abcdefghijkmnopqrstuvwxyz"
        val digits = "23456789"
        val symbols = "!@#$%^&*()-_=+[]{}"
        val pools = mutableListOf(upper, lower, digits).apply { if (includeSymbols) add(symbols) }
        val all = pools.joinToString("")

        val rnd = Random(Date().time)
        val required = buildString {
            append(upper.random(rnd))
            append(lower.random(rnd))
            append(digits.random(rnd))
            if (includeSymbols) append(symbols.random(rnd))
        }

        val remaining = (length - required.length).coerceAtLeast(0)
        val tail = CharArray(remaining) { all[rnd.nextInt(all.length)] }
        val chars = (required + String(tail)).toMutableList()

        // Fisher–Yates shuffle
        for (i in chars.lastIndex downTo 1) {
            val j = rnd.nextInt(i + 1)
            val tmp = chars[i]; chars[i] = chars[j]; chars[j] = tmp
        }

        return chars.joinToString("")
    }
}
