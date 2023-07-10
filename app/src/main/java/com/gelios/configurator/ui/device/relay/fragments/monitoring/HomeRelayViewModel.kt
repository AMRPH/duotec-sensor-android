package com.gelios.configurator.ui.device.relay.fragments.monitoring

import android.app.Application
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.util.Log
import androidx.lifecycle.MutableLiveData
import androidx.work.OneTimeWorkRequest
import androidx.work.WorkManager
import com.polidea.rxandroidble2.RxBleConnection
import com.polidea.rxandroidble2.RxBleDevice
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import com.gelios.configurator.MainPref
import com.gelios.configurator.ui.device.fuel.fragments.monitoring.HomeFuelViewModel
import com.gelios.configurator.entity.Sensor
import com.gelios.configurator.entity.Status
import com.gelios.configurator.ui.App
import com.gelios.configurator.ui.App.Companion.bleCompositeDisposable
import com.gelios.configurator.ui.base.BaseViewModel
import com.gelios.configurator.ui.datasensor.*
import com.gelios.configurator.entity.SensorParams
import com.gelios.configurator.util.BleHelper
import com.gelios.configurator.util.isConnected
import com.gelios.configurator.worker.RelayWorker
import com.polidea.rxandroidble2.Timeout
import java.util.*
import java.util.concurrent.TimeUnit

class HomeRelayViewModel(application: Application) : BaseViewModel(application) {

    companion object {
        const val MTU = 250
        const val WORKER_TAG = "DataWorker"
    }

    override val TAG: String
        get() = HomeFuelViewModel::class.java.simpleName

    val uiProgressLiveData = MutableLiveData<Boolean>()
    val errorLiveData = MutableLiveData(Pair(false, ""))
    val versionLiveData = MutableLiveData<String>()
    var broadcastReceiver: BroadcastReceiver? = null

    val dataLiveData = MutableLiveData<RelaySensorData>()
    val infoLiveData = MutableLiveData<RelaySensorInfo>()
    val rssiLiveData = MutableLiveData<Int>()
    val stateLiveData = MutableLiveData<Status>()
    val commandSendOk = MutableLiveData(false)

    var isDeviceConnected = false
        private set
        get() = device.isConnected

    private val device: RxBleDevice = App.rxBleClient.getBleDevice(MainPref.deviceMac)


    init {
        Sensor.type = Sensor.Type.RLY
        observeBleDeviceState()
        subscribeData()
    }

    private fun observeBleDeviceState() {
        device.observeConnectionStateChanges()
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe { updateState() }
            .let { compositeDisposable.add(it) }
    }

