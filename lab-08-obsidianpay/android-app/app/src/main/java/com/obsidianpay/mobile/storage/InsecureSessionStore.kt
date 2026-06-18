package com.obsidianpay.mobile.storage

import android.content.Context
import com.obsidianpay.mobile.util.Constants

/**
 * Local session store backed by plaintext SharedPreferences.
 *
 * NOTE (instructor): the storage here is intentionally simple/insecure for the
 * lab — the session token, profile/config cache and identifiers are persisted
 * in cleartext under shared_prefs/. This is a deliberate seam for a future
 * "insecure data storage" study. No encryption, no Keystore, no obfuscation by
 * design.
 */
class InsecureSessionStore(context: Context) {

    private val prefs =
        context.applicationContext.getSharedPreferences(Constants.PREFS_FILE, Context.MODE_PRIVATE)

    // --- Writes -----------------------------------------------------------------

    fun saveLoginSession(
        token: String,
        username: String?,
        userId: Int?,
        role: String?,
        plan: String? = null,
        dailyLimit: Double? = null,
        kycApproved: Boolean? = null,
        rawProfileJson: String? = null,
        baseUrlHint: String = Constants.DEFAULT_BASE_URL,
    ) {
        prefs.edit().apply {
            putString(Constants.KEY_SESSION_TOKEN, token)
            putString(Constants.KEY_USERNAME, username)
            putInt(Constants.KEY_USER_ID, userId ?: -1)
            putString(Constants.KEY_ROLE, role)
            if (plan != null) putString(Constants.KEY_PLAN, plan)
            if (dailyLimit != null) putString(Constants.KEY_DAILY_LIMIT, dailyLimit.toString())
            if (kycApproved != null) putBoolean(Constants.KEY_KYC_APPROVED, kycApproved)
            if (rawProfileJson != null) putString(Constants.KEY_PROFILE_CACHE, rawProfileJson)
            putString(Constants.KEY_BASE_URL_HINT, baseUrlHint)
            putLong(Constants.KEY_DEBUG_LAST_SYNC, System.currentTimeMillis())
        }.apply()
    }

    fun saveProfileCache(
        rawProfileJson: String,
        plan: String? = null,
        dailyLimit: Double? = null,
        kycApproved: Boolean? = null,
    ) {
        prefs.edit().apply {
            putString(Constants.KEY_PROFILE_CACHE, rawProfileJson)
            if (plan != null) putString(Constants.KEY_PLAN, plan)
            if (dailyLimit != null) putString(Constants.KEY_DAILY_LIMIT, dailyLimit.toString())
            if (kycApproved != null) putBoolean(Constants.KEY_KYC_APPROVED, kycApproved)
            putLong(Constants.KEY_DEBUG_LAST_SYNC, System.currentTimeMillis())
        }.apply()
    }

    fun saveConfigCache(rawConfigJson: String) = putAndTouch(Constants.KEY_RAW_CONFIG, rawConfigJson)

    fun saveReceiptsOffline(receiptsJson: String) =
        prefs.edit().putString(Constants.KEY_RECEIPTS_OFFLINE, receiptsJson).apply()

    fun saveSupportSync(rawJson: String) = putAndTouch(Constants.KEY_LAST_SUPPORT_SYNC, rawJson)

    fun saveDiagnostics(rawJson: String) = putAndTouch(Constants.KEY_LAST_DIAGNOSTICS, rawJson)

    fun saveTransferPreview(rawJson: String) =
        putAndTouch(Constants.KEY_LAST_TRANSFER_PREVIEW, rawJson)

    fun saveLastOpenedReceipt(receiptId: String) =
        putAndTouch(Constants.KEY_LAST_OPENED_RECEIPT, receiptId)

    fun saveLastOpenedCard(cardId: String) = putAndTouch(Constants.KEY_LAST_OPENED_CARD, cardId)

    fun saveLastDeepLink(rawUri: String, type: String) =
        putAndTouch(Constants.KEY_LAST_DEEP_LINK, "$type | $rawUri")

    fun saveLastQrPayload(payload: String, type: String) =
        putAndTouch(Constants.KEY_LAST_QR_PAYLOAD, "$type | $payload")

    fun saveLastWebViewUrl(url: String) = putAndTouch(Constants.KEY_LAST_WEBVIEW_URL, url)

    // --- Exported components (Phase 7) ------------------------------------------

    /** Didactic "operator hint" toggled by the exported debug receiver. */
    fun saveOperatorHint(value: String) = putAndTouch(Constants.KEY_OPERATOR_HINT, value)

