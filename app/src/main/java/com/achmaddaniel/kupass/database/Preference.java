package com.achmaddaniel.kupass.database;

import com.achmaddaniel.kupass.core.ConstantVar;

import android.content.Context;
import android.content.SharedPreferences;

public class Preference {
	
	private static Context mContext;
	
	private static SharedPreferences get(Context context) {
		return mContext.getSharedPreferences("preferences", mContext.MODE_PRIVATE);
	}
	
	public static void init(Context context) {
		mContext = context;
	}
	
	public static void setExportMethod(int method) {
		SharedPreferences.Editor editor = get(mContext).edit();
		editor.putInt("export_method", method);
		editor.apply();
	}
	
	public static int getExportMethod() {
		return get(mContext).getInt("export_method", ConstantVar.EXPORT_TEXT);
	}
	
	public static void setLanguage(int language) {
		SharedPreferences.Editor editor = get(mContext).edit();
		editor.putInt("language", language);
		editor.apply();
	}
	
	public static int getLanguage() {
		return get(mContext).getInt("language", ConstantVar.LANG_EN);
	}
	
	public static void setTheme(int theme) {
		SharedPreferences.Editor editor = get(mContext).edit();
		editor.putInt("theme", theme);
		editor.apply();
	}
	
	public static int getTheme() {
		return get(mContext).getInt("theme", ConstantVar.THEME_SYSTEM);
	}
}