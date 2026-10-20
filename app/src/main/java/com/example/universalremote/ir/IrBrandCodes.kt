package com.example.universalremote.ir

import com.example.universalremote.model.RemoteCommand
import com.example.universalremote.model.TvBrand

/**
 * Repository of IR Codes and Pattern Builders for major TV brands
 */
object IrBrandCodes {

    data class IrSignal(
        val frequency: Int,
        val pattern: IntArray,
        val protocolName: String,
        val hexDisplay: String
    )

    /**
     * Resolves the IR pattern for a given TV Brand and Remote Command
     */
    fun getSignal(brand: TvBrand, command: RemoteCommand, toshibaCodeSet: Int = 1): IrSignal {
        return when (brand) {
            TvBrand.TOSHIBA -> getToshibaSignal(command, toshibaCodeSet)
            TvBrand.SAMSUNG -> getSamsungSignal(command)
            TvBrand.LG -> getLgSignal(command)
            TvBrand.SONY -> getSonySignal(command)
            TvBrand.MI_XIAOMI -> getXiaomiSignal(command)
            TvBrand.TCL -> getTclSignal(command)
            TvBrand.PANASONIC -> getPanasonicSignal(command)
            TvBrand.ROKU -> getRokuIrSignal(command)
            TvBrand.GENERIC -> getGenericSignal(command)
        }
    }

