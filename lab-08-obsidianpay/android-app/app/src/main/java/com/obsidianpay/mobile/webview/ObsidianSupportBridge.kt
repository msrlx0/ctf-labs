package com.obsidianpay.mobile.webview

import android.webkit.JavascriptInterface
import com.obsidianpay.mobile.storage.InsecureSessionStore
import com.obsidianpay.mobile.storage.LocalCacheManager
import org.json.JSONArray
import org.json.JSONObject

/**
 * JavaScript bridge exposed to the support WebView under the name `ObsidianBridge`.
 *
 * NOTE (instructor): this is an intentionally over-exposed `@JavascriptInterface`
 * for the lab. It is presented as an internal "support/diagnostics" helper so the
 * page can render local context (session summary, cached payloads, on-disk
 * artifacts) without an extra round-trip to the backend. In a real app this is a
 * dangerous pattern: any JavaScript running inside the WebView (including a
 * reflected `topic`/`message`) can read these local values through the bridge.
 *
 * Controls kept deliberately tight for the lab:
 *  - the session summary never returns the full token (only a short preview);
 *  - no method returns progress markers (flags) or internal credentials;
 *  - it only exposes caches that already exist locally on the device;
 *  - every call is logged to `debug_events` so the chain is observable.
 *
 * The bridge runs on WebView's private JavaBridge thread (not the UI thread), so
 * the SharedPreferences/SQLite reads here are off the main thread by design.
 */
class ObsidianSupportBridge(
    private val store: InsecureSessionStore,
    private val cache: LocalCacheManager,
) {

    /**
     * Limited snapshot of the local session. Intentionally omits the full token —
     * only a short, masked preview is included.
     */
    @JavascriptInterface
    fun getSessionSummary(): String {
        record("webview_bridge_called", "getSessionSummary")
        val token = store.getToken()
        val summary = JSONObject().apply {
            put("username", store.getUsername() ?: JSONObject.NULL)
            put("userId", store.getUserId().takeIf { it >= 0 } ?: JSONObject.NULL)
            put("role", store.getRole() ?: JSONObject.NULL)
            put("plan", store.getPlan() ?: JSONObject.NULL)
            put("dailyLimit", store.getDailyLimit() ?: JSONObject.NULL)
            put("lastOpenedReceiptId", store.getLastOpenedReceipt() ?: JSONObject.NULL)
            put("lastOpenedCardId", store.getLastOpenedCard() ?: JSONObject.NULL)
            put("lastWebViewUrl", store.getLastWebViewUrl() ?: JSONObject.NULL)
            // Masked preview only — never the full token from this method.
            put("tokenPreview", maskToken(token))
        }
        return summary.toString()
    }

    /** Raw cached profile JSON, if the app has fetched it. */
    @JavascriptInterface
    fun getCachedProfile(): String {
        record("bridge_get_cached_profile", null)
        return store.getRawProfileJson() ?: EMPTY_OBJECT
    }

    /** Raw cached mobile config JSON, if present. */
    @JavascriptInterface
    fun getCachedConfig(): String {
        record("bridge_get_cached_config", null)
        return store.getRawConfigJson() ?: EMPTY_OBJECT
    }

    /** Last support sync payload cached locally. */
    @JavascriptInterface
    fun getLastSupportSync(): String {
        record("webview_bridge_called", "getLastSupportSync")
        return store.getLastSupportSync() ?: EMPTY_OBJECT
    }

    /** Last transfer preview payload cached locally. */
    @JavascriptInterface
    fun getLastTransferPreview(): String {
        record("webview_bridge_called", "getLastTransferPreview")
        return store.getLastTransferPreview() ?: EMPTY_OBJECT
    }

    /** Known local artifacts (paths + sizes) gathered by the cache manager. */
    @JavascriptInterface
    fun getLocalArtifacts(): String {
        record("bridge_get_artifacts", null)
        val arr = JSONArray()
        cache.listLocalArtifacts().forEach { arr.put(it) }
        return JSONObject().put("artifacts", arr).toString()
    }

    /** Bridge identity/version and the list of exposed methods. */
    @JavascriptInterface
    fun getBridgeInfo(): String {
        record("webview_bridge_called", "getBridgeInfo")
        val methods = JSONArray().apply {
            put("getSessionSummary")
            put("getCachedProfile")
            put("getCachedConfig")
            put("getLastSupportSync")
            put("getLastTransferPreview")
            put("getLocalArtifacts")
            put("getBridgeInfo")
            put("logBridgeEvent")
        }
        return JSONObject().apply {
            put("bridgeName", BRIDGE_NAME)
            put("bridgeVersion", BRIDGE_VERSION)
            put("enabledMethods", methods)
        }.toString()
    }

    /** Lets the page record a custom debug event into the local event log. */
    @JavascriptInterface
    fun logBridgeEvent(eventType: String?, details: String?): String {
        val type = eventType?.takeIf { it.isNotBlank() } ?: "bridge_custom_event"
        record("bridge_log_event", "$type | ${details ?: ""}")
        cache.addEvent(type, details)
        return JSONObject().put("status", "ok").put("event", type).toString()
    }

    // --- Helpers ----------------------------------------------------------------

    private fun record(event: String, details: String?) {
        runCatching { cache.addEvent(event, details) }
    }

    private fun maskToken(token: String?): Any {
        if (token.isNullOrEmpty()) return JSONObject.NULL
        val head = token.take(10)
        return "$head…(${token.length} chars)"
    }

    companion object {
        const val BRIDGE_NAME = "ObsidianBridge"
        const val BRIDGE_VERSION = "phase6-lab"
        private const val EMPTY_OBJECT = "{}"
    }
}
