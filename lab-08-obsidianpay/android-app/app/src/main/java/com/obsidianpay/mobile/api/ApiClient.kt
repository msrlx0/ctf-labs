package com.obsidianpay.mobile.api

import android.util.Log
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
 */
class ApiClient(private val baseUrl: String = Constants.DEFAULT_BASE_URL) {

    private val client = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .writeTimeout(15, TimeUnit.SECONDS)
        .build()

    private val jsonMedia = "application/json; charset=utf-8".toMediaType()

    private fun builder(path: String, token: String?): Request.Builder {
        val b = Request.Builder().url(baseUrl + path)
        if (!token.isNullOrEmpty()) b.header("Authorization", "Bearer $token")
        return b
    }

    private fun execute(request: Request): ApiResult<String> {
        return try {
            client.newCall(request).execute().use { resp ->
                val body = resp.body?.string().orEmpty()
                Log.d(TAG, "${request.method} ${request.url.encodedPath} -> ${resp.code}")
                if (resp.isSuccessful) {
                    ApiResult.Success(body, resp.code)
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
                ApiResult.Success(parse(data), httpCode)
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

    companion object {
        private const val TAG = "ObsidianApi"
    }
}
