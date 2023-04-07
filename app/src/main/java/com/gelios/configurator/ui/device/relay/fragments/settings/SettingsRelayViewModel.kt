package com.gelios.configurator.ui.device.relay.fragments.settings

import android.app.Application
import android.util.Log
import androidx.lifecycle.MutableLiveData
import com.polidea.rxandroidble2.RxBleDevice
import io.reactivex.Observable
import com.gelios.configurator.MainPref
import com.gelios.configurator.entity.Sensor
import com.gelios.configurator.ui.App
import com.gelios.configurator.ui.App.Companion.bleCompositeDisposable
import com.gelios.configurator.ui.MessageType
import com.gelios.configurator.ui.base.BaseViewModel
import com.gelios.configurator.ui.datasensor.RelaySensorSettings
import com.gelios.configurator.entity.SensorParams
import com.gelios.configurator.util.BleHelper
import com.gelios.configurator.util.isConnected
import com.polidea.rxandroidble2.Timeout
import java.util.*
import java.util.concurrent.TimeUnit

class SettingsRelayViewModel(application: Application) : BaseViewModel(application) {

    companion object {
        const val MTU = 250
    }

    override val TAG = "SettingsRelayViewModel"

    val uiProgressLiveData = MutableLiveData<Boolean>()
    val errorLiveData = MutableLiveData<Boolean>(false)
    val uiActiveButtons = MutableLiveData<Boolean>(false)
    val infoLiveSettings = MutableLiveData<RelaySensorSettings>()
    val messageLiveData = MutableLiveData<MessageType>()
    val commandSendOk = MutableLiveData(false)

    val device: RxBleDevice = App.rxBleClient.getBleDevice(MainPref.deviceMac)


    init {
        if (Sensor.relayCacheSettings == null) {
            readSettings()
        } else {
            infoLiveSettings.postValue(Sensor.relayCacheSettings)
        }
    }

    fun checkAuth() {
        if (Sensor.sensorAuthorized) uiActiveButtons.postValue(true)
    }

    fun initConnection() {
        if (App.connection != null) {
            uiProgressLiveData.postValue(true)
            Observable
                .defer { device.establishConnection(false, Timeout(30, TimeUnit.SECONDS)) }
                .doOnNext { Log.e("BLE_DATA ", it.toString()) }
                .compose(BleHelper.geMTUTransformer(250))
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
        if (App.connection != null) {
            val passByte = newPass.toByteArray()
            uiProgressLiveData.postValue(true)
            App.connection!!
                .writeCharacteristic(
                    UUID.fromString(SensorParams.RELAY_PASSWORD.uuid),
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
        if (App.connection != null) {
            uiProgressLiveData.postValue(true)
            App.connection!!
                .readCharacteristic(UUID.fromString(SensorParams.RELAY_SETTINGS.uuid))
                .subscribe({
                    val data = RelaySensorSettings(it)
                    infoLiveSettings.postValue(data)
                    Sensor.relayCacheSettings = data
                    Log.e("BLE_ERROR SETTINGS", it!!.contentToString())
                    uiProgressLiveData.postValue(false)
                    Log.e("BLE_ERROR SETTINGS", Sensor.relayCacheSettings!!.getBytes().contentToString())
                }, {
                    uiProgressLiveData.postValue(false)
                    Log.e("BLE_ERROR SETTINGS", it.message.toString())
                }).let { compositeDisposable.add(it) }
        } else {
            initConnection()
        }
    }

    fun saveSettings() {
        Sensor.relayCacheSettings!!.applyMasterPassword(Sensor.confirmedPass)
        if (App.connection != null) {
            uiProgressLiveData.postValue(true)
            App.connection!!
                .writeCharacteristic(
                    UUID.fromString(SensorParams.RELAY_SETTINGS.uuid),
                    Sensor.relayCacheSettings!!.getBytes()
                )
                .subscribe({
                    messageLiveData.postValue(MessageType.SAVED)
                    uiProgressLiveData.postValue(false)
                    infoLiveSettings.postValue(Sensor.relayCacheSettings)
                }, {
                    Log.e("BLE_ERROR SETTINGS", it.message.toString())
                    messageLiveData.postValue(MessageType.ERROR)
                    uiProgressLiveData.postValue(false)
                }).let { compositeDisposable.add(it) }
        }
    }

    fun replaceSettings(escort_mode: Int) {
        Sensor.relayCacheSettings!!.escort = escort_mode
    }

    fun sendCommand(intByte: Byte) {
        val command: Byte = intByte.toByte()
        if (App.connection != null) {
            uiProgressLiveData.postValue(true)
            App.connection!!
                .writeCharacteristic(
                    UUID.fromString(SensorParams.RELAY_COMMAND.uuid),
                    byteArrayOf(command)
                )
                .subscribe({
                    uiProgressLiveData.postValue(false)
                    val byte = it[0].toInt()
                    when(byte) {
                        7 ->  messageLiveData.postValue(MessageType.OPEN_RELAY)
                        8 ->  messageLiveData.postValue(MessageType.CLOSE_RELAY)
                    }
                }, {
                    Log.e("BLE_RETURN_sendCommand", it.message.toString())
                    uiProgressLiveData.postValue(false)
                    commandSendOk.postValue(true)
                }).let { compositeDisposable.add(it) }
        }
    }

}