package com.obsidianpay.mobile.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.obsidianpay.mobile.ObsidianScaffold
import com.obsidianpay.mobile.deeplink.DeepLinkData
import com.obsidianpay.mobile.deeplink.DeepLinkRouter
import com.obsidianpay.mobile.deeplink.DeepLinkType
import com.obsidianpay.mobile.storage.LocalCacheManager

/**
 * "QR Payment" — simulates scanning by letting the user paste/type a QR payload.
 * Accepts obsidianpay:// links and routes them like a real scan would.
 */
@Composable
fun QrInputScreen(
    cache: LocalCacheManager,
    onRoute: (DeepLinkData) -> Unit,
    onBack: () -> Unit,
) {
    var payload by remember {
        mutableStateOf("obsidianpay://transfer?toUserId=2001&amount=10&memo=lunch")
    }
    var status by remember { mutableStateOf("") }

    fun process(data: DeepLinkData) {
        cache.saveLastQrPayload(data.rawUri, data.type.name)
        onRoute(data)
    }

    fun handle(expected: DeepLinkType?) {
        val data = DeepLinkRouter.parse(payload)
        if (data.type == DeepLinkType.UNKNOWN) {
            status = "Payload não reconhecido."
            cache.addEvent("qr_payload_processed", "UNKNOWN | $payload")
            return
        }
        if (expected != null && data.type != expected) {
            status = "Payload não é do tipo ${expected.name.lowercase()}."
            return
        }
        process(data)
    }

    ObsidianScaffold(title = "QR Payment", onBack = onBack) { modifier ->
        Column(
            modifier = modifier.fillMaxWidth().padding(16.dp).verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text("Paste or type QR payload")

            OutlinedTextField(
                value = payload,
                onValueChange = { payload = it },
                label = { Text("QR payload") },
                modifier = Modifier.fillMaxWidth(),
            )

            Button(onClick = { handle(null) }, modifier = Modifier.fillMaxWidth()) {
                Text("Process Payload")
            }
            OutlinedButton(onClick = { handle(DeepLinkType.TRANSFER) }, modifier = Modifier.fillMaxWidth()) {
                Text("Preview Transfer")
            }
            OutlinedButton(onClick = { handle(DeepLinkType.SUPPORT) }, modifier = Modifier.fillMaxWidth()) {
                Text("Open Support Link")
            }
            OutlinedButton(onClick = { handle(DeepLinkType.RECEIPT) }, modifier = Modifier.fillMaxWidth()) {
                Text("Open Receipt")
            }

            if (status.isNotBlank()) Text(status)
        }
    }
}
