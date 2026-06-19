package com.obsidianpay.mobile.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.obsidianpay.mobile.deeplink.DeepLinkData
import com.obsidianpay.mobile.deeplink.DeepLinkRouter
import com.obsidianpay.mobile.deeplink.DeepLinkType
import com.obsidianpay.mobile.storage.LocalCacheManager
import com.obsidianpay.mobile.ui.components.PrimaryButton
import com.obsidianpay.mobile.ui.components.SecondaryButton
import com.obsidianpay.mobile.ui.components.StatusBanner
import com.obsidianpay.mobile.ui.components.StatusTone
import com.obsidianpay.mobile.ui.components.SurfaceCard
import com.obsidianpay.mobile.ui.components.VSpace

/**
 * "Pagar com QR" — simulates scanning by letting the user paste/type a QR payload.
 *
 * NOTE (instructor): the payload routing is preserved. obsidianpay:// links are
 * parsed by DeepLinkRouter and routed exactly like a real scan would, and the
 * payload is cached (saveLastQrPayload). Only labels changed.
 */
@Composable
fun QrInputScreen(
    modifier: Modifier,
    cache: LocalCacheManager,
    onRoute: (DeepLinkData) -> Unit,
) {
    var payload by remember {
        mutableStateOf("obsidianpay://transfer?toUserId=2001&amount=10&memo=lunch")
    }
    var status by remember { mutableStateOf("") }
    var tone by remember { mutableStateOf(StatusTone.NEUTRAL) }

    fun process(data: DeepLinkData) {
        cache.saveLastQrPayload(data.rawUri, data.type.name)
        onRoute(data)
    }

    fun handle(expected: DeepLinkType?) {
        val data = DeepLinkRouter.parse(payload)
        if (data.type == DeepLinkType.UNKNOWN) {
            tone = StatusTone.NEGATIVE
            status = "Não reconhecemos esse código. Verifique e tente novamente."
            cache.addEvent("qr_payload_processed", "UNKNOWN | $payload")
            return
        }
        if (expected != null && data.type != expected) {
            tone = StatusTone.CAUTION
            status = "Esse código não é do tipo selecionado."
            return
        }
        process(data)
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .imePadding()
            .padding(horizontal = 16.dp)
            .padding(bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        VSpace(4)
        SurfaceCard {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                Icon(Icons.Filled.QrCodeScanner, contentDescription = null, tint = MaterialTheme.colorScheme.secondary, modifier = Modifier.size(32.dp))
                Column {
                    Text("Pagar com QR", style = MaterialTheme.typography.titleMedium)
                    Text(
                        "Cole o conteúdo de um código de pagamento para continuar.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }

        OutlinedTextField(
            value = payload,
            onValueChange = { payload = it },
            label = { Text("Conteúdo do código") },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
        )

        PrimaryButton(text = "Continuar", onClick = { handle(null) })
        SecondaryButton(text = "Pagar / transferir", onClick = { handle(DeepLinkType.TRANSFER) })
        SecondaryButton(text = "Abrir ajuda do código", onClick = { handle(DeepLinkType.SUPPORT) })
        SecondaryButton(text = "Abrir recibo", onClick = { handle(DeepLinkType.RECEIPT) })

        StatusBanner(text = status, tone = tone)
    }
}
