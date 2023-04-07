package com.gelios.configurator.worker

import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.polidea.rxandroidble2.RxBleDevice
import io.reactivex.Observable
import io.reactivex.disposables.CompositeDisposable
import com.gelios.configurator.MainPref
import com.gelios.configurator.entity.Sensor
import com.gelios.configurator.ui.App
import com.gelios.configurator.ui.datasensor.FuelSensorData
import com.gelios.configurator.entity.SensorParams
import com.gelios.configurator.util.isConnected
import java.util.*

class FuelLevelWorker(private val context: Context, workerParams: WorkerParameters) :
    Worker(context, workerParams) {

    companion object {
        val BR_NAME = "BR_NAME"
        val BR_VALUE = "BR_VALUE"
    }


    val TAG = "Worker"
    private val compositeDisposable: CompositeDisposable by lazy { CompositeDisposable() }
    private val device: RxBleDevice = App.rxBleClient.getBleDevice(MainPref.deviceMac)

    override fun doWork(): Result {
        subscribeData()
        return Result.success()
    }


    fun subscribeData() {
        if (device.isConnected) {
            App.connection!!
                .setupNotification(UUID.fromString(SensorParams.FUEL_DATA.uuid))
                .flatMap { t: Observable<ByteArray> -> t }
                .subscribe({
                    val dData = FuelSensorData(it)
                    Sensor.fuelCacheData = dData
                    postBroadcastData(true)
                    Log.e(TAG, it!!.contentToString())
                }, {
                    Log.e(TAG, it.message.toString())
                       postBroadcastData(false)
                }).let { compositeDisposable.add(it) }
        }
    }

    private fun postBroadcastData(isResiveData: Boolean) {
        val intent = Intent(BR_NAME)
        intent.putExtra(BR_VALUE, isResiveData)
        context.sendBroadcast(intent)
    }

    override fun onStopped() {
        super.onStopped()
        compositeDisposable.clear()
    }
}