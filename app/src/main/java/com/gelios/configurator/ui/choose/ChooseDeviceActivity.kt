package com.gelios.configurator.ui.choose

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.content.Context
import android.content.Intent
import android.location.LocationManager
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.view.View
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import com.gelios.configurator.BuildConfig
import com.gelios.configurator.MainPref
import com.gelios.configurator.R
import com.gelios.configurator.databinding.ActivityChooseDeviceBinding
import com.gelios.configurator.entity.BLESensor
import com.gelios.configurator.entity.Sensor
import com.gelios.configurator.ui.base.DataBindingActivity
import com.gelios.configurator.ui.sensor.fuel.DeviceFuelActivity
import com.gelios.configurator.ui.sensor.relay.DeviceRelayActivity
import com.gelios.configurator.ui.sensor.therm.DeviceThermometerActivity
import com.gelios.configurator.ui.dialog.LoadingDialog
import com.google.android.material.snackbar.Snackbar
import com.tbruyelle.rxpermissions3.RxPermissions
import kotlinx.android.synthetic.main.activity_choose_device.*
import java.util.*
import kotlin.math.abs


class ChooseDeviceActivity :
    DataBindingActivity<ChooseDeviceViewModel, ActivityChooseDeviceBinding>(),
    DeviceAdapter.OnItemClickListener {

    override fun provideViewModel() =
        ViewModelProvider(this, viewModelFactory)[ChooseDeviceViewModel::class.java]

    override fun provideLayoutId(): Int = R.layout.activity_choose_device
    override fun provideLifecycleOwner() = this
    override val TAG = javaClass.simpleName
    private lateinit var rxPermissions: RxPermissions
    private var isTherm = false
    @SuppressLint("CheckResult")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        rxPermissions = RxPermissions(this)

        val deviceAdapter = DeviceAdapter(this)
        rv_device.adapter = deviceAdapter
        viewModel.devicesLiveData.observe(this, Observer {
            deviceAdapter.setItems(it.sortedBy { abs(it.signal) })
        })

        image_renew.setOnClickListener {
            if (!viewModel.isScanning) {
                checkBTOn()
            }
        }

        if (intent.getBooleanExtra("back", false)) {
            showDialogBack()
            if (intent.getBooleanExtra("term", false)) isTherm = true
        }
        if (intent.getBooleanExtra("error", false)) {
            showDialogError()
        }

        viewModel.uiProgressLiveData.observe(this){
            if (it){
                text_title.text = getString(R.string.device_search)
            } else {
                text_title.text = getString(R.string.start_search)
            }
        }

        viewModel.timerLiveData.observe(this) {
            text_timer.text = "${it} "+ getString(R.string.sec)
        }

        swipeRefreshLayout.setOnRefreshListener {
            swipeRefreshLayout.isRefreshing = false
        }

        text_version.text = "version\n${BuildConfig.VERSION_NAME}"
    }

    override fun onResume() {
        super.onResume()
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S){
            rxPermissions.request(
                    Manifest.permission.ACCESS_COARSE_LOCATION,
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.BLUETOOTH_SCAN,
                    Manifest.permission.BLUETOOTH_ADVERTISE,
                    Manifest.permission.BLUETOOTH_CONNECT
                ).subscribe ({ granted: Boolean ->
                    if (granted) checkBTOn()
                },{ Log.e("ChooseDeviceActivity", "error:${it.message}") })
        } else {
            rxPermissions.request(
                    Manifest.permission.ACCESS_COARSE_LOCATION,
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.BLUETOOTH,
                    Manifest.permission.BLUETOOTH_ADMIN
                ).subscribe ({ granted: Boolean ->
                    if (granted) checkBTOn()
                },{ Log.e("ChooseDeviceActivity", "error:${it.message}") })
        }
        super.onResume()
    }

    private fun checkBTOn() {
        val mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
         if (!mBluetoothAdapter.isEnabled) {
             showTurnOnBluetoothMessage()
             image_error_icon.visibility = View.VISIBLE
        } else {
             val manager: LocationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager
             if ( !manager.isProviderEnabled( LocationManager.GPS_PROVIDER ) ) {
                 showTurnOnGPS();
                 image_error_icon.setImageResource(R.drawable.ic_location_off)
             } else{
                 if (!viewModel.isScanning) viewModel.startScan()
                 image_error_icon.visibility = View.GONE
             }
        }
    }

    private fun showTurnOnBluetoothMessage() {
        val snack = Snackbar.make(layout, getString(R.string.ble_off), Snackbar.LENGTH_INDEFINITE)
            .setAction(getString(R.string.turn_on), View.OnClickListener {
                val intentOpenBluetoothSettings = Intent()
                intentOpenBluetoothSettings.action = Settings.ACTION_BLUETOOTH_SETTINGS
                startActivity(intentOpenBluetoothSettings)
            })
        snack.show()
    }

    private fun showTurnOnGPS() {
        val snack = Snackbar.make(layout, getString(R.string.location_off), Snackbar.LENGTH_INDEFINITE)
            .setAction(getString(R.string.turn_on), View.OnClickListener {
                startActivity(Intent(android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS));
            })
        snack.show()
    }

    private fun showDialogBack() {
        val snack: Snackbar = Snackbar.make(layout, "${getString(R.string.back_to_device)} \n${MainPref.deviceMac}", Snackbar.LENGTH_INDEFINITE)
        snack.setAction(getString(R.string.back), View.OnClickListener {
            viewModel.stopScan()
            when (MainPref.typeDevices.getValue(MainPref.deviceMac)){
                BLESensor.TYPE.Thermometer ->{
                    val intent =  DeviceThermometerActivity.instance(
                        context = this,
                        isFirmware = false)
                    startActivity(intent)
                }
                BLESensor.TYPE.Fuel ->{
                    val intent = DeviceFuelActivity.instance(
                        context = this,
                        isFirmware = false)
                    startActivity(intent)
                }
                BLESensor.TYPE.Relay ->{
                    val intent =  DeviceRelayActivity.instance(
                        context = this,
                        isFirmware = false)
                    startActivity(intent)
                }
            }
            showProgressConnect()
            })
        snack.show()
    }

    private fun showDialogError() {
        val snack: Snackbar = Snackbar.make(layout, getString(R.string.error), Snackbar.LENGTH_SHORT)
        snack.show()
    }

    override fun connect(item: BLESensor) {
        viewModel.stopScan()
        Sensor.name.postValue(item.name)
        MainPref.deviceMac = item.mac

        when (item.type) {
            BLESensor.TYPE.Fuel -> {
                val intent = DeviceFuelActivity.instance(
                    context = this,
                    isFirmware = item.type == BLESensor.TYPE.Firmware)
                startActivity(intent)
            }
            BLESensor.TYPE.Thermometer -> {
                val intent = DeviceThermometerActivity.instance(
                    context = this,
                    isFirmware = item.type == BLESensor.TYPE.Firmware)
                startActivity(intent)
            }
            BLESensor.TYPE.Relay -> {
                val intent = DeviceRelayActivity.instance(
                    context = this,
                    isFirmware = item.type == BLESensor.TYPE.Firmware)
                startActivity(intent)
            }
            else -> {
                val intent = DeviceFuelActivity.instance(
                    context = this,
                    isFirmware = item.type == BLESensor.TYPE.Firmware)
                startActivity(intent)
            }
        }

        showProgressConnect()
    }

    private fun showProgressConnect() {
        val dialog = LoadingDialog()
        dialog.show(supportFragmentManager, "SearchDialog")
        finish()
    }
}