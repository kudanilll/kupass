package com.nielcode.kupass.ui.activities;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.nielcode.kupass.R;
import com.nielcode.kupass.core.Permission;
import com.nielcode.kupass.databinding.ActivityHomeBinding;
import com.nielcode.kupass.ui.adapters.ListPasswordAdapter;
import com.nielcode.kupass.ui.adapters.ListPasswordItem;
import java.util.ArrayList;

public class HomeActivity extends AppCompatActivity {

  private ArrayList<ListPasswordItem> listPasswordItems;
  private ListPasswordAdapter adapter;
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

    listPasswordItems = new ArrayList<>();

    // Add dummy data
    for (int i = 0; i < 20; i++) {
      listPasswordItems.add(new ListPasswordItem(i, "Password", "Username", "Password", "Note"));
    }

    update();
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

  private void update() {
    // listPasswordItems = sql.getAll();
    adapter = new ListPasswordAdapter(this, listPasswordItems);

    binding.listPassword.setAdapter(adapter);
    binding.listPassword.setOnItemClickListener(
        (adapter, view, position, id) -> {
          // Intent intent = new Intent(this, PasswordPage.class);
          // intent.putExtra("list", (Serializable) mListItem.get(position));
          // startActivity(intent);
        });
    binding.listPassword.setOnItemLongClickListener(
        (adapter, view, position, id) -> {
          new MaterialAlertDialogBuilder(this)
              .setTitle(this.getString(R.string.dialog_title_delete))
              .setMessage(this.getString(R.string.dialog_message_delete))
              .setNegativeButton(this.getString(R.string.dialog_cancel), (dialog, which) -> {})
              .setPositiveButton(
                  this.getString(R.string.dialog_delete),
                  (dialog, which) -> {
                    // mSQL.delete(listPasswordItems.get(position).getId());
                    Toast.makeText(
                            this, this.getString(R.string.toast_success_delete), Toast.LENGTH_SHORT)
                        .show();
                    update();
                  })
              .create()
              .show();
          return true;
        });
  }
}
