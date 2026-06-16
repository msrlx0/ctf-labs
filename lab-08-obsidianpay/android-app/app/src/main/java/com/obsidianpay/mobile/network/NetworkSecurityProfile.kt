package com.obsidianpay.mobile.network

/**
 * Network security profile helpers for the ObsidianPay mobile client (Lab 08, Phase 11).
 *
 * Centralises base-URL constants and runtime profile detection so the app can
 * operate correctly against the emulator loopback (10.0.2.2), localhost
 * (127.0.0.1) and a physical-device LAN IP without code changes. All traffic
 * in this lab is HTTP cleartext to a local API; the profile logic is a
 * scaffolded teaching seam for a future certificate-pinning / HTTPS study.
 */
object NetworkSecurityProfile {

    // Base URL presets (cleartext HTTP — local lab only).
    const val DEFAULT_EMULATOR_BASE_URL = "http://10.0.2.2:8102"
    const val DEFAULT_LOCALHOST_BASE_URL = "http://127.0.0.1:8102"
    const val SAMPLE_PHONE_BASE_URL = "http://192.168.0.50:8102"

    // Profile identifiers surfaced by buildProfile() and the backend endpoint.
    const val PROFILE_CLEAR_TEXT_LOCAL = "cleartext-local"
    const val PROFILE_BURP_PROXY_READY = "burp-proxy-ready"
    const val PROFILE_PINNING_SCAFFOLD = "pinning-scaffold"

    // Didactic bypass hint identifiers — anchors for future Frida/proxy study.
    private val BYPASS_HINTS = listOf(
        "trust-user-ca",
        "okhttp-certificate-pinner-hook",
        "network-config-cleartext-override",
        "user-ca-not-trusted-by-default",
    )

    /**
     * Trims whitespace and ensures the URL does not end with a trailing slash.
     * Returns [DEFAULT_EMULATOR_BASE_URL] if the result is blank.
     */
    fun normalizeBaseUrl(input: String): String {
        val trimmed = input.trim().trimEnd('/')
        return if (trimmed.isEmpty()) DEFAULT_EMULATOR_BASE_URL else trimmed
    }

    /**
     * Resolves the single effective base URL for the whole app from a persisted
     * override (plaintext SharedPreferences). When no override is stored the
     * Android-Emulator default (10.0.2.2) is used. This is the ONE place the
     * "override ?: default" rule lives — ApiClient, the WebView and every screen
     * must derive their host from this (or from ApiClient.getBaseUrl(), which is
     * seeded from it), never from a hardcoded constant.
     */
    fun effectiveBaseUrl(override: String?): String =
        normalizeBaseUrl(override ?: DEFAULT_EMULATOR_BASE_URL)

    /**
     * Joins a base URL with a path, normalizing the trailing slash so exactly one
     * `/` separates host and path. Use this instead of string concatenation so the
     * WebView and API client build identical origins from the same base.
     */
    fun joinUrl(baseUrl: String, path: String): String {
        val base = normalizeBaseUrl(baseUrl)
        val suffix = if (path.startsWith("/")) path else "/$path"
        return base + suffix
    }

    /** Returns true when the URL uses the http:// scheme (cleartext). */
    fun isCleartext(url: String): Boolean = url.startsWith("http://")

    /** Returns true when the URL targets the Android Emulator loopback alias. */
    fun isLikelyEmulatorUrl(url: String): Boolean = url.contains("10.0.2.2")

    /**
     * Returns true when the URL looks like a private LAN address — a typical
     * indicator that a physical device is being used instead of an emulator.
     */
    fun isLikelyPhoneLanUrl(url: String): Boolean {
        val noScheme = url.removePrefix("http://").removePrefix("https://")
        return noScheme.startsWith("192.168.") ||
            noScheme.startsWith("10.") && !noScheme.startsWith("10.0.2.2") ||
            noScheme.startsWith("172.")
    }

    /** Builds a runtime [NetworkProfile] describing how the client will connect. */
    fun buildProfile(baseUrl: String): NetworkProfile {
        val normalized = normalizeBaseUrl(baseUrl)
        val cleartext = isCleartext(normalized)
        val profile = when {
            isLikelyEmulatorUrl(normalized) || normalized.contains("127.0.0.1") ->
                PROFILE_CLEAR_TEXT_LOCAL
            cleartext -> PROFILE_BURP_PROXY_READY
            else -> PROFILE_PINNING_SCAFFOLD
        }
        return NetworkProfile(
            baseUrl = normalized,
            profile = profile,
            cleartext = cleartext,
            isEmulator = isLikelyEmulatorUrl(normalized),
            isPhoneLan = isLikelyPhoneLanUrl(normalized),
            bypassHints = BYPASS_HINTS,
        )
    }

    /**
     * Returns a rotating didactic bypass-hint identifier for the current lab
     * session. Callers can log or display this hint without disclosing solutions.
     */
    fun buildBypassHintId(): String = BYPASS_HINTS.first()
}

/** Snapshot of the resolved network security profile for the current base URL. */
data class NetworkProfile(
    val baseUrl: String,
    val profile: String,
    val cleartext: Boolean,
    val isEmulator: Boolean,
    val isPhoneLan: Boolean,
    val bypassHints: List<String>,
)