    fun initConnection() {
        uiProgressLiveData.postValue(true)
        Observable.defer {
            device.establishConnection(false, Timeout(30, TimeUnit.SECONDS))
        }
            .doOnNext { Log.e("BLE_DATA", it.toString()) }
            .compose(BleHelper.geMTUTransformer(MTU))
            .subscribe({
                if(!errorLiveData.value!!.first){
                    startWorker()
                    App.connection = it
                    uiProgressLiveData.postValue(false)
                    if (App.connection != null){
                        readSensor()
                    } else {
                        errorLiveData.postValue(Pair(true, ""))
                    }
                }
            }, {
                if(!errorLiveData.value!!.first){
                    if (App.connection != null) {
                        readSensor()
                    }
                    uiProgressLiveData.postValue(false)
                    if (("GATT_CONN_TIMEOUT" in it.message!!) or ("UNKNOWN" in it.message!!)){
                        errorLiveData.postValue(Pair(true, it.message.toString()))
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

    private fun readSensor() {
        updateState()
        getRSSI()

        if (Sensor.version == null) {
            readVersion()
        } else {

            if (Sensor.relayCacheData == null) {
                readData()
            } else {
                dataLiveData.postValue(Sensor.relayCacheData)
            }

            if (Sensor.relayCacheInfo == null) {
                readInfo()
            } else {
                infoLiveData.postValue(Sensor.relayCacheInfo)
            }

            if (Sensor.softVersion == null) {
                readSoftVersion()
            } else {
                versionLiveData.postValue(Sensor.softVersion)
            }

            if (Sensor.fuelCacheSettings == null) {
                readSettings()
            }
        }
    }

    private fun updateState() {
        when (device.connectionState) {
            RxBleConnection.RxBleConnectionState.CONNECTED -> {
                stateLiveData.postValue(Status.ONLINE)
            }
            RxBleConnection.RxBleConnectionState.DISCONNECTED -> {
                stateLiveData.postValue(Status.OFFLINE)
                initConnection()
            }
        }

    }

    fun getRSSI() {
        if (App.connection != null) {
            App.connection!!.readRssi()
                .repeatWhen { t -> t.delay(2, TimeUnit.SECONDS) }
                .subscribe(
                    {
                        rssiLiveData.postValue(it)
                    }, {
                        Log.e("BLE_ERROR RSSI", it.message.toString())
                    }
                ).let { compositeDisposable.add(it) }
        }
    }

    fun readData() {
        if (App.connection != null) {
            uiProgressLiveData.postValue(true)
            App.connection!!
                .readCharacteristic(UUID.fromString(SensorParams.RELAY_DATA.uuid))
                .subscribe({
                    uiProgressLiveData.postValue(false)
                    Sensor.relayCacheData = RelaySensorData(it)
                    dataLiveData.postValue(Sensor.relayCacheData)
                }, {
                    uiProgressLiveData.postValue(false)
                    Log.e("BLE_ERROR DATA", it.message.toString())
                }).let { compositeDisposable.add(it) }
        }
    }

    fun readInfo() {
        if (App.connection != null) {
            uiProgressLiveData.postValue(true)
            App.connection!!
                .readCharacteristic(UUID.fromString(SensorParams.THERM_INFO.uuid))
                .subscribe({
                    uiProgressLiveData.postValue(false)
                    Sensor.relayCacheInfo = RelaySensorInfo(it)
                    infoLiveData.postValue(Sensor.relayCacheInfo)
                }, {
                    uiProgressLiveData.postValue(false)
                    Log.e("BLE_ERROR INFO", it.message.toString())
                }).let { compositeDisposable.add(it) }
        }
    }

    fun readSoftVersion() {
        if (App.connection != null) {
            uiProgressLiveData.postValue(true)
            App.connection!!
                .readCharacteristic(UUID.fromString(SensorParams.SENSOR_VERSION.uuid))
                .subscribe({
                    uiProgressLiveData.postValue(false)
                    val s = String(it)
                    Sensor.softVersion = "${s[2]}.${s[3]}"
                    versionLiveData.postValue(Sensor.softVersion)
                }, {
                    uiProgressLiveData.postValue(false)
                    Log.e("BLE_ERROR VERSION", it.message.toString())
                }).let { compositeDisposable.add(it) }
        }
    }

    fun readVersion() {
        if (App.connection != null) {
            uiProgressLiveData.postValue(true)
            App.connection!!
                .readCharacteristic(UUID.fromString(SensorParams.SENSOR_TYPE.uuid))
                .subscribe({
                    uiProgressLiveData.postValue(false)
                    if (String(it).contains("v")){
                        Sensor.version = String(it).split("v")[1].toInt()
                    } else {
                        Sensor.version = 1
                    }
                    readSensor()
                }, {
                    uiProgressLiveData.postValue(false)
                    Log.e("BLE_ERROR TYPE", it.message.toString())
                }).let { compositeDisposable.add(it) }
        }
    }

    fun readSettings() {
        if (App.connection != null) {
            uiProgressLiveData.postValue(true)
            App.connection!!
                .readCharacteristic(UUID.fromString(SensorParams.RELAY_SETTINGS.uuid))
                .subscribe({
                    uiProgressLiveData.postValue(false)
                    val mData = FuelSensorSettings(it)
                    Sensor.fuelCacheSettings = mData
                }, {
                    uiProgressLiveData.postValue(false)
                    Log.e("BLE_ERROR SETTINGS", it.message.toString())
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
                    readData()
                    readSettings()
                }, {
                    Log.e("BLE_ERROR COMMAND", it.message.toString())
                    uiProgressLiveData.postValue(false)
                }).let { compositeDisposable.add(it) }
        }
    }

    fun startWorker() {
        if (device.isConnected) {
            val myWorkRequest = OneTimeWorkRequest.Builder(RelayWorker::class.java)
                .addTag(WORKER_TAG)
                .build()
            WorkManager.getInstance().enqueue(myWorkRequest)
        }
    }

    fun stopWorker() {
        WorkManager.getInstance().cancelAllWorkByTag(WORKER_TAG)
    }

    fun subscribeData() {
        if (broadcastReceiver == null) {
            broadcastReceiver = object : BroadcastReceiver() {
                override fun onReceive(context: Context?, intent: Intent?) {
                    val status = intent?.extras?.getBoolean(RelayWorker.BR_VALUE, false)
                    status?.let {
                        if (it) {
                            dataLiveData.postValue(Sensor.relayCacheData)
                        }
                    }
                }
            }

            val intFilter = IntentFilter(RelayWorker.BR_NAME)
            getApplication<Application>().registerReceiver(broadcastReceiver, intFilter)
        }
    }

    fun clearCache() {
        stopWorker()
        Sensor.clearSensorData()
        bleCompositeDisposable.clear()
        compositeDisposable.clear()
    }

}