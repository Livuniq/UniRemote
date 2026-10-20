package com.example.universalremote.ir

import android.content.Context
import android.hardware.ConsumerIrManager
import android.util.Log

class IrManagerHelper(private val context: Context) {

    private val irManager: ConsumerIrManager? =
        try {
            context.getSystemService(Context.CONSUMER_IR_SERVICE) as? ConsumerIrManager
        } catch (e: Exception) {
            Log.w("IrManagerHelper", "Could not get ConsumerIrManager", e)
            null
        }

    val hasIrEmitter: Boolean
        get() = try {
            irManager?.hasIrEmitter() == true
        } catch (e: Exception) {
            false
        }

    fun getCarrierFrequenciesDescription(): String {
        return try {
            val ranges = irManager?.carrierFrequencies ?: return "No IR hardware detected"
            if (ranges.isEmpty()) "IR Present, frequency ranges unknown"
            else ranges.joinToString(", ") { "${it.minFrequency / 1000}kHz - ${it.maxFrequency / 1000}kHz" }
        } catch (e: Exception) {
            "Error querying frequencies: ${e.message}"
        }
    }

    fun transmit(frequency: Int, pattern: IntArray): Result<String> {
        return try {
            if (irManager == null || !hasIrEmitter) {
                // Return descriptive simulated transmit so users without hardware can still test the app
                return Result.success("Simulated IR (No hardware IR on device): Sent ${pattern.size} pulses at ${frequency / 1000}kHz")
            }
            irManager.transmit(frequency, pattern)
            Result.success("Transmitted ${pattern.size} pulses via IR hardware at ${frequency / 1000}kHz")
        } catch (e: Exception) {
            Log.e("IrManagerHelper", "IR transmit failed", e)
            Result.failure(e)
        }
    }
}
