package com.gelios.configurator.entity

class ScanBLESensor(val mac: String, val name: String?,
                    val signal: Int, val data: String,
                    val soft: String, val battery: String,
                    val time: Int, val type: TYPE)
{
    enum class TYPE{ Fuel, Other, Firmware, Thermometer, Relay}
}

