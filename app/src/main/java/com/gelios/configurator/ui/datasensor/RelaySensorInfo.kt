package com.gelios.configurator.ui.datasensor

//      uint32_t reset_count;
//      uint32_t connection_attempts;
//      uint32_t password_attempts;
//      uint32_t timestamp;
//      uint32_t reserve0;
//      uint16_t reserve1;
//      uint8_t reserve2;
//      uint8_t reserve[9];

class RelaySensorInfo(private val byteArray: ByteArray) {
    var reset_count: Int? = Data(byteArray).getIntValue(Data.FORMAT_UINT32, 0)

    var connection_attempts: Int? = Data(byteArray).getIntValue(Data.FORMAT_UINT32, 4)

    var password_attempts: Int? = Data(byteArray).getIntValue(Data.FORMAT_UINT32, 8)

    var timestamp: Long? = Data(byteArray).getLongValue(Data.FORMAT_UINT32, 12)


    var voltageDouble = voltageString()

    fun timeString(): String {
        val days = timestamp!! / 86400
        val hours = (timestamp!! / 3600) % 24;
        val minutes = (timestamp!! / 60) % 60;
        val seconds = timestamp!! % 60;

        return String.format("%02dd %02dh %02dm %02ds", days, hours, minutes, seconds);
    }

    fun Byte.toPositiveInt() = toInt() and 0xFF

    private fun voltageString(): Double {
        val positive: List<Int> = byteArray.map { it.toPositiveInt() }
        return positive[21]+positive[20].toDouble()/256
    }
}