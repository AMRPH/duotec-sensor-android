package com.gelios.configurator.ui.sensor.fuel.fragments.tarirovka

import android.os.Parcelable
import kotlinx.android.parcel.Parcelize

@Parcelize
class DataTarirovka(
    var counter: Int = 0,
    var fuelLevel: String = "",
    var sensorLevel: String = ""
) : Parcelable{
    override fun toString(): String {
        return "$fuelLevel; $sensorLevel"
    }
}