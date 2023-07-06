package com.gelios.configurator.ui.device.therm.fragments.settings

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
import com.gelios.configurator.ui.datasensor.ThermSensorSettings
import com.gelios.configurator.entity.SensorParams
import com.gelios.configurator.ui.datasensor.ThermSensorSettings2
import com.gelios.configurator.util.BinHelper
import com.gelios.configurator.util.BleHelper
import com.gelios.configurator.util.isConnected
import com.polidea.rxandroidble2.Timeout
import java.util.*
import java.util.concurrent.TimeUnit

class SettingsThermometerViewModel(application: Application) : BaseViewModel(application) {

    companion object {
        const val MTU = 250

    }

    val settingsLiveData = MutableLiveData<ThermSensorSettings>()
    val settings2LiveData = MutableLiveData<ThermSensorSettings2>()
    val uiProgressLiveData = MutableLiveData<Boolean>()
    val errorLiveData = MutableLiveData(false)

    val uiActiveButtons = MutableLiveData(false)
    val messageLiveData = MutableLiveData<MessageType>()
    val commandSendOk = MutableLiveData(false)

    override val TAG = "SettingsViewModel"
    val device: RxBleDevice = App.rxBleClient.getBleDevice(MainPref.deviceMac)

    var flagUUIDCorrect: Boolean = true
    var flagMAJORCorrect: Boolean = true
    var flagMINORCorrect: Boolean = true

    init {
        if (Sensor.thermCacheSettings == null) {
            readSettings()
        } else {
            settingsLiveData.postValue(Sensor.thermCacheSettings)
        }

        if (Sensor.version!! >= 5){
            if (Sensor.thermCacheSettings2 == null) {
                readSettings2()
            } else {
                settings2LiveData.postValue(Sensor.thermCacheSettings2)
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
                .defer { device.establishConnection(false, Timeout(30, TimeUnit.SECONDS))}
                .doOnNext { Log.e("BLE_DATA ", it.toString()) }
                .compose(BleHelper.geMTUTransformer(250))
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
                Log.e("BLE PARAMETRS", it.message.toString())
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
                    UUID.fromString(SensorParams.THERM_PASSWORD.uuid),
                    passByte
                )
                .subscribe({
                    Log.e("BLE_DATA", it!!.contentToString())
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
                    Log.e("BLE_DATA", it.message.toString())
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
                .readCharacteristic(UUID.fromString(SensorParams.THERM_SETTINGS.uuid))
                .subscribe({
                    Sensor.thermCacheSettings = ThermSensorSettings(it)
                    settingsLiveData.postValue(Sensor.thermCacheSettings)
                    Log.d("BLE_ERROR SETTINGS", it!!.contentToString())
                    uiProgressLiveData.postValue(false)
                }, {
                    uiProgressLiveData.postValue(false)
                    Log.e("BLE_ERROR SETTINGS", it.message.toString())
                }).let { compositeDisposable.add(it) }
        } else {
            initConnection()
        }
    }

    fun saveSettings() {
        Sensor.thermCacheSettings!!.applyMasterPassword(Sensor.confirmedPass)
        if (device.isConnected) {
            uiProgressLiveData.postValue(true)
            App.connection!!
                .writeCharacteristic(
                    UUID.fromString(SensorParams.THERM_SETTINGS.uuid),
                    Sensor.thermCacheSettings!!.getBytes()
                )
                .subscribe({
                    messageLiveData.postValue(MessageType.SAVED)
                    uiProgressLiveData.postValue(false)
                    settingsLiveData.postValue(Sensor.thermCacheSettings)
                }, {
                    Log.e("BLE_ERROR SETTINGS", it.message.toString())
                    messageLiveData.postValue(MessageType.ERROR)
                    uiProgressLiveData.postValue(false)
                }).let { compositeDisposable.add(it) }
        }
    }


    fun readSettings2() {
        if (device.isConnected) {
            uiProgressLiveData.postValue(true)
            App.connection!!
                .readCharacteristic(UUID.fromString(SensorParams.THERM_SETTINGS2.uuid))
                .subscribe({
                    Sensor.thermCacheSettings2 = ThermSensorSettings2(it)
                    settings2LiveData.postValue(Sensor.thermCacheSettings2)
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
                    UUID.fromString(SensorParams.THERM_SETTINGS2.uuid),
                    Sensor.thermCacheSettings2!!.getBytes()
                )
                .subscribe({
                    messageLiveData.postValue(MessageType.SAVED)
                    uiProgressLiveData.postValue(false)
                    settings2LiveData.postValue(Sensor.thermCacheSettings2)
                }, {
                    Log.e("BLE_ERROR SETTINGS2", it.message.toString())
                    messageLiveData.postValue(MessageType.ERROR)
                    uiProgressLiveData.postValue(false)
                }).let { compositeDisposable.add(it) }
        }
    }

    fun changeProtocol(flag: Int) {
        Sensor.thermCacheSettings!!.flag = flag
    }

    fun changeInterval(interval: Int) {
        Sensor.thermCacheSettings2!!.adv_interval = interval
    }

    fun changePower(power: Int) {
        Sensor.thermCacheSettings2!!.adv_power_mode = power
    }

    fun changeBeacon(beacon: Int) {
        Sensor.thermCacheSettings2!!.adv_beacon = beacon
    }

    fun replaceSettings2(uuid: String, major: Int, minor: Int) {
        Sensor.thermCacheSettings2!!.uuid = BinHelper.toByteArray(uuid)
        Sensor.thermCacheSettings2!!.major = BinHelper.intToUInt16ByteArray(major)
        Sensor.thermCacheSettings2!!.minor = BinHelper.intToUInt16ByteArray(minor)
    }
}