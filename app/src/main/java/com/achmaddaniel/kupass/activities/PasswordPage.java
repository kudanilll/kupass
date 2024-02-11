package com.achmaddaniel.kupass.activities;

import com.achmaddaniel.kupass.R;
import com.achmaddaniel.kupass.adapter.list.ListItem;
import com.achmaddaniel.kupass.databinding.ActivityPasswordPageBinding;
import com.achmaddaniel.kupass.fragments.HomeFragment;

import com.google.android.material.textfield.TextInputEditText;
import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.content.Intent;
import android.widget.Button;
import android.widget.Toast;

public class PasswordPage extends AppCompatActivity {
	
	private ActivityPasswordPageBinding mBinding;
	private Intent mIntent = null;
	private ListItem mItem = null;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		mBinding = ActivityPasswordPageBinding.inflate(getLayoutInflater());
		setContentView(mBinding.getRoot());
		
		setSupportActionBar(mBinding.toolbar);
		getSupportActionBar().setDisplayHomeAsUpEnabled(true);
		
		mIntent = getIntent();
		if(mIntent != null)
			mItem = (ListItem)mIntent.getSerializableExtra("list");
		if(mItem != null) {
			mBinding.toolbar.setTitle(getString(R.string.edit_password_page));
			mBinding.username.setText(mItem.getUserName());
			mBinding.password.setText(mItem.getPassword());
			mBinding.site.setText(mItem.getPasswordName());
			mBinding.note.setText(mItem.getNote());
		}
		
		mBinding.saveButton.setOnClickListener((view) -> {
			final String username = mBinding.username.getText().toString();
			final String password = mBinding.password.getText().toString();
			final String site = mBinding.site.getText().toString();
			final String note = mBinding.note.getText().toString();
			if(site.isEmpty()) {
				mBinding.site.setError(getString(R.string.error_column_is_empty));
				return;
			}
			if(username.isEmpty()) {
				mBinding.username.setError(getString(R.string.error_column_is_empty));
				return;
			}
			if(password.isEmpty()) {
				mBinding.password.setError(getString(R.string.error_column_is_empty));
				return;
			}
			if(mIntent != null && mItem != null) {
				HomeFragment.edit(mItem.getId(), site, username, password, note);
				Toast.makeText(PasswordPage.this, getString(R.string.toast_success_update), Toast.LENGTH_SHORT).show();
			} else {
				HomeFragment.add(site, username, password, note);
				Toast.makeText(PasswordPage.this, getString(R.string.toast_success), Toast.LENGTH_SHORT).show();
			}
			finish();
		});
	}
	
	@Override
	protected void onDestroy() {
		super.onDestroy();
		mBinding = null;
		mIntent  = null;
	}
}