package com.obsidianpay.mobile.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.OutlinedButton
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
import com.obsidianpay.mobile.api.Receipt
import com.obsidianpay.mobile.storage.InsecureSessionStore
import com.obsidianpay.mobile.storage.LocalCacheManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun ReceiptsScreen(
    apiClient: ApiClient,
    store: InsecureSessionStore,
    cache: LocalCacheManager,
    initialReceiptId: String? = null,
    prefillSource: String? = null,
    onBack: () -> Unit,
) {
    var receipts by remember { mutableStateOf<List<Receipt>>(emptyList()) }
    var status by remember { mutableStateOf("Carregando recibos...") }
    var lookupId by remember { mutableStateOf(initialReceiptId ?: "") }
    var detail by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()
    val token = store.token

    LaunchedEffect(Unit) {
        if (token == null) {
            status = "Sessão ausente."
            return@LaunchedEffect
        }
        val res = withContext(Dispatchers.IO) { apiClient.getReceipts(token) }
        when (res) {
            is ApiResult.Success -> {
                receipts = res.data
                cache.cacheReceiptList(res.rawBody)
                status = "${res.data.size} recibo(s)."
            }
            is ApiResult.Error -> status = "Erro: ${res.message}"
        }
    }

    // Opened via deep link / QR with a specific id: auto-open it.
    LaunchedEffect(initialReceiptId) {
        val id = initialReceiptId?.trim().orEmpty()
        if (id.isEmpty()) return@LaunchedEffect
        if (prefillSource != null) cache.addEvent("receipt_from_$prefillSource", "id=$id")
        if (token == null) {
            detail = "Sessão ausente."
            return@LaunchedEffect
        }
        detail = "Abrindo recibo $id..."
        val res = withContext(Dispatchers.IO) { apiClient.getReceipt(token, id) }
        detail = when (res) {
            is ApiResult.Success -> {
                cache.cacheReceipt(id, res.rawBody)
                res.rawBody
            }
            is ApiResult.Error -> "Erro (${res.httpCode ?: "?"}): ${res.message}"
        }
    }

    ObsidianScaffold(title = "Recibos", onBack = onBack) { modifier ->
        Column(
            modifier = modifier.fillMaxWidth().padding(16.dp).verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(status)

            receipts.forEach { r ->
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(12.dp)) {
                        Text("#${r.receiptId} · ${r.type ?: "-"} · ${r.status ?: "-"}")
                        Text("${r.amountBRL ?: 0.0} ${r.currency ?: ""} → ${r.counterparty ?: "-"}")
                        Text("Ref: ${r.reference ?: "-"}")
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                OutlinedTextField(
                    value = lookupId,
                    onValueChange = { lookupId = it },
                    label = { Text("ID do recibo") },
                    modifier = Modifier.weight(1f),
                )
                Button(onClick = {
                    val id = lookupId.trim()
                    if (token == null || id.isEmpty()) {
                        detail = "Informe um ID."
                        return@Button
                    }
                    detail = "Abrindo recibo $id..."
                    scope.launch {
                        val res = withContext(Dispatchers.IO) { apiClient.getReceipt(token, id) }
                        when (res) {
                            is ApiResult.Success -> {
                                cache.cacheReceipt(id, res.rawBody)
                                detail = res.rawBody
                            }
                            is ApiResult.Error -> detail = "Erro (${res.httpCode ?: "?"}): ${res.message}"
                        }
                    }
                }) { Text("Abrir") }
            }

            OutlinedButton(
                onClick = {
                    val cached = cache.cachedReceipts()
                    detail = if (cached.isEmpty()) {
                        "Nenhum recibo em cache local."
                    } else {
                        cached.joinToString("\n") {
                            "${it.id} · ${it.ownerRole ?: "-"} · ${it.reference ?: "-"} · ${it.amount ?: "-"}"
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth(),
            ) { Text("Cached Receipts") }

            ResponseBox(detail)
        }
    }
}
