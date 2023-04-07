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

    override val TAG = "SettingsFuelViewModel"

    val uiProgressLiveData = MutableLiveData<Boolean>()
    val errorLiveData = MutableLiveData<Boolean>(false)
    val uiActiveButtons = MutableLiveData<Boolean>(false)
    val infoLiveSettings = MutableLiveData<FuelSensorSettings>()
    val messageLiveData = MutableLiveData<MessageType>()
    val commandSendOk = MutableLiveData(false)

    val device: RxBleDevice = App.rxBleClient.getBleDevice(MainPref.deviceMac)


    init {
        if (Sensor.fuelCacheSettings != null) {
            infoLiveSettings.postValue(Sensor.fuelCacheSettings)
        } else {
            readSettings()
        }
    }

    fun checkAuth() {
        if (Sensor.sensorAuthorized) uiActiveButtons.postValue(true)
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
                    Sensor.sensorAuthorized = true
                    uiProgressLiveData.postValue(false)
                    messageLiveData.postValue(MessageType.PASSWORD_ACCEPTED)
                    uiActiveButtons.postValue(true)
                    Sensor.sensorAuthorized = true
                    Sensor.confirmedPass = newPass
                    readSettings()
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
                    infoLiveSettings.postValue(Sensor.fuelCacheSettings)
                    Log.e("BLE_RETURN_readSettings", it!!.contentToString())
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

    fun saveSettings(
        depth: Int,
        levelTop: Int,
        levelBottom: Int,
        cntMax: Int,
        cntMin: Int,
        escortMode: Int,
        netAddress: Int
    ) {
        replaceSettings(
            depth,
            levelTop,
            levelBottom,
            cntMax,
            cntMin,
            escortMode,
            netAddress
        )

        Log.e("BLE_SEND_readSettings", Sensor.fuelCacheSettings!!.getBytes().contentToString())
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
                    infoLiveSettings.postValue(Sensor.fuelCacheSettings)
                }, {
                    Log.e("BLE_DATA ", it.message.toString())
                    messageLiveData.postValue(MessageType.ERROR)
                    uiProgressLiveData.postValue(false)
                }).let { compositeDisposable.add(it) }
        }
    }

    private fun replaceSettings(
        filterDepth: Int,
        levelTop: Int,
        levelBottom: Int,
        cntMax: Int,
        cntMin: Int,
        escortMode: Int,
        netAddress: Int
    ) {
        Sensor.fuelCacheSettings!!.filter_depth = filterDepth
        Sensor.fuelCacheSettings!!.level_top = levelTop
        Sensor.fuelCacheSettings!!.level_bottom = levelBottom
        Sensor.fuelCacheSettings!!.cnt_max = cntMax
        Sensor.fuelCacheSettings!!.cnt_min = cntMin
        Sensor.fuelCacheSettings!!.escort = escortMode
        Sensor.fuelCacheSettings!!.net_address = netAddress
    }

    fun replaceEscort(escort: Int){
        Sensor.fuelCacheSettings!!.escort = escort
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