    // TOSHIBA (NEC Protocol - 38kHz, Address 0x02FD)
    private fun getToshibaSignal(command: RemoteCommand, codeSet: Int): IrSignal {
        val hex = if (codeSet == 1) {
            // Standard Toshiba Smart / LED Remote (CT-8560 / CT-32F2 / CT-9922)
            when (command) {
                // Confirmed working
                RemoteCommand.POWER -> 0x02FD48B7L      // raw 0x12 -> rev 0x48
                RemoteCommand.VOL_UP -> 0x02FD58A7L     // raw 0x1A -> rev 0x58
                RemoteCommand.VOL_DOWN -> 0x02FD7887L   // raw 0x1E -> rev 0x78
                RemoteCommand.MUTE -> 0x02FD08F7L       // raw 0x10 -> rev 0x08

                // TV / AV and Input Source
                RemoteCommand.AV -> 0x02FD28D7L         // raw 0x14 (Source toggle) -> rev 0x28
                RemoteCommand.SOURCE -> 0x02FDF00FL     // raw 0x0F (Input selector) -> rev 0xF0

                // Channels (raw 0x1B & 0x1F)
                RemoteCommand.CH_UP -> 0x02FDD827L      // raw 0x1B -> rev 0xD8
                RemoteCommand.CH_DOWN -> 0x02FDF807L    // raw 0x1F -> rev 0xF8

                // D-Pad Navigation & OK (CT-8560 / CT-32F2)
                RemoteCommand.NAV_UP -> 0x02FD9867L     // raw 0x19 -> rev 0x98
                RemoteCommand.NAV_DOWN -> 0x02FDB847L   // raw 0x1D -> rev 0xB8
                RemoteCommand.NAV_LEFT -> 0x02FD42BDL   // raw 0x42 -> rev 0x42
                RemoteCommand.NAV_RIGHT -> 0x02FD02FDL  // raw 0x40 -> rev 0x02
                RemoteCommand.NAV_OK -> 0x02FD847BL     // raw 0x21 -> rev 0x84

                // Navigation Controls
                RemoteCommand.BACK -> 0x02FD26D9L       // raw 0x64 (Return) -> rev 0x26
                RemoteCommand.HOME -> 0x02FDC23DL       // raw 0x43 (Exit/Home) -> rev 0xC2
                RemoteCommand.MENU -> 0x02FDDA25L       // raw 0x5B (Menu) -> rev 0xDA

                // Digits (raw 0x00 to 0x09)
                RemoteCommand.NUM_0 -> 0x02FD00FFL
                RemoteCommand.NUM_1 -> 0x02FD807FL
                RemoteCommand.NUM_2 -> 0x02FD40BFL
                RemoteCommand.NUM_3 -> 0x02FDC03FL
                RemoteCommand.NUM_4 -> 0x02FD20DFL
                RemoteCommand.NUM_5 -> 0x02FDA05FL
                RemoteCommand.NUM_6 -> 0x02FD609FL
                RemoteCommand.NUM_7 -> 0x02FDE01FL
                RemoteCommand.NUM_8 -> 0x02FD10EFL
                RemoteCommand.NUM_9 -> 0x02FD906FL
            }
        } else {
            // Code Set 2 (Toshiba CT-90325 / Regza LCD Series)
            when (command) {
                RemoteCommand.POWER -> 0x02FD48B7L
                RemoteCommand.VOL_UP -> 0x02FD58A7L
                RemoteCommand.VOL_DOWN -> 0x02FD7887L
                RemoteCommand.MUTE -> 0x02FD08F7L

                RemoteCommand.AV -> 0x02FDF00FL         // raw 0x0F
                RemoteCommand.SOURCE -> 0x02FD28D7L     // raw 0x14

                RemoteCommand.CH_UP -> 0x02FDD827L      // raw 0x1B
                RemoteCommand.CH_DOWN -> 0x02FDF807L    // raw 0x1F

                // Regza CT-90325 D-pad
                RemoteCommand.NAV_UP -> 0x02FD41BEL     // raw 0x82 -> rev 0x41
                RemoteCommand.NAV_DOWN -> 0x02FDC13EL   // raw 0x83 -> rev 0xC1
                RemoteCommand.NAV_LEFT -> 0x02FDB847L   // raw 0x1D -> rev 0xB8
                RemoteCommand.NAV_RIGHT -> 0x02FD9867L  // raw 0x19 -> rev 0x98
                RemoteCommand.NAV_OK -> 0x02FD916EL     // raw 0x89 -> rev 0x91

                RemoteCommand.BACK -> 0x02FD26D9L
                RemoteCommand.HOME -> 0x02FDC23DL
                RemoteCommand.MENU -> 0x02FD708FL       // raw 0x0E -> rev 0x70

                RemoteCommand.NUM_0 -> 0x02FD00FFL
                RemoteCommand.NUM_1 -> 0x02FD807FL
                RemoteCommand.NUM_2 -> 0x02FD40BFL
                RemoteCommand.NUM_3 -> 0x02FDC03FL
                RemoteCommand.NUM_4 -> 0x02FD20DFL
                RemoteCommand.NUM_5 -> 0x02FDA05FL
                RemoteCommand.NUM_6 -> 0x02FD609FL
                RemoteCommand.NUM_7 -> 0x02FDE01FL
                RemoteCommand.NUM_8 -> 0x02FD10EFL
                RemoteCommand.NUM_9 -> 0x02FD906FL
            }
        }
        return IrSignal(
            frequency = 38000,
            pattern = NecProtocol.encodeHex32(hex),
            protocolName = "Toshiba NEC Set $codeSet (38kHz)",
            hexDisplay = "0x" + hex.toString(16).uppercase()
        )
    }

    // SAMSUNG (NEC Protocol - 38kHz)
    private fun getSamsungSignal(command: RemoteCommand): IrSignal {
        val hex = when (command) {
            RemoteCommand.POWER -> 0xE0E040BFL
            RemoteCommand.AV, RemoteCommand.SOURCE -> 0xE0E0807FL
            RemoteCommand.VOL_UP -> 0xE0E0E01FL
            RemoteCommand.VOL_DOWN -> 0xE0E0D02FL
            RemoteCommand.MUTE -> 0xE0E0F00FL
            RemoteCommand.CH_UP -> 0xE0E048B7L
            RemoteCommand.CH_DOWN -> 0xE0E008F7L
            RemoteCommand.NAV_UP -> 0xE0E006F9L
            RemoteCommand.NAV_DOWN -> 0xE0E08679L
            RemoteCommand.NAV_LEFT -> 0xE0E0A659L
            RemoteCommand.NAV_RIGHT -> 0xE0E046B9L
            RemoteCommand.NAV_OK -> 0xE0E016E9L
            RemoteCommand.BACK -> 0xE0E01AE5L
            RemoteCommand.HOME, RemoteCommand.MENU -> 0xE0E058A7L
            RemoteCommand.SOURCE -> 0xE0E0807FL
            RemoteCommand.NUM_0 -> 0xE0E08877L
            RemoteCommand.NUM_1 -> 0xE0E020DFL
            RemoteCommand.NUM_2 -> 0xE0E0A05FL
            RemoteCommand.NUM_3 -> 0xE0E0609FL
            RemoteCommand.NUM_4 -> 0xE0E010EFL
            RemoteCommand.NUM_5 -> 0xE0E0906FL
            RemoteCommand.NUM_6 -> 0xE0E050AFL
            RemoteCommand.NUM_7 -> 0xE0E030CFL
            RemoteCommand.NUM_8 -> 0xE0E0B04FL
            RemoteCommand.NUM_9 -> 0xE0E0708FL
        }
        return IrSignal(
            frequency = 38000,
            pattern = NecProtocol.encodeHex32(hex),
            protocolName = "Samsung NEC 38kHz",
            hexDisplay = "0x" + hex.toString(16).uppercase()
        )
    }

