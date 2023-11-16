package com.gelios.configurator.ui

import cn.wch.blelib.ch583.CH583BluetoothManager
import cn.wch.blelib.ch583.ota.CH583OTAManager
import com.chibatching.kotpref.Kotpref
import com.gelios.configurator.di.component.DaggerAppComponent
import com.polidea.rxandroidble3.RxBleClient
import com.polidea.rxandroidble3.RxBleConnection
import com.polidea.rxandroidble3.internal.RxBleLog
import dagger.android.AndroidInjector
import dagger.android.DaggerApplication
import io.reactivex.rxjava3.disposables.CompositeDisposable

class App : DaggerApplication() {

    override fun applicationInjector(): AndroidInjector<out DaggerApplication> {
        return DaggerAppComponent
            .builder()
            .application(this)
            .build()
    }

    companion object {
        lateinit var instance: App
        lateinit var rxBleClient: RxBleClient
        var isUpdating = false
        val bleCompositeDisposable: CompositeDisposable by lazy { CompositeDisposable() }
        var connection: RxBleConnection? = null

        fun restartBleClient(){
            rxBleClient = RxBleClient.create(instance.applicationContext)
        }
    }

    override fun onCreate() {
        super.onCreate()
        instance = this
        rxBleClient = RxBleClient.create(this)

        CH583BluetoothManager.getInstance().init(this)
        CH583OTAManager.getInstance().init(this)

        RxBleClient.setLogLevel(RxBleLog.DEBUG);
        Kotpref.init(this)
    }

}