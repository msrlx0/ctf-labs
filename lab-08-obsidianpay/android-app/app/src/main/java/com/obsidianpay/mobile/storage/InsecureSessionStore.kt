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

    // Backwards-compatible accessors used by earlier-phase screens.
    val token: String? get() = getToken()
    val username: String? get() = getUsername()
    val role: String? get() = getRole()

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
