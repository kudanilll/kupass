package com.nielcode.kupass.model;

import androidx.annotation.NonNull;

import com.google.gson.annotations.SerializedName;

import java.util.HashMap;
import java.util.Map;

public class Credentials {

	@SerializedName("site")
	private String siteName;

	@SerializedName("username")
	private String username;

	@SerializedName("password")
	private String password;

	@SerializedName("note")
	private String note;

	public Credentials(String siteName, String username, String password, String note) {
		this.siteName = siteName;
		this.username = username;
		this.password = password;
		this.note = note;
	}

	public String getSiteName() {
		return siteName;
	}

	public String getUsername() {
		return username;
	}

	public String getPassword() {
		return password;
	}

	public String getNote() {
		return note;
	}

	@NonNull
	@Override
	public String toString() {
		return siteName + ":\n" +
				" - Username: " + username + "\n" +
				" - Password: " + password + "\n" +
				" - Note: " + note + "\n\n";
	}

	public CredentialsJSON toJSON() {
		return new CredentialsJSON(this);
	}
}

class CredentialsJSON {

	@SerializedName("properties")
	private final Map<String, String> properties;

	@SerializedName("site")
	private final String siteName;

	public CredentialsJSON(Credentials credentials) {
		properties = new HashMap<>();
		siteName = credentials.getSiteName();
		properties.put("username", credentials.getUsername());
		properties.put("password", credentials.getPassword());
		properties.put("note", credentials.getNote());
	}
}
