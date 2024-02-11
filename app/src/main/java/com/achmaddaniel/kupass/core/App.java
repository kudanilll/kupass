package com.achmaddaniel.kupass.core;

import com.achmaddaniel.kupass.database.Pref;
import com.google.android.material.color.DynamicColors;

import androidx.appcompat.app.AppCompatDelegate;
import android.app.Application;
import android.content.Context;
import android.content.res.Configuration;
import android.content.res.Resources;
import java.util.Locale;

public class App extends Application {
	
	@Override
	public void onCreate() {
		super.onCreate();
		DynamicColors.applyToActivitiesIfAvailable(this);
		initialise(this);
	}
	
	public static void initialise(Context context) {
		Pref.init(context);
		// Init Language
		Resources res = context.getResources();
		Configuration config = res.getConfiguration();
		switch(Pref.getLanguage()) {
		case ConstantVar.LANG_DEF:
		case ConstantVar.LANG_EN:
			config.setLocale(Locale.getDefault());
			break;
		case ConstantVar.LANG_IN:
			config.setLocale(new Locale("in"));
			break;
		}
		res.updateConfiguration(config, res.getDisplayMetrics());
		
		// Init Theme
		switch(Pref.getTheme()) {
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
	}
}