    // LG (NEC Protocol - 38kHz)
    private fun getLgSignal(command: RemoteCommand): IrSignal {
        val hex = when (command) {
            RemoteCommand.POWER -> 0x20DF10EFL
            RemoteCommand.AV, RemoteCommand.SOURCE -> 0x20DFD02FL
            RemoteCommand.VOL_UP -> 0x20DF40BFL
            RemoteCommand.VOL_DOWN -> 0x20DFC03FL
            RemoteCommand.MUTE -> 0x20DF906FL
            RemoteCommand.CH_UP -> 0x20DF00FFL
            RemoteCommand.CH_DOWN -> 0x20DF807FL
            RemoteCommand.NAV_UP -> 0x20DF02FDL
            RemoteCommand.NAV_DOWN -> 0x20DF827DL
            RemoteCommand.NAV_LEFT -> 0x20DFE01FL
            RemoteCommand.NAV_RIGHT -> 0x20DF609FL
            RemoteCommand.NAV_OK -> 0x20DF22DDL
            RemoteCommand.BACK -> 0x20DF14EBL
            RemoteCommand.HOME, RemoteCommand.MENU -> 0x20DF3EC1L
            RemoteCommand.SOURCE -> 0x20DFD02FL
            RemoteCommand.NUM_0 -> 0x20DF08F7L
            RemoteCommand.NUM_1 -> 0x20DF8877L
            RemoteCommand.NUM_2 -> 0x20DF48B7L
            RemoteCommand.NUM_3 -> 0x20DFC837L
            RemoteCommand.NUM_4 -> 0x20DF28D7L
            RemoteCommand.NUM_5 -> 0x20DFA857L
            RemoteCommand.NUM_6 -> 0x20DF6897L
            RemoteCommand.NUM_7 -> 0x20DFE817L
            RemoteCommand.NUM_8 -> 0x20DF18E7L
            RemoteCommand.NUM_9 -> 0x20DF9867L
        }
        return IrSignal(
            frequency = 38000,
            pattern = NecProtocol.encodeHex32(hex),
            protocolName = "LG NEC 38kHz",
            hexDisplay = "0x" + hex.toString(16).uppercase()
        )
    }

