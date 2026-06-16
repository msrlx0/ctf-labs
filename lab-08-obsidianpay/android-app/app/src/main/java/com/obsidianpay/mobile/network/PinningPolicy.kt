package com.obsidianpay.mobile.network

/**
 * Certificate-pinning policy scaffold for ObsidianPay (Lab 08, Phase 11).
 *
 * This object describes the pinning posture the app would enforce if a real
 * production cert were in use. For the local lab the mode is disabled-local-lab:
 * all traffic is HTTP cleartext to 127.0.0.1 / 10.0.2.2, so attaching an
 * OkHttp CertificatePinner would be pointless and would break the build.
 *
 * The constants and method stubs here exist as teaching anchors:
 *   - "Where would I hook to bypass pinning?" → shouldAttachCertificatePinner
 *   - "What hook ID do I target?" → buildPinningBypassHints
 *   - "What SHA-256 pin would I see in the real app?" → getSamplePins
 *
 * To upgrade to real pinning in a future phase:
 *   1. Switch currentMode() to PINNING_MODE_STRICT_SCAFFOLD.
 *   2. Obtain the real cert SHA-256 digest and replace SAMPLE_PIN_SHA256.
 *   3. Build the CertificatePinner in ApiClient where the comment instructs.
 */
object PinningPolicy {

    const val PINNING_MODE_DISABLED = "disabled-local-lab"
    const val PINNING_MODE_REPORT_ONLY = "report-only"
    const val PINNING_MODE_STRICT_SCAFFOLD = "strict-scaffold"

    // Placeholder SHA-256 pin — replace with `openssl s_client … | openssl x509 -pubkey …`
    const val SAMPLE_PIN_SHA256 = "sha256/AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA="

    // Didactic anchor strings — greppable by static-analysis tooling or students.
    // okhttp-certificate-pinner-hook: OkHttp CertificatePinner attach/check point.
    // trust-manager-hook: custom X509TrustManager intercept point.
    // user-ca-not-trusted-by-default: Android 7+ user-installed CA behaviour.
    // report-only: passive logging mode; no request is blocked.
    private const val HINT_OKHTTP = "okhttp-certificate-pinner-hook"
    private const val HINT_TRUST_MANAGER = "trust-manager-hook"
    private const val HINT_USER_CA = "user-ca-not-trusted-by-default"
    private const val HINT_REPORT_ONLY = "report-only"

    /** Active pinning mode for this lab build. */
    fun currentMode(): String = PINNING_MODE_DISABLED

    /**
     * Returns true only when the base URL is HTTPS and the mode is strict-scaffold.
     * For all HTTP cleartext URLs (local lab) this always returns false so no
     * CertificatePinner is ever attached — a real pinner on HTTP would be a no-op
     * and could break request building.
     */
    fun shouldAttachCertificatePinner(baseUrl: String): Boolean {
        if (baseUrl.startsWith("http://")) return false
        return currentMode() == PINNING_MODE_STRICT_SCAFFOLD
    }

    /** Hostnames the pinner would cover in a strict-scaffold or production mode. */
    fun getPinnedHosts(): List<String> = listOf("api.obsidianpay.example")

    /**
     * Sample pin map illustrating the shape of a real CertificatePinner config.
     * The SHA-256 value here is a placeholder — not a real certificate digest.
     */
    fun getSamplePins(): Map<String, String> =
        mapOf("api.obsidianpay.example" to SAMPLE_PIN_SHA256)

    /**
     * Returns the ordered list of didactic bypass-hint identifiers.
     * These are logging anchors, not solution steps.
     */
    fun buildPinningBypassHints(): List<String> = listOf(
        HINT_USER_CA,
        HINT_OKHTTP,
        HINT_TRUST_MANAGER,
        HINT_REPORT_ONLY,
        "network-config-cleartext-override",
    )
}
