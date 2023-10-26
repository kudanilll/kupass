package com.achmaddaniel.kupass.adapter;

public class ListSettings {
	
	private String mTitle;
	private String mSubtitle;
	
	public ListSettings(String title, String subtitle) {
		mTitle = title;
		mSubtitle = subtitle;
	}
	
	public String getTitle() {
		return mTitle;
	}
	
	public String getSubtitle() {
		return mSubtitle;
	}
}