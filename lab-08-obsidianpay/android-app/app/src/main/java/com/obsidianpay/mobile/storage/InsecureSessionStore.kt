package com.obsidianpay.mobile.storage

import android.content.Context
import com.obsidianpay.mobile.util.Constants

/**
 * Local session store backed by plaintext SharedPreferences.
 *
 * NOTE (instructor): the storage here is intentionally simple/insecure for the
 * lab — the session token, profile cache and identifiers are persisted in
 * cleartext. This is a deliberate seam for a future "insecure data storage"
 * study. There is no encryption, no Keystore, and no obfuscation by design.
 */
class InsecureSessionStore(context: Context) {

    private val prefs =
        context.applicationContext.getSharedPreferences(Constants.PREFS_FILE, Context.MODE_PRIVATE)

    fun saveSession(
        token: String,
        username: String?,
        userId: Int?,
        role: String?,
        profileJson: String?,
    ) {
        prefs.edit().apply {
            putString(Constants.KEY_SESSION_TOKEN, token)
            putString(Constants.KEY_USERNAME, username)
            putInt(Constants.KEY_USER_ID, userId ?: -1)
            putString(Constants.KEY_ROLE, role)
            if (profileJson != null) putString(Constants.KEY_PROFILE_CACHE, profileJson)
            putLong(Constants.KEY_DEBUG_LAST_SYNC, System.currentTimeMillis())
        }.apply()
    }

    fun saveProfileCache(profileJson: String) {
        prefs.edit()
            .putString(Constants.KEY_PROFILE_CACHE, profileJson)
            .putLong(Constants.KEY_DEBUG_LAST_SYNC, System.currentTimeMillis())
            .apply()
    }

    fun saveReceiptsOffline(receiptsJson: String) {
        prefs.edit().putString(Constants.KEY_RECEIPTS_OFFLINE, receiptsJson).apply()
    }

    fun touchLastSync() {
        prefs.edit().putLong(Constants.KEY_DEBUG_LAST_SYNC, System.currentTimeMillis()).apply()
    }

    val token: String? get() = prefs.getString(Constants.KEY_SESSION_TOKEN, null)
    val username: String? get() = prefs.getString(Constants.KEY_USERNAME, null)
    val userId: Int get() = prefs.getInt(Constants.KEY_USER_ID, -1)
    val role: String? get() = prefs.getString(Constants.KEY_ROLE, null)
    val profileCache: String? get() = prefs.getString(Constants.KEY_PROFILE_CACHE, null)
    val lastSync: Long get() = prefs.getLong(Constants.KEY_DEBUG_LAST_SYNC, 0L)

    fun isLoggedIn(): Boolean = !token.isNullOrEmpty()

    fun clear() {
        prefs.edit().clear().apply()
    }
}
