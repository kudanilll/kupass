package com.nielcode.kupass.ui.activities;

import android.content.Intent;
import android.os.Bundle;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.nielcode.kupass.R;
import com.nielcode.kupass.core.Permission;
import com.nielcode.kupass.databinding.ActivityHomeBinding;

public class HomeActivity extends AppCompatActivity {

  private ActivityHomeBinding binding;
  private Permission permissionHandler;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    binding = ActivityHomeBinding.inflate(getLayoutInflater());
    setContentView(binding.getRoot());

    // Check permission
    checkPermission();
    setupButtonListener();
  }

  @Override
  protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
    super.onActivityResult(requestCode, resultCode, data);
    permissionHandler.onActivityResult(requestCode, resultCode, data);
  }

  @Override
  protected void onDestroy() {
    super.onDestroy();
    binding = null;
  }

  private void setupButtonListener() {
    binding.searchPassword.setOnMenuItemClickListener(
        (item) -> {
          if (item.getItemId() == R.id.menu_settings) {
            startActivity(new Intent(this, SettingsActivity.class));
            return true;
          }
          return false;
        });
  }

  private void checkPermission() {
    permissionHandler = new Permission(HomeActivity.this, getApplicationContext());
    if (!permissionHandler.checkReadWritePermission()) {
      // Permission not yet granted, request permission.
      new MaterialAlertDialogBuilder(HomeActivity.this)
          .setTitle(getString(R.string.dialog_title_permission))
          .setMessage(getString(R.string.dialog_message_permission))
          .setPositiveButton(
              getString(R.string.dialog_request),
              (dialog, which) -> {
                permissionHandler.requestReadWritePermission();
              })
          .setCancelable(false)
          .create()
          .show();
    }
  }
}
