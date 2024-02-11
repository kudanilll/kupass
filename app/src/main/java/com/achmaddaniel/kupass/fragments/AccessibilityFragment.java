package com.achmaddaniel.kupass.fragments;

import com.achmaddaniel.kupass.R;
import com.achmaddaniel.kupass.activities.SettingsPage;
import com.achmaddaniel.kupass.adapter.grid.GridAdapter;
import com.achmaddaniel.kupass.adapter.grid.GridItem;
import com.achmaddaniel.kupass.adapter.list.ListItem;
import com.achmaddaniel.kupass.core.ConstantVar;
import com.achmaddaniel.kupass.core.Password;
import com.achmaddaniel.kupass.services.reader.Reader;
import com.achmaddaniel.kupass.services.writer.Writer;
import com.achmaddaniel.kupass.database.Pref;
import com.achmaddaniel.kupass.database.SQLDataHelper;
import com.achmaddaniel.kupass.databinding.FragmentAccessibilityPageBinding;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
//import com.google.android.material.transition.MaterialSharedAxis;

import androidx.fragment.app.Fragment;

import android.os.Bundle;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.view.View;
import android.widget.Toast;

import java.util.ArrayList;

public class AccessibilityFragment extends Fragment {
	
	private final int POSITION_IMPORT	= 0;
	private final int POSITION_EXPORT	= 1;
	private final int POSITION_SETTINGS	= 2;
	
	private FragmentAccessibilityPageBinding mBinding;
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		//setEnterTransition(new MaterialSharedAxis(MaterialSharedAxis.X, true));
		//setExitTransition(new MaterialSharedAxis(MaterialSharedAxis.X, false));
	}
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		mBinding = FragmentAccessibilityPageBinding.inflate(inflater, container, false);
		return mBinding.getRoot();
	}
	
	@Override
	public void onViewCreated(View view, Bundle savedInstanceState) {
		super.onViewCreated(view, savedInstanceState);
		
		// Set Grid Item
		ArrayList<GridItem> list = new ArrayList<>();
		list.add(new GridItem(R.drawable.ic_place_item, getActivity().getString(R.string.settings_import_title)));
		list.add(new GridItem(R.drawable.ic_publish, getActivity().getString(R.string.settings_export_title)));
		list.add(new GridItem(R.drawable.ic_settings, getActivity().getString(R.string.settings)));
		GridAdapter adapter = new GridAdapter(list);
		mBinding.gridMenu.setAdapter(adapter);
		
		// Grid Item OnClick
		adapter.setOnItemClickListener((position, currentItem) -> {
			switch(position) {
			case POSITION_IMPORT:
				//dialogImportFromFile();
				new Reader(getActivity()).pickJsonFile();
				break;
			case POSITION_EXPORT:
				dialogExportToFile();
				break;
			case POSITION_SETTINGS:
				getActivity().startActivity(new Intent(getActivity(), SettingsPage.class));
				break;
			}
		});
	}
	
	private void dialogImportFromFile() {
		// Show dialog
		CharSequence[] listImport = getActivity().getResources().getStringArray(R.array.import_method_list);
		new MaterialAlertDialogBuilder(getActivity())
			.setTitle(getActivity().getString(R.string.dialog_title_import))
			.setNegativeButton(getActivity().getString(R.string.dialog_cancel), (dialog, which) -> {
			})
			.setPositiveButton(getString(R.string.dialog_import), (dialog, which) -> {
			})
			.setSingleChoiceItems(listImport, Pref.getImportMethod(), (dialog, which) -> {
				Pref.setImportMethod(which);
			})
			.create()
			.show();
	}
	
	private void dialogExportToFile() {
		final ArrayList<ListItem> items = new SQLDataHelper(getActivity()).getAll();
		if(!(items.size() > 0)) { //empty
			Toast.makeText(getActivity(), getActivity().getString(R.string.toast_no_data), Toast.LENGTH_SHORT).show();
			return;
		}
		CharSequence[] listExport = getActivity().getResources().getStringArray(R.array.export_method_list);
		new MaterialAlertDialogBuilder(getActivity())
			.setTitle(getActivity().getString(R.string.dialog_title_export))
			.setNegativeButton(getActivity().getString(R.string.dialog_cancel), (dialog, which) -> {
			})
			.setPositiveButton(getString(R.string.dialog_export), (dialog, which) -> {
				ArrayList<Password> password = new ArrayList<>();
				for(ListItem item : items)
					password.add(new Password(item));
				Writer writer = new Writer(password);
				switch(Pref.getExportMethod()) {
				/*case ConstantVar.EXPORT_CSV:
					writer.toCsvFile();
					break;*/
				case ConstantVar.EXPORT_JSON:
					writer.toJsonFile();
					break;
				case ConstantVar.EXPORT_TEXT:
					writer.toTextFile();
					break;
				}
			})
			.setSingleChoiceItems(listExport, Pref.getExportMethod(), (dialog, which) -> {
				Pref.setExportMethod(which);
			})
			.create()
			.show();
	}
}