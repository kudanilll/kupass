package com.nielcode.kupass.core;

import android.content.Context;
import android.content.res.Configuration;
import android.content.res.Resources;
import androidx.appcompat.app.AppCompatDelegate;
import com.google.android.material.color.DynamicColors;
import com.nielcode.kupass.utils.Config;
import com.nielcode.kupass.utils.PreferenceManager;
import java.util.Locale;

public class Application extends android.app.Application {

  public static void initialise(Context context) {
    PreferenceManager pref = new PreferenceManager(context);
    setLanguage(pref.getLanguage(), context);
    setAppTheme(pref.getTheme());
  }

  private static void setLanguage(int languageCode, Context context) {
    Resources res = context.getResources();
    Configuration config = res.getConfiguration();
    switch (languageCode) {
      case Config.Language.Code.DEFAULT:
      case Config.Language.Code.ENGLISH:
        config.setLocale(Locale.getDefault());
        break;
      case Config.Language.Code.INDONESIA:
        config.setLocale(new Locale("in"));
        break;
    }
    res.updateConfiguration(config, res.getDisplayMetrics());
  }

  private static void setAppTheme(int themeCode) {
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

  @Override
  public void onCreate() {
    super.onCreate();
    if (DynamicColors.isDynamicColorAvailable()) {
      DynamicColors.applyToActivitiesIfAvailable(this);
    }
    initialise(this);
  }
}
