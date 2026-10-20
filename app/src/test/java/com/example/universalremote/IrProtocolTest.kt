package com.example.universalremote

import com.example.universalremote.ir.IrBrandCodes
import com.example.universalremote.ir.NecProtocol
import com.example.universalremote.ir.SonyProtocol
import com.example.universalremote.model.RemoteCommand
import com.example.universalremote.model.TvBrand
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class IrProtocolTest {

    @Test
    fun testNecLeaderAndBitLength() {
        val pattern = NecProtocol.encodeHex32(0xE0E040BFL) // Samsung Power
        assertNotNull(pattern)
        // NEC: leader mark + space + 32 bits * 2 + stop mark = 67 pulse elements
        assertEquals(67, pattern.size)
        // Leader mark should be 9000us
        assertEquals(9000, pattern[0])
        // Leader space should be 4500us
        assertEquals(4500, pattern[1])
    }

    @Test
    fun testSonyProtocolLength() {
        val pattern = SonyProtocol.encode12Bit(command = 0x15, device = 1) // Sony Power
        assertNotNull(pattern)
        // Sony 12-bit: Leader (mark, space) + 12 bits * 2 = 2 + 24 = 26 elements
        assertTrue(pattern.size >= 25)
        assertEquals(2400, pattern[0])
        assertEquals(600, pattern[1])
    }

    @Test
    fun testBrandSignalsForAllCommands() {
        val brands = listOf(TvBrand.TOSHIBA, TvBrand.SAMSUNG, TvBrand.LG, TvBrand.SONY, TvBrand.MI_XIAOMI, TvBrand.TCL)
        val commands = listOf(
            RemoteCommand.POWER, RemoteCommand.AV, RemoteCommand.SOURCE,
            RemoteCommand.VOL_UP, RemoteCommand.VOL_DOWN, RemoteCommand.MUTE,
            RemoteCommand.CH_UP, RemoteCommand.CH_DOWN,
            RemoteCommand.NAV_UP, RemoteCommand.NAV_DOWN, RemoteCommand.NAV_LEFT, RemoteCommand.NAV_RIGHT, RemoteCommand.NAV_OK
        )

        for (brand in brands) {
            for (cmd in commands) {
                val signal1 = IrBrandCodes.getSignal(brand, cmd, toshibaCodeSet = 1)
                assertNotNull("Signal should not be null for $brand and $cmd (set 1)", signal1)
                assertTrue("Carrier frequency must be valid", signal1.frequency in 30000..60000)
                assertTrue("IR Pattern must not be empty", signal1.pattern.isNotEmpty())

                val signal2 = IrBrandCodes.getSignal(brand, cmd, toshibaCodeSet = 2)
                assertNotNull("Signal should not be null for $brand and $cmd (set 2)", signal2)
            }
        }
    }
}
