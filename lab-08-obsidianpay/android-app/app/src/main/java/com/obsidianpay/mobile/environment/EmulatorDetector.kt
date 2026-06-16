package com.obsidianpay.mobile.environment

import android.os.Build

/**
 * EmulatorDetector — checks common Build.* fields to determine if the app is
 * running inside an Android emulator (AVD, Genymotion, VirtualBox, etc.).
 *
 * NOTE (instructor): just like RootDetector, every signal here is a simple
 * string check on static Build constants. The checks are intentionally simple
 * so that a student reading the smali/decompiled Kotlin can understand them
 * immediately. Each one can be hooked by Frida to return whatever the attacker
 * wants, or the constants can be changed with APK patching tools.
 */
object EmulatorDetector {

    data class EmulatorCheckResult(
        val isEmulator: Boolean,
        val score: Int,
        val signals: List<String>,
    )

    fun check(): EmulatorCheckResult {
        val signals = mutableListOf<String>()

        // Build.FINGERPRINT
        runCatching {
            val fp = Build.FINGERPRINT.lowercase()
            if (fp.contains("generic")) signals += "fingerprint:generic"
            if (fp.contains("unknown")) signals += "fingerprint:unknown"
        }

        // Build.MODEL
        runCatching {
            val model = Build.MODEL.lowercase()
            if (model.contains("google_sdk")) signals += "model:google_sdk"
            if (model.contains("emulator")) signals += "model:emulator"
            if (model.contains("android sdk built for x86")) signals += "model:android_sdk_x86"
        }

        // Build.MANUFACTURER
        runCatching {
            val mfr = Build.MANUFACTURER.lowercase()
            if (mfr.contains("genymotion")) signals += "manufacturer:Genymotion"
        }

        // Build.BRAND
        runCatching {
            val brand = Build.BRAND.lowercase()
            if (brand.contains("generic")) signals += "brand:generic"
        }

        // Build.DEVICE
        runCatching {
            val device = Build.DEVICE.lowercase()
            if (device.contains("generic")) signals += "device:generic"
        }

        // Build.PRODUCT
        runCatching {
            val product = Build.PRODUCT.lowercase()
            if (product.contains("sdk")) signals += "product:sdk"
            if (product.contains("google_sdk")) signals += "product:google_sdk"
            if (product.contains("emulator")) signals += "product:emulator"
            if (product.contains("vbox86p")) signals += "product:vbox86p"
        }

        // Build.HARDWARE
        runCatching {
            val hw = Build.HARDWARE.lowercase()
            if (hw.contains("goldfish")) signals += "hardware:goldfish"
            if (hw.contains("ranchu")) signals += "hardware:ranchu"
            if (hw.contains("vbox86")) signals += "hardware:vbox86"
        }

        val score = signals.size
        val isEmulator = score > 0
        return EmulatorCheckResult(isEmulator = isEmulator, score = score, signals = signals)
    }
}
