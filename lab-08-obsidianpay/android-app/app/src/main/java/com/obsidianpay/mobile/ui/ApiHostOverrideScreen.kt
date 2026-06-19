package com.obsidianpay.mobile.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.Smartphone
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.obsidianpay.mobile.api.ApiClient
import com.obsidianpay.mobile.api.ApiResult
import com.obsidianpay.mobile.network.NetworkSecurityProfile
import com.obsidianpay.mobile.network.PinningPolicy
import com.obsidianpay.mobile.storage.InsecureSessionStore
import com.obsidianpay.mobile.storage.LocalCacheManager
import com.obsidianpay.mobile.util.Constants
import com.obsidianpay.mobile.ui.components.DetailMonoCard
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
 * Advanced connection settings.
 *
 * Lets a tester point the app at the lab backend without rebuilding, via friendly
 * presets that cover both the emulator and a physical-device workflow:
 *   - Android Emulator        → http://10.0.2.2:8102   (the app's primary default)
 *   - Physical device (USB)   → http://127.0.0.1:8102  (use with `adb reverse tcp:8102 tcp:8102`)
 *   - Custom endpoint         → a user-editable LAN URL
 *
 * NOTE (instructor): the override is persisted in InsecureSessionStore (plaintext
 * SharedPreferences) — an intentional teaching seam — and survives restart. This
 * screen also surfaces the server-side network-security profile. No secure/
 * encrypted storage migration is performed and the network behavior is unchanged.
 */
private const val PRESET_EMULATOR = "http://10.0.2.2:8102"
private const val PRESET_PHYSICAL = "http://127.0.0.1:8102"

private enum class Preset { EMULATOR, PHYSICAL, CUSTOM }

