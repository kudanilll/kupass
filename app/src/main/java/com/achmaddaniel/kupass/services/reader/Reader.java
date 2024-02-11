package com.achmaddaniel.kupass.services.reader;

import com.achmaddaniel.kupass.core.Password;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.reflect.Type;
import java.util.ArrayList;

public class Reader {
	
	public static final int REQUEST_ACTION_READ_FILE = 99;
	private Activity mActivity;
	
	public Reader(Activity activity) {
		mActivity = activity;
	}
	
	public void pickJsonFile() {
		Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
		intent.setType("*/*");
		intent.addCategory(Intent.CATEGORY_OPENABLE);
		mActivity.startActivityForResult(Intent.createChooser(intent, "Select JSON File"), REQUEST_ACTION_READ_FILE);
	}
	
	public ArrayList<Password> getDataFromJson(Uri uri) throws IOException {
		ArrayList<Password> result = new ArrayList<>();
		ArrayList<DataModel> dataList = getData(uri);
		for(DataModel data : dataList)
			result.add(new Password(
				data.getSite(),
				data.getProperties().getUsername(),
				data.getProperties().getPassword(),
				data.getProperties().getNote()
			));
		return result;
	}
	
	private ArrayList<DataModel> getData(Uri uri) throws IOException {
		ArrayList<DataModel> dataList = null;
		Gson gson = new Gson();
		FileInputStream fis = new FileInputStream(mActivity.getContentResolver().openFileDescriptor(uri, "r").getFileDescriptor());
		InputStreamReader isr = new InputStreamReader(fis);
		BufferedReader bufferedReader = new BufferedReader(isr);
		StringBuilder sb = new StringBuilder();
		String line;
		while((line = bufferedReader.readLine()) != null)
			sb.append(line);
		String json = sb.toString();
		Type listType = new TypeToken<ArrayList<DataModel>>(){}.getType();
		dataList = gson.fromJson(json, listType);
		fis.close();
		return dataList;
	}
	
	private class DataModel {
		private String site;
		private Properties properties;
		
		public String getSite() {
			return site;
		}
		
		public Properties getProperties() {
			return properties;
		}
		
		private class Properties {
			private String username;
			private String password;
			private String note;
			
			public String getUsername() {
				return username;
			}
			
			public String getPassword() {
				return password;
			}
			
			public String getNote() {
				return note;
			}
		}
	}
}