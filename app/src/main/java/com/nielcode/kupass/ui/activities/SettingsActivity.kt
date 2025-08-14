package com.nielcode.kupass.ui.activities

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.net.toUri
import androidx.core.os.LocaleListCompat
import com.google.android.gms.oss.licenses.OssLicensesMenuActivity
import com.google.android.material.color.DynamicColors
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.nielcode.kupass.BuildConfig
import com.nielcode.kupass.R
import com.nielcode.kupass.core.PreferenceManager
import com.nielcode.kupass.databinding.ActivitySettingsBinding
import com.nielcode.kupass.utils.AppConfig

class SettingsActivity : AppCompatActivity() {

    private val TAG = "SettingsActivity"

    // Using lazy initialization for binding and preferences.
    private val binding by lazy { ActivitySettingsBinding.inflate(layoutInflater) }
    private val prefs by lazy { PreferenceManager(this) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // This must be called before setContentView to apply dynamic colors correctly on recreation.
        if (prefs.dynamicColor == AppConfig.DynamicColors.Code.ENABLE && DynamicColors.isDynamicColorAvailable()) {
            Log.d(TAG, "Applying dynamic colors")
            DynamicColors.applyToActivityIfAvailable(this)
        }
        setContentView(binding.root)

        setupToolbar()
        setupInfo()
        setupListeners()
        updateUiWithCurrentSettings()
    }

    private fun setupToolbar() {
        binding.toolbar.setNavigationOnClickListener { finish() }
    }

    @SuppressLint("SetTextI18n")
    private fun setupInfo() {
        binding.appVersion.text = "${BuildConfig.VERSION_NAME} - ${BuildConfig.BUILD_TYPE}"
        binding.developerName.text = BuildConfig.DEV_NAME
    }

    private fun setupListeners() {
        binding.apply {
            languageSwitchButton.setOnClickListener { showLanguageDialog() }
            themeSwitchButton.setOnClickListener { showThemeDialog() }
            dynamicColorsSwitchButton.setOnClickListener { showDynamicColorsDialog() }
            licenseButton.setOnClickListener { showOpenSourceLicenses() }
            aboutButton.setOnClickListener { openUrl(BuildConfig.DEV_URL) }
            githubButton.setOnClickListener { openUrl(BuildConfig.GIT_URL) }
        }
    }

    private fun updateUiWithCurrentSettings() {
        val languageList = resources.getStringArray(R.array.language_list)
        binding.currentLanguage.text = languageList[prefs.language]

        val themeList = resources.getStringArray(R.array.theme_list)
        binding.currentTheme.text = themeList[prefs.theme]

        updateDynamicColorsUi()
    }

    private fun updateDynamicColorsUi() {
        val isSupported = DynamicColors.isDynamicColorAvailable()
        binding.dynamicColorsSwitchButton.isEnabled = isSupported
        binding.dynamicColorsSwitchButton.alpha = if (isSupported) 1.0f else 0.5f

        val statusTextRes = when {
            !isSupported -> R.string.not_supported
            prefs.dynamicColor == AppConfig.DynamicColors.Code.ENABLE -> R.string.enable
            else -> R.string.disable
        }
        binding.currentDynamicColors.setText(statusTextRes)
    }

    private fun showLanguageDialog() {
        val languageNames = resources.getStringArray(R.array.language_list)
        var selectedIndex = prefs.language

        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.settings_language_title)
            .setSingleChoiceItems(languageNames, selectedIndex) { _, which ->
                selectedIndex = which
            }
            .setNegativeButton(R.string.dialog_cancel, null)
            .setPositiveButton(R.string.dialog_apply) { _, _ ->
                if (prefs.language != selectedIndex) {
                    prefs.language = selectedIndex
                    applyLanguage(selectedIndex)
                }
            }
            .show()
    }

    private fun applyLanguage(selectedIndex: Int) {
        val languageTags = resources.getStringArray(R.array.language_values)
        AppCompatDelegate.setApplicationLocales(
            LocaleListCompat.forLanguageTags(languageTags[selectedIndex])
        )
    }

    private fun showThemeDialog() {
        var selectedIndex = prefs.theme
        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.settings_theme_title)
            .setSingleChoiceItems(R.array.theme_list, selectedIndex) { _, which ->
                selectedIndex = which
            }
            .setNegativeButton(R.string.dialog_cancel, null)
            .setPositiveButton(R.string.dialog_apply) { _, _ ->
                if (prefs.theme != selectedIndex) {
                    prefs.theme = selectedIndex
                    applyTheme(selectedIndex)
                }
            }
            .show()
    }

    private fun applyTheme(themeCode: Int) {
        val nightMode = when (themeCode) {
            AppConfig.Theme.Code.LIGHT -> AppCompatDelegate.MODE_NIGHT_NO
            AppConfig.Theme.Code.DARK -> AppCompatDelegate.MODE_NIGHT_YES
            else -> AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
        }
        AppCompatDelegate.setDefaultNightMode(nightMode)
    }

    private fun showDynamicColorsDialog() {
        val options = arrayOf(getString(R.string.enable), getString(R.string.disable))
        val currentSelection =
            if (prefs.dynamicColor == AppConfig.DynamicColors.Code.ENABLE) 0 else 1
        var selectedIndex = currentSelection

        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.settings_dynamic_colors_title)
            .setSingleChoiceItems(options, selectedIndex) { _, which ->
                selectedIndex = which
            }
            .setNegativeButton(R.string.dialog_cancel, null)
            .setPositiveButton(R.string.dialog_apply) { _, _ ->
                val newSetting = if (selectedIndex == 0) {
                    AppConfig.DynamicColors.Code.ENABLE
                } else {
                    AppConfig.DynamicColors.Code.DISABLE
                }

                if (prefs.dynamicColor != newSetting) {
                    prefs.dynamicColor = newSetting
                    Log.d(TAG, "Dynamic colors state: $newSetting")

                    // Restart the app to apply dynamic colors.
                    showRestartDialog()
                }
            }
            .show()
    }

    private fun showOpenSourceLicenses() {
        startActivity(Intent(this, OssLicensesMenuActivity::class.java))
    }

    private fun openUrl(url: String) {
        val intent = Intent(Intent.ACTION_VIEW, url.toUri())
        startActivity(intent)
    }

    private fun showRestartDialog() {
        MaterialAlertDialogBuilder(this)
            .setMessage(R.string.dialog_message_language)
            .setPositiveButton(R.string.dialog_restart) { _, _ ->
                val restart = Intent(this, HomeActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
                }
                startActivity(restart)
            }
            .setCancelable(false)
            .show()
    }
}