    /** Last debug command received from the exported BroadcastReceiver. */
    fun saveExternalDebugCommand(command: String, details: String?) =
        putAndTouch(Constants.KEY_LAST_EXTERNAL_DEBUG_COMMAND, "$command | ${details ?: ""}")

    /** Last event triggered by any exported component (Activity/Receiver/Provider). */
    fun saveLastExportedEvent(value: String) = putAndTouch(Constants.KEY_LAST_EXPORTED_EVENT, value)

    // --- Stage 03 exported-components checkpoint proofs (Phase 20) ---------------
    // Written by the exported Activity / Receiver when triggered; read back (and
    // consolidated with the provider proof) by the exported ContentProvider.

    /** Proof emitted by the exported [com.obsidianpay.mobile.platform.InternalOpsActivity]. */
    fun saveCheckpointActivityProof(value: String) =
        putAndTouch(Constants.KEY_CHECKPOINT_ACTIVITY_PROOF, value)

    fun getCheckpointActivityProof(): String? =
        prefs.getString(Constants.KEY_CHECKPOINT_ACTIVITY_PROOF, null)

    /** Proof emitted by the exported [com.obsidianpay.mobile.platform.DebugCommandReceiver]. */
    fun saveCheckpointReceiverProof(value: String) =
        putAndTouch(Constants.KEY_CHECKPOINT_RECEIVER_PROOF, value)

    fun getCheckpointReceiverProof(): String? =
        prefs.getString(Constants.KEY_CHECKPOINT_RECEIVER_PROOF, null)

    // --- Device Trust / reverse-engineering trail (Phase 8) ---------------------

    fun saveLastDeviceTrustJson(rawJson: String) =
        putAndTouch(Constants.KEY_LAST_DEVICE_TRUST, rawJson)

    fun saveLastLegacySignature(signature: String) =
        putAndTouch(Constants.KEY_LAST_LEGACY_SIGNATURE, signature)

    fun saveLastEncodedOperatorHint(value: String) =
        putAndTouch(Constants.KEY_LAST_ENCODED_OPERATOR_HINT, value)

    // --- Environment / risk-check (Phase 9) ------------------------------------

    fun saveLastEnvironmentReportJson(rawJson: String) =
        putAndTouch(Constants.KEY_LAST_ENVIRONMENT_REPORT, rawJson)

    fun saveLastEnvironmentResponseJson(rawJson: String) =
        putAndTouch(Constants.KEY_LAST_ENVIRONMENT_RESPONSE, rawJson)

    fun getLastEnvironmentReportJson(): String? =
        prefs.getString(Constants.KEY_LAST_ENVIRONMENT_REPORT, null)

    fun getLastEnvironmentResponseJson(): String? =
        prefs.getString(Constants.KEY_LAST_ENVIRONMENT_RESPONSE, null)

    fun clearEnvironmentReport() {
        prefs.edit()
            .remove(Constants.KEY_LAST_ENVIRONMENT_REPORT)
            .remove(Constants.KEY_LAST_ENVIRONMENT_RESPONSE)
            .apply()
    }

    // --- Vault / local auth (Phase 10) ------------------------------------------

    fun saveVaultUnlocked(value: Boolean, reason: String) {
        prefs.edit()
            .putBoolean(Constants.KEY_VAULT_UNLOCKED, value)
            .putString(Constants.KEY_VAULT_UNLOCK_REASON, reason)
            .putLong(Constants.KEY_DEBUG_LAST_SYNC, System.currentTimeMillis())
            .apply()
    }

    fun getVaultUnlocked(): Boolean = prefs.getBoolean(Constants.KEY_VAULT_UNLOCKED, false)

    fun getLastVaultUnlockReason(): String? =
        prefs.getString(Constants.KEY_VAULT_UNLOCK_REASON, null)

    fun saveLastVaultStatusJson(rawJson: String) =
        putAndTouch(Constants.KEY_LAST_VAULT_STATUS_JSON, rawJson)

    fun saveLastVaultUnlockJson(rawJson: String) =
        putAndTouch(Constants.KEY_LAST_VAULT_UNLOCK_JSON, rawJson)

    fun getLastVaultStatusJson(): String? =
        prefs.getString(Constants.KEY_LAST_VAULT_STATUS_JSON, null)

    fun getLastVaultUnlockJson(): String? =
        prefs.getString(Constants.KEY_LAST_VAULT_UNLOCK_JSON, null)

