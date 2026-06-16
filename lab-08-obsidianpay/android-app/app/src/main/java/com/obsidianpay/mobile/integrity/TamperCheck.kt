package com.obsidianpay.mobile.integrity

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.os.Build
import java.security.MessageDigest

/**
 * TamperCheck — Phase 12 scaffold.
 *
 * Performs a set of didactic tamper-detection checks: debuggable flag,
 * installer package, signature hash and package name. Results are scored and
 * serialized to JSON for transmission to the backend integrity endpoint.
 *
 * Teaching anchors (instructor):
 *   - patch-debuggable-check: patch ApplicationInfo.FLAG_DEBUGGABLE check.
 *   - hook-package-manager: hook getInstallerPackageName / getPackageInfo.
 *   - repackage-signature-mismatch: a repackaged APK produces a different hash.
 *   - All checks are client-side only — trivially bypassable via Frida or patch.
 */
object TamperCheck {

    private const val EXPECTED_PACKAGE_NAME = "com.obsidianpay.mobile"

    // --- Public API -------------------------------------------------------------

    fun run(context: Context): TamperResult {
        val debuggable = isDebuggable(context)
        val installerPkg = getInstallerPackage(context)
        val pkgNameStatus = getPackageNameStatus(context)
        val sigHash = getSignatureHashPreview(context)
        val score = calculateScore(debuggable, installerPkg, pkgNameStatus)

        return TamperResult(
            debuggable = debuggable,
            installerPackage = installerPkg ?: "unknown-installer",
            packageNameStatus = pkgNameStatus,
            signatureHashPreview = sigHash,
            tamperScore = score,
            bypassHints = buildTamperBypassHints(),
        )
    }

    fun isDebuggable(context: Context): Boolean {
        return try {
            val flags = context.applicationContext.applicationInfo.flags
            (flags and ApplicationInfo.FLAG_DEBUGGABLE) != 0
        } catch (_: Exception) {
            false
        }
    }

    fun getInstallerPackage(context: Context): String? {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                context.packageManager
                    .getInstallSourceInfo(context.packageName)
                    .installingPackageName
            } else {
                @Suppress("DEPRECATION")
                context.packageManager.getInstallerPackageName(context.packageName)
            }
        } catch (_: Exception) {
            null
        }
    }

    fun getPackageNameStatus(context: Context): String {
        val actual = context.packageName
        return if (actual == EXPECTED_PACKAGE_NAME) {
            "package-name-check:match"
        } else {
            "package-name-check:mismatch:$actual"
        }
    }

    fun buildTamperBypassHints(): List<String> = listOf(
        "patch-debuggable-check",
        "hook-package-manager",
        "repackage-signature-mismatch",
        "debuggable-build",
        "unknown-installer",
        "signature-hash-observed",
        "package-name-check",
        "tamper-score",
    )

    fun toJson(result: TamperResult): String = buildString {
        append("{")
        append("\"tamperScore\":${result.tamperScore},")
        append("\"debuggable\":${result.debuggable},")
        append("\"installerPackage\":\"${result.installerPackage}\",")
        append("\"packageNameStatus\":\"${result.packageNameStatus}\",")
        append("\"signatureHashPreview\":\"${result.signatureHashPreview}\",")
        append("\"bypassHints\":[${result.bypassHints.joinToString(",") { "\"$it\"" }}]")
        append("}")
    }

    // --- Internal helpers -------------------------------------------------------

    private fun getSignatureHashPreview(context: Context): String {
        return try {
            val info = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                context.packageManager.getPackageInfo(
                    context.packageName,
                    PackageManager.GET_SIGNING_CERTIFICATES,
                )
            } else {
                @Suppress("DEPRECATION")
                context.packageManager.getPackageInfo(
                    context.packageName,
                    PackageManager.GET_SIGNATURES,
                )
            }

            val sigBytes: ByteArray? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                info.signingInfo?.apkContentsSigners?.firstOrNull()?.toByteArray()
            } else {
                @Suppress("DEPRECATION")
                info.signatures?.firstOrNull()?.toByteArray()
            }

            if (sigBytes != null) {
                val digest = MessageDigest.getInstance("SHA-256").digest(sigBytes)
                val hex = digest.joinToString("") { "%02x".format(it) }
                // signature-hash-observed: first 16 chars of SHA-256 as a preview
                "signature-hash-observed:${hex.take(16)}…"
            } else {
                "signature-hash-observed:none"
            }
        } catch (_: Exception) {
            "signature-hash-observed:error"
        }
    }

    private fun calculateScore(
        debuggable: Boolean,
        installerPkg: String?,
        pkgNameStatus: String,
    ): Int {
        var score = 0
        // debuggable-build adds to tamper score
        if (debuggable) score += 40
        // unknown-installer raises score
        if (installerPkg.isNullOrBlank() || installerPkg == "unknown-installer") score += 30
        // package-name-check mismatch raises score
        if (pkgNameStatus.contains("mismatch")) score += 30
        return score.coerceIn(0, 100)
    }
}

// --- Result model ---------------------------------------------------------------

data class TamperResult(
    val debuggable: Boolean,
    val installerPackage: String,
    val packageNameStatus: String,
    val signatureHashPreview: String,
    val tamperScore: Int,
    val bypassHints: List<String>,
)
