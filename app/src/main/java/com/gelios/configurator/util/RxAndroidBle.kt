package com.gelios.configurator.util

import android.os.Build
import android.util.Log
import com.polidea.rxandroidble2.RxBleConnection
import com.polidea.rxandroidble2.RxBleDevice
import io.reactivex.ObservableTransformer
import io.reactivex.Single

/**
 * Returns `true` if connection state is [CONNECTED][RxBleConnection.RxBleConnectionState.CONNECTED].
 */
internal val RxBleDevice.isConnected: Boolean
    get() = connectionState == RxBleConnection.RxBleConnectionState.CONNECTED