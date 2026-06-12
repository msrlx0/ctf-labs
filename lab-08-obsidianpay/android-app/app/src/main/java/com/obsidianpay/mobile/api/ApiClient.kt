package com.obsidianpay.mobile.api

import android.util.Log
import com.obsidianpay.mobile.network.NetworkSecurityProfile
import com.obsidianpay.mobile.network.PinningPolicy
import com.obsidianpay.mobile.util.Constants
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.util.concurrent.TimeUnit

/**
 * Simple synchronous HTTP client (OkHttp) for the ObsidianPay backend.
 *
 * All methods block; call them from a background dispatcher (the UI uses
 * Dispatchers.IO). Cleartext HTTP to the local lab host is allowed via the
 * app's network security config.
 *
 * Phase 11 — dynamic base URL:
 *   The base URL can be changed at runtime via [setBaseUrlForSession] so the app
 *   can reach the lab backend from an Android Emulator (10.0.2.2) or a physical
 *   device on the LAN (e.g. 192.168.x.x) without rebuilding.
 *
 * Certificate-pinning scaffold (Phase 11):
 *   A real OkHttp CertificatePinner would be attached here when
 *   PinningPolicy.shouldAttachCertificatePinner(baseUrl) returns true.
 *   For the local lab the policy is "disabled-local-lab" (cleartext HTTP),
 *   so no pinner is attached. Bypass hint: okhttp-certificate-pinner-hook.
 *   See PinningPolicy.buildPinningBypassHints() for the full list.
 */
class ApiClient(initialBaseUrl: String = Constants.DEFAULT_BASE_URL) {

    private var currentBaseUrl: String =
        NetworkSecurityProfile.normalizeBaseUrl(initialBaseUrl)

    // Phase 11: CertificatePinner scaffold.
    // When PinningPolicy.shouldAttachCertificatePinner(currentBaseUrl) is true,
    // attach a CertificatePinner built from PinningPolicy.getSamplePins() here.
    // For the local lab the pinner is intentionally omitted (HTTP cleartext).
    private val client = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .writeTimeout(15, TimeUnit.SECONDS)
        // .certificatePinner(buildPinner())  // Phase 11 scaffold — enable for HTTPS
        .build()

    /** Changes the target base URL for all subsequent requests in this session. */
    fun setBaseUrlForSession(baseUrl: String) {
        currentBaseUrl = NetworkSecurityProfile.normalizeBaseUrl(baseUrl)
        Log.d(TAG, "base URL updated → $currentBaseUrl " +
            "(pinning: ${PinningPolicy.shouldAttachCertificatePinner(currentBaseUrl)}, " +
            "hints: ${PinningPolicy.buildPinningBypassHints().take(2)})")
    }

    /** Returns the currently active base URL. */
    fun getBaseUrl(): String = currentBaseUrl

    private val jsonMedia = "application/json; charset=utf-8".toMediaType()

    private fun builder(path: String, token: String?): Request.Builder {
        val b = Request.Builder().url(currentBaseUrl + path)
        if (!token.isNullOrEmpty()) b.header("Authorization", "Bearer $token")
        return b
    }

