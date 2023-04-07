package com.gelios.configurator.ui.device.therm.fragments.monitoring

import android.app.Application
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.ConnectivityManager
import android.os.Build
import android.util.Log
import androidx.lifecycle.MutableLiveData
import androidx.work.OneTimeWorkRequest
import androidx.work.WorkManager
import com.gelios.configurator.BuildConfig
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
import com.gelios.configurator.ui.net.RetrofitClient
import com.gelios.configurator.util.BleHelper
import com.gelios.configurator.util.isConnected
import com.gelios.configurator.worker.ThermWorker
import com.polidea.rxandroidble2.Timeout
import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.Response
import java.util.*
import java.util.concurrent.TimeUnit

class HomeThermometerViewModel(application: Application) : BaseViewModel(application) {

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

    val dataLiveData = MutableLiveData<ThermSensorData>()
    val infoLiveData = MutableLiveData<ThermSensorInfo>()
    val rssiLiveData = MutableLiveData<Int>()
    val batteryLiveData = MutableLiveData<Int>()
    val stateLiveData = MutableLiveData<Status>()

    var timer = Timer()

    var isDeviceConnected = false
        private set
        get() = device.isConnected

    private val device: RxBleDevice = App.rxBleClient.getBleDevice(MainPref.deviceMac)

    init {
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
        if (!device.isConnected) {
            uiProgressLiveData.postValue(true)
            Observable
                .defer { device.establishConnection(false, Timeout(30, TimeUnit.SECONDS)) }
                .doOnNext { Log.e("BLE_DATA ", it.toString()) }
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
    }

    private fun readSensor() {
        updateState()
        getRSSI()

        if (Sensor.sensorType == null) {
            readType()
        } else {

            if (Sensor.thermCacheData == null) {
                readData()
            } else {
                dataLiveData.postValue(Sensor.thermCacheData)
            }

            if (Sensor.thermCacheInfo == null) {
                readInfo()
            } else {
                infoLiveData.postValue(Sensor.thermCacheInfo)
            }

            if (Sensor.sensorVersion == null) {
                readVersion()
            } else {
                versionLiveData.postValue(Sensor.sensorVersion)
            }

            if (Sensor.thermCacheSettings == null) {
                readSettings()
            }

            if (Sensor.sensorBattery == null) {
                readBattery()
            } else {
                batteryLiveData.postValue(Sensor.sensorBattery)
            }

            startTimer()
        }
    }

    private fun updateState() {
        when (device.connectionState) {
            RxBleConnection.RxBleConnectionState.CONNECTED -> {
                Log.d("STATE", device.connectionState.name)
                stateLiveData.postValue(Status.ONLINE)
            }
            RxBleConnection.RxBleConnectionState.DISCONNECTED -> {
                Log.d("STATE", device.connectionState.name)
                stateLiveData.postValue(Status.OFFLINE)
                initConnection()
            }
        }

    }

    fun getRSSI() {
        if (device.isConnected) {
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
        if (device.isConnected) {
            uiProgressLiveData.postValue(true)
            App.connection!!
                .readCharacteristic(UUID.fromString(SensorParams.THERM_DATA.uuid))
                .subscribe({
                    uiProgressLiveData.postValue(false)
                    Sensor.thermCacheData = ThermSensorData(it)
                    dataLiveData.postValue(Sensor.thermCacheData)
                }, {
                    uiProgressLiveData.postValue(false)
                    Log.e("BLE_ERROR DATA", it.message.toString())
                }).let { compositeDisposable.add(it) }
        }
    }

    fun readInfo() {
        if (device.isConnected) {
            uiProgressLiveData.postValue(true)
            App.connection!!
                .readCharacteristic(UUID.fromString(SensorParams.THERM_INFO.uuid))
                .subscribe({
                    uiProgressLiveData.postValue(false)
                    Sensor.thermCacheInfo = ThermSensorInfo(it)
                    infoLiveData.postValue(Sensor.thermCacheInfo)
                }, {
                    uiProgressLiveData.postValue(false)
                    Log.e("BLE_ERROR INFO", it.message.toString())
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
                    val s = String(it)
                    Sensor.sensorVersion = "${s[2]}.${s[3]}"
                    versionLiveData.postValue(Sensor.sensorVersion)
                }, {
                    uiProgressLiveData.postValue(false)
                    Log.e("BLE_ERROR VERSION", it.message.toString())
                }).let { compositeDisposable.add(it) }
        }
    }

    fun readType() {
        if (App.connection != null) {
            uiProgressLiveData.postValue(true)
            App.connection!!
                .readCharacteristic(UUID.fromString(SensorParams.SENSOR_TYPE.uuid))
                .subscribe({
                    uiProgressLiveData.postValue(false)
                    Sensor.sensorType = Sensor.Type.valueOf(String(it))
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
                        Sensor.sensorBattery = it[0].toInt()
                        batteryLiveData.postValue(Sensor.sensorBattery)
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
                .readCharacteristic(UUID.fromString(SensorParams.THERM_SETTINGS.uuid))
                .subscribe({
                    uiProgressLiveData.postValue(false)
                    Sensor.thermCacheSettings = ThermSensorSettings(it)
                }, {
                    uiProgressLiveData.postValue(false)
                    Log.e("BLE_ERROR SETTINGS", it.message.toString())
                }).let { compositeDisposable.add(it) }
        }
    }

    fun startWorker() {
        if (device.isConnected) {
            val myWorkRequest = OneTimeWorkRequest.Builder(ThermWorker::class.java)
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
                    val status = intent?.extras?.getBoolean(ThermWorker.BR_VALUE, false)
                    status?.let {
                        if (it) {
                            dataLiveData.postValue(Sensor.thermCacheData)
                        }
                    }
                }
            }

            val intFilt = IntentFilter(ThermWorker.BR_NAME)
            getApplication<Application>().registerReceiver(broadcastReceiver, intFilt)
        }
    }


    fun startTimer() {
        timer.scheduleAtFixedRate(object :TimerTask(){
            override fun run() {
                readInfo()
            }
        }, 0, 60000)
    }

    fun stopTimer() {
        timer.cancel()
        timer = Timer()
    }

    fun clearCache() {
        stopWorker()
        Sensor.clearSensorData()
        bleCompositeDisposable.clear()
        compositeDisposable.clear()
        stopTimer()
    }
}