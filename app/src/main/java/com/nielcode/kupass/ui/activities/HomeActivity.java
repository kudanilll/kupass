package com.nielcode.kupass.ui.activities;

import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import com.nielcode.kupass.databinding.ActivityHomeBinding;

public class HomeActivity extends AppCompatActivity {

  private ActivityHomeBinding binding;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    binding = ActivityHomeBinding.inflate(getLayoutInflater());
    setContentView(binding.getRoot());
  }

  @Override
  protected void onDestroy() {
    super.onDestroy();
    binding = null;
  }
}
