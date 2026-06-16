package com.obsidianpay.mobile.platform

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.obsidianpay.mobile.storage.InsecureSessionStore
import com.obsidianpay.mobile.storage.LocalCacheManager
import com.obsidianpay.mobile.storage.ObsidianLocalDb
import org.json.JSONObject

/**
 * Exported BroadcastReceiver that accepts "debug commands".
 *
 * NOTE (instructor): this receiver is intentionally `exported="true"` and listens
 * for a predictable action (`com.obsidianpay.mobile.DEBUG_COMMAND`) with no
 * permission. Any app (or `adb shell am broadcast`) can drive it. It is framed as
 * an internal debug/automation hook. Controlled for the lab:
 *  - it never runs system/shell commands;
 *  - it never performs any network I/O;
 *  - it never returns flags or credentials;
 *  - every invocation logs `exported_receiver_called` plus the received command.
 *
 * Supported commands operate only on the app's own local state.
 */
class DebugCommandReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val store = InsecureSessionStore(context)
        val db = ObsidianLocalDb(context)
        val cache = LocalCacheManager(context.applicationContext, store, db)

        val command = intent.getStringExtra("command")?.trim().orEmpty()
        val route = intent.getStringExtra("route")
        val note = intent.getStringExtra("note")

        // Always observable: who called us and with what.
        cache.recordExportedEvent(
            "exported_receiver_called",
            "command=$command route=${route ?: "-"} note=${note ?: "-"}",
        )
        cache.saveExternalDebugCommand(command, "route=${route ?: "-"} note=${note ?: "-"}")

        when (command) {
            "sync_marker" ->
                cache.addEvent("external_debug_sync_marker", note)

            "set_last_receipt" -> {
                val receiptId = intent.getStringExtra("receiptId")
                    ?: intent.getIntExtra("receiptId", -1).takeIf { it >= 0 }?.toString()
                if (!receiptId.isNullOrBlank()) {
                    store.saveLastOpenedReceipt(receiptId)
                    cache.addEvent("external_set_last_receipt", "id=$receiptId")
                } else {
                    cache.addEvent("external_set_last_receipt", "missing receiptId")
                }
            }

            "write_debug_export" -> {
                val payload = JSONObject().apply {
                    put("source", "external_debug_export")
                    put("route", route ?: JSONObject.NULL)
                    put("note", note ?: JSONObject.NULL)
                }.toString()
                cache.writeDebugExport(payload)
                cache.addEvent("write_debug_export", "external (route=${route ?: "-"})")
            }

            "enable_operator_hint" -> {
                cache.saveOperatorHint("true")
                cache.addEvent("enable_operator_hint", "support/operator_hint=true")
            }

            else ->
                cache.addEvent("external_debug_unknown_command", command.ifEmpty { "(empty)" })
        }
    }
}