    private fun execute(request: Request): ApiResult<String> {
        return try {
            client.newCall(request).execute().use { resp ->
                val body = resp.body?.string().orEmpty()
                Log.d(TAG, "${request.method} ${request.url.encodedPath} -> ${resp.code}")
                if (resp.isSuccessful) {
                    ApiResult.Success(body, resp.code, body)
                } else {
                    ApiResult.Error(extractMessage(body, "HTTP ${resp.code}"), resp.code)
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "request failed: ${e.message}")
            ApiResult.Error(e.message ?: "network error", null)
        }
    }

    private fun extractMessage(body: String, fallback: String): String = try {
        JSONObject(body).optString("message", fallback)
    } catch (e: Exception) {
        fallback
    }

    private inline fun <T> ApiResult<String>.mapBody(parse: (String) -> T): ApiResult<T> =
        when (this) {
            is ApiResult.Success -> try {
                ApiResult.Success(parse(data), httpCode, rawBody)
            } catch (e: Exception) {
                ApiResult.Error("parse error: ${e.message}", httpCode)
            }
            is ApiResult.Error -> this
        }

    // --- Auth ---------------------------------------------------------------
    fun login(username: String, password: String): ApiResult<LoginResponse> {
        val payload = JSONObject()
            .put("username", username)
            .put("password", password)
            .toString()
        val req = builder("/api/mobile/login", null)
            .post(payload.toRequestBody(jsonMedia))
            .build()
        return execute(req).mapBody { LoginResponse.fromJson(JSONObject(it)) }
    }

    // --- Profile ------------------------------------------------------------
    fun getProfile(token: String): ApiResult<UserProfile> =
        execute(builder("/api/mobile/profile", token).get().build())
            .mapBody { UserProfile.fromJson(JSONObject(it)) }

    fun patchProfile(token: String, jsonBody: String): ApiResult<String> =
        execute(builder("/api/mobile/profile", token).patch(jsonBody.toRequestBody(jsonMedia)).build())

    // --- Config -------------------------------------------------------------
    fun getConfig(token: String?): ApiResult<MobileConfig> =
        execute(builder("/api/mobile/config", token).get().build())
            .mapBody { MobileConfig.fromJson(JSONObject(it)) }

    // --- Receipts -----------------------------------------------------------
    fun getReceipts(token: String): ApiResult<List<Receipt>> =
        execute(builder("/api/mobile/receipts", token).get().build())
            .mapBody { parseReceiptList(JSONObject(it).optJSONArray("receipts")) }

    fun getReceipt(token: String, receiptId: String): ApiResult<Receipt> =
        execute(builder("/api/mobile/receipts/$receiptId", token).get().build())
            .mapBody { Receipt.fromJson(JSONObject(it)) }

    // --- Cards --------------------------------------------------------------
    fun getCards(token: String): ApiResult<List<Card>> =
        execute(builder("/api/mobile/cards", token).get().build())
            .mapBody { parseCardList(JSONObject(it).optJSONArray("cards")) }

    fun getCard(token: String, cardId: String): ApiResult<Card> =
        execute(builder("/api/mobile/cards/$cardId", token).get().build())
            .mapBody { Card.fromJson(JSONObject(it)) }

    // --- Support ------------------------------------------------------------
    fun supportSync(token: String?, message: String): ApiResult<String> {
        val payload = JSONObject().put("message", message).toString()
        return execute(builder("/api/mobile/support/sync", token).post(payload.toRequestBody(jsonMedia)).build())
    }

    fun getSupportDiagnostics(token: String, includeDebugHeader: Boolean): ApiResult<String> {
        val b = builder("/api/mobile/support/diagnostics", token).get()
        if (includeDebugHeader) b.header(Constants.DEBUG_HEADER_NAME, Constants.DEBUG_HEADER_VALUE)
        return execute(b.build())
    }

    fun getLegacyRoutes(token: String): ApiResult<String> =
        execute(builder("/api/mobile/legacy/routes", token).get().build())

    // --- Transfer preview ---------------------------------------------------
    fun transferPreview(
        token: String,
        toUserId: String,
        amount: String,
        memo: String,
    ): ApiResult<TransferPreviewResponse> {
        // toUserId is sent as a number when numeric, otherwise as-is (weak by design).
        val payload = JSONObject().apply {
            put("toUserId", toUserId.toIntOrNull() ?: toUserId)
            put("amount", amount)
            put("memo", memo)
        }.toString()
        val req = builder("/api/mobile/transfer/preview", token)
            .post(payload.toRequestBody(jsonMedia))
            .build()
        return execute(req).mapBody { TransferPreviewResponse.fromJson(JSONObject(it)) }
    }

    // --- Internal device trust (Phase 8) ------------------------------------
    /**
     * Calls the internal legacy device-trust endpoint. The trust headers
     * (`X-Obsidian-Client/Device/Timestamp/Signature`) are assembled locally by
     * [com.obsidianpay.mobile.security.LegacyRequestSigner] — a weak, forgeable
     * scheme by design. Returns the raw JSON body.
     */
    fun checkDeviceTrust(
        token: String,
        deviceId: String,
        attestationMode: String,
        operatorHint: String,
        legacyHeaders: Map<String, String>,
    ): ApiResult<String> {
        val payload = JSONObject().apply {
            put("deviceId", deviceId)
            put("attestationMode", attestationMode)
            put("operatorHint", operatorHint)
        }.toString()
        val b = builder(Constants.DEVICE_TRUST_PATH, token)
            .post(payload.toRequestBody(jsonMedia))
        legacyHeaders.forEach { (k, v) -> b.header(k, v) }
        return execute(b.build())
    }

    // --- Environment report (Phase 9) ---------------------------------------
    /**
     * Posts the local environment risk report (root/emulator detection result)
     * to the server's monitor-only endpoint. Returns the raw response body.
     */
    fun sendEnvironmentReport(token: String, reportJson: String): String {
        val req = builder(Constants.ENVIRONMENT_REPORT_PATH, token)
            .post(reportJson.toRequestBody(jsonMedia))
            .build()
        return when (val result = execute(req)) {
            is ApiResult.Success -> result.rawBody
            is ApiResult.Error -> """{"error":"${result.message}","httpCode":${result.httpCode ?: -1}}"""
        }
    }

    // --- Vault mobile (Phase 10) --------------------------------------------

    /**
     * Fetches the mobile vault status from the backend.
     * Returns raw JSON — the response indicates lock policy and allowed methods.
     */
    fun getMobileVaultStatus(token: String): String {
        val req = builder(Constants.VAULT_MOBILE_STATUS_PATH, token).get().build()
        return when (val result = execute(req)) {
            is ApiResult.Success -> result.rawBody
            is ApiResult.Error -> """{"error":"${result.message}","httpCode":${result.httpCode ?: -1}}"""
        }
    }

    /**
     * Requests vault unlock from the backend.
     *
     * The server trusts [localAuth] as-is — a weak gate by design (teaching seam).
     * [bypassHintId] names the Frida/patch entry point a student would use.
     * Returns raw JSON.
     */
    fun requestMobileVaultUnlock(
        token: String,
        localAuth: Boolean,
        method: String,
        bypassHintId: String,
    ): String {
        val payload = JSONObject().apply {
            put("localAuth", localAuth)
            put("method", method)
            put("bypassHintId", bypassHintId)
        }.toString()
        val req = builder(Constants.VAULT_MOBILE_UNLOCK_PATH, token)
            .post(payload.toRequestBody(jsonMedia))
            .build()
        return when (val result = execute(req)) {
            is ApiResult.Success -> result.rawBody
            is ApiResult.Error -> """{"error":"${result.message}","httpCode":${result.httpCode ?: -1}}"""
        }
    }

    /**
     * Calls the internal reverse-hint endpoint, gated by the correct
     * `X-Obsidian-Client` header. Returns the raw JSON body.
     */
    fun getReverseHint(token: String, clientId: String): ApiResult<String> =
        execute(
            builder(Constants.REVERSE_HINT_PATH, token)
                .header("X-Obsidian-Client", clientId)
                .get()
                .build()
        )

    // --- Network security profile (Phase 11) --------------------------------

    /**
     * Fetches the server-side network-security profile.
     *
     * The endpoint returns the active pinning mode, cleartext policy and bypass
     * hint IDs — all teaching anchors with no production secrets. Requires a
     * valid Bearer token. Returns raw JSON.
     *
     * Bypass hints surfaced here: okhttp-certificate-pinner-hook, trust-user-ca,
     * network-config-cleartext-override.
     */
    fun getNetworkProfile(token: String): ApiResult<String> =
        execute(builder(Constants.NETWORK_PROFILE_PATH, token).get().build())

    companion object {
        private const val TAG = "ObsidianApi"
    }
}
