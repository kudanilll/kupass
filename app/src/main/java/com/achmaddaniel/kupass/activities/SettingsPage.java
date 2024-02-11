package com.achmaddaniel.kupass.activities;

import com.achmaddaniel.kupass.R;
import com.achmaddaniel.kupass.databinding.ActivitySettingsPageBinding;
import com.achmaddaniel.kupass.fragments.SettingsFragment;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.FragmentManager;

import android.os.Bundle;

public class SettingsPage extends AppCompatActivity {
	
	private ActivitySettingsPageBinding mBinding;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		mBinding = ActivitySettingsPageBinding.inflate(getLayoutInflater());
		setContentView(mBinding.getRoot());
		
		getSupportFragmentManager()
			.beginTransaction()
			.add(mBinding.container.getId(), new SettingsFragment())
			.commit();
		
		setSupportActionBar(mBinding.toolbar);
		getSupportActionBar().setDisplayHomeAsUpEnabled(true);
	}
	
	@Override
	protected void onDestroy() {
		super.onDestroy();
		mBinding = null;
	}
}