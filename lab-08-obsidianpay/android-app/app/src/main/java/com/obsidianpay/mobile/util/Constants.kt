package com.obsidianpay.mobile.util

/**
 * App-wide constants for the ObsidianPay mobile client (Lab 08).
 *
 * The base URL targets the Android Emulator loopback alias (10.0.2.2), which
 * maps to the host's 127.0.0.1 where the lab backend is published on port 8102.
 */
object Constants {

    const val DEFAULT_BASE_URL: String = "http://10.0.2.2:8102"
    const val API_VERSION_LABEL: String = "v1"

    // SharedPreferences file used by InsecureSessionStore.
    const val PREFS_FILE: String = "obsidian_session_prefs"

    // Client storage keys — mirror the backend's mobileConfig.clientStorageKeys.
    const val KEY_SESSION_TOKEN: String = "obsidian.session.token"
    const val KEY_PROFILE_CACHE: String = "obsidian.profile.cache"
    const val KEY_RECEIPTS_OFFLINE: String = "obsidian.receipts.offline"
    const val KEY_DEBUG_LAST_SYNC: String = "obsidian.debug.last_sync"

    // Extra session fields kept locally for convenience.
    const val KEY_USERNAME: String = "obsidian.session.username"
    const val KEY_USER_ID: String = "obsidian.session.user_id"
    const val KEY_ROLE: String = "obsidian.session.role"
    const val KEY_PLAN: String = "obsidian.session.plan"
    const val KEY_DAILY_LIMIT: String = "obsidian.session.daily_limit"
    const val KEY_KYC_APPROVED: String = "obsidian.session.kyc_approved"

    // Cached raw API payloads (plaintext, intentional for the lab).
    const val KEY_RAW_CONFIG: String = "obsidian.config.cache"
    const val KEY_LAST_SUPPORT_SYNC: String = "obsidian.debug.last_support_sync"
    const val KEY_LAST_DIAGNOSTICS: String = "obsidian.debug.last_diagnostics"
    const val KEY_LAST_TRANSFER_PREVIEW: String = "obsidian.debug.last_transfer_preview"
    const val KEY_LAST_OPENED_RECEIPT: String = "obsidian.debug.last_opened_receipt"
    const val KEY_LAST_OPENED_CARD: String = "obsidian.debug.last_opened_card"
    const val KEY_BASE_URL_HINT: String = "obsidian.debug.base_url_hint"

    // Debug header expected by the backend's diagnostics endpoint.
    const val DEBUG_HEADER_NAME: String = "X-Obsidian-Debug"
    const val DEBUG_HEADER_VALUE: String = "mobile-diagnostics"
}
