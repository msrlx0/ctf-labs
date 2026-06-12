package com.obsidianpay.mobile.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Divider
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.obsidianpay.mobile.ObsidianScaffold
import com.obsidianpay.mobile.storage.LocalCacheManager
import com.obsidianpay.mobile.util.Constants

/**
 * Internal support / developer tooling screen: shows the device-local app state
 * (session prefs, cached rows, recent debug events and on-disk artifacts) and
 * offers a "clear" action. Presented as a maintenance utility, not as an
 * exploitation surface.
 */
@Composable
fun LocalStateScreen(
    cache: LocalCacheManager,
    onBack: () -> Unit,
) {
    // A simple counter to force a refresh after clearing.
    var refresh by remember { mutableStateOf(0) }
    val debugValues = remember(refresh) { cache.debugValues() }
    val receipts = remember(refresh) { cache.cachedReceipts() }
    val cards = remember(refresh) { cache.cachedCards() }
    val events = remember(refresh) { cache.debugEvents(30) }
    val artifacts = remember(refresh) { cache.listLocalArtifacts() }

    ObsidianScaffold(title = "Local State", onBack = onBack) { modifier ->
        Column(
            modifier = modifier.fillMaxWidth().padding(16.dp).verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Section("Sessão local (SharedPreferences)")
            debugValues.forEach { (k, v) ->
                Mono("$k = ${preview(v)}")
            }

            Section("Recibos em cache (SQLite)")
            if (receipts.isEmpty()) Mono("(vazio)")
            receipts.forEach { Mono("${it.id} · ${it.ownerRole ?: "-"} · ${it.reference ?: "-"}") }

            Section("Cartões em cache (SQLite)")
            if (cards.isEmpty()) Mono("(vazio)")
            cards.forEach { Mono("${it.id} · ${it.ownerRole ?: "-"} · ${it.maskedNumber ?: "-"}") }

            Section("Navegação recente (deep link / QR / Web Support)")
            Mono("lastWebViewUrl = ${preview(debugValues[Constants.KEY_LAST_WEBVIEW_URL])}")
            Mono("lastDeepLink = ${preview(debugValues[Constants.KEY_LAST_DEEP_LINK])}")
            Mono("lastQrPayload = ${preview(debugValues[Constants.KEY_LAST_QR_PAYLOAD])}")

            Section("Eventos de debug")
            if (events.isEmpty()) Mono("(vazio)")
            events.forEach { Mono("#${it.id} ${it.eventType}") }

            val bridgeEvents = events.filter {
                it.eventType.startsWith("webview_bridge") || it.eventType.startsWith("bridge_")
            }
            Section("Eventos do Web Support")
            if (bridgeEvents.isEmpty()) Mono("(nenhum)")
            bridgeEvents.forEach { Mono("#${it.id} ${it.eventType} · ${preview(it.details)}") }

            Section("Integração Android (componentes internos)")
            Mono("operatorHint = ${preview(debugValues[Constants.KEY_OPERATOR_HINT])}")
            Mono("lastExternalCommand = ${preview(debugValues[Constants.KEY_LAST_EXTERNAL_DEBUG_COMMAND])}")
            Mono("lastExportedEvent = ${preview(debugValues[Constants.KEY_LAST_EXPORTED_EVENT])}")
            val exportedEvents = events.filter {
                it.eventType.startsWith("exported_") ||
                    it.eventType.startsWith("external_debug") ||
                    it.eventType.startsWith("external_set_last_receipt") ||
                    it.eventType == "operator_hint_set" ||
                    it.eventType == "enable_operator_hint" ||
                    it.eventType == "write_debug_export"
            }
            if (exportedEvents.isEmpty()) Mono("(nenhum)")
            exportedEvents.forEach { Mono("#${it.id} ${it.eventType} · ${preview(it.details)}") }

            Section("Device Trust (security check)")
            Mono("lastDeviceTrust = ${preview(debugValues[Constants.KEY_LAST_DEVICE_TRUST])}")
            Mono("lastLegacySignature = ${preview(debugValues[Constants.KEY_LAST_LEGACY_SIGNATURE])}")
            Mono("lastEncodedOperatorHint = ${preview(debugValues[Constants.KEY_LAST_ENCODED_OPERATOR_HINT])}")
            val trustEvents = events.filter {
                it.eventType.startsWith("device_trust") ||
                    it.eventType == "weak_signature_generated" ||
                    it.eventType == "encoded_hint_decoded"
            }
            if (trustEvents.isEmpty()) Mono("(nenhum)")
            trustEvents.forEach { Mono("#${it.id} ${it.eventType} · ${preview(it.details)}") }

            Section("Environment / Risk Check (Phase 9)")
            Mono("lastEnvironmentReport = ${preview(debugValues[Constants.KEY_LAST_ENVIRONMENT_REPORT])}")
            Mono("lastEnvironmentResponse = ${preview(debugValues[Constants.KEY_LAST_ENVIRONMENT_RESPONSE])}")
            val envEvents = events.filter {
                it.eventType.startsWith("environment_") ||
                    it.eventType.startsWith("root_detection") ||
                    it.eventType.startsWith("emulator_detection")
            }
            if (envEvents.isEmpty()) Mono("(nenhum)")
            envEvents.forEach { Mono("#${it.id} ${it.eventType} · ${preview(it.details)}") }

            Section("Secure Vault / Local Auth (Phase 10)")
            Mono("vaultUnlocked = ${preview(debugValues[Constants.KEY_VAULT_UNLOCKED])}")
            Mono("vaultUnlockReason = ${preview(debugValues[Constants.KEY_VAULT_UNLOCK_REASON])}")
            Mono("lastVaultStatusJson = ${preview(debugValues[Constants.KEY_LAST_VAULT_STATUS_JSON])}")
            Mono("lastVaultUnlockJson = ${preview(debugValues[Constants.KEY_LAST_VAULT_UNLOCK_JSON])}")
            val vaultEvents = events.filter {
                it.eventType.startsWith("vault_") ||
                    it.eventType.startsWith("biometric_") ||
                    it.eventType.startsWith("local_auth_") ||
                    it.eventType == "weak_pin_fallback_used"
            }
            if (vaultEvents.isEmpty()) Mono("(nenhum)")
            vaultEvents.forEach { Mono("#${it.id} ${it.eventType} · ${preview(it.details)}") }

            Section("Network Security / API Host Override (Phase 11)")
            Mono("apiBaseUrlOverride = ${preview(debugValues[Constants.KEY_API_BASE_URL_OVERRIDE])}")
            Mono("lastNetworkProfile = ${preview(debugValues[Constants.KEY_LAST_NETWORK_PROFILE_JSON])}")
            Mono("lastPinningMode = ${preview(debugValues[Constants.KEY_LAST_PINNING_MODE])}")
            Mono("lastPinningHint = ${preview(debugValues[Constants.KEY_LAST_PINNING_HINT])}")
            val networkEvents = events.filter {
                it.eventType == "api_base_url_override_saved" ||
                    it.eventType == "api_base_url_override_cleared" ||
                    it.eventType == "network_profile_fetched" ||
                    it.eventType == "pinning_mode_observed"
            }
            if (networkEvents.isEmpty()) Mono("(nenhum)")
            networkEvents.forEach { Mono("#${it.id} ${it.eventType} · ${preview(it.details)}") }

            Section("App Integrity / NativeGate / TamperCheck (Phase 12)")
            Mono("lastNativeGateStatus = ${preview(debugValues[Constants.KEY_LAST_NATIVE_GATE_STATUS])}")
            Mono("lastTamperScore = ${preview(debugValues[Constants.KEY_LAST_TAMPER_SCORE])}")
            Mono("lastSignatureHashPreview = ${preview(debugValues[Constants.KEY_LAST_SIGNATURE_HASH_PREVIEW])}")
            Mono("lastAppIntegrityReport = ${preview(debugValues[Constants.KEY_LAST_APP_INTEGRITY_REPORT])}")
            Mono("lastAppIntegrityResponse = ${preview(debugValues[Constants.KEY_LAST_APP_INTEGRITY_RESPONSE])}")
            val integrityEvents = events.filter {
                it.eventType == "integrity_check_started" ||
                    it.eventType == "tamper_check_completed" ||
                    it.eventType == "native_gate_checked" ||
                    it.eventType == "tamper_score_calculated" ||
                    it.eventType == "native_gate_hint_viewed" ||
                    it.eventType == "integrity_report_sent" ||
                    it.eventType == "integrity_report_cached" ||
                    it.eventType == "integrity_state_cleared"
            }
            if (integrityEvents.isEmpty()) Mono("(nenhum)")
            integrityEvents.forEach { Mono("#${it.id} ${it.eventType} · ${preview(it.details)}") }

            Section("Artefatos locais (arquivos)")
            if (artifacts.isEmpty()) Mono("(nenhum)")
            artifacts.forEach { Mono(it) }

            Divider(modifier = Modifier.padding(vertical = 8.dp))
            OutlinedButton(
                onClick = {
                    cache.clearAll()
                    refresh++
                },
                modifier = Modifier.fillMaxWidth(),
            ) { Text("Clear Local Data") }
        }
    }
}

@Composable
private fun Section(title: String) {
    Text(title, modifier = Modifier.padding(top = 10.dp))
}

@Composable
private fun Mono(text: String) {
    Text(text = text, fontFamily = FontFamily.Monospace, fontSize = 12.sp)
}

private fun preview(v: String?): String {
    if (v == null) return "null"
    return if (v.length > 80) v.substring(0, 80) + "…" else v
}
