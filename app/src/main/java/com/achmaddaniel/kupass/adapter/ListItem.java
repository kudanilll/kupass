package com.achmaddaniel.kupass.adapter;

import java.io.Serializable;

public class ListItem implements Serializable {
	
	private long mId;
	private String mPasswordName;
	private String mUserName;
	private String mPassword;
	private String mNote;
	
	public ListItem(long id, String username, String password) {
		this(id, "Password", username, password);
	}
	
	public ListItem(long id, String passwordName, String username, String password) {
		this(id, passwordName, username, password, "");
	}
	
	public ListItem(long id, String passwordName, String username, String password, String note) {
		mId = id;
		mPasswordName = passwordName;
		mUserName = username;
		mPassword = password;
		mNote = note;
	}
	
	public long getId() {
		return mId;
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
	
	public void setPasswordName(String passwordName) {
		mPasswordName = passwordName;
	}
	
	public void setUserName(String username) {
		mUserName = username;
	}
	
	public void setPassword(String password) {
		mPassword = password;
	}
	
	public void setNote(String note) {
		mNote = note;
	}
}