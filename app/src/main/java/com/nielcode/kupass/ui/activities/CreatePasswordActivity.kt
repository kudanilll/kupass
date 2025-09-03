package com.nielcode.kupass.ui.activities

import android.content.Intent
import android.os.Bundle
import android.os.Parcelable
import android.util.Log
import android.util.Patterns
import android.view.inputmethod.EditorInfo
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
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
import kotlinx.parcelize.Parcelize
import kotlin.math.min
import kotlin.random.Random

class CreatePasswordActivity : AppCompatActivity() {

    // Debug tag
    private val tag = "CreatePasswordActivity"

    // Using lazy initialization for binding and preferences.
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
        // This must be called before setContentView to apply dynamic colors correctly on recreation.
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
        // Validation & enable Save if valid
        etAccount.doOnTextChanged { _, _, _, _ -> updateSaveEnabled() }
        etUsername.doOnTextChanged { text, _, _, _ ->
            if (!text.isNullOrBlank() && text.contains("@") && !Patterns.EMAIL_ADDRESS.matcher(text)
                    .matches()
            ) {
                tilUsername.error = getString(R.string.error_invalid_email)
            } else {
                tilUsername.error = null
            }
            updateSaveEnabled()
        }

        etPassword.doOnTextChanged { text, _, _, _ ->
            updateStrength(text?.toString().orEmpty())
            updateSaveEnabled()
        }

        // Submit via IME action (Done)
        etPassword.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_DONE && binding.btnSave.isEnabled) {
                performSave()
                true
            } else false
        }
    }

    private fun setupActions() = with(binding) {
        btnGenerate.setOnClickListener {
            val generated = generateStrongPassword()
            etPassword.setText(generated)
            etPassword.setSelection(generated.length)
        }

        btnSave.setOnClickListener {
            performSave()
        }
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
            // 1) Insert / upsert site (contoh ini selalu insert baru, bisa kamu modifikasi jadi upsert jika site exist)
            val siteId = repo.addOrUpdateSite(site = account, note = notes.ifBlank { null })

            // 2) Insert credential terenkripsi (AES-GCM via Tink)
            repo.addCredential(siteId, username, password)

            setResult(RESULT_OK, Intent()) // optional
            finish()
        }
//        val account = binding.etAccount.text?.toString()?.trim().orEmpty()
//        val username = binding.etUsername.text?.toString()?.trim().orEmpty()
//        val password = binding.etPassword.text?.toString()?.trim().orEmpty()
//        val notes = binding.etNotes.text?.toString()?.trim().orEmpty()
//
//        // Validate
//        if (account.isBlank()) {
//            binding.tilAccount.error = getString(R.string.error_required)
//            return
//        } else binding.tilAccount.error = null
//
//        if (password.length < 8) {
//            binding.tilPassword.error = getString(R.string.error_min_password)
//            return
//        } else binding.tilPassword.error = null
//
//        // Return the entry (or save via repository)
//        val entry = PasswordEntry(
//            account = account,
//            username = username,
//            password = password, // NOTE: for production, encrypt this before saving!
//            notes = notes
//        )
//
//        val data = Intent().apply {
//            putExtra(EXTRA_RESULT_ENTRY, entry)
//        }
//        setResult(RESULT_OK, data)
//        finish()
    }

    private fun updateSaveEnabled() {
        val accountOk = !binding.etAccount.text.isNullOrBlank()
        val passOk = (binding.etPassword.text?.length ?: 0) >= 8
        val emailTxt = binding.etUsername.text?.toString().orEmpty()
        val emailOk = !emailTxt.contains("@") || Patterns.EMAIL_ADDRESS.matcher(emailTxt).matches()
        binding.btnSave.isEnabled = accountOk && passOk && emailOk
    }

    private fun updateStrength(password: String) = with(binding) {
        val score = passwordScore(password) // 0..100
        strengthBar.progress = score

        val label = when {
            score >= 80 -> getString(R.string.strength_strong)
            score >= 50 -> getString(R.string.strength_medium)
            else -> getString(R.string.strength_weak)
        }
        strengthLabel.text = label

        // Change indicator color
        val color = when {
            score >= 80 -> ContextCompat.getColor(
                this@CreatePasswordActivity,
                R.color.md_theme_primary
            )

            score >= 50 -> ContextCompat.getColor(
                this@CreatePasswordActivity,
                R.color.md_theme_tertiary
            )

            else -> ContextCompat.getColor(
                this@CreatePasswordActivity,
                R.color.md_theme_error
            )
        }
        strengthBar.setIndicatorColor(color)
        strengthLabel.setTextColor(color)
    }

    /** Score: length + character variety */
    private fun passwordScore(pw: String): Int {
        if (pw.isEmpty()) return 0
        var sets = 0
        if (pw.any { it.isLowerCase() }) sets++
        if (pw.any { it.isUpperCase() }) sets++
        if (pw.any { it.isDigit() }) sets++
        if (pw.any { "!@#$%^&*()-_=+[]{};:'\",.<>?/\\|`~".contains(it) }) sets++

        val lengthScore = min(pw.length * 6, 60) // up to 60
        val varietyScore = sets * 10            // up to 40
        return min(lengthScore + varietyScore, 100)
    }

    /** Password generator */
    private fun generateStrongPassword(
        length: Int = 16,
        includeSymbols: Boolean = true
    ): String {
        val upper = "ABCDEFGHJKLMNPQRSTUVWXYZ" // without I/O for avoid confusion
        val lower = "abcdefghijkmnopqrstuvwxyz"
        val digits = "23456789"
        val symbols = "!@#$%^&*()-_=+[]{}"

        val pools = mutableListOf(upper, lower, digits).apply {
            if (includeSymbols) add(symbols)
        }

        val all = pools.joinToString("")
        val rnd = Random(1)

        // Make sure to include at least one character from each pool
        val required = buildString {
            append(upper.random(rnd))
            append(lower.random(rnd))
            append(digits.random(rnd))
            if (includeSymbols) append(symbols.random(rnd))
        }

        val remaining = (length - required.length).coerceAtLeast(0)
        val tail = CharArray(remaining) { all[rnd.nextInt(all.length)] }
        val chars = (required + String(tail)).toMutableList()

        // Shuffle
        for (i in chars.indices.reversed()) {
            val j = rnd.nextInt(i + 1)
            val tmp = chars[i]
            chars[i] = chars[j]
            chars[j] = tmp
        }
        return chars.joinToString("")
    }

    companion object {
        const val EXTRA_RESULT_ENTRY = "extra_result_entry"
    }
}

@Parcelize
data class PasswordEntry(
    val account: String,
    val username: String,
    val password: String,
    val notes: String
) : Parcelable