    fun clearVaultState() {
        prefs.edit()
            .remove(Constants.KEY_VAULT_UNLOCKED)
            .remove(Constants.KEY_VAULT_UNLOCK_REASON)
            .remove(Constants.KEY_LAST_VAULT_STATUS_JSON)
            .remove(Constants.KEY_LAST_VAULT_UNLOCK_JSON)
            .apply()
    }

    // --- Network security / API host override (Phase 11) ------------------------
    // Stored in plaintext SharedPreferences — intentional teaching seam.

    fun saveApiBaseUrlOverride(value: String) =
        putAndTouch(Constants.KEY_API_BASE_URL_OVERRIDE, value)

    fun getApiBaseUrlOverride(): String? =
        prefs.getString(Constants.KEY_API_BASE_URL_OVERRIDE, null)

    fun clearApiBaseUrlOverride() {
        prefs.edit().remove(Constants.KEY_API_BASE_URL_OVERRIDE).apply()
    }

    fun saveLastNetworkProfileJson(rawJson: String) =
        putAndTouch(Constants.KEY_LAST_NETWORK_PROFILE_JSON, rawJson)

    fun getLastNetworkProfileJson(): String? =
        prefs.getString(Constants.KEY_LAST_NETWORK_PROFILE_JSON, null)

    fun saveLastPinningMode(value: String) =
        putAndTouch(Constants.KEY_LAST_PINNING_MODE, value)

    fun getLastPinningMode(): String? =
        prefs.getString(Constants.KEY_LAST_PINNING_MODE, null)

    fun saveLastPinningHint(value: String) =
        putAndTouch(Constants.KEY_LAST_PINNING_HINT, value)

    fun getLastPinningHint(): String? =
        prefs.getString(Constants.KEY_LAST_PINNING_HINT, null)

    // --- App integrity / NativeGate / TamperCheck (Phase 12) --------------------

    fun saveLastAppIntegrityReport(rawJson: String) =
        putAndTouch(Constants.KEY_LAST_APP_INTEGRITY_REPORT, rawJson)

    fun getLastAppIntegrityReport(): String? =
        prefs.getString(Constants.KEY_LAST_APP_INTEGRITY_REPORT, null)

    fun saveLastAppIntegrityResponse(rawJson: String) =
        putAndTouch(Constants.KEY_LAST_APP_INTEGRITY_RESPONSE, rawJson)

    fun getLastAppIntegrityResponse(): String? =
        prefs.getString(Constants.KEY_LAST_APP_INTEGRITY_RESPONSE, null)

    fun saveLastNativeGateStatus(value: String) =
        putAndTouch(Constants.KEY_LAST_NATIVE_GATE_STATUS, value)

    fun getLastNativeGateStatus(): String? =
        prefs.getString(Constants.KEY_LAST_NATIVE_GATE_STATUS, null)

    fun saveLastTamperScore(value: String) =
        putAndTouch(Constants.KEY_LAST_TAMPER_SCORE, value)

    fun getLastTamperScore(): String? =
        prefs.getString(Constants.KEY_LAST_TAMPER_SCORE, null)

    fun saveLastSignatureHashPreview(value: String) =
        putAndTouch(Constants.KEY_LAST_SIGNATURE_HASH_PREVIEW, value)

    fun getLastSignatureHashPreview(): String? =
        prefs.getString(Constants.KEY_LAST_SIGNATURE_HASH_PREVIEW, null)

    fun clearIntegrityState() {
        prefs.edit()
            .remove(Constants.KEY_LAST_APP_INTEGRITY_REPORT)
            .remove(Constants.KEY_LAST_APP_INTEGRITY_RESPONSE)
            .remove(Constants.KEY_LAST_NATIVE_GATE_STATUS)
            .remove(Constants.KEY_LAST_TAMPER_SCORE)
            .remove(Constants.KEY_LAST_SIGNATURE_HASH_PREVIEW)
            .apply()
    }

    private fun putAndTouch(key: String, value: String) {
        prefs.edit()
            .putString(key, value)
            .putLong(Constants.KEY_DEBUG_LAST_SYNC, System.currentTimeMillis())
            .apply()
    }

    fun touchLastSync() {
        prefs.edit().putLong(Constants.KEY_DEBUG_LAST_SYNC, System.currentTimeMillis()).apply()
    }

    // --- Reads ------------------------------------------------------------------

