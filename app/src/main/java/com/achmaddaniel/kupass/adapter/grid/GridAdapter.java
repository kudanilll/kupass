package com.achmaddaniel.kupass.adapter.grid;

import com.achmaddaniel.kupass.R;
import com.achmaddaniel.kupass.databinding.ItemGridLayoutBinding;

import com.google.android.material.card.MaterialCardView;
import androidx.recyclerview.widget.RecyclerView;

import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.ArrayList;

public class GridAdapter extends RecyclerView.Adapter<GridAdapter.ViewHolder> {
	
	private ArrayList<GridItem> mList;
	private OnItemClickListener mListener;
	
	public GridAdapter(ArrayList<GridItem> list) {
		mList = list;
	}
	
	public void setOnItemClickListener(OnItemClickListener listener) {
		mListener = listener;
	}
	
	@Override
	public int getItemCount() {
		return mList.size();
	}
	
	@Override
	public ViewHolder onCreateViewHolder(ViewGroup viewGroup, int viewType) {
		return new ViewHolder(ItemGridLayoutBinding.inflate(LayoutInflater.from(viewGroup.getContext()), viewGroup, false).getRoot());
	}
	
	@Override
	public void onBindViewHolder(ViewHolder viewHolder, final int position) {
		GridItem currentItem = mList.get(position);
		viewHolder.getIconView().setImageResource(currentItem.getIcon());
		viewHolder.getTextView().setText(currentItem.getText());
		viewHolder.getView().setOnClickListener((view) -> {
			if(mListener != null)
				mListener.onClick(position, currentItem);
		});
	}
	
	public interface OnItemClickListener {
		public void onClick(int position, GridItem currentItem);
	}
	
	public static class ViewHolder extends RecyclerView.ViewHolder {
		
		private final MaterialCardView mContainer;
		private final ImageView mIconView;
		private final TextView mTextView;
		
		public ViewHolder(View view) {
			super(view);
			mContainer = view.findViewById(R.id.container);
			mIconView = view.findViewById(R.id.itemIcon);
			mTextView = view.findViewById(R.id.itemText);
		}
		
		public MaterialCardView getView() {
			return mContainer;
		}
		
		public ImageView getIconView() {
			return mIconView;
		}
		
		public TextView getTextView() {
			return mTextView;
		}
	}
}