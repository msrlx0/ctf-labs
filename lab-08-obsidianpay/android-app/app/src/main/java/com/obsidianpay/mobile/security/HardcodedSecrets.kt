package com.obsidianpay.mobile.security

/**
 * Hardcoded, fragmented "secrets" and internal config for the ObsidianPay client.
 *
 * NOTE (instructor): this object is an intentional reverse-engineering surface for
 * the lab. Real apps must never embed secrets in code — here they are split into
 * fragments and reassembled at runtime precisely so a student/agent can recover
 * them with JADX / apktool / `strings`. None of these are real production secrets,
 * and none of them is a flag. The reassembled values intentionally mirror what the
 * backend hardcodes (see api/src/data.js → legacyMobileTrust) so the weak local
 * request signing actually validates.
 */
object HardcodedSecrets {

    // --- Internal client id (fragmented) ----------------------------------------
    // Reassembles to "obsidian-mobile-legacy-client".
    private const val INTERNAL_CLIENT_PART_A = "obsidian"
    private const val INTERNAL_CLIENT_PART_B = "-mobile"
    private const val INTERNAL_CLIENT_PART_C = "-legacy-client"

    // --- Support shared hint (fragmented) ---------------------------------------
    private const val SUPPORT_SHARED_HINT_A = "support-shared"
    private const val SUPPORT_SHARED_HINT_B = "-hint:legacy"

    // --- Legacy signing salt (fragmented) ---------------------------------------
    // Reassembles to "obsidian-legacy-attestation-2026".
    private const val LEGACY_SIGNING_SALT_PART_1 = "obsidian-legacy"
    private const val LEGACY_SIGNING_SALT_PART_2 = "-attestation-2026"

    // --- Base64-"protected" operator hint ---------------------------------------
    // Base64 is used here as a fake "protection". Decodes to
    // "operator-hint:mobile-support". Treated by the app as if it were opaque.
    private const val ENCODED_OPERATOR_HINT = "b3BlcmF0b3ItaGludDptb2JpbGUtc3VwcG9ydA=="

    fun getInternalClientId(): String =
        INTERNAL_CLIENT_PART_A + INTERNAL_CLIENT_PART_B + INTERNAL_CLIENT_PART_C

    fun getSupportSharedHint(): String = SUPPORT_SHARED_HINT_A + SUPPORT_SHARED_HINT_B

    fun getLegacySigningSalt(): String = LEGACY_SIGNING_SALT_PART_1 + LEGACY_SIGNING_SALT_PART_2

    /** Returns the Base64-"protected" operator hint (still encoded). */
    fun getEncodedOperatorHint(): String = ENCODED_OPERATOR_HINT

    /** Internal/planned routes embedded in the client for "diagnostics". */
    fun getHiddenRoutes(): List<String> = listOf(
        "/api/mobile/internal/device-trust",
        "/api/mobile/internal/legacy-attestation",
        "/api/mobile/internal/reverse-hint",
    )
}
