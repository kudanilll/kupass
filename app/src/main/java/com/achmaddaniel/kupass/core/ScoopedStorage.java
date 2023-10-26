package com.achmaddaniel.kupass.core;

import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.documentfile.provider.DocumentFile;

import android.app.Activity;

import android.content.Context;
import android.content.Intent;

import android.net.Uri;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.io.IOException;

import java.text.SimpleDateFormat;

import java.util.Date;

import java.nio.charset.Charset;

/** Depedencies not support in CodeAssist
import org.apache.poi.ss.usermodel.Workbook;
**/

public class ScoopedStorage {
	
	public static final int REQUEST_ACTION_CODE = 101;
	public static final int REQUEST_ACTION_XLSX = 102;
	
	/** Not Support in CodeAssist
	private static ActivityResultLauncher<Intent> mCreateDocumentLauncher;
	**/
	
	private static Activity mActivity;
	private static Context mContext;
	private static String mContent;
	/** Not Support in CodeAssist
	private static Workbook mWorkbook;
	**/
	
	public static void init(Activity activity, Context context) {
		mActivity = activity;
		mContext  = context;
	}
	
	/** Not Support in CodeAssist
	public static void setCallback(ActivityResultCallback<Uri> callback) {
		mCreateDocumentLauncher = mActivity.registerForActivityResult(
			new ActivityResultContracts.CreateDocument(), callback);
	}
	**/
	
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
		String filename = "kupass_backup_" + formatter.format(new Date()) + extensionString;
		
		/** Not Support in CodeAssist
		Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
		mCreateDocumentLauncher.addCategory(Intent.CATEGORY_OPENABLE);
		mCreateDocumentLauncher.setType("text/plain");
		mCreateDocumentLauncher.putExtra(Intent.EXTRA_TITLE, filename);
		mCreateDocumentLauncher.launch(intent);
		**/
		
		// Deprecated code :(
		Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
		intent.addCategory(Intent.CATEGORY_OPENABLE);
		intent.setType("text/*");
		intent.putExtra(Intent.EXTRA_TITLE, filename);
		mActivity.startActivityForResult(intent, REQUEST_ACTION_CODE);
	}
	
	/** Not Support in CodeAssist
	public static void createFileXlsx(Workbook workbook) {
		mWorkbook = workbook;
		SimpleDateFormat formatter = new SimpleDateFormat("ddMMyyyyHHmmss");
		String filename = "kupass_backup_" + formatter.format(new Date()) + ".xlsx";
		Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
		intent.addCategory(Intent.CATEGORY_OPENABLE);
		intent.setType("text/*");
		intent.putExtra(Intent.EXTRA_TITLE, filename);
		mActivity.startActivityForResult(intent, REQUEST_ACTION_XLSX);
	}
	**/
	
	public static boolean create(Intent intent) {
		Uri uri = intent.getData();
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
	
	public static boolean createXlsx(Intent intent) {
		/** Not Support in CodeAssist
		Uri uri = intent.getData();
		if(uri == null)
			return false;
		try {
			// Convert Uri to DocumentFile
			DocumentFile documentFile = DocumentFile.fromSingleUri(mContext, uri);
			mWorkbook.write(new FileOutputStream(new File(documentFile.getUri().getPath())));
		} catch(IOException e) {
			e.printStackTrace();
			return false;
		}
		**/
		return true;
	}
}