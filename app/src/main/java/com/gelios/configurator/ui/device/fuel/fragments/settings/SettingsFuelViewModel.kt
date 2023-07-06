package com.gelios.configurator.ui.device.fuel.fragments.settings

import android.app.Application
import android.util.Log
import androidx.lifecycle.MutableLiveData
import com.gelios.configurator.MainPref
import com.gelios.configurator.entity.Sensor
import com.gelios.configurator.entity.SensorParams
import com.gelios.configurator.ui.App
import com.gelios.configurator.ui.App.Companion.bleCompositeDisposable
import com.gelios.configurator.ui.MessageType
import com.gelios.configurator.ui.base.BaseViewModel
import com.gelios.configurator.ui.datasensor.FuelSensorSettings
import com.gelios.configurator.ui.datasensor.FuelSensorSettings2
import com.gelios.configurator.util.BinHelper
import com.gelios.configurator.util.BleHelper
import com.gelios.configurator.util.isConnected
import com.polidea.rxandroidble2.RxBleDevice
import com.polidea.rxandroidble2.Timeout
import io.reactivex.Observable
import java.util.*
import java.util.concurrent.TimeUnit

class SettingsFuelViewModel(application: Application) : BaseViewModel(application) {

    companion object {
        const val MTU = 250
    }


    val settingsLiveData = MutableLiveData<FuelSensorSettings>()
    val settings2LiveData = MutableLiveData<FuelSensorSettings2>()
    val uiProgressLiveData = MutableLiveData<Boolean>()
    val errorLiveData = MutableLiveData(false)

    val uiActiveButtons = MutableLiveData(false)
    val messageLiveData = MutableLiveData<MessageType>()
    val commandSendOk = MutableLiveData(false)

    override val TAG = "SettingsFuelViewModel"
    val device: RxBleDevice = App.rxBleClient.getBleDevice(MainPref.deviceMac)

    var flagUUIDCorrect: Boolean = true
    var flagMAJORCorrect: Boolean = true
    var flagMINORCorrect: Boolean = true


    init {
        if (Sensor.fuelCacheSettings == null) {
            readSettings()
        } else {
            settingsLiveData.postValue(Sensor.fuelCacheSettings)
        }

        if (Sensor.version!! >= 5){
            if (Sensor.fuelCacheSettings2 == null) {
                readSettings2()
            } else {
                settings2LiveData.postValue(Sensor.fuelCacheSettings2)
            }
        }
    }

    fun checkDataCorrect(): Boolean {
        return flagUUIDCorrect && flagMAJORCorrect && flagMINORCorrect
    }

    fun checkAuth() {
        if (Sensor.authorized) uiActiveButtons.postValue(true)
    }

    fun initConnection() {
        if (!device.isConnected) {
            uiProgressLiveData.postValue(true)
            Observable
                .defer { device.establishConnection(false, Timeout(30, TimeUnit.SECONDS)) }
                .doOnNext { Log.e("BLE_DATA ", it.toString()) }
                .compose(BleHelper.geMTUTransformer(MTU))
                .subscribe({
                    if(!errorLiveData.value!!){
                        App.connection = it
                        uiProgressLiveData.postValue(false)
                        if (App.connection != null){
                            readSettings()
                            if (Sensor.version!! >= 5){
                                readSettings2()
                            }
                        } else {
                            errorLiveData.postValue(true)
                        }
                    }
                           }, {
                    if(!errorLiveData.value!!){
                        uiProgressLiveData.postValue(false)
                        if (("GATT_CONN_TIMEOUT" in it.message!!) or ("UNKNOWN" in it.message!!)){
                            errorLiveData.postValue(true)
                        }
                        if ("GATT_CONN_TERMINATE_PEER_USER" in it.message!!){
                            initConnection()
                        }
                    }
                    Log.e("BLE PARAMETRS ", it.message.toString())
            })?.let {
                bleCompositeDisposable.add(it)
            }
        }
    }

    fun enterPassword(newPass: String) {
        if (device.isConnected) {
            val passByte = newPass.toByteArray()
            uiProgressLiveData.postValue(true)
            App.connection!!
                .writeCharacteristic(
                    UUID.fromString(SensorParams.FUEL_PASSWORD.uuid),
                    passByte
                )
                .subscribe({
                    Log.e("BLE_DATA ", it!!.contentToString())
                    Sensor.authorized = true
                    uiProgressLiveData.postValue(false)
                    messageLiveData.postValue(MessageType.PASSWORD_ACCEPTED)
                    uiActiveButtons.postValue(true)
                    Sensor.authorized = true
                    Sensor.confirmedPass = newPass
                    readSettings()
                    if (Sensor.version!! >= 5){
                        readSettings2()
                    }
                }, {
                    Log.e("BLE_DATA ", it.message.toString())
                    messageLiveData.postValue(MessageType.PASSWORD_NOT_ACCEPTED)
                    uiProgressLiveData.postValue(false)
                }).let { compositeDisposable.add(it) }
        } else {
            initConnection()
        }
    }



    fun readSettings() {
        if (device.isConnected) {
            uiProgressLiveData.postValue(true)
            App.connection!!
                .readCharacteristic(UUID.fromString(SensorParams.FUEL_SETTINGS.uuid))
                .subscribe({
                    Sensor.fuelCacheSettings = FuelSensorSettings(it)
                    settingsLiveData.postValue(Sensor.fuelCacheSettings)
                    Log.d("BLE_RETURN_readSettings", it!!.contentToString())
                    uiProgressLiveData.postValue(false)
                    Log.e("BLE_SET_ARRAY", Sensor.fuelCacheSettings!!.getBytes().contentToString())
                }, {
                    uiProgressLiveData.postValue(false)
                    Log.e("BLE_RETURN_readSettings", it.message.toString())
                }).let { compositeDisposable.add(it) }
        } else {
            initConnection()
        }
    }

