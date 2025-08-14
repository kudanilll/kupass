package com.nielcode.kupass.ui.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import com.nielcode.kupass.databinding.ItemListLayoutBinding;
import java.util.ArrayList;

public class ListPasswordAdapter extends BaseAdapter {

  private final Context context;
  private ItemListLayoutBinding binding;
  private ArrayList<ListPasswordItem> list;

  public ListPasswordAdapter(Context context, ArrayList<ListPasswordItem> list) {
    this.context = context;
    this.list = list;
  }

  public ArrayList<ListPasswordItem> getList() {
    return this.list;
  }

  public void setList(ArrayList<ListPasswordItem> list) {
    this.list = list;
    notifyDataSetChanged();
  }

  public ListPasswordItem getCurrentItem(int position) {
    return this.list.get(position);
  }

  @Override
  public int getCount() {
    return this.list.size();
  }

  @Override
  public Object getItem(int position) {
    return this.list.get(position);
  }

  @Override
  public long getItemId(int position) {
    return position;
  }

  @Override
  public View getView(int position, View convertView, ViewGroup parent) {
    if (convertView == null) {
      this.binding =
          ItemListLayoutBinding.inflate(LayoutInflater.from(this.context), parent, false);
      convertView = this.binding.getRoot();
    }
    this.binding.itemTitle.setText(getCurrentItem(position).getPasswordName());
    this.binding.itemSubtitle.setText(getCurrentItem(position).getNote());
    return convertView;
  }
}