    fun getToken(): String? = prefs.getString(Constants.KEY_SESSION_TOKEN, null)
    fun getUsername(): String? = prefs.getString(Constants.KEY_USERNAME, null)
    fun getUserId(): Int = prefs.getInt(Constants.KEY_USER_ID, -1)
    fun getRole(): String? = prefs.getString(Constants.KEY_ROLE, null)
    fun getPlan(): String? = prefs.getString(Constants.KEY_PLAN, null)
    fun getDailyLimit(): String? = prefs.getString(Constants.KEY_DAILY_LIMIT, null)
    fun getRawProfileJson(): String? = prefs.getString(Constants.KEY_PROFILE_CACHE, null)
    fun getRawConfigJson(): String? = prefs.getString(Constants.KEY_RAW_CONFIG, null)
    fun getLastSupportSync(): String? = prefs.getString(Constants.KEY_LAST_SUPPORT_SYNC, null)
    fun getLastTransferPreview(): String? = prefs.getString(Constants.KEY_LAST_TRANSFER_PREVIEW, null)
    fun getLastOpenedReceipt(): String? = prefs.getString(Constants.KEY_LAST_OPENED_RECEIPT, null)
    fun getLastOpenedCard(): String? = prefs.getString(Constants.KEY_LAST_OPENED_CARD, null)
    fun getLastDeepLink(): String? = prefs.getString(Constants.KEY_LAST_DEEP_LINK, null)
    fun getLastQrPayload(): String? = prefs.getString(Constants.KEY_LAST_QR_PAYLOAD, null)
    fun getLastWebViewUrl(): String? = prefs.getString(Constants.KEY_LAST_WEBVIEW_URL, null)
    fun getOperatorHint(): String? = prefs.getString(Constants.KEY_OPERATOR_HINT, null)
    fun getLastExternalDebugCommand(): String? =
        prefs.getString(Constants.KEY_LAST_EXTERNAL_DEBUG_COMMAND, null)
    fun getLastExportedEvent(): String? = prefs.getString(Constants.KEY_LAST_EXPORTED_EVENT, null)
    fun getLastDeviceTrustJson(): String? = prefs.getString(Constants.KEY_LAST_DEVICE_TRUST, null)
    fun getLastLegacySignature(): String? = prefs.getString(Constants.KEY_LAST_LEGACY_SIGNATURE, null)
    fun getLastEncodedOperatorHint(): String? =
        prefs.getString(Constants.KEY_LAST_ENCODED_OPERATOR_HINT, null)
    fun getLastSyncTimestamp(): Long = prefs.getLong(Constants.KEY_DEBUG_LAST_SYNC, 0L)

    /**
     * Masked preview of the session token. Never returns the full token — only a
     * short head plus the length, so callers (e.g. the exported provider) can hint
     * "a token exists" without disclosing it.
     */
    fun getTokenPreview(): String? {
        val t = getToken()
        if (t.isNullOrEmpty()) return null
        return "${t.take(8)}…(${t.length} chars)"
    }

    // NOTE (Phase 20): the session getters are exposed ONLY as explicit methods
    // (getToken()/getUsername()/getRole()). Do NOT re-add Kotlin `val token`/
    // `val username`/`val role` properties: each would synthesise a JVM getter
    // with the SAME signature as the method above (e.g. `getToken()`), producing
    // a "platform declaration clash" that breaks compilation. The single-API rule
    // (methods, not properties) is the fix — see scripts/validate-phase20.sh.
    fun isLoggedIn(): Boolean = !getToken().isNullOrEmpty()

