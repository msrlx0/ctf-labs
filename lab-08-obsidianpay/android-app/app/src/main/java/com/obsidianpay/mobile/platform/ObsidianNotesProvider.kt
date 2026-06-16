package com.obsidianpay.mobile.platform

import android.content.ContentProvider
import android.content.ContentValues
import android.content.UriMatcher
import android.database.Cursor
import android.database.MatrixCursor
import android.net.Uri
import com.obsidianpay.mobile.storage.InsecureSessionStore
import com.obsidianpay.mobile.storage.LocalCacheManager
import com.obsidianpay.mobile.storage.ObsidianLocalDb
import com.obsidianpay.mobile.util.Constants

/**
 * Exported ContentProvider that surfaces local support notes / debug state.
 *
 * NOTE (instructor): this provider is intentionally `exported="true"` on a
 * predictable authority (`com.obsidianpay.mobile.provider.notes`) with no
 * read permission. Any app can `content query` it. It is framed as an internal
 * "support notes" provider. Controlled for the lab:
 *  - `/notes`  → static, generic support notes with didactic hints (no secrets);
 *  - `/debug`  → local debug values, but the full session token is **removed** and
 *                replaced by a masked `token_preview` (never the whole token);
 *  - `/cache`  → list of known local artifacts (path + size).
 * It never returns flag markers, never returns internal credentials, and only
 * exposes data that already exists locally on the device. Writes are not
 * implemented.
 */
class ObsidianNotesProvider : ContentProvider() {

    private lateinit var store: InsecureSessionStore
    private lateinit var cache: LocalCacheManager

    private val matcher = UriMatcher(UriMatcher.NO_MATCH).apply {
        addURI(Constants.PROVIDER_AUTHORITY, "notes", CODE_NOTES)
        addURI(Constants.PROVIDER_AUTHORITY, "debug", CODE_DEBUG)
        addURI(Constants.PROVIDER_AUTHORITY, "cache", CODE_CACHE)
        addURI(Constants.PROVIDER_AUTHORITY, Constants.PROVIDER_PATH_CHECKPOINT, CODE_CHECKPOINT)
    }

    override fun onCreate(): Boolean {
        val ctx = context ?: return false
        store = InsecureSessionStore(ctx)
        val db = ObsidianLocalDb(ctx)
        cache = LocalCacheManager(ctx.applicationContext, store, db)
        return true
    }

    override fun query(
        uri: Uri,
        projection: Array<out String>?,
        selection: String?,
        selectionArgs: Array<out String>?,
        sortOrder: String?,
    ): Cursor? {
        val code = matcher.match(uri)
        runCatching {
            cache.recordExportedEvent("exported_provider_query", uri.toString())
        }
        return when (code) {
            CODE_NOTES -> notesCursor()
            CODE_DEBUG -> debugCursor()
            CODE_CACHE -> cacheCursor()
            CODE_CHECKPOINT -> checkpointCursor()
            else -> null // unknown URI → controlled empty result
        }
    }

    private fun notesCursor(): Cursor = MatrixCursor(arrayOf("id", "title", "body")).apply {
        addRow(arrayOf(1, "Support onboarding", "Internal support notes for ObsidianPay operators."))
        addRow(arrayOf(2, "Diagnostics", "Use the Internal Operations console for support sessions."))
        addRow(
            arrayOf(
                3,
                "Local data hint",
                "Operators can review device-local cache and recent debug events.",
            ),
        )
        addRow(
            arrayOf(
                4,
                "Bridge hint",
                "The Web Support portal exposes a support bridge inside the app.",
            ),
        )
    }

    private fun debugCursor(): Cursor = MatrixCursor(arrayOf("key", "value")).apply {
        // Provider-safe view: full token removed, only a masked token_preview.
        cache.getSafeDebugValuesForProvider().forEach { (k, v) ->
            addRow(arrayOf(k, v ?: ""))
        }
    }

    private fun cacheCursor(): Cursor = MatrixCursor(arrayOf("item", "value")).apply {
        cache.listLocalArtifacts().forEachIndexed { idx, artifact ->
            addRow(arrayOf("artifact_$idx", artifact))
        }
    }

    /**
     * Stage 03 checkpoint consolidation (Phase 20).
     *
     * Surfaces the proofs left in local state by the exported Activity and
     * BroadcastReceiver, and — only once BOTH are present — adds the provider's
     * own consolidating proof. This is the single read point a student queries via
     * `adb shell content query --uri content://com.obsidianpay.mobile.provider.notes/checkpoint`
     * after driving the Activity and Receiver. The three proofs are then POSTed to
     * the backend checkpoint endpoint, which returns the stage-03 flag. No flag is
     * ever exposed here — only exploitation evidence that already exists on-device.
     */
    private fun checkpointCursor(): Cursor = MatrixCursor(arrayOf("proof", "value")).apply {
        val activityProof = store.getCheckpointActivityProof()
        val receiverProof = store.getCheckpointReceiverProof()
        if (!activityProof.isNullOrEmpty()) addRow(arrayOf("activityProof", activityProof))
        if (!receiverProof.isNullOrEmpty()) addRow(arrayOf("receiverProof", receiverProof))
        if (!activityProof.isNullOrEmpty() && !receiverProof.isNullOrEmpty()) {
            addRow(arrayOf("providerProof", Constants.CHECKPOINT_PROVIDER_PROOF))
        }
    }

    // Read-only provider: writes are intentionally not implemented.
    override fun getType(uri: Uri): String? = when (matcher.match(uri)) {
        CODE_NOTES -> "vnd.android.cursor.dir/vnd.obsidianpay.notes"
        CODE_DEBUG -> "vnd.android.cursor.dir/vnd.obsidianpay.debug"
        CODE_CACHE -> "vnd.android.cursor.dir/vnd.obsidianpay.cache"
        CODE_CHECKPOINT -> "vnd.android.cursor.dir/vnd.obsidianpay.checkpoint"
        else -> null
    }

    override fun insert(uri: Uri, values: ContentValues?): Uri? = null

    override fun update(
        uri: Uri,
        values: ContentValues?,
        selection: String?,
        selectionArgs: Array<out String>?,
    ): Int = 0

    override fun delete(uri: Uri, selection: String?, selectionArgs: Array<out String>?): Int = 0

    companion object {
        private const val CODE_NOTES = 1
        private const val CODE_DEBUG = 2
        private const val CODE_CACHE = 3
        private const val CODE_CHECKPOINT = 4
    }
}
