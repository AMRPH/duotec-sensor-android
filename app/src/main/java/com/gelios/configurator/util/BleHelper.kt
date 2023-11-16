package com.gelios.configurator.util

import android.os.Build
import android.util.Log
import com.polidea.rxandroidble3.RxBleConnection
import io.reactivex.rxjava3.core.ObservableTransformer
import io.reactivex.rxjava3.core.Single
import java.io.File

class BleHelper {
    companion object {
        fun geMTUTransformer(mtu: Int): ObservableTransformer<RxBleConnection, RxBleConnection> {
            return ObservableTransformer<RxBleConnection, RxBleConnection> { upstream ->
                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
                    upstream.doOnSubscribe { Log.i("MTU", "MTU negotiation is not supported") }
                } else {
                    upstream.doOnSubscribe { Log.i("MTU", "MTU negotiation is supported") }
                        .flatMapSingle { connection ->
                            connection.requestMtu(mtu)
                                .doOnSubscribe { Log.i("MTU", "Negotiating MTU started") }
                                .doOnSuccess { Log.i("MTU", "Negotiated MTU: $it") }
                                .ignoreElement()
                                .andThen(Single.just(connection))
                        }
                }
            }
        }
    }
}