package com.gelios.configurator.ui.device.fuel.fragments.tarirovka

import android.app.Application
import android.content.*
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Environment
import android.util.Log
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import androidx.work.OneTimeWorkRequest
import androidx.work.WorkManager
import com.gelios.configurator.BuildConfig
import com.gelios.configurator.MainPref
import com.gelios.configurator.entity.Sensor
import com.gelios.configurator.ui.App
import com.gelios.configurator.ui.base.BaseViewModel
import com.gelios.configurator.ui.datasensor.differentTime
import com.gelios.configurator.ui.net.RetrofitClient
import com.gelios.configurator.util.isConnected
import com.gelios.configurator.worker.FuelLevelWorker
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.polidea.rxandroidble2.RxBleDevice
import io.reactivex.Observable
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.Response
import java.io.OutputStream
import java.nio.charset.Charset
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit


class TarirovkaViewModel(application: Application) : BaseViewModel(application) {

    override val TAG = "TarirovkaViewModel"



    val uiProgressLiveData = MutableLiveData<Boolean>()
    val uiAuthorization = MutableLiveData<Boolean>()

    val errorMessageLiveData = MutableLiveData<String>()
    val dataLiveData = MutableLiveData<Int>()

    val device: RxBleDevice = App.rxBleClient.getBleDevice(MainPref.deviceMac)

    val uiTableTarirovka = MutableLiveData<List<DataTarirovka>>()

    val uiFuelLevel = MutableLiveData<Int>(0)
    val uiFuelPercent = MutableLiveData<Double>(0.0)
    val uiFuelStability = MutableLiveData<Boolean>(null)


    val tableLevels = mutableListOf<DataTarirovka>()

    var broadcastReceiver: BroadcastReceiver? = null

    var tarirovkaStep = MutableLiveData(MainPref.stepFuel)

    var textComment = ""

    init {
        setFirstData()
        subscribeData()
        initLevelTable()
        updateStabilityIndicator()
    }

    private fun setFirstData() {
        Sensor.fuelCacheData?.let {
            uiFuelLevel.postValue(it.fuel)
            val fuel = (Sensor.fuelCacheData!!.fuelPercent * 100)
            if (fuel > 100) {
                uiFuelPercent.postValue(0.0)
            } else {
                uiFuelPercent.postValue(fuel)
            }
        }
    }

    fun sendDataToServer() {
        var lov = ""
        var lol = ""
        for (arrow in tableLevels) {
            lol = "${lol}${arrow.fuelLevel},"
            lov = "${lov}${arrow.sensorLevel},"
        }
        RetrofitClient.getApi()
            .sensorTarrirovka(
                "1",
                MainPref.deviceMac.replace(":", ""),
                lov,
                lol,
                textComment
            ).enqueue(object : retrofit2.Callback<ResponseBody> {
                override fun onResponse(call: Call<ResponseBody>, response: Response<ResponseBody>) {
                    Log.d("INET sensorTarrirovka", response.body()!!.string())
                }

                override fun onFailure(call: Call<ResponseBody>, t: Throwable) {
                    Log.d("INET sensorTarrirovka", t.message!!)
                }
            })
    }


    private fun calculateStability(messageTime: Date): Boolean {
        val timeNow = Calendar.getInstance().time
        return timeNow.time - messageTime.time > differentTime
    }

    fun initLevelTable() {
        loadTarirovkaInPreffArray()
        if (tableLevels.isEmpty()) {
            tableLevels.add(
                DataTarirovka(
                    fuelLevel = "0",
                    sensorLevel = Sensor.fuelCacheData?.fuel.toString()
                )
            )

        }
    }

    fun updateStabilityIndicator() {
        if (Sensor.fuelCacheData?.messageTime != null) {
            uiFuelStability.postValue(calculateStability(Sensor.fuelCacheData?.messageTime!!))
        }
        Observable.interval(3, TimeUnit.SECONDS)
            .subscribe {
                if (Sensor.fuelCacheData?.messageTime != null) {
                    uiFuelStability.postValue(calculateStability(Sensor.fuelCacheData?.messageTime!!))
                }
            }.let {
                compositeDisposable.add(it)
            }
    }

    fun startWorker() {
        if (device.isConnected) {
            val myWorkRequest = OneTimeWorkRequest.Builder(FuelLevelWorker::class.java).build()
            WorkManager.getInstance().enqueue(myWorkRequest)
        }
    }

    fun subscribeData() {
        if (broadcastReceiver == null) {
            broadcastReceiver = object : BroadcastReceiver() {
                override fun onReceive(context: Context?, intent: Intent?) {
                    val status = intent?.extras?.getBoolean(FuelLevelWorker.BR_VALUE, false)
                    status?.let {
                        if (it) {
                            if (tableLevels.isEmpty()) {
                                tableLevels.add(
                                    DataTarirovka(
                                        fuelLevel = tarirovkaStep.toString(),
                                        sensorLevel = Sensor.fuelCacheData?.fuel.toString()
                                    )
                                )
                            } else {
                                tableLevels.last().sensorLevel = Sensor.fuelCacheData?.fuel.toString()
                            }
                            uiFuelLevel.postValue(Sensor.fuelCacheData?.fuel)

                            if (Sensor.fuelCacheData != null){
                                val fuel = (Sensor.fuelCacheData!!.fuelPercent * 100)
                                uiFuelPercent.postValue(fuel)
                            }

                            uiTableTarirovka.postValue(tableLevels)
                            saveTarirovkaInPreffArray()
                        }
                    }
                }
            }

            val intFilt = IntentFilter(FuelLevelWorker.BR_NAME)
            getApplication<Application>().registerReceiver(broadcastReceiver, intFilt)
        }

    }

