package com.nielcode.kupass.ui.accessibility;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.nielcode.kupass.databinding.FragmentAccessibilityBinding;

public class AccessibilityFragment extends Fragment {

	private FragmentAccessibilityBinding binding;

	public View onCreateView(@NonNull LayoutInflater inflater,
	                         ViewGroup container, Bundle savedInstanceState) {
		AccessibilityViewModel viewModel =
				new ViewModelProvider(this).get(AccessibilityViewModel.class);

		binding = FragmentAccessibilityBinding.inflate(inflater, container, false);
		return binding.getRoot();
	}

	@Override
	public void onDestroyView() {
		super.onDestroyView();
		binding = null;
	}
}