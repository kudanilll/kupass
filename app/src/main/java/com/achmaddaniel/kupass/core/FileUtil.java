package com.achmaddaniel.kupass.core;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.annotations.SerializedName;

/** Depedencies not support in CodeAssist
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
**/

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.ArrayList;

public class FileUtil {
	
	private ArrayList<Password> mListPassword;
	
	public FileUtil() {
		mListPassword = new ArrayList<>();
	}
	
	public FileUtil(ArrayList<Password> listPassword) {
		mListPassword = listPassword;
	}
	
	public void generateToCsvFile() {
	}
	
	public void generateToJsonFile() {
		Gson gson = new GsonBuilder().setPrettyPrinting().create();
		ArrayList<PasswordJSON> json = new ArrayList<>();
		for(Password password : mListPassword)
			json.add(convertToJSON(password));
		ScoopedStorage.createFile(gson.toJson(json), ConstantVar.EXPORT_JSON);
	}
	
	public void generateToTextFile() {
		StringBuilder sb = new StringBuilder();
		for(Password password : mListPassword)
			sb.append(password.toString());
		ScoopedStorage.createFile(sb.toString(), ConstantVar.EXPORT_TEXT);
	}
	
	public void generateToXlsxFile() {
		/** Not Support in CodeAssist
		Gson gson = new GsonBuilder().setPrettyPrinting().create();
		ArrayList<PasswordJSON> json = new ArrayList<>();
		for(Password password : mListPassword)
			json.add(convertToJSON(password));
		
		JSONArray jsonArray = new JSONArray(gson.toJson(json));
		Workbook workbook = new XSSFWorkbook();
		Sheet sheet = workbook.createSheet("Password");
		Row headerRow = sheet.createRow(0);
		
		String[] headers = {
			"password_name",
			"username",
			"password",
			"note"
		};
		
		for(int i = 0; i < headers.length; i++) {
			Cell cell = headerRow.createCell(i);
			cell.setCellValue(headers[i]);
		}
		
		// Write to Excel
		for(int i = 0; i < jsonArray.length(); i++) {
			JSONObject jsonObject = jsonArray.getJSONObject(i);
			Row dataRow = sheet.createRow(i++);
			for(int j = 0; j < headers.length; j++)
				dataRow.createCell(j).setCellValue(jsonObject.getString(headers[j]));
		}
		ScoopedStorage.createFileXlsx(workbook);
		**/
	}
	
	private PasswordJSON convertToJSON(Password password) {
		PasswordJSON result = new PasswordJSON(password.getPasswordName());
		result.add("username", password.getUserName());
		result.add("password", password.getPassword());
		result.add("note", password.getNote());
		return result;
	}
	
	private class PasswordJSON {
		
		@SerializedName("properties")
		private Map<String, String> mProperties;
		
		@SerializedName("password_name")
		private String mPasswordName;
		
		public PasswordJSON(String name) {
			mProperties = new LinkedHashMap<>();
			mPasswordName = name;
		}
		
		public void add(String key, String value) {
			mProperties.put(key, value);
		}
	}
}