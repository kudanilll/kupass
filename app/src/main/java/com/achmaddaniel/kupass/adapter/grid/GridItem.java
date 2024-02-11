package com.achmaddaniel.kupass.adapter.grid;

public class GridItem {
	
	private int mIcon;
	private String mText;
	
	public GridItem(int icon, String text) {
		mIcon = icon;
		mText = text;
	}
	
	public int getIcon() {
		return mIcon;
	}
	
	public String getText() {
		return mText;
	}
}