package net.osmand.plus.plugins.antplus.dialogs;

import android.os.Bundle;
import android.view.View;

import androidx.fragment.app.FragmentManager;

import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.base.BottomSheetBehaviourDialogFragment;
import net.osmand.plus.base.bottomsheetmenu.BaseBottomSheetItem;
import net.osmand.plus.base.bottomsheetmenu.BottomSheetItemWithDescription;
import net.osmand.plus.base.bottomsheetmenu.simpleitems.DividerItem;
import net.osmand.plus.base.bottomsheetmenu.simpleitems.TitleItem;
import net.osmand.plus.utils.AndroidUtils;

public class PairNewSensorBottomSheet extends BottomSheetBehaviourDialogFragment {

	public static final String TAG = PairNewSensorBottomSheet.class.getSimpleName();
	public static final int BOTTOM_SHEET_HEIGHT_DP = 427;

	@Override
	public void createMenuItems(Bundle savedInstanceState) {
		items.add(new TitleItem(getString(R.string.ant_plus_pair_new_sensor)));

		BaseBottomSheetItem pairBluetooth = new BottomSheetItemWithDescription.Builder()
				.setIcon(getContentIcon(R.drawable.ic_action_bluetooth))
				.setTitle(getString(R.string.ant_plus_pair_new_sensor_ble))
				.setLayoutId(R.layout.bottom_sheet_item_simple_pad_32dp)
				.setOnClickListener(new View.OnClickListener() {
					@Override
					public void onClick(View v) {
						dismiss();
						MapActivity mapActivity = (MapActivity) getActivity();
						if (mapActivity != null) {
							AntPlusSearchFragment.Companion.showInstance(mapActivity.getSupportFragmentManager());
						}
					}
				})
				.create();
		items.add(pairBluetooth);

		BaseBottomSheetItem pairAntItem = new BottomSheetItemWithDescription.Builder()
				.setIcon(getContentIcon(R.drawable.ic_action_ant_plus))
				.setTitle(getString(R.string.ant_plus_pair_new_sensor_ant_plus))
				.setLayoutId(R.layout.bottom_sheet_item_simple_pad_32dp)
				.setOnClickListener(new View.OnClickListener() {
					@Override
					public void onClick(View v) {
						dismiss();
					}
				})
				.create();
		items.add(pairAntItem);

		items.add(new DividerItem(getContext()));

		BaseBottomSheetItem helpItem = new BottomSheetItemWithDescription.Builder()
				.setIcon(getContentIcon(R.drawable.ic_action_help))
				.setTitle(getString(R.string.ant_plus_help_title))
				.setLayoutId(R.layout.bottom_sheet_item_simple_pad_32dp)
				.setOnClickListener(new View.OnClickListener() {
					@Override
					public void onClick(View v) {
						MapActivity mapActivity = (MapActivity) getActivity();
						if (mapActivity != null) {
							hideBottomSheet();
						}
					}
				})
				.create();
		items.add(helpItem);

	}

	@Override
	protected int getPeekHeight() {
		return AndroidUtils.dpToPx(requiredMyApplication(), BOTTOM_SHEET_HEIGHT_DP);
	}


	public static void showInstance(FragmentManager fragmentManager) {
		if (!fragmentManager.isStateSaved()) {
			PairNewSensorBottomSheet fragment = new PairNewSensorBottomSheet();
			fragment.setRetainInstance(true);
			fragment.show(fragmentManager, TAG);
		}
	}

	protected void hideBottomSheet() {
		MapActivity mapActivity = (MapActivity) getActivity();
		if (mapActivity != null) {
			FragmentManager manager = mapActivity.getSupportFragmentManager();
			manager.beginTransaction()
					.hide(this)
					.commitAllowingStateLoss();
		}
	}

	protected boolean hideButtonsContainer() {
		return true;
	}

}