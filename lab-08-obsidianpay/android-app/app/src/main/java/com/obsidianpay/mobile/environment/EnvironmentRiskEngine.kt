package com.obsidianpay.mobile.environment

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

/**
 * EnvironmentRiskEngine — orchestrates root and emulator detection and
 * produces a structured risk report stored in local SharedPreferences.
 *
 * NOTE (instructor): the risk level computation is purely client-side. The
 * bypassHintId field in the report is a didactic pointer: it names the bypass
 * strategy (hook the return values, patch the risk engine result) without
 * spelling out the exact tool or script. A student who finds this value in
 * local storage, the SQLite db, or via the API response has a clear next step.
 *
 * Bypass strategies surfaced here:
 *   - "env-check-local-only"       → all checks run in-process; no server oracle
 *   - "hooks-change-return-values" → Frida can override RootDetector/EmulatorDetector
 *   - "patch-risk-engine-result"   → APK patching can hard-code riskLevel="low"
 */
object EnvironmentRiskEngine {

    data class EnvironmentReport(
        val root: Boolean,
        val emulator: Boolean,
        val rootScore: Int,
        val emulatorScore: Int,
        val riskLevel: String,
        val signals: List<String>,
        val generatedAt: Long,
        val bypassHintId: String,
    )

    fun run(context: Context): EnvironmentReport {
        val rootResult = RootDetector.check(context)
        val emulatorResult = EmulatorDetector.check()

        val totalScore = rootResult.score + emulatorResult.score
        val riskLevel = when {
            rootResult.isRooted && totalScore >= 3 -> "high"
            rootResult.isRooted || emulatorResult.isEmulator -> "medium"
            else -> "low"
        }

        val bypassHintId = when (riskLevel) {
            "high" -> "hooks-change-return-values"
            "medium" -> "env-check-local-only"
            else -> "patch-risk-engine-result"
        }

        val signals = rootResult.signals + emulatorResult.signals

        return EnvironmentReport(
            root = rootResult.isRooted,
            emulator = emulatorResult.isEmulator,
            rootScore = rootResult.score,
            emulatorScore = emulatorResult.score,
            riskLevel = riskLevel,
            signals = signals,
            generatedAt = System.currentTimeMillis(),
            bypassHintId = bypassHintId,
        )
    }

    fun toJson(report: EnvironmentReport): String {
        val signalsArr = JSONArray()
        report.signals.forEach { signalsArr.put(it) }
        return JSONObject()
            .put("root", report.root)
            .put("emulator", report.emulator)
            .put("rootScore", report.rootScore)
            .put("emulatorScore", report.emulatorScore)
            .put("riskLevel", report.riskLevel)
            .put("signals", signalsArr)
            .put("generatedAt", report.generatedAt)
            .put("bypassHintId", report.bypassHintId)
            .toString()
    }
}
