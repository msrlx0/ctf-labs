package com.obsidianpay.mobile.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.obsidianpay.mobile.ObsidianScaffold
import com.obsidianpay.mobile.ResponseBox
import com.obsidianpay.mobile.api.ApiClient
import com.obsidianpay.mobile.api.ApiResult
import com.obsidianpay.mobile.storage.InsecureSessionStore
import com.obsidianpay.mobile.storage.LocalCacheManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun SupportScreen(
    apiClient: ApiClient,
    store: InsecureSessionStore,
    cache: LocalCacheManager,
    onBack: () -> Unit,
) {
    var response by remember { mutableStateOf("") }
    var status by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()
    val token = store.token

    // Runs a call, shows the response and persists the raw body via [onCache].
    fun run(label: String, block: suspend () -> ApiResult<String>, onCache: (String) -> Unit) {
        status = "$label..."
        scope.launch {
            val res = withContext(Dispatchers.IO) { block() }
            when (res) {
                is ApiResult.Success -> {
                    status = "$label: ${res.httpCode}"
                    response = res.data
                    onCache(res.rawBody)
                }
                is ApiResult.Error -> {
                    status = "$label: erro ${res.httpCode ?: "?"}"
                    response = res.message
                }
            }
        }
    }

    ObsidianScaffold(title = "Suporte", onBack = onBack) { modifier ->
        Column(
            modifier = modifier.fillMaxWidth().padding(16.dp).verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            OutlinedButton(
                onClick = {
                    run("Sync de suporte", { apiClient.supportSync(token, "ping do app") }) {
                        cache.cacheSupportSync(it)
                    }
                },
                modifier = Modifier.fillMaxWidth(),
            ) { Text("Enviar sync de suporte") }

            OutlinedButton(
                onClick = {
                    if (token == null) { status = "Sessão ausente."; return@OutlinedButton }
                    run("Diagnostics (sem header)", { apiClient.getSupportDiagnostics(token, includeDebugHeader = false) }) {
                        cache.cacheDiagnostics(it)
                    }
                },
                modifier = Modifier.fillMaxWidth(),
            ) { Text("Diagnostics (sem header)") }

            OutlinedButton(
                onClick = {
                    if (token == null) { status = "Sessão ausente."; return@OutlinedButton }
                    run("Diagnostics (com header)", { apiClient.getSupportDiagnostics(token, includeDebugHeader = true) }) {
                        cache.cacheDiagnostics(it)
                    }
                },
                modifier = Modifier.fillMaxWidth(),
            ) { Text("Diagnostics (com debug header)") }

            OutlinedButton(
                onClick = {
                    if (token == null) { status = "Sessão ausente."; return@OutlinedButton }
                    run("Rotas legadas", { apiClient.getLegacyRoutes(token) }) {
                        cache.cacheLegacyRoutes(it)
                    }
                },
                modifier = Modifier.fillMaxWidth(),
            ) { Text("Rotas legadas") }

            OutlinedButton(
                onClick = {
                    val artifacts = cache.listLocalArtifacts()
                    response = if (artifacts.isEmpty()) {
                        "Nenhum artefato local ainda."
                    } else {
                        artifacts.joinToString("\n")
                    }
                    status = "Artefatos locais: ${artifacts.size}"
                },
                modifier = Modifier.fillMaxWidth(),
            ) { Text("Local Artifacts") }

            if (status.isNotBlank()) Text(status)
            ResponseBox(response)
        }
    }
}
