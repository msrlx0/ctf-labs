package com.obsidianpay.mobile.storage

import android.content.Context
import android.util.Log
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

/**
 * Coordinates the app's local caching across SharedPreferences, SQLite and the
 * filesystem (internal files/cache and external app-specific storage).
 *
 * NOTE (instructor): everything is written in cleartext on purpose. These are
 * the local-storage surfaces a future phase will explore. No exploitation here.
 */
class LocalCacheManager(
    private val context: Context,
    private val store: InsecureSessionStore,
    private val db: ObsidianLocalDb,
) {

    // --- Profile / config -------------------------------------------------------
    fun cacheProfile(rawJson: String) {
        store.saveProfileCache(rawJson)
        db.addDebugEvent("cache_profile", rawJson)
    }

    fun cacheConfig(rawJson: String) {
        store.saveConfigCache(rawJson)
        db.addDebugEvent("cache_config", rawJson)
    }

    // --- Receipts ---------------------------------------------------------------
    fun cacheReceiptList(rawListJson: String) {
        store.saveReceiptsOffline(rawListJson)
        runCatching {
            val arr = JSONObject(rawListJson).optJSONArray("receipts") ?: JSONArray()
            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                cacheReceiptObject(o.optString("receiptId"), o)
            }
        }
        db.addDebugEvent("cache_receipt_list", "count=${count(rawListJson, "receipts")}")
    }

    fun cacheReceipt(receiptId: String, rawJson: String) {
        runCatching { cacheReceiptObject(receiptId, JSONObject(rawJson)) }
        store.saveLastOpenedReceipt(receiptId)
        writeReceiptSnapshot(receiptId, rawJson)
        db.addDebugEvent("open_receipt", "id=$receiptId")
    }

    private fun cacheReceiptObject(id: String, o: JSONObject) {
        db.cacheReceipt(
            id = id,
            ownerUserId = o.optString("ownerUserId", null),
            ownerRole = o.optString("ownerRole", null),
            reference = o.optString("reference", null),
            amount = o.optString("amountBRL", null),
            merchant = o.optString("counterparty", null),
            rawJson = o.toString(),
        )
    }

    // --- Cards ------------------------------------------------------------------
    fun cacheCardList(rawListJson: String) {
        runCatching {
            val arr = JSONObject(rawListJson).optJSONArray("cards") ?: JSONArray()
            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                cacheCardObject(o.optString("cardId"), o)
            }
        }
        db.addDebugEvent("cache_card_list", "count=${count(rawListJson, "cards")}")
    }

    fun cacheCard(cardId: String, rawJson: String) {
        runCatching { cacheCardObject(cardId, JSONObject(rawJson)) }
        store.saveLastOpenedCard(cardId)
        db.addDebugEvent("open_card", "id=$cardId")
    }

    private fun cacheCardObject(id: String, o: JSONObject) {
        db.cacheCard(
            id = id,
            ownerUserId = o.optString("ownerUserId", null),
            ownerRole = o.optString("ownerRole", null),
            maskedNumber = o.optString("maskedNumber", null),
            cardType = o.optString("brand", null),
            rawJson = o.toString(),
        )
    }

    // --- Support / diagnostics / transfer --------------------------------------
    fun cacheSupportSync(rawJson: String) {
        store.saveSupportSync(rawJson)
        writeTempSupportSnapshot(rawJson)
        db.addDebugEvent("support_sync", rawJson)
    }

    fun cacheDiagnostics(rawJson: String) {
        store.saveDiagnostics(rawJson)
        db.addDebugEvent("diagnostics", rawJson)
    }

    fun cacheLegacyRoutes(rawJson: String) {
        writeDebugExport(rawJson)
        db.addDebugEvent("legacy_routes", rawJson)
    }

    fun cacheTransferPreview(rawJson: String) {
        store.saveTransferPreview(rawJson)
        db.addDebugEvent("transfer_preview", rawJson)
    }

    fun addEvent(eventType: String, details: String?) = db.addDebugEvent(eventType, details)

    // --- Deep link / QR / WebView (Phase 5) -------------------------------------
    fun saveLastDeepLink(rawUri: String, type: String) {
        store.saveLastDeepLink(rawUri, type)
        db.addDebugEvent("deep_link_opened", "$type | $rawUri")
    }

    fun saveLastQrPayload(payload: String, type: String) {
        store.saveLastQrPayload(payload, type)
        db.addDebugEvent("qr_payload_processed", "$type | $payload")
    }

    fun saveLastWebViewUrl(url: String) {
        store.saveLastWebViewUrl(url)
        db.addDebugEvent("webview_support_opened", url)
    }

    // --- File artifacts ---------------------------------------------------------
    fun writeTempSupportSnapshot(rawJson: String): String? =
        writeFile(File(context.cacheDir, "obsidian-support-last-sync.json"), rawJson)

    fun writeReceiptSnapshot(receiptId: String, rawJson: String): String? {
        val dir = File(context.filesDir, "receipts").apply { mkdirs() }
        return writeFile(File(dir, "receipt-$receiptId.json"), rawJson)
    }

    fun writeDebugExport(rawJson: String): String? {
        val dir = File(context.filesDir, "debug").apply { mkdirs() }
        val internal = writeFile(File(dir, "obsidian-debug-export.json"), rawJson)
        // Also mirror to external app-specific storage when available.
        writeExternalExport(rawJson)
        return internal
    }

    /** External app-specific storage (NOT public/global). Scaffold for a later phase. */
    fun writeExternalExport(rawJson: String): String? {
        val extDir = context.getExternalFilesDir(null) ?: return null
        return writeFile(File(extDir, "obsidian-export.txt"), rawJson)
    }

    private fun writeFile(file: File, content: String): String? = runCatching {
        file.writeText(content)
        file.absolutePath
    }.onFailure { Log.w(TAG, "write failed: ${it.message}") }.getOrNull()

    fun listLocalArtifacts(): List<String> {
        val candidates = buildList {
            add(File(context.cacheDir, "obsidian-support-last-sync.json"))
            add(File(context.filesDir, "debug/obsidian-debug-export.json"))
            val receiptsDir = File(context.filesDir, "receipts")
            if (receiptsDir.isDirectory) receiptsDir.listFiles()?.forEach { add(it) }
            context.getExternalFilesDir(null)?.let { add(File(it, "obsidian-export.txt")) }
        }
        return candidates.filter { it.exists() }.map { "${it.absolutePath} (${it.length()} bytes)" }
    }

    fun clearLocalArtifacts() {
        runCatching {
            File(context.cacheDir, "obsidian-support-last-sync.json").delete()
            File(context.filesDir, "debug").deleteRecursively()
            File(context.filesDir, "receipts").deleteRecursively()
            context.getExternalFilesDir(null)?.let { File(it, "obsidian-export.txt").delete() }
        }
    }

    // --- Aggregate clear --------------------------------------------------------
    fun clearAll() {
        store.clear()
        db.clearAll()
        clearLocalArtifacts()
    }

    // Convenience pass-throughs for the local state screen.
    fun debugValues(): Map<String, String?> = store.getAllDebugValues()
    fun cachedReceipts(): List<ObsidianLocalDb.CachedReceipt> = db.listCachedReceipts()
    fun cachedCards(): List<ObsidianLocalDb.CachedCard> = db.listCachedCards()
    fun debugEvents(limit: Int = 30): List<ObsidianLocalDb.DebugEvent> = db.listDebugEvents(limit)

    private fun count(rawJson: String, field: String): Int =
        runCatching { JSONObject(rawJson).optJSONArray(field)?.length() ?: 0 }.getOrDefault(0)

    companion object {
        private const val TAG = "ObsidianCache"
    }
}
