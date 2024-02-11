package com.achmaddaniel.kupass.services.writer;

import com.achmaddaniel.kupass.core.ConstantVar;

//import com.opencsv.CSVWriter;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;

import java.io.OutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.nio.charset.Charset;

public class ScoopedStorage {
	
	public static final int REQUEST_ACTION_CREATE_FILE = 101;
	public static final int REQUEST_ACTION_CREATE_CSV  = 102;
	
	private static Activity mActivity;
	private static Context mContext;
	private static String mContent;
	
	public static void init(Activity activity, Context context) {
		mActivity = activity;
		mContext  = context;
	}
	
	public static void createFile(String content, int extension) {
		mContent = content;
		String extensionString = ConstantVar.STRING_EMPTY;
		switch(extension) {
		case ConstantVar.EXPORT_JSON:
			extensionString = ".json";
			break;
		case ConstantVar.EXPORT_TEXT:
			extensionString = ".txt";
			break;
		}
		
		SimpleDateFormat formatter = new SimpleDateFormat("ddMMyyyyHHmmss");
		String filename = ConstantVar.FILES_PREFIX + formatter.format(new Date()) + extensionString;
		
		Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
		intent.addCategory(Intent.CATEGORY_OPENABLE);
		intent.setType("text/*");
		intent.putExtra(Intent.EXTRA_TITLE, filename);
		mActivity.startActivityForResult(intent, REQUEST_ACTION_CREATE_FILE);
	}
	
	public static boolean create(Uri uri) {
		if(uri == null)
			return false;
		try {
			OutputStream fstream = mContext.getContentResolver().openOutputStream(uri);
			fstream.write(mContent.getBytes(Charset.forName("ASCII")));
			fstream.close();
		} catch(IOException e) {
			e.printStackTrace();
			return false;
		}
		return true;
	}
	
	public static void createFileCsv() {
		SimpleDateFormat formatter = new SimpleDateFormat("ddMMyyyyHHmmss");
		String filename = ConstantVar.FILES_PREFIX + formatter.format(new Date()) + ".csv";
	}
	
	public static boolean createCsv(Intent intent) {
		
		String[] header = {
			"password_name",
			"username",
			"password",
			"note"
		};
		
		return true;
	}
}