    // SONY (SIRC 12-bit Protocol - 40kHz, Device 1 = TV)
    private fun getSonySignal(command: RemoteCommand): IrSignal {
        val sircCommand = when (command) {
            RemoteCommand.POWER -> 0x15 // 21
            RemoteCommand.AV, RemoteCommand.SOURCE -> 0x25
            RemoteCommand.VOL_UP -> 0x12 // 18
            RemoteCommand.VOL_DOWN -> 0x13 // 19
            RemoteCommand.MUTE -> 0x14 // 20
            RemoteCommand.CH_UP -> 0x10 // 16
            RemoteCommand.CH_DOWN -> 0x11 // 17
            RemoteCommand.NAV_UP -> 0x74
            RemoteCommand.NAV_DOWN -> 0x75
            RemoteCommand.NAV_LEFT -> 0x33
            RemoteCommand.NAV_RIGHT -> 0x34
            RemoteCommand.NAV_OK -> 0x65
            RemoteCommand.BACK -> 0x23
            RemoteCommand.HOME, RemoteCommand.MENU -> 0x60
            RemoteCommand.SOURCE -> 0x25
            RemoteCommand.NUM_0 -> 0x09
            RemoteCommand.NUM_1 -> 0x00
            RemoteCommand.NUM_2 -> 0x01
            RemoteCommand.NUM_3 -> 0x02
            RemoteCommand.NUM_4 -> 0x03
            RemoteCommand.NUM_5 -> 0x04
            RemoteCommand.NUM_6 -> 0x05
            RemoteCommand.NUM_7 -> 0x06
            RemoteCommand.NUM_8 -> 0x07
            RemoteCommand.NUM_9 -> 0x08
        }
        return IrSignal(
            frequency = 40000,
            pattern = SonyProtocol.encode12Bit(sircCommand, device = 1),
            protocolName = "Sony SIRC 40kHz",
            hexDisplay = "Cmd: 0x" + sircCommand.toString(16).uppercase() + " (Dev 1)"
        )
    }

    // MI / XIAOMI (NEC Protocol - 38kHz)
    private fun getXiaomiSignal(command: RemoteCommand): IrSignal {
        val hex = when (command) {
            RemoteCommand.POWER -> 0xA55A38C7L
            RemoteCommand.AV, RemoteCommand.SOURCE -> 0xA55A42BDL
            RemoteCommand.VOL_UP -> 0xA55A58A7L
            RemoteCommand.VOL_DOWN -> 0xA55A7887L
            RemoteCommand.MUTE -> 0xA55A00FFL
            RemoteCommand.CH_UP -> 0xA55A18E7L
            RemoteCommand.CH_DOWN -> 0xA55A9867L
            RemoteCommand.NAV_UP -> 0xA55A02FDL
            RemoteCommand.NAV_DOWN -> 0xA55A827DL
            RemoteCommand.NAV_LEFT -> 0xA55AE01FL
            RemoteCommand.NAV_RIGHT -> 0xA55A609FL
            RemoteCommand.NAV_OK -> 0xA55A22DDL
            RemoteCommand.BACK -> 0xA55A52ADL
            RemoteCommand.HOME, RemoteCommand.MENU -> 0xA55A12EDL
            RemoteCommand.SOURCE -> 0xA55A42BDL
            else -> 0xA55A38C7L
        }
        return IrSignal(
            frequency = 38000,
            pattern = NecProtocol.encodeHex32(hex),
            protocolName = "Mi NEC 38kHz",
            hexDisplay = "0x" + hex.toString(16).uppercase()
        )
    }

    // TCL / THOMSON (NEC Protocol - 38kHz)
    private fun getTclSignal(command: RemoteCommand): IrSignal {
        val hex = when (command) {
            RemoteCommand.POWER -> 0x00FD48B7L
            RemoteCommand.AV, RemoteCommand.SOURCE -> 0x00FDF00FL
            RemoteCommand.VOL_UP -> 0x00FD58A7L
            RemoteCommand.VOL_DOWN -> 0x00FD7887L
            RemoteCommand.MUTE -> 0x00FD08F7L
            RemoteCommand.CH_UP -> 0x00FD807FL
            RemoteCommand.CH_DOWN -> 0x00FD40BFL
            RemoteCommand.NAV_UP -> 0x00FDA05FL
            RemoteCommand.NAV_DOWN -> 0x00FD609FL
            RemoteCommand.NAV_LEFT -> 0x00FDE01FL
            RemoteCommand.NAV_RIGHT -> 0x00FD906FL
            RemoteCommand.NAV_OK -> 0x00FD22DDL
            RemoteCommand.BACK -> 0x00FD1AE5L
            RemoteCommand.HOME, RemoteCommand.MENU -> 0x00FD50AFL
            RemoteCommand.SOURCE -> 0x00FDF00FL
            else -> 0x00FD48B7L
        }
        return IrSignal(
            frequency = 38000,
            pattern = NecProtocol.encodeHex32(hex),
            protocolName = "TCL NEC 38kHz",
            hexDisplay = "0x" + hex.toString(16).uppercase()
        )
    }

