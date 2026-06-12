package com.obsidianpay.mobile.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.obsidianpay.mobile.ObsidianScaffold
import com.obsidianpay.mobile.ResponseBox
import com.obsidianpay.mobile.Screen
import com.obsidianpay.mobile.api.ApiClient
import com.obsidianpay.mobile.api.ApiResult
import com.obsidianpay.mobile.api.UserProfile
import com.obsidianpay.mobile.storage.InsecureSessionStore
import com.obsidianpay.mobile.storage.LocalCacheManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject

@Composable
fun HomeScreen(
    apiClient: ApiClient,
    store: InsecureSessionStore,
    cache: LocalCacheManager,
    onNavigate: (Screen) -> Unit,
    onLogout: () -> Unit,
) {
    var profile by remember { mutableStateOf<UserProfile?>(null) }
    var status by remember { mutableStateOf("") }
    var configText by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()
    val token = store.token

    // Hydrate the header from the locally cached profile on first show.
    LaunchedEffect(Unit) {
        val cached = store.getRawProfileJson()
        if (cached != null && profile == null) {
            runCatching { profile = UserProfile.fromJson(JSONObject(cached)) }
        }
    }

    ObsidianScaffold(title = "Início") { modifier ->
        Column(
            modifier = modifier.fillMaxWidth().padding(16.dp).verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("Usuário: ${profile?.username ?: store.getUsername() ?: "-"}")
                    Text("Papel: ${profile?.role ?: store.getRole() ?: "-"}")
                    Text("Plano: ${profile?.plan ?: store.getPlan() ?: "-"}")
                    Text("Limite diário: ${profile?.dailyLimit?.toString() ?: store.getDailyLimit() ?: "-"}")
                    Text("Saldo (BRL): ${profile?.balanceBRL?.toString() ?: "-"}")
                }
            }

            Button(
                onClick = {
                    if (token == null) {
                        status = "Sessão ausente."
                        return@Button
                    }
                    status = "Atualizando perfil..."
                    scope.launch {
                        val res = withContext(Dispatchers.IO) { apiClient.getProfile(token) }
                        when (res) {
                            is ApiResult.Success -> {
                                profile = res.data
                                cache.cacheProfile(res.rawBody)
                                status = "Perfil atualizado."
                            }
                            is ApiResult.Error -> status = "Erro: ${res.message}"
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth(),
            ) { Text("Atualizar perfil") }

            OutlinedButton(onClick = { onNavigate(Screen.Receipts) }, modifier = Modifier.fillMaxWidth()) { Text("Recibos") }
            OutlinedButton(onClick = { onNavigate(Screen.Cards) }, modifier = Modifier.fillMaxWidth()) { Text("Cartões") }
            OutlinedButton(onClick = { onNavigate(Screen.Support) }, modifier = Modifier.fillMaxWidth()) { Text("Suporte") }
            OutlinedButton(onClick = { onNavigate(Screen.Transfer) }, modifier = Modifier.fillMaxWidth()) { Text("Prévia de transferência") }
            OutlinedButton(onClick = { onNavigate(Screen.Qr) }, modifier = Modifier.fillMaxWidth()) { Text("QR Payment") }

            OutlinedButton(
                onClick = {
                    status = "Buscando config..."
                    scope.launch {
                        val res = withContext(Dispatchers.IO) { apiClient.getConfig(token) }
                        when (res) {
                            is ApiResult.Success -> {
                                configText = res.data.raw
                                cache.cacheConfig(res.rawBody)
                                status = "Config carregada."
                            }
                            is ApiResult.Error -> status = "Erro: ${res.message}"
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth(),
            ) { Text("Configuração") }

            // Routine device security/attestation check.
            OutlinedButton(onClick = { onNavigate(Screen.DeviceTrust) }, modifier = Modifier.fillMaxWidth()) { Text("Device Trust") }

            // Environment / risk check (Phase 9).
            OutlinedButton(onClick = { onNavigate(Screen.SecurityCheck) }, modifier = Modifier.fillMaxWidth()) { Text("Security Check") }

            // Secure Vault (Phase 10): local auth / biometric gate.
            OutlinedButton(onClick = { onNavigate(Screen.Vault) }, modifier = Modifier.fillMaxWidth()) { Text("Secure Vault") }

            // API host override (Phase 11): switch between emulator and physical device.
            OutlinedButton(onClick = { onNavigate(Screen.ApiHost) }, modifier = Modifier.fillMaxWidth()) { Text("API Host") }

            // Internal support/dev tooling — local state inspector.
            OutlinedButton(onClick = { onNavigate(Screen.LocalState) }, modifier = Modifier.fillMaxWidth()) { Text("Local State") }
            OutlinedButton(
                onClick = {
                    cache.clearLocalArtifacts()
                    cache.addEvent("clear_local_data", "home")
                    status = "Dados locais limpos."
                },
                modifier = Modifier.fillMaxWidth(),
            ) { Text("Clear Local Data") }

            Button(onClick = onLogout, modifier = Modifier.fillMaxWidth()) { Text("Sair") }

            if (status.isNotBlank()) Text(status)
            ResponseBox(configText)
        }
    }
}