    fun addArrow() {
        var lastLevel = 0
        if (tableLevels.isNotEmpty()) {
            lastLevel = tableLevels.last().fuelLevel.toInt()
        }
        tableLevels.add(
            DataTarirovka(
                fuelLevel = (lastLevel + tarirovkaStep.value!!).toString(),
                sensorLevel = Sensor.fuelCacheData?.fuel.toString()
            )
        )
        uiTableTarirovka.postValue(tableLevels)
        saveTarirovkaInPreffArray()
    }


    fun removeArrow() {
        if (tableLevels.size > 1) {
            val removed = tableLevels.last()
            tableLevels.remove(removed)
            uiTableTarirovka.postValue(tableLevels)
            saveTarirovkaInPreffArray()
        }
    }

    fun clearTable() {
        tableLevels.clear()
        tableLevels.add(
            DataTarirovka(
                fuelLevel = 0.toString(),
                sensorLevel = Sensor.fuelCacheData?.fuel.toString()
            )
        )
        uiTableTarirovka.postValue(tableLevels)
        saveTarirovkaInPreffArray()
    }

    fun setStep(step: Int) {
        tarirovkaStep.postValue(step)
    }

    fun changeFuelValue(data: DataTarirovka) {
        val line = tableLevels.get(data.counter-1)
        line.fuelLevel = data.fuelLevel
        line.sensorLevel = data.sensorLevel
        uiTableTarirovka.postValue(tableLevels)
        saveTarirovkaInPreffArray()
    }

    fun getSavedString(): String {
        val mac = MainPref.deviceMac
        val date = SimpleDateFormat("dd-MM-yyyy HH:mm", Locale.getDefault()).format(Date())
        val tempVal = Sensor.fuelCacheData?.temperatura.toString()
        val temperatura = "${tempVal} °C"
        val voltVal = Sensor.fuelCacheInfo?.voltageDouble
        val voltage = String.format("%.2f В", voltVal)
        val swVersionSensor = Sensor.softVersion ?: "-"
        val sWApp = BuildConfig.VERSION_NAME
        val depth: String = Sensor.fuelCacheSettings?.filter_depth?.toString() ?: "-"
        val cntMax = Sensor.fuelCacheSettings?.cnt_max?.toString() ?: "-"
        val cntMin = Sensor.fuelCacheSettings?.cnt_min?.toString() ?: "-"




        val outString = StringBuilder()
        outString.append("MAC адрес: $mac\n")
        outString.append("Дата: $date\n")
        outString.append("Температура: $temperatura\n")
        outString.append("Напряжение батареи: $voltage\n")
        outString.append("Версия ПО датчика: $swVersionSensor\n")
        outString.append("Версия ПО приложения: $sWApp\n")
        outString.append("Глубина фильтрации: $depth\n")
        outString.append("Счетчик \"Пустой\": $cntMin\n")
        outString.append("Счетчик \"Полный\": $cntMax\n")
        outString.append("Комментарий: $textComment\n\n")

        outString.append("Уровень\t\tЛитры\n")
        for (item in tableLevels) {
            outString.append(" ${item.sensorLevel}\t\t${item.fuelLevel}")
            outString.append("\n")
        }

        return outString.toString()
    }

    fun saveTarirovkaInPreffArray() {
        var str = Gson().toJson(tableLevels)
        MainPref.tatirovkaValue = str
    }

    fun loadTarirovkaInPreffArray() {
        try {
            val listType = object : TypeToken<List<DataTarirovka>>() {}.type
            val inArray = Gson().fromJson<List<DataTarirovka>>(MainPref.tatirovkaValue, listType)
            tableLevels.addAll(inArray)
            uiTableTarirovka.postValue(tableLevels)
        } catch (e: RuntimeException) {

        }
    }


    fun write(source: Uri) {

        viewModelScope.launch(Dispatchers.Main) {
            write(getApplication(), source, getSavedString())
        }
    }

    private suspend fun write(context: Context, source: Uri, text: String) =
        withContext(Dispatchers.IO) {
            val resolver: ContentResolver = context.contentResolver

            try {
                resolver.openOutputStream(source)?.use { stream ->
                    stream.writeText(text)

                    val externalRoot =
                        Environment.getExternalStorageDirectory().absolutePath

                    if (source.scheme == "file" && source.path!!.startsWith(externalRoot)) {
                        MediaScannerConnection
                            .scanFile(
                                context,
                                arrayOf(source.path),
                                arrayOf("text/plain"),
                                null
                            )
                    }

                } ?: throw IllegalStateException("could not open $source")
            } catch (t: Throwable) {
                errorMessageLiveData.postValue(t.message)
            }
        }
}

private fun OutputStream.writeText(
    text: String,
    charset: Charset = Charsets.UTF_8
): Unit = write(text.toByteArray(charset))