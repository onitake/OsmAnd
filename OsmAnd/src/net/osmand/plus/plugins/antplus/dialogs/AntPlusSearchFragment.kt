package net.osmand.plus.plugins.antplus.dialogs

import android.bluetooth.le.ScanResult
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.FragmentManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import me.zhanghai.android.materialprogressbar.MaterialProgressBar
import net.osmand.plus.R
import net.osmand.plus.helpers.AndroidUiHelper
import net.osmand.plus.plugins.antplus.AntPlusPlugin.ScanForDevicesListener
import net.osmand.plus.plugins.antplus.ExternalDevice
import net.osmand.plus.plugins.antplus.adapters.FoundDevicesAdapter
import net.osmand.plus.utils.AndroidUtils
import net.osmand.plus.utils.UiUtilities

class AntPlusSearchFragment : AntPlusBaseFragment(), ScanForDevicesListener,
    FoundDevicesAdapter.DeviceClickListener {
    private var currentState = SearchStates.NOTHING_FOUND
    private var stateNoBluetoothView: View? = null
    private var stateSearchingView: View? = null
    private var stateNothingFoundView: View? = null
    private var stateDevicesListView: View? = null
    private var foundDevicesCountView: TextView? = null
    private lateinit var adapter: FoundDevicesAdapter

    companion object {
        val TAG: String = AntPlusSearchFragment.javaClass.simpleName
        fun showInstance(manager: FragmentManager) {
            if (AndroidUtils.isFragmentCanBeAdded(manager, TAG)) {
                val fragment = AntPlusSearchFragment()
                fragment.retainInstance = true
                fragment.show(manager, TAG)
            }
        }
    }

    override fun getLayoutId(): Int {
        return R.layout.fragment_ant_plus_search
    }

    override fun setupUI(view: View) {
        super.setupUI(view)
        setupNoBluetoothView(view)
        setupSearchingView(view)
        setupNothingFoundView(view)
        setupDevicesListView(view)
    }

    private fun setupNoBluetoothView(parentView: View) {
        stateNoBluetoothView = parentView.findViewById(R.id.state_no_bluetooth)
        val openSettingButton = stateNoBluetoothView?.findViewById<View>(R.id.dismiss_button)
        val buttonTextId = R.string.ant_plus_open_settings
        UiUtilities.setupDialogButton(
            nightMode,
            openSettingButton,
            UiUtilities.DialogButtonType.SECONDARY,
            buttonTextId
        )
        openSettingButton?.setOnClickListener {
            val intentOpenBluetoothSettings = Intent()
            intentOpenBluetoothSettings.action = Settings.ACTION_BLUETOOTH_SETTINGS
            startActivity(intentOpenBluetoothSettings)
        }
        AndroidUiHelper.updateVisibility(openSettingButton, true)
    }

    private fun setupSearchingView(parentView: View) {
        stateSearchingView = parentView.findViewById(R.id.state_searching)
        val progressBar = parentView.findViewById<MaterialProgressBar>(R.id.progressBar)
        progressBar.showProgressBackground = true
    }

    private fun setupNothingFoundView(parentView: View) {
        stateNothingFoundView = parentView.findViewById(R.id.state_nothing_found)
        val searchAgain = stateNothingFoundView?.findViewById<View>(R.id.dismiss_button)
        val buttonTextId = R.string.ble_search_again
        UiUtilities.setupDialogButton(
            nightMode,
            searchAgain,
            UiUtilities.DialogButtonType.SECONDARY,
            buttonTextId
        )
        searchAgain?.setOnClickListener {
            setCurrentState(SearchStates.SEARCHING)
            startSearch()
        }
        AndroidUiHelper.updateVisibility(searchAgain, true)
    }

    private fun setupDevicesListView(parentView: View) {
        stateDevicesListView = parentView.findViewById(R.id.state_found_devices_list)
        foundDevicesCountView = stateDevicesListView?.findViewById(R.id.found_devices_count)
        val recyclerView: RecyclerView? =
            stateDevicesListView?.findViewById(R.id.found_devices_list)
        recyclerView?.layoutManager = LinearLayoutManager(context)
        adapter = FoundDevicesAdapter(app, ArrayList<ExternalDevice>(), nightMode, this)
        recyclerView?.adapter = adapter
    }

    private fun bindFoundDevices() {
        var foundDevices = plugin.lastFoundDevices
        var formatString = activity?.resources?.getString(R.string.bluetooth_found_title)
        formatString?.let {
            foundDevicesCountView?.text =
                String.format(formatString, foundDevices.size)
        }
        adapter.setItems(foundDevices)

    }

    private fun changeUIState() {
        Handler(Looper.getMainLooper()).postDelayed({
            var ordinal = currentState.ordinal
            ordinal++
            if (ordinal >= SearchStates.values().size) {
                ordinal = 0
            }
            setCurrentState(SearchStates.values()[ordinal])
            changeUIState()
        }, 2000)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val newView = super.onCreateView(inflater, container, savedInstanceState)
        updateCurrentStateView()
        return newView
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        currentState = if (!plugin.isBlueToothEnabled) {
            SearchStates.NO_BLUETOOTH
        } else {
            SearchStates.SEARCHING
        }
    }

    override fun onResume() {
        super.onResume()
        if (currentState == SearchStates.SEARCHING) {
            startSearch()
        } else if (currentState == SearchStates.DEVICES_LIST) {
            bindFoundDevices()
        }
    }

    private fun startSearch() {
        plugin.setScanForDevicesListener(this)
        plugin.searchForDevices()
    }

    override fun onPause() {
        super.onPause()
        plugin.setScanForDevicesListener(null)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        stateNoBluetoothView = null
        stateSearchingView = null
        stateNothingFoundView = null
        stateDevicesListView = null
    }

    private fun setCurrentState(newState: SearchStates) {
        if (currentState != newState) {
            currentState = newState
            updateCurrentStateView()
        }
    }

    private fun updateCurrentStateView() {
        AndroidUiHelper.updateVisibility(
            stateNoBluetoothView,
            currentState == SearchStates.NO_BLUETOOTH
        )
        AndroidUiHelper.updateVisibility(stateSearchingView, currentState == SearchStates.SEARCHING)
        AndroidUiHelper.updateVisibility(
            stateNothingFoundView,
            currentState == SearchStates.NOTHING_FOUND
        )
        AndroidUiHelper.updateVisibility(
            stateDevicesListView,
            currentState == SearchStates.DEVICES_LIST
        )
    }

    override fun onScanFinished(scanResults: HashMap<String, ScanResult>) {
        if (scanResults.isEmpty()) {
            setCurrentState(SearchStates.NOTHING_FOUND)
        } else {
            setCurrentState(SearchStates.DEVICES_LIST)
            bindFoundDevices()
            bindFoundDevices()
        }
    }

    internal enum class SearchStates {
        NO_BLUETOOTH, SEARCHING, NOTHING_FOUND, DEVICES_LIST
    }

    override fun onDeviceClicked(device: ExternalDevice) {
        ExternalDeviceDetailsFragment.showInstance(activity!!.supportFragmentManager, device)
    }
}