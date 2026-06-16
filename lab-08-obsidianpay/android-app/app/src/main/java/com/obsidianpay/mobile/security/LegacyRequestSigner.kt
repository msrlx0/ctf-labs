package com.obsidianpay.mobile.security

/**
 * Builds the weak "legacy" request signature/headers for internal endpoints.
 *
 * NOTE (instructor): this is an intentionally weak local signing scheme. The
 * signature is a plain SHA-1 over predictable fields joined with a hardcoded salt
 * (no HMAC, no server nonce, no real secret). Anyone who recovers the salt from
 * [HardcodedSecrets] can forge a valid signature offline — which is the whole
 * teaching point. The backend (api/src/server.js) verifies the exact same scheme.
 */
object LegacyRequestSigner {

    /**
     * sha1(username:deviceId:timestamp:legacySigningSalt) — predictable by design.
     */
    fun sign(username: String, deviceId: String, timestamp: String): String {
        val salt = HardcodedSecrets.getLegacySigningSalt()
        val base = "$username:$deviceId:$timestamp:$salt"
        return WeakCrypto.sha1Hex(base)
    }

    /** Assembles the legacy trust headers expected by the internal endpoints. */
    fun buildHeaders(username: String, deviceId: String, timestamp: String): Map<String, String> =
        linkedMapOf(
            "X-Obsidian-Client" to HardcodedSecrets.getInternalClientId(),
            "X-Obsidian-Device" to deviceId,
            "X-Obsidian-Timestamp" to timestamp,
            "X-Obsidian-Signature" to sign(username, deviceId, timestamp),
        )
}
