package com.nielcode.kupass.core;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.widget.Toast;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import com.nielcode.kupass.R;
import com.nielcode.kupass.services.rw.Reader;
import com.nielcode.kupass.utils.ScoopedStorage;
import java.util.Objects;

public class Permission {

  private static final int READ_WRITE_PERMISSION_REQUEST_CODE = 100;
  private final Activity activity;
  private final ScoopedStorage scoopedStorage;

  public Permission(Activity activity, Context context) {
    this.activity = activity;
    scoopedStorage = new ScoopedStorage(activity, context);
  }

  public boolean checkReadWritePermission() {
    int readPermission =
        ContextCompat.checkSelfPermission(activity, Manifest.permission.READ_EXTERNAL_STORAGE);
    int writePermission =
        ContextCompat.checkSelfPermission(activity, Manifest.permission.WRITE_EXTERNAL_STORAGE);
    return readPermission == PackageManager.PERMISSION_GRANTED
        && writePermission == PackageManager.PERMISSION_GRANTED;
  }

  public void requestReadWritePermission() {
    String[] permissions = {
      Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE
    };
    ActivityCompat.requestPermissions(activity, permissions, READ_WRITE_PERMISSION_REQUEST_CODE);
  }

  @Deprecated
  public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
    if (requestCode == ScoopedStorage.REQUEST_ACTION_CREATE_FILE) {
      if (resultCode == Activity.RESULT_OK) {
        if (scoopedStorage.create(data.getData()))
          Toast.makeText(
                  activity, activity.getString(R.string.toast_success_export), Toast.LENGTH_SHORT)
              .show();
        else
          Toast.makeText(
                  activity, activity.getString(R.string.toast_failed_export), Toast.LENGTH_SHORT)
              .show();
      } else
        Toast.makeText(
                activity, activity.getString(R.string.toast_failed_export), Toast.LENGTH_SHORT)
            .show();
    }
    if (requestCode == Reader.REQUEST_ACTION_READ_FILE) {
      if (resultCode == Activity.RESULT_OK) {
        Uri uri = Objects.requireNonNull(data).getData();
        if (uri != null) {
          /*
          HomeFragment homeFragment = new HomeFragment();
          try {
            ArrayList<Password> dataList = new Reader(activity).getDataFromJson(uri);
            for (Password item : dataList) homeFragment.add(item);
            Toast.makeText(
                    activity, activity.getString(R.string.toast_success_import), Toast.LENGTH_SHORT)
                .show();
          } catch (IOException e) {
            e.printStackTrace();
            Toast.makeText(
                    activity, activity.getString(R.string.toast_failed_import), Toast.LENGTH_SHORT)
                .show();
          }
           */
        }
      }
    }
  }
}
