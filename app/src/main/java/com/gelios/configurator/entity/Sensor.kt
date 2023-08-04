package com.gelios.configurator.entity

import androidx.lifecycle.MutableLiveData
import com.gelios.configurator.ui.datasensor.*

object Sensor {

    var authorized = false
    var name = MutableLiveData<String?>()

    var softVersion: String? = null
    var battery: Int? = null
    var type: Type? = null
    var version: Int? = null

    var fuelCacheData: FuelSensorData? = null
    var fuelCacheInfo: FuelSensorInfo? = null
    var fuelCacheSettings: FuelSensorSettings? = null
    var fuelCacheSettings2: FuelSensorSettings2? = null

    var thermCacheData: ThermSensorData? = null
    var thermCacheInfo: ThermSensorInfo? = null
    var thermCacheSettings: ThermSensorSettings? = null
    var thermCacheSettings2: ThermSensorSettings2? = null

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
        name.postValue(null)
        softVersion = null
        battery = null
        type = null
        version = null
        authorized = false

        fuelCacheData = null
        fuelCacheInfo = null
        fuelCacheSettings = null
        fuelCacheSettings2 = null

        thermCacheData = null
        thermCacheInfo = null
        thermCacheSettings = null
        thermCacheSettings2 = null

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

    enum class Type{
        TMP, LLS, RLY
    }
}