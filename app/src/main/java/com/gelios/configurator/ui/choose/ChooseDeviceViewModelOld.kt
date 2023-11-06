package com.gelios.configurator.ui.choose

import android.app.Application
import android.os.CountDownTimer
import android.util.Log
import androidx.lifecycle.MutableLiveData
import com.gelios.configurator.MainPref
import com.polidea.rxandroidble2.scan.ScanResult
import com.polidea.rxandroidble2.scan.ScanSettings
import com.gelios.configurator.entity.BLESensor
import com.gelios.configurator.ui.App
import com.gelios.configurator.ui.base.BaseViewModel
import com.gelios.configurator.util.BinHelper
import javax.inject.Inject

class ChooseDeviceViewModelOld @Inject constructor(application: Application) : BaseViewModel(application) {

    override val TAG: String
        get() = javaClass.simpleName

    private lateinit var timer: CountDownTimer
    private val list = mutableSetOf<Pair<ScanResult, Int>>()
    val uiDeviceList = MutableLiveData<List<BLESensor>>()
    val uiProgressLiveData = MutableLiveData<Boolean>()
    val timerLiveData = MutableLiveData<Int>()
    var isScan = false


    fun scanDevices() {
        Log.d("TTT", "Scan")
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
                    Log.d("TTT", "2")
                    for (items in it.scanRecord.serviceUuids!!) {
                        Log.d("TTT", "3")
                        when {
                            (items.uuid.toString().contains("a4825243")) -> {
                                list.add(it to timerLiveData.value!!)
                                MainPref.typeDevices[it.bleDevice.macAddress] = BLESensor.TYPE.Fuel
                            }
                            (items.uuid.toString().contains("a4825263")) -> {
                                list.add(it to timerLiveData.value!!)
                                MainPref.typeDevices[it.bleDevice.macAddress] = BLESensor.TYPE.Relay
                            }
                            (items.uuid.toString().contains("a4825253")) -> {
                                list.add(it to timerLiveData.value!!)
                                MainPref.typeDevices[it.bleDevice.macAddress] = BLESensor.TYPE.Thermometer
                            }
                            (items.uuid.toString().contains("0000ffc0")) -> {
                                list.add(it to timerLiveData.value!!)
                                MainPref.typeDevices[it.bleDevice.macAddress] = BLESensor.TYPE.Firmware
                            }
                        }

                    }
                    val sList = mutableListOf<BLESensor>()
                    for (item in list.toList().distinctBy { listItem -> listItem.first.bleDevice.macAddress }) {
                        if (BinHelper.toHex(item.first.scanRecord.bytes).contains("160F")){
                            var data = ""
                            var soft = ""
                            var battery = ""

                            when (MainPref.typeDevices.getValue(item.first.bleDevice.macAddress)){
                                BLESensor.TYPE.Thermometer ->{
                                    val hex = BinHelper.toHex(item.first.scanRecord.bytes).split("160F")
                                    val hexBytes = hex[1].chunked(2)
                                    val length = hex[0].chunked(2)[hex[0].chunked(2).size-2]

                                    data = calculateThermData(hexBytes[2] + hexBytes[1])

                                    when (length){
                                        "09" -> soft = "MINI"
                                        "0A" -> soft = (hexBytes[6].toInt(16)/10.0).toString()
                                        else -> soft = (hexBytes[6].toInt(16)/10.0).toString()
                                    }

                                    when (hexBytes[0]){
                                        "03" -> battery = (hexBytes[5].toInt(16)/10.0).toString() + "V"
                                        "63" -> battery = hexBytes[5].toInt(16).toString() + "%"
                                        else -> battery = (hexBytes[5].toInt(16)/10.0).toString() + "V"
                                    }
                                }
                                BLESensor.TYPE.Fuel ->{
                                    val hex = BinHelper.toHex(item.first.scanRecord.bytes).split("160F")
                                    val hexBytes = hex[1].chunked(2)
                                    val length = hex[0].chunked(2)[hex[0].chunked(2).size-2]

                                    data = if (((hexBytes[2] + hexBytes[1]).toInt(16) / 4095.0 * 100).toInt() > 100){
                                        "error"
                                    } else{
                                        ((hexBytes[2] + hexBytes[1]).toInt(16) / 4095.0 * 100).toInt().toString()
                                    }


                                    when (length){
                                        "08" -> soft = "MINI"
                                        "0F" -> soft = (hexBytes[5].toInt(16)/10.0).toString()
                                        else -> soft = (hexBytes[6].toInt(16)/10.0).toString()
                                    }

                                    when (hexBytes[0]){
                                        "01" -> battery = (hexBytes[3].toInt(16)/10.0).toString() + "V"
                                        "61" -> battery = hexBytes[3].toInt(16).toString() + "%"
                                        else -> battery = (hexBytes[5].toInt(16)/10.0).toString() + "V"
                                    }
                                }
                                BLESensor.TYPE.Relay ->{
                                    val hex = BinHelper.toHex(item.first.scanRecord.bytes).split("160F")
                                    val hexBytes = hex[1].chunked(2)

                                    data = hexBytes[1].toInt(16).toString()
                                    soft = (hexBytes[3].toInt(16)/10.0).toString()
                                    battery = (hexBytes[2].toInt(16)/10.0).toString() + "V"
                                }
                            }

                            sList.add(
                                BLESensor(
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
                    }
                    if ((uiDeviceList.value?.size ?: 0) != sList.size) {
                        Log.d("TTT", "4")
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
        timer.cancel()
    }

    private fun startTimer() {
        timer = object : CountDownTimer(60000, 1000){
            override fun onTick(millisUntilFinished: Long) {
                timerLiveData.postValue(60 - (millisUntilFinished / 1000).toInt())
            }

            override fun onFinish() {
                stopScan()
            }
        }
        timer.start()
    }

    private fun stopTimer() {
        timer.cancel()
    }

}