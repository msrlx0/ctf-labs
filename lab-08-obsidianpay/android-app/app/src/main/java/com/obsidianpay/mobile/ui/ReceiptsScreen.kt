package com.obsidianpay.mobile.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
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
import com.obsidianpay.mobile.ResponseBox
import com.obsidianpay.mobile.api.ApiClient
import com.obsidianpay.mobile.api.ApiResult
import com.obsidianpay.mobile.api.Receipt
import com.obsidianpay.mobile.storage.InsecureSessionStore
import com.obsidianpay.mobile.storage.LocalCacheManager
import com.obsidianpay.mobile.ui.components.ActivityRow
import com.obsidianpay.mobile.ui.components.LoadingState
import com.obsidianpay.mobile.ui.components.PrimaryButton
import com.obsidianpay.mobile.ui.components.SectionHeader
import com.obsidianpay.mobile.ui.components.StatusBanner
import com.obsidianpay.mobile.ui.components.StatusTone
import com.obsidianpay.mobile.ui.components.SurfaceCard
import com.obsidianpay.mobile.ui.components.VSpace
import com.obsidianpay.mobile.ui.components.formatBrl
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun ReceiptsScreen(
    modifier: Modifier,
    apiClient: ApiClient,
    store: InsecureSessionStore,
    cache: LocalCacheManager,
    initialReceiptId: String? = null,
    prefillSource: String? = null,
) {
    var receipts by remember { mutableStateOf<List<Receipt>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var status by remember { mutableStateOf("") }
    var tone by remember { mutableStateOf(StatusTone.NEUTRAL) }
    var lookupId by remember { mutableStateOf(initialReceiptId ?: "") }
    var detail by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()
    val token = store.getToken()

    LaunchedEffect(Unit) {
        if (token == null) { loading = false; tone = StatusTone.NEGATIVE; status = "Sua sessão expirou. Entre novamente."; return@LaunchedEffect }
        val res = withContext(Dispatchers.IO) { apiClient.getReceipts(token) }
        loading = false
        when (res) {
            is ApiResult.Success -> { receipts = res.data; cache.cacheReceiptList(res.rawBody) }
            is ApiResult.Error -> { tone = StatusTone.NEGATIVE; status = "Não foi possível carregar sua atividade." }
        }
    }

    // Opened via deep link / QR with a specific id: auto-open it.
    LaunchedEffect(initialReceiptId) {
        val id = initialReceiptId?.trim().orEmpty()
        if (id.isEmpty()) return@LaunchedEffect
        if (prefillSource != null) cache.addEvent("receipt_from_$prefillSource", "id=$id")
        if (token == null) { detail = "Sua sessão expirou."; return@LaunchedEffect }
        detail = "Abrindo recibo $id..."
        val res = withContext(Dispatchers.IO) { apiClient.getReceipt(token, id) }
        detail = when (res) {
            is ApiResult.Success -> { cache.cacheReceipt(id, res.rawBody); res.rawBody }
            is ApiResult.Error -> "Não foi possível abrir o recibo (${res.httpCode ?: "?"})."
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .imePadding()
            .padding(horizontal = 16.dp)
            .padding(bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        VSpace(4)
        SectionHeader(title = "Suas movimentações")
        if (loading) {
            LoadingState("Carregando sua atividade...")
        } else if (receipts.isEmpty()) {
            Text(
                "Nenhuma movimentação encontrada.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        } else {
            SurfaceCard {
                receipts.forEachIndexed { i, r ->
                    val credit = r.type?.contains("credit", true) == true || r.type?.contains("received", true) == true
                    ActivityRow(
                        title = r.counterparty ?: "Movimentação",
                        subtitle = "${r.type ?: "transação"} · ${r.reference ?: "#${r.receiptId}"}",
                        amount = formatBrl(r.amountBRL),
                        amountPositive = credit,
                        statusLabel = r.status,
                    )
                    if (i < receipts.size - 1) HorizontalDivider(color = MaterialTheme.colorScheme.outline)
                }
            }
        }

        StatusBanner(text = status, tone = tone)

        SectionHeader(title = "Buscar recibo")
        SurfaceCard {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = lookupId,
                    onValueChange = { lookupId = it },
                    label = { Text("Número do recibo") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                )
                PrimaryButton(text = "Abrir recibo", onClick = {
                    val id = lookupId.trim()
                    if (token == null || id.isEmpty()) { detail = "Informe o número do recibo."; return@PrimaryButton }
                    detail = "Abrindo recibo $id..."
                    scope.launch {
                        val res = withContext(Dispatchers.IO) { apiClient.getReceipt(token, id) }
                        detail = when (res) {
                            is ApiResult.Success -> { cache.cacheReceipt(id, res.rawBody); res.rawBody }
                            is ApiResult.Error -> "Não foi possível abrir o recibo (${res.httpCode ?: "?"})."
                        }
                    }
                })
                ResponseBox(detail)
            }
        }
    }
}
