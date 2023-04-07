package com.gelios.configurator.ui

import android.content.Context
import android.net.ConnectivityManager
import com.chibatching.kotpref.Kotpref
import com.polidea.rxandroidble2.RxBleClient
import com.polidea.rxandroidble2.RxBleConnection
import com.polidea.rxandroidble2.internal.RxBleLog
import dagger.android.AndroidInjector
import dagger.android.DaggerApplication
import io.reactivex.disposables.CompositeDisposable
import com.gelios.configurator.di.component.DaggerAppComponent

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
        connection

        RxBleClient.setLogLevel(RxBleLog.DEBUG);
        Kotpref.init(this)
    }

}