    /** Snapshot of the main local values, for the internal state screen. */
    fun getAllDebugValues(): Map<String, String?> = linkedMapOf(
        Constants.KEY_SESSION_TOKEN to getToken(),
        Constants.KEY_USERNAME to getUsername(),
        Constants.KEY_USER_ID to getUserId().takeIf { it >= 0 }?.toString(),
        Constants.KEY_ROLE to getRole(),
        Constants.KEY_PLAN to getPlan(),
        Constants.KEY_DAILY_LIMIT to getDailyLimit(),
        Constants.KEY_KYC_APPROVED to prefs.all[Constants.KEY_KYC_APPROVED]?.toString(),
        Constants.KEY_BASE_URL_HINT to prefs.getString(Constants.KEY_BASE_URL_HINT, null),
        Constants.KEY_PROFILE_CACHE to getRawProfileJson(),
        Constants.KEY_RAW_CONFIG to getRawConfigJson(),
        Constants.KEY_RECEIPTS_OFFLINE to prefs.getString(Constants.KEY_RECEIPTS_OFFLINE, null),
        Constants.KEY_LAST_SUPPORT_SYNC to prefs.getString(Constants.KEY_LAST_SUPPORT_SYNC, null),
        Constants.KEY_LAST_DIAGNOSTICS to prefs.getString(Constants.KEY_LAST_DIAGNOSTICS, null),
        Constants.KEY_LAST_TRANSFER_PREVIEW to prefs.getString(Constants.KEY_LAST_TRANSFER_PREVIEW, null),
        Constants.KEY_LAST_OPENED_RECEIPT to prefs.getString(Constants.KEY_LAST_OPENED_RECEIPT, null),
        Constants.KEY_LAST_OPENED_CARD to prefs.getString(Constants.KEY_LAST_OPENED_CARD, null),
        Constants.KEY_LAST_DEEP_LINK to prefs.getString(Constants.KEY_LAST_DEEP_LINK, null),
        Constants.KEY_LAST_QR_PAYLOAD to prefs.getString(Constants.KEY_LAST_QR_PAYLOAD, null),
        Constants.KEY_LAST_WEBVIEW_URL to prefs.getString(Constants.KEY_LAST_WEBVIEW_URL, null),
        Constants.KEY_OPERATOR_HINT to getOperatorHint(),
        Constants.KEY_LAST_EXTERNAL_DEBUG_COMMAND to getLastExternalDebugCommand(),
        Constants.KEY_LAST_EXPORTED_EVENT to getLastExportedEvent(),
        Constants.KEY_CHECKPOINT_ACTIVITY_PROOF to getCheckpointActivityProof(),
        Constants.KEY_CHECKPOINT_RECEIVER_PROOF to getCheckpointReceiverProof(),
        Constants.KEY_LAST_DEVICE_TRUST to getLastDeviceTrustJson(),
        Constants.KEY_LAST_LEGACY_SIGNATURE to getLastLegacySignature(),
        Constants.KEY_LAST_ENCODED_OPERATOR_HINT to getLastEncodedOperatorHint(),
        Constants.KEY_LAST_ENVIRONMENT_REPORT to getLastEnvironmentReportJson(),
        Constants.KEY_LAST_ENVIRONMENT_RESPONSE to getLastEnvironmentResponseJson(),
        Constants.KEY_VAULT_UNLOCKED to getVaultUnlocked().toString(),
        Constants.KEY_VAULT_UNLOCK_REASON to getLastVaultUnlockReason(),
        Constants.KEY_LAST_VAULT_STATUS_JSON to getLastVaultStatusJson(),
        Constants.KEY_LAST_VAULT_UNLOCK_JSON to getLastVaultUnlockJson(),
        // Phase 11 — network security / API host override (preview only, not full URL token).
        Constants.KEY_API_BASE_URL_OVERRIDE to getApiBaseUrlOverride(),
        Constants.KEY_LAST_NETWORK_PROFILE_JSON to getLastNetworkProfileJson(),
        Constants.KEY_LAST_PINNING_MODE to getLastPinningMode(),
        Constants.KEY_LAST_PINNING_HINT to getLastPinningHint(),
        // Phase 12 — app integrity / NativeGate / TamperCheck (preview; no full token).
        Constants.KEY_LAST_APP_INTEGRITY_REPORT to getLastAppIntegrityReport(),
        Constants.KEY_LAST_APP_INTEGRITY_RESPONSE to getLastAppIntegrityResponse(),
        Constants.KEY_LAST_NATIVE_GATE_STATUS to getLastNativeGateStatus(),
        Constants.KEY_LAST_TAMPER_SCORE to getLastTamperScore(),
        Constants.KEY_LAST_SIGNATURE_HASH_PREVIEW to getLastSignatureHashPreview(),
        Constants.KEY_DEBUG_LAST_SYNC to getLastSyncTimestamp().takeIf { it > 0 }?.toString(),
    )

    /**
     * Provider-safe view of the local debug values: identical to
     * [getAllDebugValues] but the full session token is **removed** and replaced
     * by a masked `token_preview`. Used by the exported [ObsidianNotesProvider] so
     * an external query never reads the full token.
     */
    fun getSafeDebugValuesForProvider(): Map<String, String?> {
        val out = linkedMapOf<String, String?>()
        getAllDebugValues().forEach { (k, v) ->
            if (k == Constants.KEY_SESSION_TOKEN) return@forEach // never expose full token
            out[k] = v
        }
        getTokenPreview()?.let { out["token_preview"] = it }
        return out
    }

    fun clear() {
        prefs.edit().clear().apply()
    }
}
