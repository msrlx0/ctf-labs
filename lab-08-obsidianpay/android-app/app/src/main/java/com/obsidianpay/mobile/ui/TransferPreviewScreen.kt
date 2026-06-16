package com.obsidianpay.mobile.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
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
import com.obsidianpay.mobile.api.ApiClient
import com.obsidianpay.mobile.api.ApiResult
import com.obsidianpay.mobile.storage.InsecureSessionStore
import com.obsidianpay.mobile.storage.LocalCacheManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun TransferPreviewScreen(
    apiClient: ApiClient,
    store: InsecureSessionStore,
    cache: LocalCacheManager,
    initialToUserId: String? = null,
    initialAmount: String? = null,
    initialMemo: String? = null,
    prefillSource: String? = null,
    onBack: () -> Unit,
) {
    var toUserId by remember { mutableStateOf(initialToUserId ?: "2001") }
    var amount by remember { mutableStateOf(initialAmount ?: "10") }
    var memo by remember { mutableStateOf(initialMemo ?: "teste") }
    var status by remember { mutableStateOf("") }
    var response by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()
    val token = store.getToken()

    // When opened from a deep link / QR, record where the prefill came from.
    LaunchedEffect(prefillSource) {
        if (prefillSource != null) {
            cache.addEvent("transfer_preview_from_$prefillSource", "toUserId=$toUserId amount=$amount")
            status = "Prefilled via $prefillSource."
        }
    }

    ObsidianScaffold(title = "Prévia de transferência", onBack = onBack) { modifier ->
        Column(
            modifier = modifier.fillMaxWidth().padding(16.dp).verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            OutlinedTextField(
                value = toUserId,
                onValueChange = { toUserId = it },
                label = { Text("Para (userId)") },
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = amount,
                onValueChange = { amount = it },
                label = { Text("Valor") },
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = memo,
                onValueChange = { memo = it },
                label = { Text("Memo") },
                modifier = Modifier.fillMaxWidth(),
            )

            Button(
                onClick = {
                    if (token == null) { status = "Sessão ausente."; return@Button }
                    status = "Gerando prévia..."
                    scope.launch {
                        val res = withContext(Dispatchers.IO) {
                            apiClient.transferPreview(token, toUserId.trim(), amount.trim(), memo)
                        }
                        when (res) {
                            is ApiResult.Success -> {
                                status = "Prévia gerada (executará: ${res.data.willExecute})."
                                response = res.data.raw
                                cache.cacheTransferPreview(res.rawBody)
                            }
                            is ApiResult.Error -> {
                                status = "Erro ${res.httpCode ?: "?"}"
                                response = res.message
                            }
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth(),
            ) { Text("Gerar prévia") }

            if (status.isNotBlank()) Text(status)
            ResponseBox(response)
        }
    }
}
