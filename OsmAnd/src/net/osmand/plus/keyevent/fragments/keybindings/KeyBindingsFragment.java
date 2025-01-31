package net.osmand.plus.keyevent.fragments.keybindings;

import static net.osmand.plus.settings.fragments.BaseSettingsFragment.APP_MODE_KEY;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.Toolbar;
import androidx.core.view.ViewCompat;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.base.BaseOsmAndFragment;
import net.osmand.plus.helpers.AndroidUiHelper;
import net.osmand.plus.keyevent.InputDeviceHelper;
import net.osmand.plus.keyevent.InputDeviceHelper.InputDeviceHelperListener;
import net.osmand.plus.settings.backend.ApplicationMode;
import net.osmand.plus.utils.AndroidUtils;
import net.osmand.plus.utils.ColorUtilities;

public class KeyBindingsFragment extends BaseOsmAndFragment implements InputDeviceHelperListener {

	public static final String TAG = KeyBindingsFragment.class.getSimpleName();

	private KeyBindingsAdapter adapter;
	private KeyBindingsController controller;

	private ApplicationMode appMode;
	private InputDeviceHelper deviceHelper;

	@Override
	public void onCreate(@Nullable Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		Bundle arguments = getArguments();
		String appModeKey = arguments != null ? arguments.getString(APP_MODE_KEY) : "";
		appMode = ApplicationMode.valueOfStringKey(appModeKey, settings.getApplicationMode());
		controller = new KeyBindingsController(app, appMode);
		deviceHelper = app.getInputDeviceHelper();
	}

	@Nullable
	@Override
	public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
	                         @Nullable Bundle savedInstanceState) {
		updateNightMode();
		View view = themedInflater.inflate(R.layout.fragment_external_input_device_type, container, false);
		AndroidUtils.addStatusBarPadding21v(requireMyActivity(), view);
		setupToolbar(view);

		adapter = new KeyBindingsAdapter(app, appMode, controller, isUsedOnMap());
		RecyclerView recyclerView = view.findViewById(R.id.recycler_view);
		recyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));
		recyclerView.setAdapter(adapter);
		updateViewContent();
		return view;
	}

	private void setupToolbar(@NonNull View view) {
		Toolbar toolbar = view.findViewById(R.id.toolbar);

		ImageView closeButton = toolbar.findViewById(R.id.close_button);
		closeButton.setImageResource(AndroidUtils.getNavigationIconResId(app));
		closeButton.setOnClickListener(v -> {
			FragmentActivity activity = getActivity();
			if (activity != null) {
				activity.onBackPressed();
			}
		});

		TextView title = toolbar.findViewById(R.id.toolbar_title);
		title.setText(getString(R.string.key_bindings));
		toolbar.findViewById(R.id.toolbar_subtitle).setVisibility(View.GONE);

		View actionButton = toolbar.findViewById(R.id.action_button);
		AndroidUiHelper.updateVisibility(actionButton, false);
		ViewCompat.setElevation(view.findViewById(R.id.appbar), 5.0f);
	}

	@Override
	public void onInputDeviceHelperEvent() {
		updateViewContent();
	}

	private void updateViewContent() {
		adapter.setScreenData(controller.populateScreenItems(), controller.isDeviceEditable());
	}

	@Override
	public void onResume() {
		super.onResume();
		MapActivity mapActivity = getMapActivity();
		if (mapActivity != null) {
			mapActivity.disableDrawer();
			controller.setActivity(mapActivity);
		}
		deviceHelper.addListener(this);
	}

	@Override
	public void onPause() {
		super.onPause();
		MapActivity mapActivity = getMapActivity();
		if (mapActivity != null) {
			mapActivity.enableDrawer();
		}
		controller.setActivity(null);
		deviceHelper.removeListener(this);
	}

	@Nullable
	private MapActivity getMapActivity() {
		return (MapActivity) getActivity();
	}

	@Override
	public int getStatusBarColorId() {
		AndroidUiHelper.setStatusBarContentColor(getView(), nightMode);
		return ColorUtilities.getListBgColorId(nightMode);
	}

	public static void showInstance(@NonNull FragmentManager manager,
	                                @NonNull ApplicationMode appMode) {
		if (AndroidUtils.isFragmentCanBeAdded(manager, TAG)) {
			KeyBindingsFragment fragment = new KeyBindingsFragment();
			Bundle arguments = new Bundle();
			arguments.putString(APP_MODE_KEY, appMode.getStringKey());
			fragment.setArguments(arguments);
			manager.beginTransaction()
					.replace(R.id.fragmentContainer, fragment, TAG)
					.addToBackStack(TAG)
					.commitAllowingStateLoss();
		}
	}
}
