package com.gelios.configurator.ui.datasensor

import android.util.Log
import java.nio.ByteBuffer

//  0 1         uint16_t timeout; // Таймаут в секундах. Мак значение 36000 секунд (10 часов)
//  2 3         uint16_t reserve0;
//  4           uint8_t reserve1;
//  5 6 7 8     uint32_t reserve4;
//  9 10 11 12  uint32_t reserve5;
//  13          uint8_t flags;  // Флаги.
//  14          uint8_t passwd[8]; // установка пароля, при чтении всегда заполнен нулями
//  15          uint8_t reserve6;
//  16          uint8_t reserve[9]; // резерв



class RelaySensorSettings(private var byteArray: ByteArray) {

    var escort: Int = byteArray[13].toInt()
        set(value) {
            field = value
            byteArray[13] = field.toByte()
        }

    fun applyMasterPassword(passw: String) {
        var masterPasswArray = byteArrayOf()
        val passByte = passw.toByteArray()
        masterPasswArray = passByte.copyOf(8)
        masterPasswArray.copyInto(byteArray, 14)
    }

    private fun isBitSet(b: ByteArray): Boolean {
        val value = Data(b).getIntValue(Data.FORMAT_UINT8,0)
        return value == 1
    }

    fun getBytes(): ByteArray {
        return byteArray
    }


}