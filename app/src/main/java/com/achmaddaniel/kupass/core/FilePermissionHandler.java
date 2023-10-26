package com.achmaddaniel.kupass.core;

import com.achmaddaniel.kupass.R;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.app.Activity;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;

import android.Manifest;

import android.widget.Toast;

public class FilePermissionHandler {
	
	private static final int READ_WRITE_PERMISSION_REQUEST_CODE = 100;
	private Activity mActivity;
	
	public FilePermissionHandler(Activity activity, Context context) {
		mActivity = activity;
		ScoopedStorage.init(mActivity, context);
	}
	
	public boolean checkReadWritePermission() {
		int readPermission	= ContextCompat.checkSelfPermission(mActivity, Manifest.permission.READ_EXTERNAL_STORAGE);
		int writePermission = ContextCompat.checkSelfPermission(mActivity, Manifest.permission.WRITE_EXTERNAL_STORAGE);
		return readPermission == PackageManager.PERMISSION_GRANTED && writePermission == PackageManager.PERMISSION_GRANTED;
	}
	
	public void requestReadWritePermission() {
		String[] permissions = {
			Manifest.permission.READ_EXTERNAL_STORAGE,
			Manifest.permission.WRITE_EXTERNAL_STORAGE
		};
		ActivityCompat.requestPermissions(mActivity, permissions, READ_WRITE_PERMISSION_REQUEST_CODE);
	}
	
	// Deprecated code :(
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		if(requestCode == ScoopedStorage.REQUEST_ACTION_CODE) {
			if(resultCode == mActivity.RESULT_OK) {
				if(ScoopedStorage.create(data))
					Toast.makeText(mActivity, mActivity.getString(R.string.toast_success_export), Toast.LENGTH_SHORT).show();
				else
					Toast.makeText(mActivity, mActivity.getString(R.string.toast_failed_export), Toast.LENGTH_SHORT).show();
			} else
				Toast.makeText(mActivity, mActivity.getString(R.string.toast_failed_export), Toast.LENGTH_SHORT).show();
		} /* else if(requestCode == ScoopedStorage.REQUEST_ACTION_XLSX) {
			if(resultCode == mActivity.RESULT_OK) {
				if(ScoopedStorage.createXlsx(data))
					Toast.makeText(mActivity, mActivity.getString(R.string.toast_success_export), Toast.LENGTH_SHORT).show();
				else
					Toast.makeText(mActivity, mActivity.getString(R.string.toast_failed_export), Toast.LENGTH_SHORT).show();
			} else
				Toast.makeText(mActivity, mActivity.getString(R.string.toast_failed_export), Toast.LENGTH_SHORT).show();
		} */
	}
}