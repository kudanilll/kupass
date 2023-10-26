package com.achmaddaniel.kupass.core;

import com.achmaddaniel.kupass.adapter.ListItem;

import java.util.ArrayList;

public class Password {
	
	private String mPasswordName;
	private String mUserName;
	private String mPassword;
	private String mNote;
	
	public Password(ListItem item) {
		mPasswordName = item.getPasswordName();
		mUserName = item.getUserName();
		mPassword = item.getPassword();
		mNote = item.getNote();
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
}