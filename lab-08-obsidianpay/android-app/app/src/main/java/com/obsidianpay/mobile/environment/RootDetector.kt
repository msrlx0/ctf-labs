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
 *
 * Phase 20: the path list was widened so that BASIC, VISIBLE root (common `su`
 * locations incl. /system_ext, /vendor, /data/local; the Magisk directories;
 * known root packages; `which su`) is actually detected — the previous list
 * missed real-device paths and could report root:false on a rooted phone. This
 * remains intentionally bypassable: Frida hooks on File.exists()/return values,
 * Magisk Hide / Zygisk path hiding, or APK patching all defeat it. Root
 * detection is a signal, never a standalone security decision.
 */
object RootDetector {

    data class RootCheckResult(
        val isRooted: Boolean,
        val score: Int,
        val signals: List<String>,
    )

    // Common `su` binary locations across real ROMs / root solutions. Phase 20
    // widened this list so a genuinely rooted device (incl. system_ext, vendor and
    // data-local variants) is detected instead of silently reporting root:false.
    private val SU_PATHS = listOf(
        "/system/bin/su",
        "/system/xbin/su",
        "/system_ext/bin/su",
        "/sbin/su",
        "/su/bin/su",
        "/vendor/bin/su",
        "/data/local/bin/su",
        "/data/local/xbin/su",
        "/system/app/Superuser.apk",
        "/system/bin/.ext/.su",
    )

    // Magisk / root-manager directories that exist on a rooted device.
    private val ROOT_DIRECTORIES = listOf(
        "/data/adb/magisk",
        "/data/adb/modules",
    )

    private val SUSPICIOUS_PACKAGES = listOf(
        "com.topjohnwu.magisk",
        "eu.chainfire.supersu",
        "com.koushikdutta.superuser",
        "com.noshufou.android.su",
    )

    fun check(context: Context): RootCheckResult {
        val signals = mutableListOf<String>()

        // 1 — su binary paths
        for (path in SU_PATHS) {
            runCatching {
                if (File(path).exists()) signals += "file:$path"
            }
        }

        // 1b — root-manager directories (Magisk and its module store)
        for (dir in ROOT_DIRECTORIES) {
            runCatching {
                if (File(dir).exists()) signals += "directory:$dir"
            }
        }

        // 2 — suspicious packages via PackageManager
        val pm = context.packageManager
        for (pkg in SUSPICIOUS_PACKAGES) {
            runCatching {
                pm.getPackageInfo(pkg, 0)
                signals += "package:$pkg"
            }
        }

        // 2b — `which su` on PATH (best-effort; safe, read-only, never blocks).
        runCatching {
            val process = ProcessBuilder("which", "su").redirectErrorStream(true).start()
            val located = process.inputStream.bufferedReader().use { it.readLine() }?.trim()
            process.waitFor()
            if (!located.isNullOrEmpty()) signals += "which:su=$located"
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
