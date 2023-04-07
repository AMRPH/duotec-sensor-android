package com.gelios.configurator.ui.datasensor

import android.util.Log
import java.nio.ByteBuffer

//    uint8_t filter_depth; // Глубина фильтрации от 0 до 20
//    uint16_t level_top; // Верхняя граница изменения уровня. От 1 до 4095.
//    uint16_t level_bottom; //Нижняя граница изменения уровня. От 0 до 4095.
//    uint32_t cnt_max; //нижняя граница диапазона изменения периода входного сигнала
//    uint32_t cnt_min; //верхняя граница диапазона изменения периода входного сигнала
//    uint8_t flags;  // Флаги: если младший бит выставлен в единицу - включен режим Эскорт
//    uint8_t passwd[8]; // установка пароля, при чтении всегда заполнен нулями
// ///////////   uint8_t reserve[10]; // резерв

class ThermSensorSettings(private var byteArray: ByteArray) {

    var escort: Int = byteArray[13].toInt()
        set(value) {
            field = value
            byteArray[13] = field.toByte()
        }

    var sensorOpening: Int = byteArray[0].toInt()
        set(value) {
            field = value
            Log.e("BLE___", byteArray.contentToString())
            byteArray[0] = field.toByte()
            Log.e("BLE___", byteArray.contentToString())
        }

    fun applyMasterPassword(passw: String) {
        var masterPasswArray = byteArrayOf()
        val passByte = passw.toByteArray()
        masterPasswArray = passByte.copyOf(8)
        masterPasswArray.copyInto(byteArray, 14)
    }

    private fun isBitSet(b: ByteArray): Int {
        val value = Data(b).getIntValue(Data.FORMAT_UINT8, 13)
        return value!!
    }

    fun getBytes(): ByteArray {
        return byteArray
    }

    private fun shortToBytes(value: Short): ByteArray {
        val buffer = ByteBuffer.allocate(2)
        buffer.putShort(value)
        return buffer.array()
    }

    private fun intToBytes(value: Int): ByteArray {
        val buffer = ByteBuffer.allocate(4)
        buffer.putInt(value)
        return buffer.array()
    }
}