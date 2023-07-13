package com.gelios.configurator.ui.choose

import android.app.Application
import android.bluetooth.BluetoothAdapter
import android.bluetooth.le.ScanCallback
import android.content.pm.PackageManager
import android.os.Looper
import android.util.Log
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.lifecycle.MutableLiveData
import com.gelios.configurator.MainPref
import com.gelios.configurator.R
import com.polidea.rxandroidble2.scan.ScanResult
import com.polidea.rxandroidble2.scan.ScanSettings
import com.gelios.configurator.entity.ScanBLESensor
import com.gelios.configurator.entity.Sensor
import com.gelios.configurator.ui.App
import com.gelios.configurator.ui.base.BaseViewModel
import com.gelios.configurator.util.BinHelper
import io.reactivex.disposables.Disposable
import okio.ByteString.Companion.decodeHex
import java.sql.Time
import java.util.*
import java.util.logging.Handler
import javax.inject.Inject
import kotlin.collections.HashMap

class ChooseDeviceViewModel @Inject constructor(application: Application) : BaseViewModel(application) {

    override val TAG: String
        get() = javaClass.simpleName

    private val list = mutableSetOf<Pair<ScanResult, Int>>()
    private var macAddress = ""
    private var params: MutableList<HashMap<String, String>> = mutableListOf()
    val uiDeviceList = MutableLiveData<List<ScanBLESensor>>()
    val uiProgressLiveData = MutableLiveData<Boolean>()
    val sensorData = MutableLiveData<ByteArray>()
    val uiSensor = MutableLiveData<Sensor>()
    val timerLiveData = MutableLiveData<Int>()
    var timer = Timer()
    var isScan = false


