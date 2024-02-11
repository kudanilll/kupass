package com.achmaddaniel.kupass.adapter.list;

import com.achmaddaniel.kupass.core.ConstantVar;

import java.io.Serializable;

public class ListItem implements Serializable {
	
	private long mId = 0;
	private String mPasswordName = ConstantVar.STRING_EMPTY;
	private String mUserName = ConstantVar.STRING_EMPTY;
	private String mPassword = ConstantVar.STRING_EMPTY;
	private String mNote = ConstantVar.STRING_EMPTY;
	
	public ListItem(long id, String username, String password) {
		this(id, "Password", username, password);
	}
	
	public ListItem(long id, String passwordName, String username, String password) {
		this(id, passwordName, username, password, ConstantVar.STRING_EMPTY);
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
}