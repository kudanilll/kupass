package com.nielcode.kupass.core;

import android.content.Context;
import android.content.SharedPreferences;
import com.nielcode.kupass.utils.Config;

public class PreferenceManager {

  private static final String PREF_NAME = "preferences";

  private final SharedPreferences sharedPreferences;
  private final SharedPreferences.Editor editor;

  public PreferenceManager(Context context) {
    sharedPreferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    editor = sharedPreferences.edit();
  }

  public int getExportMethod() {
    return sharedPreferences.getInt("export_method", Config.FileType.TEXT);
  }

  public void setExportMethod(int method) {
    editor.putInt("export_method", method).apply();
  }

  public int getImportMethod() {
    return sharedPreferences.getInt("import_method", Config.FileType.JSON);
  }

  public void setImportMethod(int method) {
    editor.putInt("import_method", method).apply();
  }

  public int getLanguage() {
    return sharedPreferences.getInt("language", Config.Language.Code.DEFAULT);
  }

  public void setLanguage(int language) {
    editor.putInt("language", language).apply();
  }

  public int getTheme() {
    return sharedPreferences.getInt("theme", Config.Theme.Code.SYSTEM);
  }

  public void setTheme(int theme) {
    editor.putInt("theme", theme).apply();
  }

  public int getDynamicColor() {
    return sharedPreferences.getInt("dynamic_color", Config.DynamicColors.Code.DISABLE);
  }

  public void setDynamicColor(int dynamicColor) {
    editor.putInt("dynamic_color", dynamicColor).apply();
  }
}