@Composable
fun ApiHostOverrideScreen(
    modifier: Modifier,
    apiClient: ApiClient,
    store: InsecureSessionStore,
    cache: LocalCacheManager,
) {
    val scope = rememberCoroutineScope()
    val token = store.getToken()

    fun presetForUrl(url: String): Preset = when (url) {
        PRESET_EMULATOR -> Preset.EMULATOR
        PRESET_PHYSICAL -> Preset.PHYSICAL
        else -> Preset.CUSTOM
    }

    var selected by remember { mutableStateOf(presetForUrl(apiClient.getBaseUrl())) }
    var customUrl by remember {
        mutableStateOf(
            if (presetForUrl(apiClient.getBaseUrl()) == Preset.CUSTOM) apiClient.getBaseUrl()
            else NetworkSecurityProfile.SAMPLE_PHONE_BASE_URL,
        )
    }
    var status by remember { mutableStateOf("") }
    var tone by remember { mutableStateOf(StatusTone.NEUTRAL) }
    var profileText by remember { mutableStateOf("") }

    fun applyAndPersist(url: String, label: String) {
        val normalized = NetworkSecurityProfile.normalizeBaseUrl(url)
        store.saveApiBaseUrlOverride(normalized)
        apiClient.setBaseUrlForSession(normalized)
        cache.addEvent("api_base_url_override_saved", normalized)
        tone = StatusTone.POSITIVE
        status = "$label salvo: $normalized"
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .imePadding()
            .padding(horizontal = 16.dp)
            .padding(bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        VSpace(4)
        SurfaceCard {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                Icon(Icons.Filled.Tune, contentDescription = null, tint = MaterialTheme.colorScheme.secondary)
                Column {
                    Text("Conexão atual", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(apiClient.getBaseUrl(), style = MaterialTheme.typography.titleMedium)
                }
            }
        }

        SectionHeader(title = "Como você está usando o app?")

        PresetRow(
            icon = Icons.Filled.Smartphone,
            title = "Emulador Android",
            subtitle = PRESET_EMULATOR,
            selected = selected == Preset.EMULATOR,
            onSelect = { selected = Preset.EMULATOR; applyAndPersist(PRESET_EMULATOR, "Emulador") },
        )
        PresetRow(
            icon = Icons.Filled.PhoneAndroid,
            title = "Dispositivo físico (USB)",
            subtitle = "$PRESET_PHYSICAL · use adb reverse tcp:8102 tcp:8102",
            selected = selected == Preset.PHYSICAL,
            onSelect = { selected = Preset.PHYSICAL; applyAndPersist(PRESET_PHYSICAL, "Dispositivo físico") },
        )
        PresetRow(
            icon = Icons.Filled.Tune,
            title = "Endpoint personalizado",
            subtitle = "Defina o IP de LAN do seu computador",
            selected = selected == Preset.CUSTOM,
            onSelect = { selected = Preset.CUSTOM },
        )

        if (selected == Preset.CUSTOM) {
            SurfaceCard {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = customUrl,
                        onValueChange = { customUrl = it },
                        label = { Text("Endereço do servidor") },
                        placeholder = { Text(NetworkSecurityProfile.SAMPLE_PHONE_BASE_URL) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
                    )
                    PrimaryButton(text = "Salvar endpoint", onClick = {
                        customUrl = NetworkSecurityProfile.normalizeBaseUrl(customUrl)
                        applyAndPersist(customUrl, "Endpoint personalizado")
                    })
                }
            }
        }

        SecondaryButton(text = "Testar conexão", onClick = {
            tone = StatusTone.NEUTRAL
            status = "Testando conexão com ${apiClient.getBaseUrl()}..."
            scope.launch {
                val res = withContext(Dispatchers.IO) { apiClient.getConfig(token) }
                when (res) {
                    is ApiResult.Success -> { tone = StatusTone.POSITIVE; status = "Conexão bem-sucedida com os serviços do ObsidianPay." }
                    is ApiResult.Error -> {
                        tone = StatusTone.NEGATIVE
                        status = "Não foi possível conectar aos serviços do ObsidianPay. Revise as configurações de conexão e tente novamente."
                    }
                }
            }
        })

        SecondaryButton(text = "Restaurar padrão", onClick = {
            store.clearApiBaseUrlOverride()
            val def = Constants.DEFAULT_BASE_URL
            apiClient.setBaseUrlForSession(def)
            cache.addEvent("api_base_url_override_cleared", def)
            selected = presetForUrl(def)
            tone = StatusTone.NEUTRAL
            status = "Conexão restaurada para o padrão: $def"
        })

        StatusBanner(text = status, tone = tone)

        // Advanced diagnostics — server network profile.
        SectionHeader(title = "Diagnóstico de rede")
        SecondaryButton(text = "Verificar proteção da conexão", onClick = {
            if (token == null) { tone = StatusTone.NEGATIVE; status = "Sua sessão expirou."; return@SecondaryButton }
            status = "Consultando..."
            scope.launch {
                val result = withContext(Dispatchers.IO) { apiClient.getNetworkProfile(token) }
                when (result) {
                    is ApiResult.Success -> {
                        profileText = result.rawBody
                        store.saveLastNetworkProfileJson(result.rawBody)
                        cache.addEvent("network_profile_fetched", "ok")
                        runCatching {
                            val obj = org.json.JSONObject(result.rawBody)
                            val mode = obj.optString("pinningMode", PinningPolicy.currentMode())
                            store.saveLastPinningMode(mode)
                            cache.addEvent("pinning_mode_observed", mode)
                            val hints = obj.optJSONArray("bypassHintIds")
                            val hint = hints?.optString(0) ?: PinningPolicy.buildPinningBypassHints().first()
                            store.saveLastPinningHint(hint)
                        }
                        tone = StatusTone.POSITIVE; status = "Proteção da conexão verificada."
                    }
                    is ApiResult.Error -> { tone = StatusTone.NEGATIVE; status = "Não foi possível verificar a conexão."; profileText = "" }
                }
            }
        })

        DetailMonoCard("Detalhes técnicos", profileText)
    }
}

@Composable
private fun PresetRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    selected: Boolean,
    onSelect: () -> Unit,
) {
    Surface(
        onClick = onSelect,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        color = if (selected) MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
        else MaterialTheme.colorScheme.surfaceContainer,
    ) {
        Row(
            Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.secondary)
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleMedium)
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            RadioButton(selected = selected, onClick = onSelect)
            if (selected) {
                Icon(Icons.Filled.CheckCircle, contentDescription = "Selecionado", tint = MaterialTheme.colorScheme.secondary)
            }
        }
    }
}
