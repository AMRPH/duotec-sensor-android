package com.gelios.configurator.entity

import com.gelios.configurator.R

class ScanBLESensor(val mac: String, val name: String?,
                    val signal: Int, val data: String,
                    val soft: String, val battery: String,
                    val time: Int, val type: TYPE)
{

    fun getSignalString(): String {
        return "${signal}dB"
    }

    fun getTimeString(): String {
        return "$time sec"
    }

    fun getSoftString(): String {
        return "FW $soft"
    }

    fun getDataString(): String {
        return when (type){
            TYPE.Thermometer ->{
                "$data °C"
            }
            TYPE.Fuel ->{
                "${data}%"
            }
            TYPE.Relay ->{
                data
            }
            else -> ""
        }
    }

    enum class TYPE{ Fuel, Other, Firmware, Thermometer, Relay}
}

