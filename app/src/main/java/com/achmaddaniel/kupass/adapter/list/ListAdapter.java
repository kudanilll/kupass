package com.achmaddaniel.kupass.adapter.list;

import com.achmaddaniel.kupass.R;
import com.achmaddaniel.kupass.databinding.ItemListLayoutBinding;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.view.View;
import android.widget.BaseAdapter;
import android.widget.TextView;

import java.util.ArrayList;

public class ListAdapter extends BaseAdapter {
	
	private ItemListLayoutBinding mBinding;
	private Context mContext;
	private ArrayList<ListItem> mList;
	
	public ListAdapter(Context context, ArrayList<ListItem> list) {
		mContext = context;
		mList = list;
	}
	
	public void setList(ArrayList<ListItem> list) {
		mList = list;
		notifyDataSetChanged();
	}
	
	public ArrayList<ListItem> getList() {
		return mList;
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
		if(convertView == null) {
			mBinding = ItemListLayoutBinding.inflate(LayoutInflater.from(mContext), parent, false);
			convertView = mBinding.getRoot();
		}
		mBinding.itemTitle.setText(getCurrentItem(position).getPasswordName());
		mBinding.itemSubtitle.setText(getCurrentItem(position).getNote());
		return convertView;
	}
}