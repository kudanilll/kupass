package com.nielcode.kupass.ui.activities;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import com.google.android.gms.oss.licenses.OssLicensesMenuActivity;
import com.google.android.material.color.DynamicColors;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.nielcode.kupass.BuildConfig;
import com.nielcode.kupass.R;
import com.nielcode.kupass.core.KupassApplication;
import com.nielcode.kupass.core.PreferenceManager;
import com.nielcode.kupass.databinding.ActivitySettingsBinding;
import com.nielcode.kupass.utils.Config;

public class SettingsActivity extends AppCompatActivity {

  private ActivitySettingsBinding binding;
  private PreferenceManager pref;

  @SuppressLint("SetTextI18n")
  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    binding = ActivitySettingsBinding.inflate(getLayoutInflater());
    setContentView(binding.getRoot());

    pref = new PreferenceManager(this);
    binding.appVersion.setText(BuildConfig.VERSION_NAME + " - " + BuildConfig.BUILD_TYPE);
    binding.developerName.setText(BuildConfig.DEV_NAME);

    updateUi();
    setupButtonListener();
  }

  @Override
  protected void onDestroy() {
    super.onDestroy();
    binding = null;
  }

  private void setupButtonListener() {
    binding.toolbar.setNavigationOnClickListener(v -> finish());
    binding.languageSwitchButton.setOnClickListener(v -> showLanguageDialog());
    binding.themeSwitchButton.setOnClickListener(v -> showThemeDialog());
    binding.dynamicColorsSwitchButton.setOnClickListener(v -> showDynamicColorsDialog());
    binding.aboutButton.setOnClickListener(
        v -> startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(BuildConfig.GIT_URL))));
    binding.licenseButton.setOnClickListener(
        v -> startActivity(new Intent(this, OssLicensesMenuActivity.class)));
  }

  private void updateUi() {
    String[] languageList = getResources().getStringArray(R.array.language_list);
    binding.currentLanguage.setText(languageList[pref.getLanguage()]);

    String[] themeList = getResources().getStringArray(R.array.theme_list);
    binding.currentTheme.setText(themeList[pref.getTheme()]);

    handleDynamicColors();
  }

  private void handleDynamicColors() {
    if (DynamicColors.isDynamicColorAvailable()) {
      binding.dynamicColorsSwitchButton.setEnabled(true);
      binding.dynamicColorsSwitchButton.setAlpha(1.0f);
      boolean isEnabled = pref.getDynamicColor() == Config.DynamicColors.Code.ENABLE;
      binding.currentDynamicColors.setText(isEnabled ? R.string.enable : R.string.disable);
    } else {
      binding.dynamicColorsSwitchButton.setEnabled(false);
      binding.dynamicColorsSwitchButton.setAlpha(0.5f);
      binding.currentDynamicColors.setText(R.string.not_supported);
    }
  }

  private void showLanguageDialog() {
    final int[] selectedItem = {pref.getLanguage()};
    new MaterialAlertDialogBuilder(this)
        .setTitle(R.string.settings_language_title)
        .setSingleChoiceItems(
            R.array.language_list,
            selectedItem[0], // Default selection
            (dialog, which) -> selectedItem[0] = which)
        .setNegativeButton(R.string.dialog_cancel, (dialog, which) -> dialog.dismiss())
        .setPositiveButton(
            R.string.dialog_apply,
            (dialog, which) -> {
              // Apply changes if has changed
              if (pref.getLanguage() != selectedItem[0]) {
                pref.setLanguage(selectedItem[0]);
                updateUi();
                showRestartDialog();
              }
            })
        .show();
  }

  private void showThemeDialog() {
    final int[] selectedItem = {pref.getTheme()};
    new MaterialAlertDialogBuilder(this)
        .setTitle(R.string.settings_theme_title)
        .setSingleChoiceItems(
            R.array.theme_list, selectedItem[0], (dialog, which) -> selectedItem[0] = which)
        .setNegativeButton(R.string.dialog_cancel, (dialog, which) -> dialog.dismiss())
        .setPositiveButton(
            R.string.dialog_apply,
            (dialog, which) -> {
              if (pref.getTheme() != selectedItem[0]) {
                pref.setTheme(selectedItem[0]);
                applyTheme(selectedItem[0]);
                updateUi();
              }
              dialog.dismiss();
            })
        .show();
  }

  private void showDynamicColorsDialog() {
    String[] options = {getString(R.string.enable), getString(R.string.disable)};
    int currentSelection = pref.getDynamicColor() == Config.DynamicColors.Code.ENABLE ? 0 : 1;
    final int[] selectedItem = {currentSelection};

    new MaterialAlertDialogBuilder(this)
        .setTitle(R.string.settings_dynamic_colors_title)
        .setSingleChoiceItems(options, selectedItem[0], (dialog, which) -> selectedItem[0] = which)
        .setNegativeButton(R.string.dialog_cancel, (dialog, which) -> dialog.dismiss())
        .setPositiveButton(
            R.string.dialog_apply,
            (dialog, which) -> {
              int newSetting =
                  (selectedItem[0] == 0)
                      ? Config.DynamicColors.Code.ENABLE
                      : Config.DynamicColors.Code.DISABLE;
              if (pref.getDynamicColor() != newSetting) {
                pref.setDynamicColor(newSetting);
                updateUi();
                showRestartDialog();
              }
            })
        .show();
  }

  private void applyTheme(int themeCode) {
    switch (themeCode) {
      case Config.Theme.Code.SYSTEM:
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
        break;
      case Config.Theme.Code.LIGHT:
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
        break;
      case Config.Theme.Code.DARK:
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
        break;
    }
  }

  /** Show dialog to restart the app. */
  private void showRestartDialog() {
    new MaterialAlertDialogBuilder(this)
        .setMessage(R.string.dialog_message_language)
        .setPositiveButton(
            R.string.dialog_restart,
            (dialog, which) -> {
              // Restart app
              KupassApplication.updateUi(getApplication(), this);
              Intent restart = new Intent(this, HomeActivity.class);
              restart.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
              restart.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
              startActivity(restart);
              recreate();
            })
        .setCancelable(false)
        .show();
  }
}
