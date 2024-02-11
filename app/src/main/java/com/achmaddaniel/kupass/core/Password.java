package com.achmaddaniel.kupass.core;

import com.achmaddaniel.kupass.adapter.list.ListItem;

import com.google.gson.annotations.SerializedName;

import java.util.LinkedHashMap;
import java.util.Map;

public class Password {
	
	@SerializedName("site")
	private String mPasswordName;
	
	@SerializedName("username")
	private String mUserName;
	
	@SerializedName("password")
	private String mPassword;
	
	@SerializedName("note")
	private String mNote;
	
	public Password(ListItem item) {
		mPasswordName = item.getPasswordName();
		mUserName = item.getUserName();
		mPassword = item.getPassword();
		mNote = item.getNote();
	}
	
	public Password(String passwordName, String username, String password, String note) {
		mPasswordName = passwordName;
		mUserName = username;
		mPassword = password;
		mNote = note;
	}
	
	public String getPasswordName() {
		return mPasswordName;
	}
	
	public String getUserName() {
		return mUserName;
	}
	
	public String getPassword() {
		return mPassword;
	}
	
	public String getNote() {
		return mNote;
	}
	
	public String toString() {
		return mPasswordName + ":\n" +
			   " - username: " + mUserName + "\n" +
			   " - password: " + mPassword + "\n" +
			   " - note: " + mNote + "\n\n";
	}
	
	public JSON toJSON() {
		JSON result = new JSON(this);
		return result;
	}
	
	public class JSON {
		
		@SerializedName("properties")
		private Map<String, String> mProperties;
		
		@SerializedName("site")
		private String mPasswordName;
	
		public JSON(Password password) {
			mProperties = new LinkedHashMap<>();
			mPasswordName = password.getPasswordName();
			mProperties.put("username", password.getUserName());
			mProperties.put("password", password.getPassword());
			mProperties.put("note", password.getNote());
		}
	}
}