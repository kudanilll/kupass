package com.achmaddaniel.kupass.services.writer;

import com.achmaddaniel.kupass.core.ConstantVar;
import com.achmaddaniel.kupass.core.Password;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.util.ArrayList;

public class Writer {
	
	private ArrayList<Password> mListPassword;
	
	public Writer() {
		mListPassword = new ArrayList<>();
	}
	
	public Writer(ArrayList<Password> listPassword) {
		mListPassword = listPassword;
	}
	
	public void toCsvFile() {
	}
	
	public void toJsonFile() {
		Gson gson = new GsonBuilder().setPrettyPrinting().create();
		ArrayList<Password.JSON> json = new ArrayList<>();
		for(Password password : mListPassword)
			json.add(password.toJSON());
		ScoopedStorage.createFile(gson.toJson(json), ConstantVar.EXPORT_JSON);
	}
	
	public void toTextFile() {
		StringBuilder sb = new StringBuilder();
		for(Password password : mListPassword)
			sb.append(password.toString());
		ScoopedStorage.createFile(sb.toString(), ConstantVar.EXPORT_TEXT);
	}
}