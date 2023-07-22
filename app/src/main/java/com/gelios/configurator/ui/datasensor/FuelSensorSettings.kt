package com.gelios.configurator.ui.datasensor

import android.util.Log
import java.nio.ByteBuffer

//  0  uint8_t filter_depth; // Глубина фильтрации от 0 до 20
//  1  uint16_t level_top; // Верхняя граница изменения уровня. От 1 до 4095.
//  3  uint16_t level_bottom; //Нижняя граница изменения уровня. От 0 до 4095.
//  5  uint32_t cnt_max; //нижняя граница диапазона изменения периода входного сигнала
//  9  uint32_t cnt_min; //верхняя граница диапазона изменения периода входного сигнала
//  12  uint8_t flags;  // Флаги: если младший бит выставлен в единицу - включен режим Эскорт
//    uint8_t passwd[8]; // установка пароля, при чтении всегда заполнен нулями
//    uint8_t measurement_periods; // количество периодов для измерения
//    uint8_t net_address; // сетевой адрес по проводному интерфейсу
// ///////////   uint8_t reserve[10]; // резерв

class FuelSensorSettings(private var byteArray: ByteArray) {
    var filter_depth: Int? = Data(byteArray).getIntValue(Data.FORMAT_UINT8,0)
    set(value) {
        field = value
        val byte = value?.toByte()
        if (byte != null) {
            byteArray[0] = byte
        }
    }

    var level_top: Int? = Data(byteArray).getIntValue(Data.FORMAT_UINT16,1)
        set(value) {
            field = value
            shortToBytes(value!!.toShort()).reversed().toByteArray().copyInto(byteArray,1)
        }

    var level_bottom: Int? = Data(byteArray).getIntValue(Data.FORMAT_UINT16,3)
        set(value) {
            field = value
            shortToBytes(value!!.toShort()).reversed().toByteArray().copyInto(byteArray, 3)
        }

    var cnt_max: Int? = Data(byteArray).getIntValue(Data.FORMAT_UINT32,5)
        set(value) {
            field = value
            Log.e("BLE___", byteArray.contentToString())
            intToBytes(value!!).reversed().toByteArray().copyInto(byteArray, 5)
            Log.e("BLE___", byteArray.contentToString())
        }

    var cnt_min: Int? =  Data(byteArray).getIntValue(Data.FORMAT_UINT32,9)
        set(value) {
            field = value
            intToBytes(value!!).reversed().toByteArray().copyInto(byteArray, 9)
        }

    var flag: Int = byteArray[13].toInt()
        set(value) {
            field = value
            byteArray[13] = field.toByte()
        }

    var measurement_periods: Int? = Data(byteArray).getIntValue(Data.FORMAT_UINT8,22)


    var net_address: Int? = Data(byteArray).getIntValue(Data.FORMAT_UINT8,23)
        set(value) {
            field = value
            val byte = value?.toByte()
            if (byte != null) {
                byteArray[23] = byte
            }
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

    private fun shortToBytes(value: Short) : ByteArray {
        val buffer = ByteBuffer.allocate(2)
        buffer.putShort(value)
        return buffer.array()
    }

    private fun intToBytes(value: Int) : ByteArray {
        val buffer = ByteBuffer.allocate(4)
        buffer.putInt(value)
        return buffer.array()
    }
}