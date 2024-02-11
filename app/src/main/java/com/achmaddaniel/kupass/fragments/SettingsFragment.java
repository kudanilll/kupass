package com.achmaddaniel.kupass.fragments;

import com.achmaddaniel.kupass.R;
import com.achmaddaniel.kupass.activities.MainPage;
import com.achmaddaniel.kupass.BuildConfig;
import com.achmaddaniel.kupass.core.App;
import com.achmaddaniel.kupass.core.ConstantVar;
import com.achmaddaniel.kupass.database.Pref;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.gms.oss.licenses.OssLicensesMenuActivity;

import androidx.appcompat.app.AppCompatDelegate;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;

import android.os.Bundle;
import android.content.Intent;
import android.net.Uri;

public class SettingsFragment extends PreferenceFragmentCompat {
	
	private Preference mLanguage;
	private Preference mTheme;
	private Preference mVersion;
	private Preference mLicense;
	private Preference mAboutUs;
	
	private String[] mLanguageList;
	private String[] mThemeList;
	
	@Override
	public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
		setPreferencesFromResource(R.xml.preferences, rootKey);
		
		initPreference();
		mLanguage.setOnPreferenceChangeListener((preference, newValue) -> {
			switch(String.valueOf(newValue)) {
			case ConstantVar.PREF_LANG_DEF:
				Pref.setLanguage(ConstantVar.LANG_DEF);
				break;
			case ConstantVar.PREF_LANG_EN:
				Pref.setLanguage(ConstantVar.LANG_EN);
				break;
			case ConstantVar.PREF_LANG_IN:
				Pref.setLanguage(ConstantVar.LANG_IN);
				break;
			}
			dialogRestart();
			return true;
		});
		mTheme.setOnPreferenceChangeListener((preference, newValue) -> {
			switch(String.valueOf(newValue)) {
			case ConstantVar.PREF_THEME_SYSTEM:
				Pref.setTheme(ConstantVar.THEME_SYSTEM);
				AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
				break;
			case ConstantVar.PREF_THEME_LIGHT:
				Pref.setTheme(ConstantVar.THEME_LIGHT);
				AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
				break;
			case ConstantVar.PREF_THEME_DARK:
				Pref.setTheme(ConstantVar.THEME_DARK);
				AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
				break;
			}
			mTheme.setSummary(mThemeList[Pref.getTheme()]);
			return true;
		});
		mVersion.setOnPreferenceClickListener((preference) -> {
			getActivity().startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(BuildConfig.GIT_URL)));
			return true;
		});
		mLicense.setOnPreferenceClickListener((preference) -> {
			getActivity().startActivity(new Intent(getActivity(), OssLicensesMenuActivity.class));
			return true;
		});
		mAboutUs.setOnPreferenceClickListener((preference) -> {
			getActivity().startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(BuildConfig.DEV_URL)));
			return true;
		});
	}
	
	private void initPreference() {
		mLanguage = findPreference("language");
		mTheme	  = findPreference("theme");
		mVersion  = findPreference("version");
		mLicense  = findPreference("license");
		mAboutUs  = findPreference("developer");
		
		mLanguageList = getActivity().getResources().getStringArray(R.array.language_list);
		mThemeList = getActivity().getResources().getStringArray(R.array.theme_list);
		
		mLanguage.setSummary(mLanguageList[Pref.getLanguage()]);
		mTheme.setSummary(mThemeList[Pref.getTheme()]);
		mVersion.setSummary(BuildConfig.VERSION_NAME + " - " + BuildConfig.BUILD_TYPE);
		mAboutUs.setSummary(BuildConfig.DEV_NAME);
	}
	
	private void dialogRestart() {
		new MaterialAlertDialogBuilder(getActivity())
			.setMessage(getActivity().getString(R.string.dialog_message_language))
			.setPositiveButton(getActivity().getString(R.string.dialog_restart), (dialog, which) -> {
				App.initialise(getContext());
				Intent restart = new Intent(getActivity(), MainPage.class);
				restart.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
				restart.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
				getActivity().startActivity(restart);
			})
			.setCancelable(false)
			.create()
			.show();
	}
}