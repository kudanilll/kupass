package com.achmaddaniel.kupass.activity;

import com.achmaddaniel.kupass.R;
import com.achmaddaniel.kupass.adapter.ListAdapter;
import com.achmaddaniel.kupass.adapter.ListItem;
import com.achmaddaniel.kupass.database.Preference;
import com.achmaddaniel.kupass.database.SQLDataHelper;
import com.achmaddaniel.kupass.core.ConstantVar;
import com.achmaddaniel.kupass.core.FilePermissionHandler;
import com.achmaddaniel.kupass.core.FileUtil;
import com.achmaddaniel.kupass.core.Password;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.appcompat.widget.SearchView;
import androidx.core.content.ContextCompat;
import androidx.annotation.Nullable;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.TextInputEditText;

import android.os.Bundle;

import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.content.res.Resources;

import android.view.View;
import android.view.LayoutInflater;
import android.view.animation.AnimationUtils;

import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.Locale;
import java.util.ArrayList;

public class HomePage extends AppCompatActivity {
	
	// Initialize widget variable
	private SearchView mSearchView;
	private FloatingActionButton mFab;
	private FloatingActionButton mCreatePassFab;
	private FloatingActionButton mExportFab;
	private FloatingActionButton mSettingsFab;
	
	private FilePermissionHandler mPermissionHandler;
	private boolean mIsFabClicked = false;
	private ListAdapter mAdapter;
	private ArrayList<ListItem> mListItem;
	private SQLDataHelper mSQL;
	private ListView mListView;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_home_page);
		checkPermission();
		initialisation();
	}
	
	@Override
	protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		mPermissionHandler.onActivityResult(requestCode, resultCode, data);
	}
	
	private void initialisation() {
		
		Preference.init(this);
		mSQL = new SQLDataHelper(this);
		mAdapter  = new ListAdapter(this);
		mListItem = new ArrayList<>();
		mListView = findViewById(R.id.list_view);
		
		// Init language
		Resources res = getResources();
		Configuration config = res.getConfiguration();
		switch(Preference.getLanguage()) {
		case ConstantVar.LANG_EN:
			config.setLocale(Locale.getDefault());
			break;
		case ConstantVar.LANG_IN:
			config.setLocale(new Locale("in"));
			break;
		}
		res.updateConfiguration(config, res.getDisplayMetrics());
		
		// Theme
		switch(Preference.getTheme()) {
		case ConstantVar.THEME_SYSTEM:
			AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
			break;
		case ConstantVar.THEME_LIGHT:
			AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
			break;
		case ConstantVar.THEME_DARK:
			AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
			break;
		}
		
		// FAB Initialisation
		mFab = findViewById(R.id.fab);
		mCreatePassFab = findViewById(R.id.fab_add);
		mExportFab = findViewById(R.id.fab_export);
		mSettingsFab = findViewById(R.id.fab_settings);
		fabHandle();
		setFabVisibility(false);
		
		// Search function
		mSearchView = findViewById(R.id.search_view);
		mSearchView.clearFocus();
		mSearchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
			@Override
			public boolean onQueryTextSubmit(String query) {
				return false;
			}
			@Override
			public boolean onQueryTextChange(String newText) {
				if(mIsFabClicked) {
					mIsFabClicked = !mIsFabClicked;
					setFabVisibility(mIsFabClicked);
				}
				ArrayList<ListItem> filter = new ArrayList<>();
				for(ListItem item : mListItem)
					if(item.getPasswordName().toLowerCase().contains(newText.toLowerCase()))
						filter.add(item);
				if(!filter.isEmpty())
					mAdapter.setFilteredList(filter);
				return true;
			}
		});
		update();
	}
	
	private void update() {
		mListItem = mSQL.getAll();
		mAdapter.setList(mListItem);
		
		if(!(mListItem.size() > 0))
			mListView.setBackgroundColor(getResources().getColor(R.color.transparent));
		else
			mListView.setBackgroundResource(R.drawable.list_item_background);
		
		mListView.setAdapter(mAdapter);
		mListView.setOnItemClickListener((adapter, view, position, id) -> {
			ListItem currentItem = mAdapter.getCurrentItem(position);
			final View inflater = LayoutInflater.from(HomePage.this).inflate(R.layout.dialog_layout, null);
			((TextInputEditText)inflater.findViewById(R.id.password_name)).setText(currentItem.getPasswordName());
			((TextInputEditText)inflater.findViewById(R.id.username)).setText(currentItem.getUserName());
			((TextInputEditText)inflater.findViewById(R.id.password)).setText(currentItem.getPassword());
			((TextInputEditText)inflater.findViewById(R.id.note)).setText(currentItem.getNote());
			new MaterialAlertDialogBuilder(HomePage.this)
				.setTitle(getString(R.string.dialog_title_edit))
				.setNegativeButton(getString(R.string.dialog_cancel), (dialog, which) -> {
				})
				.setPositiveButton(getString(R.string.dialog_save), (dialog, which) -> {
					final String passwordName = ((TextInputEditText)inflater.findViewById(R.id.password_name)).getText().toString();
					final String username = ((TextInputEditText)inflater.findViewById(R.id.username)).getText().toString();
					final String password = ((TextInputEditText)inflater.findViewById(R.id.password)).getText().toString();
					final String note = ((TextInputEditText)inflater.findViewById(R.id.note)).getText().toString();
					mSQL.update(currentItem.getId(), passwordName, username, password, note);
					showToast(getString(R.string.toast_success_update));
					if(mIsFabClicked) {
						mIsFabClicked = !mIsFabClicked;
						setFabVisibility(mIsFabClicked);
					}
					update();
				})
				.setView(inflater)
				.create()
				.show();
		});
		mListView.setOnItemLongClickListener((adapter, view, position, id) -> {
			new MaterialAlertDialogBuilder(HomePage.this)
				.setTitle(getString(R.string.dialog_title_delete))
				.setMessage(getString(R.string.dialog_message_delete))
				.setNegativeButton(getString(R.string.dialog_cancel), (dialog, which) -> {
				})
				.setPositiveButton(getString(R.string.dialog_delete), (dialog, which) -> {
					mSQL.delete(mListItem.get(position).getId());
					showToast(getString(R.string.toast_success_delete));
					update();
				})
				.create()
				.show();
			return true;
		});
	}
	
	// Handle FAB onClickListener
	private void fabHandle() {
		
		mFab.setOnClickListener((view) -> {
			mIsFabClicked = !mIsFabClicked;
			setFabVisibility(mIsFabClicked);
		});
		
		mCreatePassFab.setOnClickListener((view) -> {
			final View inflater = LayoutInflater.from(HomePage.this).inflate(R.layout.dialog_layout, null);
			new MaterialAlertDialogBuilder(HomePage.this)
				.setTitle(getString(R.string.dialog_title_create))
				.setNegativeButton(getString(R.string.dialog_cancel), (dialog, which) -> {
				})
				.setPositiveButton(getString(R.string.dialog_create), (dialog, which) -> {
					final String passwordName = ((TextInputEditText)inflater.findViewById(R.id.password_name)).getText().toString();
					final String username = ((TextInputEditText)inflater.findViewById(R.id.username)).getText().toString();
					final String password = ((TextInputEditText)inflater.findViewById(R.id.password)).getText().toString();
					final String note = ((TextInputEditText)inflater.findViewById(R.id.note)).getText().toString();
					mSQL.create(passwordName, username, password, note);
					showToast(getString(R.string.toast_success));
					if(mIsFabClicked) {
						mIsFabClicked = !mIsFabClicked;
						setFabVisibility(mIsFabClicked);
					}
					update();
				})
				.setView(inflater)
				.create()
				.show();
		});
		
		// Export
		mExportFab.setOnClickListener((view) -> {
			if(!(mListItem.size() > 0)) {
				setFabVisibility(false);
				showToast(getString(R.string.toast_no_data));
				return;
			}
			CharSequence[] listExport = getResources().getStringArray(R.array.export_method_list);
			new MaterialAlertDialogBuilder(HomePage.this)
				.setTitle(getString(R.string.dialog_title_export))
				.setNegativeButton(getString(R.string.dialog_cancel), (dialog, which) -> {
				})
				.setPositiveButton(getString(R.string.dialog_export), (dialog, which) -> {
					checkPermission();
					if(mPermissionHandler.checkReadWritePermission()) {
						ArrayList<Password> listPassword = new ArrayList<>();
						for(ListItem item : mListItem)
							listPassword.add(new Password(item));
						FileUtil data = new FileUtil(listPassword);
						switch(Preference.getExportMethod()) {
						/**
						case ConstantVar.EXPORT_CSV:
							data.generateToCsvFile();
							break;
						**/
						case ConstantVar.EXPORT_JSON:
							data.generateToJsonFile();
							break;
						case ConstantVar.EXPORT_TEXT:
							data.generateToTextFile();
							break;
						/** Depedencies not Support in CodeAssist
						case ConstantVar.EXPORT_XLSX:
							data.generateToXlsxFile();
							break;
						**/
						}
					}
					if(mIsFabClicked) {
						mIsFabClicked = !mIsFabClicked;
						setFabVisibility(mIsFabClicked);
					}
				})
				.setSingleChoiceItems(listExport, Preference.getExportMethod(), (dialog, which) -> {
					Preference.setExportMethod(which);
				})
				.create()
				.show();
		});
		
		mSettingsFab.setOnClickListener((view) -> {
			startActivity(new Intent(HomePage.this, SettingsPage.class));
		});
	}
	
	private void setFabVisibility(boolean visible) {
		mIsFabClicked = visible;
		if(visible) {
			mCreatePassFab.startAnimation(AnimationUtils.loadAnimation(HomePage.this, R.anim.fade_in));
			mExportFab.startAnimation(AnimationUtils.loadAnimation(HomePage.this, R.anim.fade_in));
			mSettingsFab.startAnimation(AnimationUtils.loadAnimation(HomePage.this, R.anim.fade_in));
			mFab.setImageDrawable(ContextCompat.getDrawable(HomePage.this, R.drawable.ic_minus));
		} else {
			mCreatePassFab.startAnimation(AnimationUtils.loadAnimation(HomePage.this, R.anim.fade_out));
			mExportFab.startAnimation(AnimationUtils.loadAnimation(HomePage.this, R.anim.fade_out));
			mSettingsFab.startAnimation(AnimationUtils.loadAnimation(HomePage.this, R.anim.fade_out));
			mFab.setImageDrawable(ContextCompat.getDrawable(HomePage.this, R.drawable.ic_plus));
		}
		mCreatePassFab.setVisibility(visible ? View.VISIBLE : View.INVISIBLE);
		mExportFab.setVisibility(visible ? View.VISIBLE : View.INVISIBLE);
		mSettingsFab.setVisibility(visible ? View.VISIBLE : View.INVISIBLE);
	}
	
	private void checkPermission() {
		mPermissionHandler = new FilePermissionHandler(HomePage.this, getApplicationContext());
		if(!mPermissionHandler.checkReadWritePermission()) {
			// Permission not yet granted, request permission.
			new MaterialAlertDialogBuilder(HomePage.this)
				.setTitle(getString(R.string.dialog_title_permission))
				.setMessage(getString(R.string.dialog_message_permission))
				.setNegativeButton(getString(R.string.dialog_cancel), (dialog, which) -> {
				})
				.setPositiveButton(getString(R.string.dialog_request), (dialog, which) -> {
					mPermissionHandler.requestReadWritePermission();
				})
				.setCancelable(false)
				.create()
				.show();
		}
	}
	
	private void showToast(String message) {
		Toast.makeText(HomePage.this, message, Toast.LENGTH_SHORT).show();
	}
}