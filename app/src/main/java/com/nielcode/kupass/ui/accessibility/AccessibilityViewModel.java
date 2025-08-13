package com.nielcode.kupass.ui.accessibility;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

public class AccessibilityViewModel extends ViewModel {

	private final MutableLiveData<String> mText;

	public AccessibilityViewModel() {
		mText = new MutableLiveData<>();
		mText.setValue("This is accessibility fragment");
	}

	public LiveData<String> getText() {
		return mText;
	}
}