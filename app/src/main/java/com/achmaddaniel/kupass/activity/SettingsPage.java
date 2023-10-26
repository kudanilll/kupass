package com.achmaddaniel.kupass.activity;

import com.achmaddaniel.kupass.R;
import com.achmaddaniel.kupass.adapter.SettingsAdapter;
import com.achmaddaniel.kupass.adapter.ListSettings;
import com.achmaddaniel.kupass.database.Preference;
import com.achmaddaniel.kupass.core.ConstantVar;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import android.os.Bundle;

import android.net.Uri;

import android.content.Intent;

import android.view.View;
import android.view.LayoutInflater;

import android.widget.ListView;

import java.util.ArrayList;

public class SettingsPage extends AppCompatActivity {
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_settings_page);
		
		ArrayList<ListSettings> listSettings = new ArrayList<>();
		String[] settingsItemTitle = getResources().getStringArray(R.array.settings_item_title);
		String[] settingsItemSubtitle = getResources().getStringArray(R.array.settings_item_subtitle);
		
		ListView listView = findViewById(R.id.list_view);
		for(int i = 0; i < settingsItemTitle.length; i++)
			listSettings.add(new ListSettings(settingsItemTitle[i], settingsItemSubtitle[i]));
		listView.setAdapter(new SettingsAdapter(SettingsPage.this, listSettings));
		listView.setOnItemClickListener((adapter, view, position, id) -> {
			switch(position) {
			case 0: // Appearance
				dialogChangeTheme();
				break;
			case 1: // Language
				dialogChangeLanguage();
				break;
			case 2: // About
				Intent intent = new Intent(Intent.ACTION_VIEW);
				intent.setData(Uri.parse("https://linktr.ee/achmaddaniel")); // Dont change the link, Respect for the author!
				startActivity(intent);
				break;
			}
		});
	}
	
	private void dialogChangeTheme() {
		CharSequence[] listTheme = getResources().getStringArray(R.array.theme_list);
		new MaterialAlertDialogBuilder(SettingsPage.this)
			.setTitle(getString(R.string.dialog_title_theme))
			.setNegativeButton(getString(R.string.dialog_cancel), (dialog, which) -> {
			})
			.setPositiveButton(getString(R.string.dialog_apply), (dialog, which) -> {
				switch(Preference.getTheme()) {
				case ConstantVar.THEME_SYSTEM:
					AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
					break;
				case ConstantVar.THEME_LIGHT:
					AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
					break;
				case ConstantVar.THEME_DARK:
					AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
					break;
				}
			})
			.setSingleChoiceItems(listTheme, Preference.getTheme(), (dialog, which) -> {
				Preference.setTheme(which);
			})
			.create()
			.show();
	}
	
	private void dialogChangeLanguage() {
		CharSequence[] listLang = getResources().getStringArray(R.array.language_list);
		new MaterialAlertDialogBuilder(SettingsPage.this)
			.setTitle(getString(R.string.dialog_title_language))
			.setNegativeButton(getString(R.string.dialog_cancel), (dialog, which) -> {
			})
			.setPositiveButton(getString(R.string.dialog_apply), (dialog, which) -> {
				dialogRestart();
			})
			.setSingleChoiceItems(listLang, Preference.getLanguage(), (dialog, which) -> {
				Preference.setLanguage(which);
			})
			.create()
			.show();
	}
	
	private void dialogRestart() {
		new MaterialAlertDialogBuilder(SettingsPage.this)
			.setMessage(getString(R.string.dialog_message_language))
			.setPositiveButton(getString(R.string.dialog_restart), (dialog, which) -> {
				System.exit(0);
			})
			.setCancelable(false)
			.create()
			.show();
	}
}