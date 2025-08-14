package com.nielcode.kupass.core;

import android.app.Application;
import android.content.Context;
import android.content.res.Configuration;
import android.content.res.Resources;
import androidx.appcompat.app.AppCompatDelegate;
import com.google.android.material.color.DynamicColors;
import com.nielcode.kupass.utils.Config;
import java.util.Locale;

public class KupassApplication extends Application {

  public static void updateUi(Application application, Context context) {

    // Initialize preferences
    PreferenceManager pref = new PreferenceManager(context);

    // Apply preferences
    setLanguage(pref.getLanguage(), context);
    setAppTheme(pref.getTheme());

    // Dynamic colors
    if (DynamicColors.isDynamicColorAvailable()) {
      if (pref.getDynamicColor() == Config.DynamicColors.Code.ENABLE) {
        DynamicColors.applyToActivitiesIfAvailable(application);
      }
    } else {
      pref.setDynamicColor(Config.DynamicColors.Code.NOT_SUPPORTED);
    }
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
        config.setLocale(new Locale(Config.Language.INDONESIA));
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
    updateUi(this, this);
  }
}
