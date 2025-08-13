package com.nielcode.kupass.ui.settings;

import com.nielcode.kupass.App;
import com.nielcode.kupass.R;
import com.nielcode.kupass.ui.MainActivity;
import com.nielcode.kupass.BuildConfig;
import com.nielcode.kupass.utils.Config;

import com.nielcode.kupass.utils.PreferenceManager;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
//import com.google.android.gms.oss.licenses.OssLicensesMenuActivity;

import androidx.appcompat.app.AppCompatDelegate;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;

import android.os.Bundle;
import android.content.Intent;
import android.net.Uri;

public class SettingsFragment extends PreferenceFragmentCompat  {

	private Preference language;
	private Preference theme;
	private Preference version;
	private Preference license;
	private Preference dev;

	private String[] themes;

	private PreferenceManager preferenceManager;


	@Override
	public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
		setPreferencesFromResource(R.xml.preferences, rootKey);

		preferenceManager = new PreferenceManager(requireContext());
		initPreference();
		setPreferenceListener();
	}

	private void initPreference() {
		language = findPreference("language");
		theme = findPreference("theme");
		version = findPreference("version");
		license = findPreference("license");
		dev = findPreference("developer");

		String[] languages = requireActivity().getResources().getStringArray(R.array.language_list);
		themes = requireActivity().getResources().getStringArray(R.array.theme_list);

		language.setSummary(languages[preferenceManager.getLanguage()]);
		theme.setSummary(themes[preferenceManager.getTheme()]);
		version.setSummary(BuildConfig.VERSION_NAME + " - " + BuildConfig.BUILD_TYPE);
		dev.setSummary(BuildConfig.DEV_NAME);
	}

	private void setPreferenceListener() {
		language.setOnPreferenceChangeListener((preference, newValue) -> {
			switch(String.valueOf(newValue)) {
				case Config.Language.DEFAULT:
					preferenceManager.setLanguage(Config.Language.Code.DEFAULT);
					break;
				case Config.Language.ENGLISH:
					preferenceManager.setLanguage(Config.Language.Code.ENGLISH);
					break;
				case Config.Language.INDONESIA:
					preferenceManager.setLanguage(Config.Language.Code.INDONESIA);
					break;
			}
			dialogRestart();
			return true;
		});
		theme.setOnPreferenceChangeListener((preference, newValue) -> {
			switch(String.valueOf(newValue)) {
				case Config.Theme.SYSTEM:
					preferenceManager.setTheme(Config.Theme.Code.SYSTEM);
					AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
					break;
				case Config.Theme.LIGHT:
					preferenceManager.setTheme(Config.Theme.Code.LIGHT);
					AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
					break;
				case Config.Theme.DARK:
					preferenceManager.setTheme(Config.Theme.Code.DARK);
					AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
					break;
			}
			theme.setSummary(themes[preferenceManager.getTheme()]);
			return true;
		});
		version.setOnPreferenceClickListener((preference) -> {
			requireActivity().startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(BuildConfig.GIT_URL)));
			return true;
		});
		license.setOnPreferenceClickListener((preference) -> {
//			requireActivity().startActivity(new Intent(requireActivity(), OssLicensesMenuActivity.class));
			return true;
		});
		dev.setOnPreferenceClickListener((preference) -> {
			requireActivity().startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(BuildConfig.DEV_URL)));
			return true;
		});
	}

	private void dialogRestart() {
		new MaterialAlertDialogBuilder(requireActivity())
				.setMessage(requireActivity().getString(R.string.restart_required))
				.setPositiveButton(requireActivity().getString(R.string.restart), (dialog, which) -> {
					App.initialise(getContext());
					Intent restart = new Intent(requireActivity(), MainActivity.class);
					restart.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
					restart.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
					requireActivity().startActivity(restart);
				})
				.setCancelable(false)
				.create()
				.show();
	}
}