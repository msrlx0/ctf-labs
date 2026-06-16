package com.obsidianpay.mobile.environment

import android.content.Context
import android.content.pm.PackageManager
import java.io.File

/**
 * RootDetector — checks for common indicators that the device may be rooted.
 *
 * NOTE (instructor): every check here is didactic and best-effort. None of
 * these signals is individually conclusive; a real root-detection library
 * uses far more heuristics. The point is that ALL of these checks run
 * client-side, their results are readable in local storage, and each one
 * can be trivially bypassed via Frida hooks or APK patching. The
 * bypassHintId in EnvironmentRiskEngine points students here.
 */
object RootDetector {

    data class RootCheckResult(
        val isRooted: Boolean,
        val score: Int,
        val signals: List<String>,
    )

    private val SU_PATHS = listOf(
        "/system/bin/su",
        "/system/xbin/su",
        "/sbin/su",
        "/su/bin/su",
        "/system/app/Superuser.apk",
        "/system/bin/.ext/.su",
    )

    private val SUSPICIOUS_PACKAGES = listOf(
        "com.topjohnwu.magisk",
        "eu.chainfire.supersu",
        "com.koushikdutta.superuser",
    )

    fun check(context: Context): RootCheckResult {
        val signals = mutableListOf<String>()

        // 1 — su binary paths
        for (path in SU_PATHS) {
            runCatching {
                if (File(path).exists()) signals += "su-path:$path"
            }
        }

        // 2 — suspicious packages via PackageManager
        val pm = context.packageManager
        for (pkg in SUSPICIOUS_PACKAGES) {
            runCatching {
                pm.getPackageInfo(pkg, 0)
                signals += "suspicious-pkg:$pkg"
            }
        }

        // 3 — test-keys build tag (AOSP debug/test builds)
        runCatching {
            val tags = android.os.Build.TAGS ?: ""
            if (tags.contains("test-keys")) signals += "build-tags:test-keys"
        }

        // 4 — ro.debuggable system property
        runCatching {
            val cls = Class.forName("android.os.SystemProperties")
            val get = cls.getMethod("get", String::class.java, String::class.java)
            val debuggable = get.invoke(null, "ro.debuggable", "0") as? String
            if (debuggable == "1") signals += "prop:ro.debuggable=1"
        }

        // 5 — ro.secure system property (0 = unlocked bootloader region)
        runCatching {
            val cls = Class.forName("android.os.SystemProperties")
            val get = cls.getMethod("get", String::class.java, String::class.java)
            val secure = get.invoke(null, "ro.secure", "1") as? String
            if (secure == "0") signals += "prop:ro.secure=0"
        }

        val score = signals.size
        val isRooted = score > 0
        return RootCheckResult(isRooted = isRooted, score = score, signals = signals)
    }
}
