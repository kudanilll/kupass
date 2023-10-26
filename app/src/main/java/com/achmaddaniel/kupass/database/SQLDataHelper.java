package com.achmaddaniel.kupass.database;

import com.achmaddaniel.kupass.adapter.ListItem;

import android.content.Context;
import android.content.ContentValues;

import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.Cursor;

import android.provider.BaseColumns;

import java.util.ArrayList;

public class SQLDataHelper extends SQLiteOpenHelper {
	
	private static final String DATABASE_NAME = "local_database.db";
	private static final int DATABASE_VERSION = 1;
	
	// Table & Column name
	public static final String TABLE_NAME = "list_password";
	public static final String COLUMN_ID = BaseColumns._ID;
	public static final String COLUMN_PASSWORD_NAME = "password_name";
	public static final String COLUMN_USERNAME = "username";
	public static final String COLUMN_PASSWORD = "password";
	public static final String COLUMN_NOTE = "note";
	
	// Query
	private static final String TABLE_CREATE =
		"CREATE TABLE " + TABLE_NAME + " (" +
		COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
		COLUMN_PASSWORD_NAME + " TEXT, " +
		COLUMN_USERNAME + " TEXT, " +
		COLUMN_PASSWORD + " TEXT, " +
		COLUMN_NOTE + " TEXT);";

	public SQLDataHelper(Context context) {
		super(context, DATABASE_NAME, null, DATABASE_VERSION);
	}

	@Override
	public void onCreate(SQLiteDatabase db) {
		db.execSQL(TABLE_CREATE);
	}

	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
		db.execSQL("DROP TABLE IF EXISTS " + TABLE_NAME);
		onCreate(db);
	}
	
	public long create(String passwordName, String username, String password, String note) {
		SQLiteDatabase db = this.getWritableDatabase();
		ContentValues values = new ContentValues();
		values.put(COLUMN_PASSWORD_NAME, passwordName);
		values.put(COLUMN_USERNAME, username);
		values.put(COLUMN_PASSWORD, password);
		values.put(COLUMN_NOTE, note);
		long newRowId = db.insert(TABLE_NAME, null, values);
		db.close();
		return newRowId;
	}
	
	public int update(long id, String passwordName, String username, String password, String note) {
		SQLiteDatabase db = this.getWritableDatabase();
		ContentValues values = new ContentValues();
		values.put(COLUMN_PASSWORD_NAME, passwordName);
		values.put(COLUMN_USERNAME, username);
		values.put(COLUMN_PASSWORD, password);
		values.put(COLUMN_NOTE, note);
		int rowsUpdated = db.update(TABLE_NAME, values, COLUMN_ID + " = ?", new String[]{String.valueOf(id)});
		db.close();
		return rowsUpdated;
	}
	
	public int delete(long id) {
		SQLiteDatabase db = this.getWritableDatabase();
		int rowsDeleted = db.delete(TABLE_NAME, COLUMN_ID + " = ?", new String[]{String.valueOf(id)});
		db.close();
		return rowsDeleted;
	}
	
	public int getCount() {
		SQLiteDatabase db = this.getReadableDatabase();
		Cursor cursor = db.rawQuery("SELECT * FROM " + TABLE_NAME, null);
		int count = cursor.getCount();
		cursor.close();
		db.close();
		return count;
	}
	
	public ArrayList<ListItem> getAll() {
		ArrayList<ListItem> result = new ArrayList<>();
		String selectQuery = "SELECT * FROM " + TABLE_NAME;
		SQLiteDatabase db = this.getWritableDatabase();
		Cursor cursor = db.rawQuery(selectQuery, null);
		if(cursor.moveToFirst()) {
			do {
				result.add(new ListItem(
					cursor.getLong(cursor.getColumnIndex(COLUMN_ID)),
					cursor.getString(cursor.getColumnIndex(COLUMN_PASSWORD_NAME)),
					cursor.getString(cursor.getColumnIndex(COLUMN_USERNAME)),
					cursor.getString(cursor.getColumnIndex(COLUMN_PASSWORD)),
					cursor.getString(cursor.getColumnIndex(COLUMN_NOTE))
				));
			} while(cursor.moveToNext());
		}
		cursor.close();
		db.close();
		return result;
	}
}