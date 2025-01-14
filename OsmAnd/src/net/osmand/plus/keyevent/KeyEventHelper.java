package net.osmand.plus.keyevent;

import android.view.KeyEvent;
import android.view.KeyEvent.Callback;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.StateChangedListener;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.keyevent.commands.KeyEventCommand;
import net.osmand.plus.keyevent.commands.MapZoomCommand;
import net.osmand.plus.keyevent.devices.InputDeviceProfile;
import net.osmand.plus.settings.backend.OsmandSettings;

import java.util.HashMap;
import java.util.Map;

public class KeyEventHelper implements KeyEvent.Callback {

	private final OsmandApplication app;
	private final OsmandSettings settings;
	private final InputDeviceHelper deviceHelper;
	private MapActivity mapActivity;

	private final Map<Integer, KeyEventCommand> globalCommands = new HashMap<>();

	private StateChangedListener<Boolean> volumeButtonsPrefListener;
	private KeyEvent.Callback externalCallback;

	public KeyEventHelper(@NonNull OsmandApplication app) {
		this.app = app;
		settings = app.getSettings();
		deviceHelper = app.getInputDeviceHelper();

		// Update commands when related preferences updated
		volumeButtonsPrefListener = aBoolean -> updateGlobalCommands();
		settings.USE_VOLUME_BUTTONS_AS_ZOOM.addListener(volumeButtonsPrefListener);
		updateGlobalCommands();
	}

	public void updateGlobalCommands() {
		globalCommands.clear();
		if (settings.USE_VOLUME_BUTTONS_AS_ZOOM.get()) {
			bindCommand(KeyEvent.KEYCODE_VOLUME_DOWN, MapZoomCommand.CONTINUOUS_ZOOM_OUT_ID);
			bindCommand(KeyEvent.KEYCODE_VOLUME_UP, MapZoomCommand.CONTINUOUS_ZOOM_IN_ID);
		}
	}

	public void setMapActivity(@Nullable MapActivity mapActivity) {
		this.mapActivity = mapActivity;
	}

	/**
	 * Sets an external callback to process key events in another place with a custom algorithm.
	 */
	public void setExternalCallback(@Nullable Callback externalCallback) {
		this.externalCallback = externalCallback;
	}

	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		if (externalCallback != null) {
			return externalCallback.onKeyDown(keyCode, event);
		}
		KeyEventCommand command = findCommand(keyCode);
		if (command != null && command.onKeyDown(keyCode, event)) {
			return true;
		}
		return app.getAidlApi().onKeyEvent(event);
	}

	@Override
	public boolean onKeyLongPress(int keyCode, KeyEvent event) {
		if (externalCallback != null) {
			return externalCallback.onKeyLongPress(keyCode, event);
		}
		KeyEventCommand command = findCommand(keyCode);
		return command != null && command.onKeyLongPress(keyCode, event);
	}

	@Override
	public boolean onKeyUp(int keyCode, KeyEvent event) {
		if (settings.SHOW_INFO_ABOUT_PRESSED_KEY.get()) {
			showToastAboutPressedKey(event);
		}
		if (externalCallback != null) {
			return externalCallback.onKeyUp(keyCode, event);
		}
		KeyEventCommand command = findCommand(keyCode);
		if (command != null && command.onKeyUp(keyCode, event)) {
			return true;
		}
		return app.getAidlApi().onKeyEvent(event);
	}

	@Override
	public boolean onKeyMultiple(int keyCode, int count, KeyEvent event) {
		if (externalCallback != null) {
			return externalCallback.onKeyMultiple(keyCode, count, event);
		}
		KeyEventCommand command = findCommand(keyCode);
		return command != null && command.onKeyMultiple(keyCode, count, event);
	}

	private void showToastAboutPressedKey(@NonNull KeyEvent keyEvent) {
		int keyCode = keyEvent.getKeyCode();
		int deviceId = keyEvent.getDeviceId();
		String keyLabel = KeySymbolMapper.getKeySymbol(keyCode);
		app.showShortToastMessage("Device id: " + deviceId + ", key code: " + keyCode + ", label: \"" + keyLabel + "\"");
	}

	@Nullable
	private KeyEventCommand findCommand(int keyCode) {
		if (mapActivity == null || isLetterKeyCode(keyCode) && !mapActivity.isMapVisible()) {
			// Reject using of letter keycodes when the focus isn't on the Activity
			return null;
		}
		// Search command in global bound commands
		KeyEventCommand globalCommand = globalCommands.get(keyCode);
		if (globalCommand != null) {
			return globalCommand;
		}
		// Search command for current input device profile
		InputDeviceProfile inputDevice = deviceHelper.getEnabledDevice();
		return inputDevice != null ? inputDevice.findCommand(keyCode) : null;
	}

	private void bindCommand(int keyCode, @NonNull String commandId) {
		KeyEventCommand command = KeyEventCommandsCache.getCommand(app, commandId);
		if (command != null) {
			globalCommands.put(keyCode, command);
		}
	}

	@Nullable
	public MapActivity getMapActivity() {
		return mapActivity;
	}

	private static boolean isLetterKeyCode(int keyCode) {
		return keyCode >= KeyEvent.KEYCODE_A && keyCode <= KeyEvent.KEYCODE_Z;
	}
}

