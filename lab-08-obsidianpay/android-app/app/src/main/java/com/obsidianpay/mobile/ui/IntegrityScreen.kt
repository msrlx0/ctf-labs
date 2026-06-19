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
import androidx.compose.material.icons.filled.GppGood
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
import com.obsidianpay.mobile.api.ApiClient
import com.obsidianpay.mobile.integrity.NativeGate
import com.obsidianpay.mobile.integrity.TamperCheck
import com.obsidianpay.mobile.storage.InsecureSessionStore
import com.obsidianpay.mobile.storage.LocalCacheManager
import com.obsidianpay.mobile.ui.components.DetailMonoCard
import com.obsidianpay.mobile.ui.components.PrimaryButton
import com.obsidianpay.mobile.ui.components.SecondaryButton
import com.obsidianpay.mobile.ui.components.StatusBanner
import com.obsidianpay.mobile.ui.components.StatusTone
import com.obsidianpay.mobile.ui.components.SurfaceCard
import com.obsidianpay.mobile.ui.components.VSpace
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * App integrity screen — internal maintenance/diagnostic surface.
 *
 * NOTE (instructor): the NativeGate / TamperCheck scaffold and report contract are
 * unchanged. All checks remain client-side (a teaching seam); the report still
 * includes the same fields (tamperScore, signature preview, bypassHintIds, …) and
 * is posted to the same report-only attestation endpoint. Only the labels are
 * customer-friendly; technical detail stays in an advanced "details" card.
 */
@Composable
fun IntegrityScreen(
    modifier: Modifier,
    apiClient: ApiClient,
    store: InsecureSessionStore,
    cache: LocalCacheManager,
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val scope = rememberCoroutineScope()

    var status by remember { mutableStateOf("") }
    var tone by remember { mutableStateOf(StatusTone.NEUTRAL) }
    var integrityReportText by remember { mutableStateOf(store.getLastAppIntegrityReport() ?: "") }
    var integrityResponseText by remember { mutableStateOf(store.getLastAppIntegrityResponse() ?: "") }
    var verified by remember { mutableStateOf<Boolean?>(null) }

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
                Icon(Icons.Filled.GppGood, contentDescription = null, tint = MaterialTheme.colorScheme.secondary)
                Column(Modifier.weight(1f)) {
                    Text("Integridade do aplicativo", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    Text(
                        "Confirme que esta instalação do ObsidianPay não foi adulterada.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }

        PrimaryButton(text = "Verificar integridade", onClick = {
            tone = StatusTone.NEUTRAL
            status = "Verificando a integridade do app..."
            cache.addEvent("integrity_check_started", "screen")
            scope.launch {
                val tamperResult = withContext(Dispatchers.Default) { TamperCheck.run(context) }
                val nativeResult = NativeGate.getNativeGateStatus()

                val reportJson = buildString {
                    append("{")
                    append("\"tamperScore\":${tamperResult.tamperScore},")
                    append("\"debuggable\":${tamperResult.debuggable},")
                    append("\"installerPackage\":\"${tamperResult.installerPackage}\",")
                    append("\"packageNameStatus\":\"${tamperResult.packageNameStatus}\",")
                    append("\"signatureHashPreview\":\"${tamperResult.signatureHashPreview}\",")
                    append("\"nativeLibraryLoaded\":${nativeResult.loaded},")
                    append("\"nativeGateStatus\":\"${nativeResult.statusLabel}\",")
                    append("\"bypassHintIds\":[${tamperResult.bypassHints.joinToString(",") { "\"$it\"" }}]")
                    append("}")
                }

                cache.addEvent("tamper_check_completed", "score=${tamperResult.tamperScore}")
                cache.addEvent("native_gate_checked", "loaded=${nativeResult.loaded}")
                cache.addEvent("tamper_score_calculated", "score=${tamperResult.tamperScore}")

                store.saveLastTamperScore(tamperResult.tamperScore.toString())
                store.saveLastSignatureHashPreview(tamperResult.signatureHashPreview)
                store.saveLastNativeGateStatus(nativeResult.statusLabel)
                cache.saveAppIntegrityReport(reportJson)

                integrityReportText = reportJson
                verified = tamperResult.tamperScore == 0
                tone = if (verified == true) StatusTone.POSITIVE else StatusTone.CAUTION
                status = if (verified == true) "Integridade confirmada." else "Verificação adicional recomendada."
            }
        })

        StatusBanner(text = status, tone = tone)

        SecondaryButton(text = "Enviar relatório de integridade", onClick = {
            val token = store.getToken()
            if (token.isNullOrEmpty()) { tone = StatusTone.NEGATIVE; status = "Sua sessão expirou. Entre novamente."; return@SecondaryButton }
            val report = store.getLastAppIntegrityReport()
            if (report.isNullOrEmpty()) { tone = StatusTone.CAUTION; status = "Verifique a integridade primeiro."; return@SecondaryButton }
            status = "Enviando relatório..."
            cache.addEvent("integrity_report_sent", "sending")
            scope.launch {
                val responseJson = withContext(Dispatchers.IO) { apiClient.sendAppIntegrityReport(token, report) }
                cache.saveAppIntegrityResponse(responseJson)
                cache.addEvent("integrity_report_cached", "received")
                integrityResponseText = responseJson
                tone = StatusTone.POSITIVE
                status = "Relatório enviado."
            }
        })

        SecondaryButton(text = "Limpar dados de integridade", onClick = {
            store.clearIntegrityState()
            cache.addEvent("integrity_state_cleared", "screen")
            integrityReportText = ""; integrityResponseText = ""; verified = null
            tone = StatusTone.NEUTRAL; status = "Dados de integridade removidos."
        })

        DetailMonoCard("Detalhes técnicos", integrityReportText)
        DetailMonoCard("Resposta do serviço", integrityResponseText)
    }
}
