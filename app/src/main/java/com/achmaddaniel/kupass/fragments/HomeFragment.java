package com.achmaddaniel.kupass.fragments;

import com.achmaddaniel.kupass.R;
import com.achmaddaniel.kupass.activities.PasswordPage;
import com.achmaddaniel.kupass.adapter.list.ListAdapter;
import com.achmaddaniel.kupass.adapter.list.ListItem;
import com.achmaddaniel.kupass.core.Password;
import com.achmaddaniel.kupass.database.Pref;
import com.achmaddaniel.kupass.database.SQLDataHelper;
import com.achmaddaniel.kupass.databinding.FragmentHomePageBinding;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton;
//import com.google.android.material.transition.MaterialSharedAxis;

import androidx.appcompat.widget.SearchView;
import androidx.fragment.app.Fragment;

import android.os.Bundle;
import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.view.View;
import android.widget.Toast;

import java.util.ArrayList;
import java.io.Serializable;

public class HomeFragment extends Fragment {
	
	private static FragmentHomePageBinding mBinding;
	private static SQLDataHelper mSQL;
	private static ListAdapter mAdapter;
	private static ArrayList<ListItem> mListItem;
	private static Context mContext;
	
	public HomeFragment() {
		// Empty Constructors
	}
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		//setEnterTransition(new MaterialSharedAxis(MaterialSharedAxis.X, false));
		//setExitTransition(new MaterialSharedAxis(MaterialSharedAxis.X, true));
	}
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		mBinding = FragmentHomePageBinding.inflate(inflater, container, false);
		return mBinding.getRoot();
	}
	
	@Override
	public void onViewCreated(View view, Bundle savedInstanceState) {
		super.onViewCreated(view, savedInstanceState);
		
		mContext  = getActivity();
		mListItem = new ArrayList<>();
		mSQL = new SQLDataHelper(mContext);
		
		mBinding.searchPassword.clearFocus();
		mBinding.searchPassword.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
			@Override
			public boolean onQueryTextSubmit(String query) {
				return false;
			}
			@Override
			public boolean onQueryTextChange(String newText) {
				ArrayList<ListItem> filter = new ArrayList<>();
				for(ListItem item : mListItem)
					if(item.getPasswordName().toLowerCase().contains(newText.toLowerCase()))
						filter.add(item);
				if(!filter.isEmpty())
					mAdapter.setList(filter);
				return true;
			}
		});
		
		// FAB Handle
		mBinding.fab.extend();
		mBinding.fab.setOnClickListener((v) -> {
			mContext.startActivity(new Intent(mContext, PasswordPage.class));
		});
		
		update();
	}
	
	public static void add(Password password) {
		mSQL.create(
			password.getPasswordName(),
			password.getUserName(),
			password.getPassword(),
			password.getNote()
		);
		update();
	}
	
	public static void add(String site, String username, String password, String note) {
		mSQL.create(site, username, password, note);
		update();
	}
	
	public static void edit(long id, String site, String username, String password, String note) {
		mSQL.update(id, site, username, password, note);
		update();
	}
	
	private static void update() {
		mListItem = mSQL.getAll();
		mAdapter = new ListAdapter(mContext, mListItem);
		
		mBinding.listPassword.setAdapter(mAdapter);
		mBinding.listPassword.setOnItemClickListener((adapter, view, position, id) -> {
			Intent intent = new Intent(mContext, PasswordPage.class);
			intent.putExtra("list", (Serializable)mListItem.get(position));
			mContext.startActivity(intent);
		});
		mBinding.listPassword.setOnItemLongClickListener((adapter, view, position, id) -> {
			new MaterialAlertDialogBuilder(mContext)
				.setTitle(mContext.getString(R.string.dialog_title_delete))
				.setMessage(mContext.getString(R.string.dialog_message_delete))
				.setNegativeButton(mContext.getString(R.string.dialog_cancel), (dialog, which) -> {
				})
				.setPositiveButton(mContext.getString(R.string.dialog_delete), (dialog, which) -> {
					mSQL.delete(mListItem.get(position).getId());
					Toast.makeText(mContext, mContext.getString(R.string.toast_success_delete), Toast.LENGTH_SHORT).show();
					update();
				})
				.create()
				.show();
			return true;
		});
	}
}