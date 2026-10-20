package com.example.universalremote.ir

/**
 * Sony SIRC (12-bit) Protocol Encoder
 * Carrier Frequency: 40 kHz
 * Leader: 2400µs mark, 600µs space
 * Bit 0: 600µs mark, 600µs space
 * Bit 1: 1200µs mark, 600µs space
 */
object SonyProtocol {

    private const val LEADER_MARK = 2400
    private const val SPACE = 600
    private const val ZERO_MARK = 600
    private const val ONE_MARK = 1200

    /**
     * Encodes 7-bit command and 5-bit device address (12-bit SIRC).
     * Transmitted LSB first.
     */
    fun encode12Bit(command: Int, device: Int = 1): IntArray {
        val pattern = ArrayList<Int>(25)

        // Leader
        pattern.add(LEADER_MARK)
        pattern.add(SPACE)

        // 7-bit command (LSB first)
        for (i in 0 until 7) {
            val bit = (command shr i) and 1
            pattern.add(if (bit == 1) ONE_MARK else ZERO_MARK)
            pattern.add(SPACE)
        }

        // 5-bit device address (LSB first)
        for (i in 0 until 5) {
            val bit = (device shr i) and 1
            pattern.add(if (bit == 1) ONE_MARK else ZERO_MARK)
            pattern.add(SPACE)
        }

        return pattern.toIntArray()
    }
}
