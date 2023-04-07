package com.gelios.configurator.ui.device.relay.fragments.service_menu

import android.app.Application
import android.util.Log
import androidx.lifecycle.MutableLiveData
import androidx.work.WorkManager
import com.polidea.rxandroidble2.RxBleDevice
import io.reactivex.Observable
import com.gelios.configurator.MainPref
import com.gelios.configurator.entity.Sensor
import com.gelios.configurator.ui.App
import com.gelios.configurator.ui.App.Companion.bleCompositeDisposable
import com.gelios.configurator.ui.MessageType
import com.gelios.configurator.ui.base.BaseViewModel
import com.gelios.configurator.ui.datasensor.*
import com.gelios.configurator.entity.SensorParams
import com.gelios.configurator.util.BleHelper
import com.gelios.configurator.util.isConnected
import com.polidea.rxandroidble2.Timeout
import java.util.*
import java.util.concurrent.TimeUnit

class CommandsRelayViewModel(application: Application)  : BaseViewModel(application) {

    companion object {
        const val MTU = 250
        const val WORKER_TAG = "DataWorker"
    }

    val infoLiveData = MutableLiveData<RelaySensorInfo>()
    val uiProgressLiveData = MutableLiveData<Boolean>()
    val errorLiveData = MutableLiveData(false)
    val versionLiveData = MutableLiveData<String>()

    val messageLiveData = MutableLiveData<MessageType>()
    val uiActiveButton = MutableLiveData(false)
    val readyToUpdate = MutableLiveData<Boolean>()
    val commandSendOk = MutableLiveData(false)

    var upgrade = false

    override val TAG = "CommandsVM"
    private val device: RxBleDevice = App.rxBleClient.getBleDevice(MainPref.deviceMac)

    fun initVM() {
        if (App.connection != null) {
            if (Sensor.relayCacheInfo == null) {
                readInfo()
            } else {
                infoLiveData.postValue(Sensor.relayCacheInfo)
            }

            if (Sensor.sensorVersion == null) {
                readVersion()
            } else {
                versionLiveData.postValue(Sensor.sensorVersion)
            }

            if (Sensor.relayCacheSettings == null) {
                readSettings()
            }
        } else {
            initConnection()
        }
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
                            readInfo()
                        } else {
                            errorLiveData.postValue(true)
                        }
                    }
                           }, {
                    if(!errorLiveData.value!!){
                        if (device.isConnected && App.connection != null) {
                            readInfo()
                        }
                        uiProgressLiveData.postValue(false)
                        if (("GATT_CONN_TIMEOUT" in it.message!!) or ("UNKNOWN" in it.message!!)){
                            errorLiveData.postValue(true)
                        }
                        if ("GATT_CONN_TERMINATE_PEER_USER" in it.message!!){
                            initConnection()
                        }
                    }
                    Log.e("BLE_ERROR", it.message.toString())
            })?.let {
                bleCompositeDisposable.add(it)
            }
        }
    }

    fun readInfo() {
        if (App.connection != null) {
            uiProgressLiveData.postValue(true)
            App.connection!!
                .readCharacteristic(UUID.fromString(SensorParams.RELAY_INFO.uuid))
                .subscribe({
                    uiProgressLiveData.postValue(false)
                    val dData = RelaySensorInfo(it)
                    Sensor.relayCacheInfo = dData
                    infoLiveData.postValue(dData)
                    Log.e("BLE DATA ", it!!.contentToString())
                }, {
                    uiProgressLiveData.postValue(false)
                    Log.e("BLE_ERROR INFO", it.message.toString())
                    messageLiveData.postValue(MessageType.ERROR)
                }).let { compositeDisposable.add(it) }
        }
    }

    fun readVersion() {
        if (App.connection != null) {
            uiProgressLiveData.postValue(true)
            App.connection!!
                .readCharacteristic(UUID.fromString(SensorParams.SENSOR_VERSION.uuid))
                .subscribe({
                    uiProgressLiveData.postValue(false)
                    val sVersion = String(it)
                    Sensor.sensorVersion = sVersion
                }, {
                    uiProgressLiveData.postValue(false)
                    Log.e("BLE NAME", it.message.toString())
                }).let { compositeDisposable.add(it) }
        }
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
                    commandSendOk.postValue(true)
                    uiProgressLiveData.postValue(false)
                    if (it[0] == 2.toByte()) {
                        upgrade = true
                    }
                    readSettings()
                }, {
                    Log.e("BLE_ERROR COMMAND", it.message.toString())
                    uiProgressLiveData.postValue(false)
                }).let { compositeDisposable.add(it) }
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
                    Sensor.sensorAuthorized = true
                    uiProgressLiveData.postValue(false)
                    messageLiveData.postValue(MessageType.PASSWORD_ACCEPTED)
                    uiActiveButton.postValue(true)
                    Sensor.confirmedPass = newPass
                    readSettings()
                }, {
                    Log.e("BLE_ERROR PASSWORD", it.message.toString())
                    messageLiveData.postValue(MessageType.PASSWORD_NOT_ACCEPTED)
                    uiProgressLiveData.postValue(false)
                }).let { compositeDisposable.add(it) }
        } else {
            initConnection()
        }
    }

    fun changePassword(newPass: String) {
        Sensor.confirmedPass = newPass
        Sensor.relayCacheSettings?.applyMasterPassword(newPass)
        saveSettings()
    }

    fun saveSettings() {
        Sensor.relayCacheSettings!!.applyMasterPassword(Sensor.confirmedPass)
        if (App.connection != null) {
            uiProgressLiveData.postValue(true)
            App.connection!!
                .writeCharacteristic(
                    UUID.fromString(SensorParams.THERM_SETTINGS.uuid),
                    Sensor.relayCacheSettings!!.getBytes()
                )
                .subscribe({
                    messageLiveData.postValue(MessageType.PASSWORD_ACCEPTED)
                    uiProgressLiveData.postValue(false)
                }, {
                    Log.e("BLE_ERROR SETTINGS", it.message.toString())
                    messageLiveData.postValue(MessageType.PASSWORD_NOT_ACCEPTED)
                    uiProgressLiveData.postValue(false)
                }).let { compositeDisposable.add(it) }
        }
    }

    fun readSettings() {
        if (App.connection != null) {
            uiProgressLiveData.postValue(true)
            App.connection!!
                .readCharacteristic(UUID.fromString(SensorParams.RELAY_SETTINGS.uuid))
                .subscribe({
                    Sensor.relayCacheSettings = RelaySensorSettings(it)
                    uiProgressLiveData.postValue(false)
                }, {
                    uiProgressLiveData.postValue(false)
                    Log.e("BLE_ERROR SETTINGS", it.message.toString())
                    messageLiveData.postValue(MessageType.ERROR)
                    if (upgrade) {
                        readyToUpdate.postValue(true)
                    }
                }).let { compositeDisposable.add(it) }
        } else {
            initConnection()
        }
    }

    fun checkAuth() {
        uiActiveButton.postValue(Sensor.sensorAuthorized)
    }

    private fun stopWorker() {
        WorkManager.getInstance().cancelAllWorkByTag(WORKER_TAG)
    }

    fun clearCache() {
        stopWorker()
        Sensor.clearSensorData()
        bleCompositeDisposable.clear()
        compositeDisposable.clear()
    }
}