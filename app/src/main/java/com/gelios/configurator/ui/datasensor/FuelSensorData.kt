package com.gelios.configurator.ui.datasensor

import com.gelios.configurator.entity.Sensor
import java.util.*

//      int8_t temp; // Число со знаком. Температура в градусах цельсия
//      uint16_t level; // Уровень топлива. 0 - пустой бак, 4096 - полный

const val differentTime = 15000L
class FuelSensorData(val byteArray: ByteArray) {

    var fuel: Int = 0
    var temperatura: Int? = 0
    var messageTime: Date


    init {
        temperatura = Data(byteArray).getIntValue(Data.FORMAT_SINT8, 0)
        fuel = Data(byteArray).getIntValue(Data.FORMAT_UINT16, 1)!!
        if (Sensor.fuelCacheData?.fuel != null) {
            if (fuel == Sensor.fuelCacheData!!.fuel!!) {
                messageTime = Sensor.fuelCacheData!!.messageTime
            } else {
                messageTime = Calendar.getInstance().time
            }
        } else {
            messageTime = Calendar.getInstance().time
        }
    }

    var fuelPercent = calculateFuel()

    private fun calculateFuel(): Double {
        if (fuel == 32768) {
            return 32768.0
        }
            if (Sensor.fuelCacheSettings != null) {
                val upLevel = Sensor.fuelCacheSettings!!.level_top!!
                val downLevel = Sensor.fuelCacheSettings!!.level_bottom!!
                return (fuel.toDouble() - downLevel.toDouble())/(upLevel.toDouble()-downLevel.toDouble())

            } else {
                return fuel / 4095.0
            }

    }

}