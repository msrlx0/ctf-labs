package com.obsidianpay.mobile.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.obsidianpay.mobile.ResponseBox
import com.obsidianpay.mobile.api.ApiClient
import com.obsidianpay.mobile.api.ApiResult
import com.obsidianpay.mobile.api.TransferPreviewResponse
import com.obsidianpay.mobile.storage.InsecureSessionStore
import com.obsidianpay.mobile.storage.LocalCacheManager
import com.obsidianpay.mobile.ui.components.PrimaryButton
import com.obsidianpay.mobile.ui.components.SectionHeader
import com.obsidianpay.mobile.ui.components.StatusBanner
import com.obsidianpay.mobile.ui.components.StatusPill
import com.obsidianpay.mobile.ui.components.StatusTone
import com.obsidianpay.mobile.ui.components.SurfaceCard
import com.obsidianpay.mobile.ui.components.VSpace
import com.obsidianpay.mobile.ui.components.formatBrl
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Transfer flow. The customer enters a recipient, an amount and an optional note,
 * then reviews a preview returned by the backend before any execution.
 *
 * NOTE (instructor): the deep-link / QR prefill path is preserved — values from
 * obsidianpay://transfer (toUserId / amount / memo) still populate the form and
 * the preview is generated against the same /api/mobile/transfer/preview contract
 * (toUserId sent as a number when numeric, otherwise as-is — weak by design).
 */
@Composable
fun TransferPreviewScreen(
    modifier: Modifier,
    apiClient: ApiClient,
    store: InsecureSessionStore,
    cache: LocalCacheManager,
    initialToUserId: String? = null,
    initialAmount: String? = null,
    initialMemo: String? = null,
    prefillSource: String? = null,
) {
    var toUserId by remember { mutableStateOf(initialToUserId ?: "2001") }
    var amount by remember { mutableStateOf(initialAmount ?: "10") }
    var memo by remember { mutableStateOf(initialMemo ?: "Pagamento") }
    var status by remember { mutableStateOf("") }
    var tone by remember { mutableStateOf(StatusTone.NEUTRAL) }
    var loading by remember { mutableStateOf(false) }
    var preview by remember { mutableStateOf<TransferPreviewResponse?>(null) }
    var raw by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()
    val token = store.getToken()

    // When opened from a deep link / QR, record where the prefill came from.
    LaunchedEffect(prefillSource) {
        if (prefillSource != null) {
            cache.addEvent("transfer_preview_from_$prefillSource", "toUserId=$toUserId amount=$amount")
            tone = StatusTone.NEUTRAL
            status = "Dados preenchidos automaticamente. Revise antes de continuar."
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
        Text("Para quem você quer transferir?", style = MaterialTheme.typography.titleMedium)

        SurfaceCard {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = toUserId,
                    onValueChange = { toUserId = it },
                    label = { Text("Destinatário (conta)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                )
                OutlinedTextField(
                    value = amount,
                    onValueChange = { amount = it },
                    label = { Text("Valor (R$)") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                )
                OutlinedTextField(
                    value = memo,
                    onValueChange = { memo = it },
                    label = { Text("Descrição (opcional)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                )
            }
        }

        PrimaryButton(
            text = "Revisar transferência",
            loading = loading,
            onClick = {
                if (token == null) { tone = StatusTone.NEGATIVE; status = "Sua sessão expirou. Entre novamente."; return@PrimaryButton }
                loading = true
                tone = StatusTone.NEUTRAL
                status = "Gerando prévia..."
                scope.launch {
                    val res = withContext(Dispatchers.IO) {
                        apiClient.transferPreview(token, toUserId.trim(), amount.trim(), memo)
                    }
                    loading = false
                    when (res) {
                        is ApiResult.Success -> {
                            preview = res.data
                            raw = res.data.raw
                            cache.cacheTransferPreview(res.rawBody)
                            tone = StatusTone.POSITIVE
                            status = "Prévia pronta. Confira os detalhes abaixo."
                        }
                        is ApiResult.Error -> {
                            preview = null
                            raw = ""
                            tone = StatusTone.NEGATIVE
                            status = "Não foi possível gerar a prévia. Tente novamente."
                        }
                    }
                }
            },
        )

        StatusBanner(text = status, tone = tone)

        preview?.let { p ->
            SectionHeader(title = "Revisão")
            SurfaceCard {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    ReviewRow("Destinatário", p.recipientDisplayName ?: "Conta $toUserId")
                    ReviewRow("Valor", formatBrl(p.amount ?: amount.toDoubleOrNull()))
                    ReviewRow("Descrição", p.memo ?: memo)
                    VSpace(2)
                    StatusPill(
                        text = if (p.willExecute == true) "Pronta para confirmar" else "Revisão necessária",
                        tone = if (p.willExecute == true) StatusTone.POSITIVE else StatusTone.CAUTION,
                    )
                }
            }
            // Discreet technical detail kept available for advanced review.
            ResponseBox(raw)
        }
    }
}

@Composable
private fun ReviewRow(label: String, value: String) {
    androidx.compose.foundation.layout.Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.bodyLarge)
    }
}