    fun saveSettings(depth: Int, cntMax: Int, cntMin: Int, escortMode: Int) {
        replaceSettings(depth, cntMax, cntMin, escortMode)
        Sensor.fuelCacheSettings!!.applyMasterPassword(Sensor.confirmedPass)
        if (device.isConnected) {
            uiProgressLiveData.postValue(true)
            App.connection!!
                .writeCharacteristic(
                    UUID.fromString(SensorParams.FUEL_SETTINGS.uuid),
                    Sensor.fuelCacheSettings!!.getBytes()
                )
                .subscribe({
                    Log.e("BLE_SEND_RETURN ", it!!.contentToString())
                    messageLiveData.postValue(MessageType.SAVED)
                    uiProgressLiveData.postValue(false)
                    settingsLiveData.postValue(Sensor.fuelCacheSettings)
                }, {
                    Log.e("BLE_DATA ", it.message.toString())
                    messageLiveData.postValue(MessageType.ERROR)
                    uiProgressLiveData.postValue(false)
                }).let { compositeDisposable.add(it) }
        }
    }



    fun readSettings2() {
        if (device.isConnected) {
            uiProgressLiveData.postValue(true)
            App.connection!!
                .readCharacteristic(UUID.fromString(SensorParams.FUEL_SETTINGS2.uuid))
                .subscribe({
                    Sensor.fuelCacheSettings2 = FuelSensorSettings2(it)
                    settings2LiveData.postValue(Sensor.fuelCacheSettings2)
                    Log.d("BLE_ERROR SETTINGS2", it!!.contentToString())
                    uiProgressLiveData.postValue(false)
                }, {
                    uiProgressLiveData.postValue(false)
                    Log.e("BLE_ERROR SETTINGS2", it.message.toString())
                }).let { compositeDisposable.add(it) }
        } else {
            initConnection()
        }
    }

    fun saveSettings2(uuid: String, major: Int, minor: Int) {
        replaceSettings2(uuid, major, minor)

        Sensor.thermCacheSettings2!!.setConstant()
        Sensor.thermCacheSettings2!!.applyMasterPassword(Sensor.confirmedPass)
        if (device.isConnected) {
            uiProgressLiveData.postValue(true)
            App.connection!!
                .writeCharacteristic(
                    UUID.fromString(SensorParams.FUEL_SETTINGS2.uuid),
                    Sensor.fuelCacheSettings2!!.getBytes()
                )
                .subscribe({
                    messageLiveData.postValue(MessageType.SAVED)
                    uiProgressLiveData.postValue(false)
                    settings2LiveData.postValue(Sensor.fuelCacheSettings2)
                }, {
                    Log.e("BLE_ERROR SETTINGS2", it.message.toString())
                    messageLiveData.postValue(MessageType.ERROR)
                    uiProgressLiveData.postValue(false)
                }).let { compositeDisposable.add(it) }
        }
    }

    fun changeProtocol(flag: Int) {
        Sensor.fuelCacheSettings!!.flag = flag
    }

    fun changeInterval(interval: Int) {
        Sensor.fuelCacheSettings2!!.adv_interval = interval
    }

    fun changePower(power: Int) {
        Sensor.fuelCacheSettings2!!.adv_power_mode = power
    }

    fun changeBeacon(beacon: Int) {
        Sensor.fuelCacheSettings2!!.adv_beacon = beacon
    }

    fun replaceSettings2(uuid: String, major: Int, minor: Int) {
        Sensor.fuelCacheSettings2!!.uuid = BinHelper.toByteArray(uuid)
        Sensor.fuelCacheSettings2!!.major = BinHelper.intToUInt16ByteArray(major)
        Sensor.fuelCacheSettings2!!.minor = BinHelper.intToUInt16ByteArray(minor)
    }

    private fun replaceSettings(filterDepth: Int, cntMax: Int, cntMin: Int, escortMode: Int) {
        Sensor.fuelCacheSettings!!.filter_depth = filterDepth
        Sensor.fuelCacheSettings!!.cnt_max = cntMax
        Sensor.fuelCacheSettings!!.cnt_min = cntMin
        Sensor.fuelCacheSettings!!.flag = escortMode
    }

    fun sendCommand(intByte: Byte) {
        val command: Byte = intByte.toByte()
        Log.e("BLE_SEND_sendCommand", byteArrayOf(command).contentToString())
        if (device.isConnected) {
            uiProgressLiveData.postValue(true)
            App.connection!!
                .writeCharacteristic(
                    UUID.fromString(SensorParams.FUEL_COMMAND.uuid),
                    byteArrayOf(command)
                )
                .subscribe({
                    Log.e("BLE_RETURN_sendCommand", it!!.contentToString())
                    uiProgressLiveData.postValue(false)
                    val byte = it[0].toInt()
                    when(byte) {
                        5 ->  messageLiveData.postValue(MessageType.APPLY_EMPTY)
                        6 ->  messageLiveData.postValue(MessageType.APPLY_FULL)
                    }
                    readSettings()
                }, {
                    Log.e("BLE_RETURN_sendCommand", it.message.toString())
                    uiProgressLiveData.postValue(false)
                    commandSendOk.postValue(true)
                }).let { compositeDisposable.add(it) }
        }
    }

}