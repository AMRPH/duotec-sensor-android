package com.gelios.configurator.entity

object Commands {
    val CMD_FUEL_RESET: Byte = 1
    val CMD_FUEL_RESET_TO_PERSISTENT: Byte = 2
    val CMD_FUEL_RESET_SCE: Byte = 3
    val CMD_FUEL_POWER_DOWN: Byte = 4
    val CMD_FUEL_SET_EMPTY : Byte = 5
    val CMD_FUEL_SET_FULL: Byte = 6

    val CMD_THERM_RESET : Byte = 1
    val CMD_THERM_RESET_TO_PERSISTENT : Byte = 2
    val CMD_THERM_RESET_SCE : Byte = 3
    val CMD_THERM_POWER_DOWN : Byte = 44

    val CMD_RELAY_RESET: Byte = 1
    val CMD_RELAY_RESET_TO_PERSISTENT: Byte = 2
    val CMD_RELAY_RESET_SCE: Byte = 3
    val CMD_RELAY_POWER_DOWN: Byte = 4
    val CMD_RELAY_OPEN: Byte = 7
    val CMD_RELAY_CLOSE: Byte = 8


}