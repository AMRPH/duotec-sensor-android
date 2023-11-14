package com.gelios.configurator.ui.choose

import android.app.Application
import android.os.CountDownTimer
import android.util.Log
import androidx.lifecycle.MutableLiveData
import com.gelios.configurator.MainPref
import com.gelios.configurator.entity.Scan
import com.gelios.configurator.entity.BLESensor
import com.gelios.configurator.ui.App
import com.gelios.configurator.ui.base.BaseViewModel
import com.gelios.configurator.util.BinHelper
import com.polidea.rxandroidble2.scan.ScanFilter
import com.polidea.rxandroidble2.scan.ScanResult
import com.polidea.rxandroidble2.scan.ScanSettings
import io.reactivex.BackpressureStrategy
import io.reactivex.Observable
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import java.util.concurrent.TimeUnit
import javax.inject.Inject

class ChooseDeviceViewModel @Inject constructor(application: Application) : BaseViewModel(application) {

    override val TAG: String
        get() = javaClass.simpleName

    private var observable: Observable<ScanResult>

    private lateinit var timer: CountDownTimer
    private val list = mutableListOf<BLESensor>()
    val devicesLiveData = MutableLiveData<List<BLESensor>>()
    val uiProgressLiveData = MutableLiveData<Boolean>()
    val timerLiveData = MutableLiveData<Int>()
    var isScanning = false

    init {
        val settings = ScanSettings
            .Builder()
            .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
            .setCallbackType(ScanSettings.CALLBACK_TYPE_ALL_MATCHES)
            .build()

        observable = App.rxBleClient.scanBleDevices(settings)
    }

    fun startScan(){
        isScanning = true
        uiProgressLiveData.postValue(true)
        startTimer()

        observable.take(60, TimeUnit.SECONDS).subscribe({
            analyze(it)
        }, {
            Log.d("GGGG", "onError")
            Log.d("SCAN", it.stackTraceToString())
            isScanning = false
            uiProgressLiveData.postValue(false)
        }, {
            Log.d("GGGG", "onFinish")
            isScanning = false
            uiProgressLiveData.postValue(false)
        }).let { compositeDisposable.add(it) }

        devicesLiveData.postValue(emptyList())
        list.clear()
    }

    fun stopScan(){
        isScanning = false
        uiProgressLiveData.postValue(false)
        timer.cancel()
        compositeDisposable.dispose()
    }

    private fun analyze(result: ScanResult){
        if (!isDeviceInList(result.bleDevice.macAddress) && result.scanRecord.serviceUuids != null) {
            for (uuid in result.scanRecord.serviceUuids!!) {
                val s = uuid.uuid.toString()
                when {
                    (s.contains("a4825243")) -> {
                        val item = Scan(
                            result.bleDevice.macAddress,
                            result.bleDevice.name.toString(),
                            result.scanRecord.bytes,
                            result.rssi,
                            timerLiveData.value!!,
                            result.scanRecord.serviceUuids)
                        MainPref.typeDevices[item.mac] = BLESensor.TYPE.Fuel
                        addItem(item)
                    }
                    (s.contains("a4825253")) -> {
                        val item = Scan(
                            result.bleDevice.macAddress,
                            result.bleDevice.name.toString(),
                            result.scanRecord.bytes,
                            result.rssi,
                            timerLiveData.value!!,
                            result.scanRecord.serviceUuids)
                        MainPref.typeDevices[item.mac] = BLESensor.TYPE.Thermometer
                        addItem(item)
                    }
                    (s.contains("a4825263")) -> {
                        val item = Scan(
                            result.bleDevice.macAddress,
                            result.bleDevice.name.toString(),
                            result.scanRecord.bytes,
                            result.rssi,
                            timerLiveData.value!!,
                            result.scanRecord.serviceUuids)
                        MainPref.typeDevices[item.mac] = BLESensor.TYPE.Relay
                        addItem(item)
                    }
                    (s.contains("0000ffc0")) -> {
                        val item = Scan(
                            result.bleDevice.macAddress,
                            result.bleDevice.name.toString(),
                            result.scanRecord.bytes,
                            result.rssi,
                            timerLiveData.value!!,
                            result.scanRecord.serviceUuids)
                        MainPref.typeDevices[item.mac] = BLESensor.TYPE.Firmware
                        addItem(item)
                    }
                }

            }
        }
    }

    private fun addItem(item: Scan){
        if (BinHelper.toHex(item.data).contains("160F")){
            var data = ""
            var soft = ""
            var battery = ""

            when (MainPref.typeDevices.getValue(item.mac)){
                BLESensor.TYPE.Thermometer ->{
                    val hex = BinHelper.toHex(item.data).split("160F")
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
                    val hex = BinHelper.toHex(item.data).split("160F")
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
                    val hex = BinHelper.toHex(item.data).split("160F")
                    val hexBytes = hex[1].chunked(2)

                    data = hexBytes[1].toInt(16).toString()
                    soft = (hexBytes[3].toInt(16)/10.0).toString()
                    battery = (hexBytes[2].toInt(16)/10.0).toString() + "V"
                }
            }
            list.add(BLESensor(
                item.mac,
                item.name,
                item.rssi,
                data,
                soft,
                battery,
                item.time,
                MainPref.typeDevices.getValue(item.mac)
            ))

            devicesLiveData.value = list
        }
    }

    private fun calculateThermData(hex: String): String{
        return if (hex.toInt(16) > 32767){
            ((hex.toInt(16) - 65536)/10.0).toString()
        } else {
            (hex.toInt(16)/10.0).toString()
        }
    }

    private fun isDeviceInList(mac: String): Boolean {
        return devicesLiveData.value?.firstOrNull{ device -> device.mac == mac } != null
    }


    private fun startTimer() {
        timer = object : CountDownTimer(60000, 1000){
            override fun onTick(millisUntilFinished: Long) {
                timerLiveData.postValue(60 - (millisUntilFinished / 1000).toInt())
            }

            override fun onFinish() {
            }
        }
        timer.start()
    }
}