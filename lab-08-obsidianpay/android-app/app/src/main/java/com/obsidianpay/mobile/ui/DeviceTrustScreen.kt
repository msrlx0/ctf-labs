package com.obsidianpay.mobile.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.obsidianpay.mobile.ResponseBox
import com.obsidianpay.mobile.api.ApiClient
import com.obsidianpay.mobile.api.ApiResult
import com.obsidianpay.mobile.security.HardcodedSecrets
import com.obsidianpay.mobile.security.LegacyRequestSigner
import com.obsidianpay.mobile.security.WeakCrypto
import com.obsidianpay.mobile.storage.InsecureSessionStore
import com.obsidianpay.mobile.storage.LocalCacheManager
import com.obsidianpay.mobile.util.Constants
import com.obsidianpay.mobile.ui.components.PrimaryButton
import com.obsidianpay.mobile.ui.components.SecondaryButton
import com.obsidianpay.mobile.ui.components.SectionHeader
import com.obsidianpay.mobile.ui.components.StatusBanner
import com.obsidianpay.mobile.ui.components.StatusTone
import com.obsidianpay.mobile.ui.components.SurfaceCard
import com.obsidianpay.mobile.ui.components.VSpace
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * "Device verification" — presented as a routine trusted-device attestation.
 *
 * NOTE (instructor): the Phase 8 reverse-engineering trail is fully preserved. It
 * still assembles a weak legacy request signature locally (LegacyRequestSigner)
 * with a hardcoded salt (HardcodedSecrets), decodes a Base64-"protected" operator
 * hint (WeakCrypto), and calls /api/mobile/internal/device-trust. Nothing here is
 * labelled as a vulnerability; the advanced diagnostics that drive the trail
 * remain reachable for analysis.
 */
@Composable
fun DeviceTrustScreen(
    modifier: Modifier,
    apiClient: ApiClient,
    store: InsecureSessionStore,
    cache: LocalCacheManager,
) {
    val scope = rememberCoroutineScope()
    val token = store.getToken()
    val username = store.getUsername() ?: "guest"
    val deviceId = Constants.DEFAULT_DEVICE_ID
    val clientId = HardcodedSecrets.getInternalClientId()

    var status by remember { mutableStateOf("") }
    var tone by remember { mutableStateOf(StatusTone.NEUTRAL) }
    var output by remember { mutableStateOf("") }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp)
            .padding(bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        VSpace(4)
        SurfaceCard {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                Icon(Icons.Filled.VerifiedUser, contentDescription = null, tint = MaterialTheme.colorScheme.secondary)
                Column(Modifier.weight(1f)) {
                    Text("Dispositivo confiável", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    Text(
                        "Confirme que este aparelho pode realizar operações sensíveis com segurança.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }

        PrimaryButton(text = "Verificar este dispositivo", onClick = {
            if (token == null) { tone = StatusTone.NEGATIVE; status = "Sua sessão expirou. Entre novamente."; return@PrimaryButton }
            tone = StatusTone.NEUTRAL
            status = "Verificando..."
            cache.addEvent("device_trust_check_started", "device=$deviceId")
            scope.launch {
                val timestamp = System.currentTimeMillis().toString()
                val signature = LegacyRequestSigner.sign(username, deviceId, timestamp)
                cache.saveLegacySignature(signature)
                val headers = LegacyRequestSigner.buildHeaders(username, deviceId, timestamp)
                val decodedHint = WeakCrypto.base64Decode(HardcodedSecrets.getEncodedOperatorHint())
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
                        cache.cacheDeviceTrust(res.rawBody)
                        output = res.rawBody
                        tone = StatusTone.POSITIVE
                        status = "Verificação concluída."
                    }
                    is ApiResult.Error -> {
                        output = res.message
                        tone = StatusTone.NEGATIVE
                        status = "Não foi possível concluir a verificação (HTTP ${res.httpCode ?: "-"})."
                    }
                }
            }
        })

        StatusBanner(text = status, tone = tone)

        SectionHeader(title = "Diagnóstico avançado")
        SurfaceCard {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                SecondaryButton(text = "Mostrar token de verificação", onClick = {
                    val encoded = HardcodedSecrets.getEncodedOperatorHint()
                    cache.saveEncodedOperatorHint(encoded)
                    output = "verificationToken = $encoded"
                    tone = StatusTone.NEUTRAL; status = ""
                })
                SecondaryButton(text = "Decodificar token localmente", onClick = {
                    val decoded = WeakCrypto.base64Decode(HardcodedSecrets.getEncodedOperatorHint())
                    cache.addEvent("encoded_hint_decoded", decoded)
                    output = "decodedToken = $decoded"
                    tone = StatusTone.NEUTRAL; status = ""
                })
                SecondaryButton(text = "Endpoints de diagnóstico", onClick = {
                    output = HardcodedSecrets.getHiddenRoutes().joinToString("\n")
                    tone = StatusTone.NEUTRAL; status = ""
                })
            }
        }

        ResponseBox(output)
    }
}
