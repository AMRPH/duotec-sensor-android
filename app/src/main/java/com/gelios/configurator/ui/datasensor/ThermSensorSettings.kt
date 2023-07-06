package com.gelios.configurator.ui.datasensor


class ThermSensorSettings(private var byteArray: ByteArray) {
    var contact_type: Int? = Data(byteArray).getIntValue(Data.FORMAT_UINT8, 0)
        set(value) {
            field = value
            byteArray[0] = field!!.toByte()
        }

    var flag: Int? = Data(byteArray).getIntValue(Data.FORMAT_UINT8, 13)
        set(value) {
            field = value
            byteArray[13] = field!!.toByte()
        }

    fun applyMasterPassword(passw: String) {
        var masterPasswArray = byteArrayOf()
        val passByte = passw.toByteArray()
        masterPasswArray = passByte.copyOf(8)
        masterPasswArray.copyInto(byteArray, 14)
    }

    fun getBytes(): ByteArray {
        return byteArray
    }
}