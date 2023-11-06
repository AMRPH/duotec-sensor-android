package com.gelios.configurator.entity

import android.os.ParcelUuid

data class Scan(val mac: String, val name: String,
                val data: ByteArray, val rssi: Int,
                val time: Int, val uuids: List<ParcelUuid>)