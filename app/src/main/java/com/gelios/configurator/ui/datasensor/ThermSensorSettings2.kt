package com.gelios.configurator.ui.datasensor


class ThermSensorSettings2(private var byteArray: ByteArray) {
    var adv_interval: Int? = Data(byteArray).getIntValue(Data.FORMAT_UINT8, 0)
        set(value) {
            field = value
            byteArray[0] = field!!.toByte()
        }

    var adv_power_mode: Int? = Data(byteArray).getIntValue(Data.FORMAT_UINT8, 1)
        set(value) {
            field = value
            byteArray[1] = field!!.toByte()
        }

    var adv_beacon: Int? = Data(byteArray).getIntValue(Data.FORMAT_UINT8, 2)
        set(value) {
            field = value
            byteArray[2] = field!!.toByte()
        }

    var uuid: ByteArray? = byteArray.copyOfRange(38, 38 + 16)
        set(value) {
            field = value
            for (i in 0..15){
                byteArray[38 + i] = field!![i]
            }
        }

    var major: ByteArray? = byteArray.copyOfRange(54, 56)
        set(value) {
            field = value
            byteArray[54] = field!![0]
            byteArray[55] = field!![1]
        }

    var minor: ByteArray? = byteArray.copyOfRange(56, 58)
        set(value) {
            field = value
            byteArray[56] = field!![0]
            byteArray[57] = field!![1]
        }


    fun applyMasterPassword(passw: String) {
        var masterPasswArray = byteArrayOf()
        val passByte = passw.toByteArray()
        masterPasswArray = passByte.copyOf(8)
        masterPasswArray.copyInto(byteArray, 14)
    }

    fun setConstant(){
        byteArray[32] = 0x1A.toByte()
        byteArray[33] = 0xFF.toByte()
        byteArray[34] = 0x4C.toByte()
        byteArray[35] = 0x00.toByte()
        byteArray[36] = 0x02.toByte()
        byteArray[37] = 0x15.toByte()

        byteArray[58] = 0xC4.toByte()
        byteArray[59] = 0x00.toByte()
    }


    fun getBytes(): ByteArray {
        return byteArray
    }
}