package com.gelios.configurator

import com.chibatching.kotpref.KotprefModel
import com.gelios.configurator.entity.BLESensor

object MainPref : KotprefModel() {

    var deviceMac by stringPrefVar("00:11:22:33:44:55")
    var stepFuel by intPrefVar(10)

    var tatirovkaValue by stringPrefVar()
    var comment by stringPrefVar("")

    val typeDevices = mutableMapOf<String, BLESensor.TYPE>()
}