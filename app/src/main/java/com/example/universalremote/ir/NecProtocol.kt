package com.example.universalremote.ir

/**
 * NEC Infrared Protocol Encoder
 * Carrier Frequency: 38 kHz (duty cycle ~ 1/3)
 * Leader: 9000µs mark, 4500µs space
 * Bit 0: 560µs mark, 560µs space
 * Bit 1: 560µs mark, 1690µs space
 * Stop bit: 560µs mark
 */
object NecProtocol {

    private const val LEADER_MARK = 9000
    private const val LEADER_SPACE = 4500
    private const val BIT_MARK = 560
    private const val ZERO_SPACE = 560
    private const val ONE_SPACE = 1690
    private const val STOP_MARK = 560

    /**
     * Encodes a 32-bit hex code into microsecond pulse intervals.
     * (E.g. Samsung 0xE0E040BF, LG 0x20DF10EF)
     */
    fun encodeHex32(hexCode: Long): IntArray {
        val pattern = ArrayList<Int>(67)
        // Leader pulse
        pattern.add(LEADER_MARK)
        pattern.add(LEADER_SPACE)

        // 32 bits, MSB first
        for (i in 31 downTo 0) {
            val bit = (hexCode shr i) and 1L
            pattern.add(BIT_MARK)
            if (bit == 1L) {
                pattern.add(ONE_SPACE)
            } else {
                pattern.add(ZERO_SPACE)
            }
        }

        // Stop bit
        pattern.add(STOP_MARK)
        return pattern.toIntArray()
    }

    /**
     * Standard NEC encoding from address and command (with logical inverses)
     */
    fun encodeAddressCommand(address: Int, command: Int): IntArray {
        val addr = address and 0xFF
        val addrInv = (addr.inv()) and 0xFF
        val cmd = command and 0xFF
        val cmdInv = (cmd.inv()) and 0xFF

        val full32 = ((addr.toLong() and 0xFF) shl 24) or
                ((addrInv.toLong() and 0xFF) shl 16) or
                ((cmd.toLong() and 0xFF) shl 8) or
                (cmdInv.toLong() and 0xFF)

        return encodeHex32(full32)
    }
}