    // PANASONIC
    private fun getPanasonicSignal(command: RemoteCommand): IrSignal {
        val hex = when (command) {
            RemoteCommand.POWER -> 0x40040100BCBDL
            RemoteCommand.VOL_UP -> 0x400401000405L
            RemoteCommand.VOL_DOWN -> 0x400401008485L
            RemoteCommand.MUTE -> 0x400401004C4DL
            RemoteCommand.CH_UP -> 0x400401002C2DL
            RemoteCommand.CH_DOWN -> 0x40040100ACADL
            else -> 0x40040100BCBDL
        }
        return IrSignal(
            frequency = 36700,
            pattern = NecProtocol.encodeHex32(hex and 0xFFFFFFFFL),
            protocolName = "Panasonic 36.7kHz",
            hexDisplay = "0x" + hex.toString(16).uppercase()
        )
    }

    // ROKU IR
    private fun getRokuIrSignal(command: RemoteCommand): IrSignal {
        val hex = when (command) {
            RemoteCommand.POWER -> 0x5743D827L
            RemoteCommand.VOL_UP -> 0x5743B847L
            RemoteCommand.VOL_DOWN -> 0x57437887L
            RemoteCommand.MUTE -> 0x5743F807L
            RemoteCommand.NAV_UP -> 0x57439867L
            RemoteCommand.NAV_DOWN -> 0x5743CC33L
            RemoteCommand.NAV_LEFT -> 0x5743708FL
            RemoteCommand.NAV_RIGHT -> 0x5743B04FL
            RemoteCommand.NAV_OK -> 0x574354ABL
            RemoteCommand.BACK -> 0x57436699L
            RemoteCommand.HOME, RemoteCommand.MENU -> 0x5743C03FL
            else -> 0x5743D827L
        }
        return IrSignal(
            frequency = 38000,
            pattern = NecProtocol.encodeHex32(hex),
            protocolName = "Roku NEC 38kHz",
            hexDisplay = "0x" + hex.toString(16).uppercase()
        )
    }

    // GENERIC / UNIVERSAL NEC
    private fun getGenericSignal(command: RemoteCommand): IrSignal {
        val cmdByte = when (command) {
            RemoteCommand.POWER -> 0x12
            RemoteCommand.AV, RemoteCommand.SOURCE -> 0x0A
            RemoteCommand.VOL_UP -> 0x10
            RemoteCommand.VOL_DOWN -> 0x11
            RemoteCommand.MUTE -> 0x0D
            RemoteCommand.CH_UP -> 0x1E
            RemoteCommand.CH_DOWN -> 0x1F
            RemoteCommand.NAV_UP -> 0x01
            RemoteCommand.NAV_DOWN -> 0x02
            RemoteCommand.NAV_LEFT -> 0x03
            RemoteCommand.NAV_RIGHT -> 0x04
            RemoteCommand.NAV_OK -> 0x05
            RemoteCommand.BACK -> 0x08
            RemoteCommand.HOME, RemoteCommand.MENU -> 0x09
            RemoteCommand.SOURCE -> 0x0A
            RemoteCommand.NUM_0 -> 0x30
            RemoteCommand.NUM_1 -> 0x31
            RemoteCommand.NUM_2 -> 0x32
            RemoteCommand.NUM_3 -> 0x33
            RemoteCommand.NUM_4 -> 0x34
            RemoteCommand.NUM_5 -> 0x35
            RemoteCommand.NUM_6 -> 0x36
            RemoteCommand.NUM_7 -> 0x37
            RemoteCommand.NUM_8 -> 0x38
            RemoteCommand.NUM_9 -> 0x39
        }
        return IrSignal(
            frequency = 38000,
            pattern = NecProtocol.encodeAddressCommand(0x00, cmdByte),
            protocolName = "Generic NEC 38kHz",
            hexDisplay = "Cmd: 0x" + cmdByte.toString(16).uppercase()
        )
    }
}
