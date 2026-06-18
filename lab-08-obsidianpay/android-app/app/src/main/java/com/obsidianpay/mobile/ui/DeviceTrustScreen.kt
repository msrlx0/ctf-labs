package com.obsidianpay.mobile.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Divider
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.obsidianpay.mobile.ObsidianScaffold
import com.obsidianpay.mobile.ResponseBox
import com.obsidianpay.mobile.api.ApiClient
import com.obsidianpay.mobile.api.ApiResult
import com.obsidianpay.mobile.security.HardcodedSecrets
import com.obsidianpay.mobile.security.LegacyRequestSigner
import com.obsidianpay.mobile.security.WeakCrypto
import com.obsidianpay.mobile.storage.InsecureSessionStore
import com.obsidianpay.mobile.storage.LocalCacheManager
import com.obsidianpay.mobile.util.Constants
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * "Device Trust" screen — presented as a routine security/attestation check.
 *
 * NOTE (instructor): this screen drives the Phase 8 reverse-engineering trail. It
 * assembles a weak legacy request signature locally (see
 * [com.obsidianpay.mobile.security.LegacyRequestSigner]) using a hardcoded salt
 * from [HardcodedSecrets], decodes a Base64-"protected" operator hint, and calls
 * the internal `/api/mobile/internal/device-trust` endpoint. Nothing here is
 * labelled as a vulnerability in the UI.
 */
@Composable
fun DeviceTrustScreen(
    apiClient: ApiClient,
    store: InsecureSessionStore,
    cache: LocalCacheManager,
    onBack: () -> Unit,
) {
    val scope = rememberCoroutineScope()
    val token = store.getToken()
    val username = store.getUsername() ?: "guest"
    val deviceId = Constants.DEFAULT_DEVICE_ID
    val clientId = HardcodedSecrets.getInternalClientId()

    var status by remember { mutableStateOf("") }
    var output by remember { mutableStateOf("") }

    ObsidianScaffold(title = "Device Trust", onBack = onBack) { modifier ->
        Column(
            modifier = modifier.fillMaxWidth().padding(16.dp).verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("Security check do dispositivo")
                    Mono("deviceId  = $deviceId")
                    Mono("clientId  = ${maskMiddle(clientId)}")
                    Mono("attestation = legacy")
                }
            }

            Button(
                onClick = {
                    if (token == null) {
                        status = "Sessão ausente."
                        return@Button
                    }
                    status = "Executando trust check..."
                    cache.addEvent("device_trust_check_started", "device=$deviceId")
                    scope.launch {
                        val timestamp = System.currentTimeMillis().toString()
                        val signature = LegacyRequestSigner.sign(username, deviceId, timestamp)
                        cache.saveLegacySignature(signature) // -> weak_signature_generated
                        val headers = LegacyRequestSigner.buildHeaders(username, deviceId, timestamp)
                        val decodedHint =
                            WeakCrypto.base64Decode(HardcodedSecrets.getEncodedOperatorHint())
                        cache.saveEncodedOperatorHint(HardcodedSecrets.getEncodedOperatorHint())

                        val res = withContext(Dispatchers.IO) {
                            apiClient.checkDeviceTrust(
                                token = token,
                                deviceId = deviceId,
                                attestationMode = "legacy",
                                operatorHint = decodedHint,
                                legacyHeaders = headers,
                            )
                        }
                        when (res) {
                            is ApiResult.Success -> {
                                cache.cacheDeviceTrust(res.rawBody) // -> device_trust_response_cached
                                output = res.rawBody
                                status = "Trust check concluído."
                            }
                            is ApiResult.Error -> {
                                output = res.message
                                status = "Falha no trust check (HTTP ${res.httpCode ?: "-"})."
                            }
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth(),
            ) { Text("Run Trust Check") }

            OutlinedButton(
                onClick = {
                    val encoded = HardcodedSecrets.getEncodedOperatorHint()
                    cache.saveEncodedOperatorHint(encoded)
                    output = "encodedOperatorHint = $encoded"
                    status = "Hint codificado exibido."
                },
                modifier = Modifier.fillMaxWidth(),
            ) { Text("Show Encoded Hint") }

            OutlinedButton(
                onClick = {
                    val decoded = WeakCrypto.base64Decode(HardcodedSecrets.getEncodedOperatorHint())
                    cache.addEvent("encoded_hint_decoded", decoded)
                    output = "decodedOperatorHint = $decoded"
                    status = "Hint decodificado localmente."
                },
                modifier = Modifier.fillMaxWidth(),
            ) { Text("Decode Local Hint") }

            Divider(modifier = Modifier.padding(vertical = 4.dp))
            Text("Diagnostics", fontSize = 13.sp)

            OutlinedButton(
                onClick = {
                    output = HardcodedSecrets.getHiddenRoutes().joinToString("\n")
                    status = "Rotas internas listadas (diagnóstico)."
                },
                modifier = Modifier.fillMaxWidth(),
            ) { Text("Show Hidden Routes") }

            if (status.isNotBlank()) Text(status)
            ResponseBox(output)
        }
    }
}

@Composable
private fun Mono(text: String) {
    Text(text = text, fontFamily = FontFamily.Monospace, fontSize = 12.sp)
}

/** Masks the middle of a value, keeping a short head/tail for display. */
private fun maskMiddle(value: String): String {
    if (value.length <= 8) return "****"
    return value.take(5) + "…" + value.takeLast(4)
}
