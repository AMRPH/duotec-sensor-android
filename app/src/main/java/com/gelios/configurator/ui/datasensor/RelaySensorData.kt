package com.gelios.configurator.ui.datasensor

//          int8_t output; // 0 - реле отключено, 1 - включено
//          uint16_t Vbat; // Напряжение бортсети в 1/256 долях Вольта

class RelaySensorData(val byteArray: ByteArray) {

    var output: Int? = 0
    var voltage = voltage()


    init {
        output = Data(byteArray).getIntValue(Data.FORMAT_SINT8, 0)
    }

    fun isOutput(): Boolean {
        return output == 1
    }


    fun Byte.toPositiveInt() = toInt() and 0xFF

    private fun voltage(): Double {
        val positive: List<Int> = byteArray.map { it.toPositiveInt() }
        return positive[2]+positive[1].toDouble()/256
    }
}