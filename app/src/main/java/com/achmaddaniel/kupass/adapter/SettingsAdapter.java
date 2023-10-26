package com.achmaddaniel.kupass.adapter;

import com.achmaddaniel.kupass.R;

import android.content.Context;

import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.view.View;

import android.widget.BaseAdapter;
import android.widget.TextView;

import java.util.ArrayList;

public class SettingsAdapter extends BaseAdapter {
	
	private ArrayList<ListSettings> mList = new ArrayList<>();
	private Context mContext;
	
	public SettingsAdapter(Context context, ArrayList<ListSettings> list) {
		mContext = context;
		mList = list;
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
		convertView = LayoutInflater.from(mContext).inflate(R.layout.settings_item_list_layout, parent, false);
		((TextView)convertView.findViewById(R.id.title)).setText(mList.get(position).getTitle());
		((TextView)convertView.findViewById(R.id.subtitle)).setText(mList.get(position).getSubtitle());
		return convertView;
	}
}