package com.gelios.configurator.ui.device.fuel.fragments.monitoring

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
import com.gelios.configurator.entity.Sensor
import com.gelios.configurator.ui.App
import com.gelios.configurator.ui.App.Companion.bleCompositeDisposable
import com.gelios.configurator.ui.base.BaseViewModel
import com.gelios.configurator.ui.datasensor.FuelSensorData
import com.gelios.configurator.ui.datasensor.FuelSensorInfo
import com.gelios.configurator.ui.datasensor.FuelSensorSettings
import com.gelios.configurator.ui.datasensor.differentTime
import com.gelios.configurator.entity.Status
import com.gelios.configurator.entity.SensorParams
import com.gelios.configurator.util.BleHelper
import com.gelios.configurator.util.isConnected
import com.gelios.configurator.worker.FuelLevelWorker
import com.polidea.rxandroidble2.Timeout
import java.util.*
import java.util.concurrent.TimeUnit

class HomeFuelViewModel(application: Application) : BaseViewModel(application) {

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

    val dataLiveData = MutableLiveData<FuelSensorData>()
    val infoLiveData = MutableLiveData<FuelSensorInfo>()
    val rssiLiveData = MutableLiveData<Int>()
    val batteryLiveData = MutableLiveData<Int>()
    val stateLiveData = MutableLiveData<Status>()
    val uiFuelStability = MutableLiveData<Boolean>(null)

    var isDeviceConnected = false
        private set
        get() = device.isConnected

    private val device: RxBleDevice = App.rxBleClient.getBleDevice(MainPref.deviceMac)


    init {
        Sensor.type = Sensor.Type.LLS
        observeBleDeviceState()
        updateStabilityIndicator()
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
                    if (device.isConnected && App.connection != null) {
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

            if (Sensor.fuelCacheData == null) {
                readData()
            } else {
                dataLiveData.postValue(Sensor.fuelCacheData)
            }

            if (Sensor.fuelCacheInfo == null) {
                readInfo()
            } else {
                infoLiveData.postValue(Sensor.fuelCacheInfo)
            }

            if (Sensor.softVersion == null) {
                readSoftVersion()
            } else {
                versionLiveData.postValue(Sensor.softVersion)
            }

            if (Sensor.fuelCacheSettings == null) {
                readSettings()
            }

            if (Sensor.battery == null) {
                readBattery()
            } else {
                batteryLiveData.postValue(Sensor.battery)
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


    private fun updateStabilityIndicator() {
        if (Sensor.fuelCacheData?.messageTime != null) {
            uiFuelStability.postValue(calculateStability(Sensor.fuelCacheData?.messageTime))
        }

        Observable.interval(5, TimeUnit.SECONDS)
            .subscribe {
                if (Sensor.fuelCacheData?.messageTime != null) {
                    uiFuelStability.postValue(calculateStability(Sensor.fuelCacheData?.messageTime))
                    stateLiveData.postValue(checkStatus())
                }
            }.let {
                compositeDisposable.add(it)
            }
    }

    private fun checkStatus(): Status {
        var isStable = calculateStability(Sensor.fuelCacheData?.messageTime)

        return when {
            (Sensor.fuelCacheSettings?.cnt_max == 1000000) || (Sensor.fuelCacheSettings?.cnt_min == 100 ) -> Status.NEED_CALIBRATION
            isStable -> Status.STABLY
            !isStable -> Status.STABILISATION
            else -> Status.UNKNOWN
        }
    }



    private fun calculateStability(messageTime: Date?): Boolean {
        var result = false
        try {
            val timeNow = Calendar.getInstance().time
            result = timeNow.time - messageTime!!.time > differentTime
        } catch (e: RuntimeException) {

        }

        return result
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
                .readCharacteristic(UUID.fromString(SensorParams.FUEL_DATA.uuid))
                .subscribe({
                    uiProgressLiveData.postValue(false)
                    Sensor.fuelCacheData = FuelSensorData(it)
                    dataLiveData.postValue(Sensor.fuelCacheData)
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
                .readCharacteristic(UUID.fromString(SensorParams.FUEL_INFO.uuid))
                .subscribe({
                    uiProgressLiveData.postValue(false)
                    val dData = FuelSensorInfo(it)
                    Sensor.fuelCacheInfo = dData
                    infoLiveData.postValue(dData)
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
                    Sensor.version = String(it).split("v")[1].toInt()
                    readSensor()
                }, {
                    uiProgressLiveData.postValue(false)
                    Log.e("BLE_ERROR TYPE", it.message.toString())
                }).let { compositeDisposable.add(it) }
        }
    }

    fun readBattery() {
        if (App.connection != null) {
            uiProgressLiveData.postValue(true)
            App.connection!!
                .readCharacteristic(UUID.fromString(SensorParams.SENSOR_BATTERY.uuid))
                .subscribe({
                    uiProgressLiveData.postValue(false)
                    if (it.isNotEmpty()){
                        Sensor.flagSensorBattery = true
                        Sensor.battery = it[0].toInt()
                        batteryLiveData.postValue(Sensor.battery)
                    }
                }, {
                    uiProgressLiveData.postValue(false)
                    Log.e("BLE_ERROR BATTERY", it.message.toString())
                }).let { compositeDisposable.add(it) }
        }
    }

    fun readSettings() {
        if (App.connection != null) {
            uiProgressLiveData.postValue(true)
            App.connection!!
                .readCharacteristic(UUID.fromString(SensorParams.FUEL_SETTINGS.uuid))
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

    fun startWorker() {
        if (device.isConnected) {
            val myWorkRequest = OneTimeWorkRequest.Builder(FuelLevelWorker::class.java)
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
                    val status = intent?.extras?.getBoolean(FuelLevelWorker.BR_VALUE, false)
                    status?.let {
                        if (it) {
                            dataLiveData.postValue(Sensor.fuelCacheData)
                        }
                    }
                }
            }

            val intFilt = IntentFilter(FuelLevelWorker.BR_NAME)
            getApplication<Application>().registerReceiver(broadcastReceiver, intFilt)
        }

    }

    fun clearCache() {
        stopWorker()
        Sensor.clearSensorData()
        bleCompositeDisposable.clear()
        compositeDisposable.clear()
    }

}