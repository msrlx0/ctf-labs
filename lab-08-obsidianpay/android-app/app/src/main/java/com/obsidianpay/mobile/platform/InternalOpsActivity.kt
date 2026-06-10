package com.obsidianpay.mobile.platform

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Divider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.obsidianpay.mobile.storage.InsecureSessionStore
import com.obsidianpay.mobile.storage.LocalCacheManager
import com.obsidianpay.mobile.storage.ObsidianLocalDb
import com.obsidianpay.mobile.util.Constants

/**
 * "Internal Operations" screen for support/operations staff.
 *
 * NOTE (instructor): this Activity is intentionally `exported="true"` and gated
 * by a predictable action (`com.obsidianpay.mobile.INTERNAL_OPS`) with no strong
 * auth. It is dressed up as an internal support/diagnostics console so any app
 * (or `adb`) can launch it and feed predictable intent extras
 * (`obsidian.intent.extra.*`). Controlled for the lab: it only echoes the extras,
 * logs an `exported_activity_opened` event, and — if a RECEIPT_ID is supplied —
 * records it as the last opened receipt. It does not fetch flags, does not
 * require login, and cannot crash the app on bad input.
 */
class InternalOpsActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val store = InsecureSessionStore(this)
        val db = ObsidianLocalDb(this)
        val cache = LocalCacheManager(applicationContext, store, db)

        val route = intent?.getStringExtra(Constants.EXTRA_INTERNAL_ROUTE)
        val sessionHint = intent?.getStringExtra(Constants.EXTRA_SESSION_HINT)
        val operatorMode = intent?.getStringExtra(Constants.EXTRA_OPERATOR_MODE)
        val receiptId = intent?.let { i ->
            i.getStringExtra(Constants.EXTRA_RECEIPT_ID)
                ?: i.getIntExtra(Constants.EXTRA_RECEIPT_ID, -1).takeIf { it >= 0 }?.toString()
        }

        cache.recordExportedEvent(
            "exported_activity_opened",
            "route=${route ?: "-"} operatorMode=${operatorMode ?: "-"} receiptId=${receiptId ?: "-"}",
        )

        // A supplied receipt id is treated as the "last opened receipt" — the same
        // local state the rest of the app maintains.
        if (!receiptId.isNullOrBlank()) {
            store.saveLastOpenedReceipt(receiptId)
        }

        setContent {
            MaterialTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    InternalOpsScreen(
                        route = route,
                        sessionHint = sessionHint,
                        operatorMode = operatorMode,
                        receiptId = receiptId,
                    )
                }
            }
        }
    }
}

@Composable
private fun InternalOpsScreen(
    route: String?,
    sessionHint: String?,
    operatorMode: String?,
    receiptId: String?,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text("Internal Operations", fontSize = 22.sp)
        Text(
            "Support / internal diagnostics console. Restricted to operations staff.",
            fontSize = 13.sp,
        )
        Divider(modifier = Modifier.padding(vertical = 8.dp))

        Mono("route        = ${route ?: "(none)"}")
        Mono("sessionHint  = ${sessionHint ?: "(none)"}")
        Mono("operatorMode = ${operatorMode ?: "(none)"}")
        Mono("receiptId    = ${receiptId ?: "(none)"}")

        Divider(modifier = Modifier.padding(vertical = 8.dp))
        Text(
            "This console reflects the operation context it was opened with. For " +
                "support sessions, the receipt is tracked as the last opened item.",
            fontSize = 12.sp,
        )
    }
}

@Composable
private fun Mono(text: String) {
    Text(text = text, fontFamily = FontFamily.Monospace, fontSize = 13.sp)
}
