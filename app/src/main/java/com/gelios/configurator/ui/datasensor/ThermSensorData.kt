package com.gelios.configurator.ui.datasensor

import android.util.Log
import com.gelios.configurator.entity.Sensor
import com.gelios.configurator.util.BinHelper
import com.gelios.configurator.util.BinHelper.Companion.toLong
import com.google.android.gms.common.util.Hex
import java.util.stream.IntStream.range

//      int16_t temp; // Число со знаком. Температура в 1/128 градусов цельсия

class ThermSensorData(val byteArray: ByteArray) {

    var temperature: Int? = 0
    var wire: Boolean? = false
    var magnet: Boolean? = false
    var light: Long? = 0

    init {
        temperature = Data(byteArray).getIntValue(Data.FORMAT_SINT16, 0)
        if (byteArray.size == 3){
            val b = BinHelper.getBitSet(byteArray, 2, 0, 8)
            wire = b!![7]
            magnet = b[6]
            light = 0L
        } else {
            val b = BinHelper.getBitSet(byteArray, 2, 0, 16)
            wire = b!![15]
            magnet = b[14]
            light = toLong(b.get(0, 11))
        }
    }

    fun getValueTherm(): Double{
        return temperature?.toDouble()?.div(128) ?: 0.0
    }

    fun getValueWire(): Boolean {
        return wire!!
    }

    fun getValueMagnet(): Boolean {
        return magnet!!
    }

    fun getValueLight(): Long {
        return light!!
    }
}