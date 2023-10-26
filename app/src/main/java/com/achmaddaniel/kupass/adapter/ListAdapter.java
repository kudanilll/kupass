package com.achmaddaniel.kupass.adapter;

import com.achmaddaniel.kupass.R;

import android.content.Context;

import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.view.View;

import android.widget.BaseAdapter;
import android.widget.TextView;

import java.util.ArrayList;

public class ListAdapter extends BaseAdapter {
	
	private ArrayList<ListItem> mList;
	private Context mContext;
	
	public ListAdapter(Context context) {
		mContext = context;
	}
	
	public void setFilteredList(ArrayList<ListItem> list) {
		mList = list;
		notifyDataSetChanged();
	}
	
	public void setList(ArrayList<ListItem> list) {
		mList = list;
	}
	
	public ArrayList<ListItem> getList() {
		return mList;
	}
	
	public String getItemTitle(int position) {
		ListItem item = mList.get(position);
		return item.getPasswordName();
	}
	
	public String getItemSubtitle(int position) {
		ListItem item = mList.get(position);
		return item.getNote();
	}
	
	public ListItem getCurrentItem(int position) {
		return mList.get(position);
	}
	
	@Override
	public int getCount() {
		return mList.size();
	}
	
	@Override
	public Object getItem(int position) {
		return mList.get(position);
	}
	
	@Override
	public long getItemId(int position) {
		return position;
	}
	
	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		convertView = LayoutInflater.from(mContext).inflate(R.layout.item_list_layout, parent, false);
		((TextView)convertView.findViewById(R.id.item_title)).setText(getItemTitle(position));
		((TextView)convertView.findViewById(R.id.item_subtitle)).setText(getItemSubtitle(position));
		return convertView;
	}
}