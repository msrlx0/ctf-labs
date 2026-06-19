package com.obsidianpay.mobile.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import com.obsidianpay.mobile.storage.LocalCacheManager
import com.obsidianpay.mobile.util.Constants
import com.obsidianpay.mobile.ui.components.SecondaryButton
import com.obsidianpay.mobile.ui.components.SectionHeader
import com.obsidianpay.mobile.ui.components.SurfaceCard
import com.obsidianpay.mobile.ui.components.VSpace

/**
 * "Dados do dispositivo" — a maintenance/diagnostics view of the app's local
 * state (session prefs, cached rows, recent events and on-disk artifacts) with a
 * "clear" action.
 *
 * NOTE (instructor): this intentionally shows what the app persists locally — the
 * local-storage inspection seam. Content and the values displayed are unchanged;
 * only the presentation is themed. Presented as a support utility, not as an
 * exploitation surface.
 */
@Composable
fun LocalStateScreen(
    modifier: Modifier,
    cache: LocalCacheManager,
) {
    var refresh by remember { mutableStateOf(0) }
    val debugValues = remember(refresh) { cache.debugValues() }
    val receipts = remember(refresh) { cache.cachedReceipts() }
    val cards = remember(refresh) { cache.cachedCards() }
    val events = remember(refresh) { cache.debugEvents(30) }
    val artifacts = remember(refresh) { cache.listLocalArtifacts() }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp)
            .padding(bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        VSpace(4)
        Text(
            "Veja o que o ObsidianPay guarda localmente neste aparelho.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        Group("Sessão local") {
            debugValues.forEach { (k, v) -> Mono("$k = ${preview(v)}") }
        }

        Group("Recibos em cache") {
            if (receipts.isEmpty()) Mono("(vazio)")
            receipts.forEach { Mono("${it.id} · ${it.ownerRole ?: "-"} · ${it.reference ?: "-"}") }
        }

        Group("Cartões em cache") {
            if (cards.isEmpty()) Mono("(vazio)")
            cards.forEach { Mono("${it.id} · ${it.ownerRole ?: "-"} · ${it.maskedNumber ?: "-"}") }
        }

        Group("Navegação recente") {
            Mono("lastWebViewUrl = ${preview(debugValues[Constants.KEY_LAST_WEBVIEW_URL])}")
            Mono("lastDeepLink = ${preview(debugValues[Constants.KEY_LAST_DEEP_LINK])}")
            Mono("lastQrPayload = ${preview(debugValues[Constants.KEY_LAST_QR_PAYLOAD])}")
        }

        Group("Eventos recentes") {
            if (events.isEmpty()) Mono("(vazio)")
            events.forEach { Mono("#${it.id} ${it.eventType}") }
        }

        Group("Integração do dispositivo") {
            Mono("operatorHint = ${preview(debugValues[Constants.KEY_OPERATOR_HINT])}")
            Mono("lastExternalCommand = ${preview(debugValues[Constants.KEY_LAST_EXTERNAL_DEBUG_COMMAND])}")
            Mono("lastExportedEvent = ${preview(debugValues[Constants.KEY_LAST_EXPORTED_EVENT])}")
        }

        Group("Verificação do dispositivo") {
            Mono("lastDeviceTrust = ${preview(debugValues[Constants.KEY_LAST_DEVICE_TRUST])}")
            Mono("lastLegacySignature = ${preview(debugValues[Constants.KEY_LAST_LEGACY_SIGNATURE])}")
            Mono("lastEncodedOperatorHint = ${preview(debugValues[Constants.KEY_LAST_ENCODED_OPERATOR_HINT])}")
        }

        Group("Status do dispositivo") {
            Mono("lastEnvironmentReport = ${preview(debugValues[Constants.KEY_LAST_ENVIRONMENT_REPORT])}")
            Mono("lastEnvironmentResponse = ${preview(debugValues[Constants.KEY_LAST_ENVIRONMENT_RESPONSE])}")
        }

        Group("Cofre seguro") {
            Mono("vaultUnlocked = ${preview(debugValues[Constants.KEY_VAULT_UNLOCKED])}")
            Mono("vaultUnlockReason = ${preview(debugValues[Constants.KEY_VAULT_UNLOCK_REASON])}")
            Mono("lastVaultStatusJson = ${preview(debugValues[Constants.KEY_LAST_VAULT_STATUS_JSON])}")
            Mono("lastVaultUnlockJson = ${preview(debugValues[Constants.KEY_LAST_VAULT_UNLOCK_JSON])}")
        }

        Group("Conexão") {
            Mono("apiBaseUrlOverride = ${preview(debugValues[Constants.KEY_API_BASE_URL_OVERRIDE])}")
            Mono("lastNetworkProfile = ${preview(debugValues[Constants.KEY_LAST_NETWORK_PROFILE_JSON])}")
            Mono("lastPinningMode = ${preview(debugValues[Constants.KEY_LAST_PINNING_MODE])}")
            Mono("lastPinningHint = ${preview(debugValues[Constants.KEY_LAST_PINNING_HINT])}")
        }

        Group("Integridade do app") {
            Mono("lastNativeGateStatus = ${preview(debugValues[Constants.KEY_LAST_NATIVE_GATE_STATUS])}")
            Mono("lastTamperScore = ${preview(debugValues[Constants.KEY_LAST_TAMPER_SCORE])}")
            Mono("lastSignatureHashPreview = ${preview(debugValues[Constants.KEY_LAST_SIGNATURE_HASH_PREVIEW])}")
            Mono("lastAppIntegrityReport = ${preview(debugValues[Constants.KEY_LAST_APP_INTEGRITY_REPORT])}")
            Mono("lastAppIntegrityResponse = ${preview(debugValues[Constants.KEY_LAST_APP_INTEGRITY_RESPONSE])}")
        }

        Group("Arquivos locais") {
            if (artifacts.isEmpty()) Mono("(nenhum)")
            artifacts.forEach { Mono(it) }
        }

        VSpace(4)
        SecondaryButton(text = "Limpar dados locais", onClick = {
            cache.clearAll()
            refresh++
        })
    }
}

@Composable
private fun Group(title: String, content: @Composable () -> Unit) {
    SectionHeader(title = title)
    SurfaceCard {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) { content() }
    }
}

@Composable
private fun Mono(text: String) {
    Text(
        text = text,
        fontFamily = FontFamily.Monospace,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

private fun preview(v: String?): String {
    if (v == null) return "null"
    return if (v.length > 80) v.substring(0, 80) + "…" else v
}
