package com.gelios.configurator.util

import java.util.*
import kotlin.experimental.and
import kotlin.experimental.or

class BinHelper {
    companion object {
        fun longToUInt32ByteArray(value: Long): ByteArray {
            val bytes = ByteArray(4)
            bytes[3] = (value and 0xFFFF).toByte()
            bytes[2] = ((value ushr 8) and 0xFFFF).toByte()
            bytes[1] = ((value ushr 16) and 0xFFFF).toByte()
            bytes[0] = ((value ushr 24) and 0xFFFF).toByte()
            return bytes
        }

        fun convertBytesToHex(bytes: ByteArray): String? {
            var str = ""
            for (b in bytes) {
                val st = String.format("%02X", b)
                str = "$str $st"
            }

            return str
        }

        fun getBitSet(array: ByteArray, fromByteIndex: Int, fromByteBitIndex: Int, bitsCount: Int): BitSet? {
            val reversedBitsOrderArray = ByteArray(array.size)
            for (i in array.indices) {
                reversedBitsOrderArray[i] = reverseBitsOrder(array[i])
            }
            val arrayBitSet = BitSet.valueOf(array)
            val fromIndex = fromByteIndex * 8 + fromByteBitIndex
            return arrayBitSet[fromIndex, fromIndex + bitsCount]
        }

        private fun reverseBitsOrder(b: Byte): Byte {
            var from: Byte = b
            var to: Byte = 0
            for (i in 0..7) {
                to = (to.toInt() shl 1).toByte()
                to = to or (from and 1)
                from = (from.toInt() shr 1).toByte()
            }
            return to
        }

        fun toLong(bits: BitSet): Long {
            var value = 0L
            for (i in 0 until bits.length()) {
                value += if (bits[i]) 1L shl i else 0L
            }
            return value
        }
    }

}