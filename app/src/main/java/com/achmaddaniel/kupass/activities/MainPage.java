package com.achmaddaniel.kupass.activities;

import com.achmaddaniel.kupass.R;
import com.achmaddaniel.kupass.fragments.AccessibilityFragment;
import com.achmaddaniel.kupass.fragments.HomeFragment;
import com.achmaddaniel.kupass.services.permissions.FilePermissionHandler;
import com.achmaddaniel.kupass.databinding.ActivityMainPageBinding;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.net.Uri;

public class MainPage extends AppCompatActivity {
	
	private ActivityMainPageBinding mBinding;
	private FragmentManager mFragmentManager;
	private FilePermissionHandler mPermissionHandler;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		mBinding = ActivityMainPageBinding.inflate(getLayoutInflater());
		setContentView(mBinding.getRoot());
		
		checkPermission();
		mFragmentManager = getSupportFragmentManager();
		
		mBinding.bottomNavigation.setOnItemSelectedListener((item) -> {
			final Fragment fragment = mFragmentManager.findFragmentById(mBinding.frameContainer.getId());
			if(item.getItemId() == R.id.menu_home) {
				if(fragment instanceof AccessibilityFragment) {
					showFragment(new HomeFragment());
					return true;
				}
			} else if(item.getItemId() == R.id.menu_accessibility) {
				if(fragment instanceof HomeFragment) {
					showFragment(new AccessibilityFragment());
					return true;
				}
			}
			return false;
		});
		
		if(savedInstanceState == null) {
			mBinding.bottomNavigation.setSelectedItemId(R.id.menu_home);
			showFragment(new HomeFragment());
		}
	}
	
	@Override
	protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		mPermissionHandler.onActivityResult(requestCode, resultCode, data);
	}
	
	@Override
	protected void onDestroy() {
		super.onDestroy();
		mBinding = null;
	}
	
	private void showFragment(Fragment fragment) {
		final FragmentTransaction fragmentTransaction = mFragmentManager.beginTransaction();
		final Fragment showFragment = mFragmentManager.findFragmentById(mBinding.frameContainer.getId());
		if(showFragment == null) {
			fragmentTransaction.add(mBinding.frameContainer.getId(), fragment);
			fragmentTransaction.commit();
		} else {
			fragmentTransaction.replace(mBinding.frameContainer.getId(), fragment);
			fragmentTransaction.commit();
		}
	}
	
	public static void createFile(Intent intent) {
	}
	
	private void checkPermission() {
		mPermissionHandler = new FilePermissionHandler(MainPage.this, getApplicationContext());
		if(!mPermissionHandler.checkReadWritePermission()) {
			// Permission not yet granted, request permission.
			new MaterialAlertDialogBuilder(MainPage.this)
				.setTitle(getString(R.string.dialog_title_permission))
				.setMessage(getString(R.string.dialog_message_permission))
				.setPositiveButton(getString(R.string.dialog_request), (dialog, which) -> {
					mPermissionHandler.requestReadWritePermission();
				})
				.setCancelable(false)
				.create()
				.show();
		}
	}
}