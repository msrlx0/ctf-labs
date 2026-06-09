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
    fun getLastSyncTimestamp(): Long = prefs.getLong(Constants.KEY_DEBUG_LAST_SYNC, 0L)

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
        Constants.KEY_DEBUG_LAST_SYNC to getLastSyncTimestamp().takeIf { it > 0 }?.toString(),
    )

    fun clear() {
        prefs.edit().clear().apply()
    }
}
