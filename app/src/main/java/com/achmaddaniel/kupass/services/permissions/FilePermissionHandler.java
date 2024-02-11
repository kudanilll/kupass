package com.achmaddaniel.kupass.services.permissions;

import com.achmaddaniel.kupass.R;
import com.achmaddaniel.kupass.core.Password;
import com.achmaddaniel.kupass.fragments.HomeFragment;
import com.achmaddaniel.kupass.services.reader.Reader;
import com.achmaddaniel.kupass.services.writer.ScoopedStorage;

import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.Manifest;
import android.net.Uri;
import android.widget.Toast;

import java.util.ArrayList;
import java.io.IOException;

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
	
	@Deprecated
	public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
		if(requestCode == ScoopedStorage.REQUEST_ACTION_CREATE_FILE) {
			if(resultCode == mActivity.RESULT_OK) {
				if(ScoopedStorage.create(data.getData()))
					Toast.makeText(mActivity, mActivity.getString(R.string.toast_success_export), Toast.LENGTH_SHORT).show();
				else
					Toast.makeText(mActivity, mActivity.getString(R.string.toast_failed_export), Toast.LENGTH_SHORT).show();
			} else
				Toast.makeText(mActivity, mActivity.getString(R.string.toast_failed_export), Toast.LENGTH_SHORT).show();
		}
		if(requestCode == Reader.REQUEST_ACTION_READ_FILE) {
			if(resultCode == mActivity.RESULT_OK) {
				Uri uri = data.getData();
				if(uri != null) {
					HomeFragment homeFragment = new HomeFragment();
					try {
						ArrayList<Password> dataList = new Reader(mActivity).getDataFromJson(uri);
						for(Password item : dataList)
							homeFragment.add(item);
						Toast.makeText(mActivity, mActivity.getString(R.string.toast_success_import), Toast.LENGTH_SHORT).show();
					} catch(IOException e) {
						e.printStackTrace();
						Toast.makeText(mActivity, mActivity.getString(R.string.toast_failed_import), Toast.LENGTH_SHORT).show();
					}
				}
			}
		}
	}
}