    fun scanDevices() {
        isScan = true
        uiProgressLiveData.postValue(true)

        val scanSubscription =
            App.rxBleClient.scanBleDevices(
                ScanSettings.Builder()
                    .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
                    .setCallbackType(ScanSettings.CALLBACK_TYPE_ALL_MATCHES)
                    .build()
            ).subscribe({
                if (!deviceInList(it.bleDevice.macAddress) && it.scanRecord.serviceUuids != null) {
                    for (items in it.scanRecord.serviceUuids!!) {
                        when {
                            (items.uuid.toString().contains("a4825243")) -> {
                                list.add(it to timerLiveData.value!!)
                                MainPref.typeDevices[it.bleDevice.macAddress] = ScanBLESensor.TYPE.Fuel
                            }
                            (items.uuid.toString().contains("a4825263")) -> {
                                list.add(it to timerLiveData.value!!)
                                MainPref.typeDevices[it.bleDevice.macAddress] = ScanBLESensor.TYPE.Relay
                            }
                            (items.uuid.toString().contains("a4825253")) -> {
                                list.add(it to timerLiveData.value!!)
                                MainPref.typeDevices[it.bleDevice.macAddress] = ScanBLESensor.TYPE.Thermometer
                            }
                            (items.uuid.toString().contains("0000ffc0")) -> {
                                list.add(it to timerLiveData.value!!)
                                MainPref.typeDevices[it.bleDevice.macAddress] = ScanBLESensor.TYPE.Firmware
                            }
                        }

                    }
                    val sList = mutableListOf<ScanBLESensor>()
                    for (item in list.toList().distinctBy { listItem -> listItem.first.bleDevice.macAddress }) {
                        var data = ""
                        var soft = ""
                        var battery = ""

                        when (MainPref.typeDevices.getValue(item.first.bleDevice.macAddress)){
                            ScanBLESensor.TYPE.Thermometer ->{
                                if (BinHelper.toHex(item.first.scanRecord.bytes).contains("160F")){
                                    val hexBytes = BinHelper.toHex(item.first.scanRecord.bytes).split("160F")[1].chunked(2)

                                    when (hexBytes[0]){
                                        "03" -> {
                                            data = calculateThermData(hexBytes[2] + hexBytes[1])
                                            soft = (hexBytes[6].toInt(16)/10.0).toString()
                                            battery = (hexBytes[5].toInt(16)/10.0).toString() + "V"
                                        }
                                        "63" -> {
                                            data = calculateThermData(hexBytes[2] + hexBytes[1])
                                            soft = (hexBytes[6].toInt(16)/10.0).toString()
                                            battery = hexBytes[5].toInt(16).toString() + "%"
                                        }
                                        else -> {
                                            data = calculateThermData(hexBytes[2] + hexBytes[1])
                                            soft = (hexBytes[6].toInt(16)/10.0).toString()
                                            battery = (hexBytes[5].toInt(16)/10.0).toString() + "V"
                                        }
                                    }
                                }
                            }
                            ScanBLESensor.TYPE.Fuel ->{
                                if (BinHelper.toHex(item.first.scanRecord.bytes).contains("160F")){
                                    val hexBytes = BinHelper.toHex(item.first.scanRecord.bytes).split("160F")[1].chunked(2)

                                    when (hexBytes[0]){
                                        "01" -> {
                                            data = ((hexBytes[2] + hexBytes[1]).toInt(16) / 4095.0 * 100).toInt().toString()
                                            soft = (hexBytes[5].toInt(16)/10.0).toString()
                                            battery = (hexBytes[3].toInt(16)/10.0).toString() + "V"
                                        }
                                        "61" -> {
                                            data = ((hexBytes[2] + hexBytes[1]).toInt(16) / 4095.0 * 100).toInt().toString()
                                            soft = (hexBytes[5].toInt(16)/10.0).toString()
                                            battery = hexBytes[3].toInt(16).toString() + "%"
                                        }
                                        else -> {
                                            data = ((hexBytes[2] + hexBytes[1]).toInt(16) / 4095.0 * 100).toInt().toString()
                                            soft = (hexBytes[5].toInt(16)/10.0).toString()
                                            battery = (hexBytes[3].toInt(16)/10.0).toString() + "V"
                                        }
                                    }
                                }
                            }
                            ScanBLESensor.TYPE.Relay ->{
                                if (BinHelper.toHex(item.first.scanRecord.bytes).contains("160F")){
                                    val hexBytes = BinHelper.toHex(item.first.scanRecord.bytes).split("160F")[1].chunked(2)

                                    data = hexBytes[1].toInt(16).toString()
                                    soft = (hexBytes[3].toInt(16)/10.0).toString()
                                    battery = (hexBytes[2].toInt(16)/10.0).toString() + "V"
                                }
                            }
                        }
                        sList.add(
                            ScanBLESensor(
                                item.first.bleDevice.macAddress,
                                item.first.bleDevice.name,
                                item.first.rssi,
                                data,
                                soft,
                                battery,
                                item.second,
                                MainPref.typeDevices.getValue(item.first.bleDevice.macAddress)
                            )
                        )
                    }
                    if ((uiDeviceList.value?.size ?: 0) != sList.size) {
                        uiDeviceList.value = sList
                    }

                    Log.d("SCAN_","Проверка")
                } else{
                    Log.d("SCAN_","Пропустить")
                }

            }, {
                uiProgressLiveData.postValue(false)
                Log.d("BLE", it.message.toString())
                Log.d("BLE", it.stackTraceToString())
            }, {
                uiProgressLiveData.postValue(false)
            })

        compositeDisposable.add(scanSubscription)

        android.os.Handler(Looper.getMainLooper()).postDelayed({
            pauseScan()
        }, 60 * 1000)
        startTimer()
    }

    private fun calculateThermData(hex: String): String{
        return if (hex.toInt(16) > 32767){
            ((hex.toInt(16) - 65536)/10.0).toString()
        } else {
            (hex.toInt(16)/10.0).toString()
        }
    }

    private fun deviceInList(macAddress: String?): Boolean {
        val result = uiDeviceList.value?.firstOrNull{ device -> device.mac == macAddress }
        return result != null
    }

    fun stopScan() {
        isScan = false
        list.clear()
        compositeDisposable.dispose()
        uiProgressLiveData.postValue(false)
        stopTimer()
    }

    fun pauseScan() {
        isScan = false
        list.clear()
        compositeDisposable.clear()
        uiProgressLiveData.postValue(false)
        stopTimer()
    }

    fun startTimer() {
        var sec = 0
        timer.scheduleAtFixedRate(object :TimerTask(){
            override fun run() {
                timerLiveData.postValue(sec)
                sec += 1
            }
        }, 0, 1000)
    }

    fun stopTimer() {
        timer.cancel()
        timer = Timer()
    }

}