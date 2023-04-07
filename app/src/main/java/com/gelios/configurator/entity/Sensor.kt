package com.gelios.configurator.entity

import androidx.lifecycle.MutableLiveData
import com.gelios.configurator.ui.datasensor.*

object Sensor {

    var sensorAuthorized = false
    var sensorName = MutableLiveData<String?>()

    var sensorVersion: String? = null
    var sensorBattery: Int? = null
    var sensorType: Type? = null

    var fuelCacheData: FuelSensorData? = null
    var fuelCacheInfo: FuelSensorInfo? = null
    var fuelCacheSettings: FuelSensorSettings? = null

    var thermCacheData: ThermSensorData? = null
    var thermCacheInfo: ThermSensorInfo? = null
    var thermCacheSettings: ThermSensorSettings? = null

    var relayCacheData: RelaySensorData? = null
    var relayCacheInfo: RelaySensorInfo? = null
    var relayCacheSettings: RelaySensorSettings? = null

    var confirmedPass: String = "00000000"

    var flagBase = false
    var flagVersion = false
    var flagData = false
    var flagInfo = false
    var flagSettings = false
    var flagSensorBattery = false

    fun clearSensorData() {
        sensorName.postValue(null)
        sensorVersion = null
        sensorType = null
        sensorVersion = null
        sensorBattery = null
        sensorAuthorized = false

        fuelCacheData = null
        fuelCacheInfo = null
        fuelCacheSettings = null

        thermCacheData = null
        thermCacheInfo = null
        thermCacheSettings = null

        relayCacheData = null
        relayCacheInfo = null
        relayCacheSettings = null

        flagBase = false
        flagVersion = false
        flagData = false
        flagInfo = false
        flagSettings = false
        flagSensorBattery = false
    }

    enum class Type(val type: String){
        TMP("TMP"),
        TMPv3("TMPv3"),
        TMPv4("TMPv4"),
        TMPv5("TMPv5"),

        LLS("LLS"),
        LLSv3("LLSv3"),
        LLSv4("LLSv4"),
        LLSv5("LLSv5"),

        REL("REL"),
        RLY("RLY"),
        RLYv3("RLYv3"),
        RLYv4("RLYv4"),
        RLYv5("RLYv5")
    }
}