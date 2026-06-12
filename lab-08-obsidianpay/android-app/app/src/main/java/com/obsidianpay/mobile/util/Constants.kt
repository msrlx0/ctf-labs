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

    // Deep link / QR / WebView surfaces (Phase 5).
    const val KEY_LAST_DEEP_LINK: String = "obsidian.debug.last_deep_link"
    const val KEY_LAST_QR_PAYLOAD: String = "obsidian.debug.last_qr_payload"
    const val KEY_LAST_WEBVIEW_URL: String = "obsidian.debug.last_webview_url"

    // Exported Android components (Phase 7). These local values are written by
    // the exported Activity/Receiver and surfaced by the exported Provider — all
    // intentionally reachable by other apps, as a controlled study seam.
    const val KEY_OPERATOR_HINT: String = "obsidian.support.operator_hint"
    const val KEY_LAST_EXTERNAL_DEBUG_COMMAND: String = "obsidian.debug.last_external_command"
    const val KEY_LAST_EXPORTED_EVENT: String = "obsidian.debug.last_exported_event"

    // Predictable intent action / extra keys for the exported components (Phase 7).
    // Predictability is intentional: any app can craft these intents.
    const val ACTION_INTERNAL_OPS: String = "com.obsidianpay.mobile.INTERNAL_OPS"
    const val ACTION_DEBUG_COMMAND: String = "com.obsidianpay.mobile.DEBUG_COMMAND"
    const val PROVIDER_AUTHORITY: String = "com.obsidianpay.mobile.provider.notes"

    const val EXTRA_INTERNAL_ROUTE: String = "obsidian.intent.extra.INTERNAL_ROUTE"
    const val EXTRA_SESSION_HINT: String = "obsidian.intent.extra.SESSION_HINT"
    const val EXTRA_OPERATOR_MODE: String = "obsidian.intent.extra.OPERATOR_MODE"
    const val EXTRA_RECEIPT_ID: String = "obsidian.intent.extra.RECEIPT_ID"

    // Default WebView support path on the backend.
    const val WEBVIEW_SUPPORT_PATH: String = "/api/mobile/webview/support"

    // Debug header expected by the backend's diagnostics endpoint.
    const val DEBUG_HEADER_NAME: String = "X-Obsidian-Debug"
    const val DEBUG_HEADER_VALUE: String = "mobile-diagnostics"

    // Device Trust / reverse-engineering trail (Phase 8).
    const val DEFAULT_DEVICE_ID: String = "android-emulator-obsidian"
    const val DEVICE_TRUST_PATH: String = "/api/mobile/internal/device-trust"
    const val REVERSE_HINT_PATH: String = "/api/mobile/internal/reverse-hint"

    // Local cache keys for the Device Trust flow (plaintext, intentional).
    const val KEY_LAST_DEVICE_TRUST: String = "obsidian.debug.last_device_trust"
    const val KEY_LAST_LEGACY_SIGNATURE: String = "obsidian.debug.last_legacy_signature"
    const val KEY_LAST_ENCODED_OPERATOR_HINT: String = "obsidian.debug.last_encoded_operator_hint"

    // Environment / risk-check (Phase 9). Stored plaintext; didactic.
    const val ENVIRONMENT_REPORT_PATH: String = "/api/mobile/internal/environment-report"
    const val KEY_LAST_ENVIRONMENT_REPORT: String = "obsidian.debug.last_environment_report"
    const val KEY_LAST_ENVIRONMENT_RESPONSE: String = "obsidian.debug.last_environment_response"

    // Vault / local auth (Phase 10). Client-side state stored plaintext; didactic.
    const val VAULT_MOBILE_STATUS_PATH: String = "/api/mobile/internal/vault-mobile/status"
    const val VAULT_MOBILE_UNLOCK_PATH: String = "/api/mobile/internal/vault-mobile/unlock"
    const val KEY_VAULT_UNLOCKED: String = "obsidian.vault.unlocked"
    const val KEY_VAULT_UNLOCK_REASON: String = "obsidian.vault.unlock_reason"
    const val KEY_LAST_VAULT_STATUS_JSON: String = "obsidian.debug.last_vault_status"
    const val KEY_LAST_VAULT_UNLOCK_JSON: String = "obsidian.debug.last_vault_unlock"

    // Network security / certificate pinning scaffold (Phase 11).
    // The override is stored in plaintext SharedPreferences — intentional teaching seam.
    const val NETWORK_PROFILE_PATH: String = "/api/mobile/internal/network-profile"
    const val KEY_API_BASE_URL_OVERRIDE: String = "obsidian.network.api_base_url_override"
    const val KEY_LAST_NETWORK_PROFILE_JSON: String = "obsidian.debug.last_network_profile"
    const val KEY_LAST_PINNING_MODE: String = "obsidian.debug.last_pinning_mode"
    const val KEY_LAST_PINNING_HINT: String = "obsidian.debug.last_pinning_hint"
}
