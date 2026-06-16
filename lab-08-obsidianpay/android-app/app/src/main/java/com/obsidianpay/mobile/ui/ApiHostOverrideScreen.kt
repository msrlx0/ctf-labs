package com.obsidianpay.mobile.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Divider
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.obsidianpay.mobile.ObsidianScaffold
import com.obsidianpay.mobile.api.ApiClient
import com.obsidianpay.mobile.api.ApiResult
import com.obsidianpay.mobile.network.NetworkSecurityProfile
import com.obsidianpay.mobile.network.PinningPolicy
import com.obsidianpay.mobile.storage.InsecureSessionStore
import com.obsidianpay.mobile.storage.LocalCacheManager
import com.obsidianpay.mobile.util.Constants
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * API Host configuration screen (Phase 11).
 *
 * Allows testers to point the app at a different backend host without
 * rebuilding — useful when switching between the Android Emulator
 * (http://10.0.2.2:8102) and a physical device on the LAN
 * (e.g. http://192.168.0.x:8102). The override is persisted in
 * InsecureSessionStore (plaintext SharedPreferences) — an intentional
 * teaching seam for the local-storage module.
 *
 * This screen also surfaces the server-side network-security profile and
 * the pinning bypass hints used in the certificate-pinning study (Phase 11).
 */
@Composable
fun ApiHostOverrideScreen(
    apiClient: ApiClient,
    store: InsecureSessionStore,
    cache: LocalCacheManager,
    onBack: () -> Unit,
) {
    var urlInput by remember { mutableStateOf(apiClient.getBaseUrl()) }
    var status by remember { mutableStateOf("") }
    var profileText by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()
    val token = store.getToken()

    ObsidianScaffold(title = "API Host", onBack = onBack) { modifier ->
        Column(
            modifier = modifier
                .fillMaxWidth()
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            InfoRow("Base URL atual", apiClient.getBaseUrl())
            InfoRow("Modo de pinning", PinningPolicy.currentMode())
            InfoRow("Perfil de rede", NetworkSecurityProfile.buildProfile(apiClient.getBaseUrl()).profile)

            Divider()

            OutlinedTextField(
                value = urlInput,
                onValueChange = { urlInput = it },
                label = { Text("Novo Base URL") },
                placeholder = { Text("http://10.0.2.2:8102") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
            )

            Button(
                onClick = {
                    val normalized = NetworkSecurityProfile.normalizeBaseUrl(urlInput)
                    urlInput = normalized
                    store.saveApiBaseUrlOverride(normalized)
                    apiClient.setBaseUrlForSession(normalized)
                    cache.addEvent("api_base_url_override_saved", normalized)
                    status = "Base URL salvo: $normalized"
                },
                modifier = Modifier.fillMaxWidth(),
            ) { Text("Save Base URL") }

            OutlinedButton(
                onClick = {
                    urlInput = NetworkSecurityProfile.DEFAULT_EMULATOR_BASE_URL
                    store.saveApiBaseUrlOverride(urlInput)
                    apiClient.setBaseUrlForSession(urlInput)
                    cache.addEvent("api_base_url_override_saved", urlInput)
                    status = "Emulator default: $urlInput"
                },
                modifier = Modifier.fillMaxWidth(),
            ) { Text("Use Emulator Default") }

            OutlinedButton(
                onClick = {
                    urlInput = NetworkSecurityProfile.SAMPLE_PHONE_BASE_URL
                    store.saveApiBaseUrlOverride(urlInput)
                    apiClient.setBaseUrlForSession(urlInput)
                    cache.addEvent("api_base_url_override_saved", urlInput)
                    status = "Phone LAN example: $urlInput"
                },
                modifier = Modifier.fillMaxWidth(),
            ) { Text("Use Phone LAN Example") }

            OutlinedButton(
                onClick = {
                    store.clearApiBaseUrlOverride()
                    val def = Constants.DEFAULT_BASE_URL
                    urlInput = def
                    apiClient.setBaseUrlForSession(def)
                    cache.addEvent("api_base_url_override_cleared", def)
                    status = "Override limpo. Usando: $def"
                },
                modifier = Modifier.fillMaxWidth(),
            ) { Text("Clear Override") }

            Divider()

            OutlinedButton(
                onClick = {
                    if (token == null) { status = "Sessão ausente."; return@OutlinedButton }
                    status = "Buscando network profile..."
                    scope.launch {
                        val result = withContext(Dispatchers.IO) {
                            apiClient.getNetworkProfile(token)
                        }
                        when (result) {
                            is ApiResult.Success -> {
                                profileText = result.rawBody
                                store.saveLastNetworkProfileJson(result.rawBody)
                                cache.addEvent("network_profile_fetched", "ok")
                                // Extract pinning mode and first bypass hint for local state.
                                runCatching {
                                    val obj = org.json.JSONObject(result.rawBody)
                                    val mode = obj.optString("pinningMode", PinningPolicy.currentMode())
                                    store.saveLastPinningMode(mode)
                                    cache.addEvent("pinning_mode_observed", mode)
                                    val hints = obj.optJSONArray("bypassHintIds")
                                    val hint = hints?.optString(0) ?: PinningPolicy.buildPinningBypassHints().first()
                                    store.saveLastPinningHint(hint)
                                }
                                status = "Network profile carregado."
                            }
                            is ApiResult.Error -> {
                                status = "Erro: ${result.message}"
                                profileText = ""
                            }
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth(),
            ) { Text("Fetch Network Profile") }

            if (profileText.isNotBlank()) {
                Divider()
                SmallLabel("Network Profile (raw)")
                SmallMono(profileText)
            }

            Divider()
            SmallLabel("Pinning bypass hints (didático)")
            PinningPolicy.buildPinningBypassHints().forEach { SmallMono("• $it") }

            if (status.isNotBlank()) {
                Divider()
                Text(status, fontSize = 13.sp)
            }
        }
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    Text(
        text = "$label: $value",
        fontFamily = FontFamily.Monospace,
        fontSize = 12.sp,
    )
}

@Composable
private fun SmallLabel(text: String) {
    Text(text, modifier = Modifier.padding(top = 4.dp), fontSize = 13.sp)
}

@Composable
private fun SmallMono(text: String) {
    Text(text = text, fontFamily = FontFamily.Monospace, fontSize = 11.sp)
}
