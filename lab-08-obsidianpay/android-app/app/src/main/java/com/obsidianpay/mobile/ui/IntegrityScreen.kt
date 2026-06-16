package com.obsidianpay.mobile.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Divider
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.obsidianpay.mobile.ObsidianScaffold
import com.obsidianpay.mobile.api.ApiClient
import com.obsidianpay.mobile.integrity.NativeGate
import com.obsidianpay.mobile.integrity.TamperCheck
import com.obsidianpay.mobile.storage.InsecureSessionStore
import com.obsidianpay.mobile.storage.LocalCacheManager
import com.obsidianpay.mobile.util.Constants
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * App Integrity screen — Phase 12 scaffold.
 *
 * Shows the NativeGate status, TamperCheck results and backend integrity
 * attestation response. Presented as an internal maintenance/diagnostic surface,
 * not as a vulnerability showcase. All checks are client-side (teaching seam).
 */
@Composable
fun IntegrityScreen(
    apiClient: ApiClient,
    store: InsecureSessionStore,
    cache: LocalCacheManager,
    onBack: () -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var status by remember { mutableStateOf("") }
    var nativeGateText by remember { mutableStateOf(store.getLastNativeGateStatus() ?: "") }
    var tamperScoreText by remember { mutableStateOf(store.getLastTamperScore() ?: "") }
    var sigHashText by remember { mutableStateOf(store.getLastSignatureHashPreview() ?: "") }
    var integrityReportText by remember { mutableStateOf(store.getLastAppIntegrityReport() ?: "") }
    var integrityResponseText by remember { mutableStateOf(store.getLastAppIntegrityResponse() ?: "") }

    ObsidianScaffold(title = "App Integrity", onBack = onBack) { modifier ->
        Column(
            modifier = modifier
                .fillMaxWidth()
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {

            // --- Summary card -------------------------------------------------------
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("Native gate loaded: ${NativeGate.isNativeLibraryLoaded()}")
                    if (nativeGateText.isNotBlank()) IntegrityMono("Native status: $nativeGateText")
                    if (tamperScoreText.isNotBlank()) IntegrityMono("Tamper score: $tamperScoreText")
                    if (sigHashText.isNotBlank()) IntegrityMono("Sig hash: $sigHashText")
                }
            }

            // --- Run Integrity Check -----------------------------------------------
            Button(
                onClick = {
                    status = "Executando integrity check..."
                    cache.addEvent("integrity_check_started", "screen")
                    scope.launch {
                        val tamperResult = withContext(Dispatchers.Default) {
                            TamperCheck.run(context)
                        }
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

                        tamperScoreText = tamperResult.tamperScore.toString()
                        sigHashText = tamperResult.signatureHashPreview
                        nativeGateText = nativeResult.statusLabel
                        integrityReportText = reportJson
                        status = "Integrity check concluído."
                    }
                },
                modifier = Modifier.fillMaxWidth(),
            ) { Text("Run Integrity Check") }

            // --- Show Native Gate --------------------------------------------------
            OutlinedButton(
                onClick = {
                    val hint = NativeGate.getNativeSecretHint()
                    val gateStatus = NativeGate.getNativeGateStatus()
                    store.saveLastNativeGateStatus(gateStatus.statusLabel)
                    nativeGateText = gateStatus.toJson()
                    cache.addEvent("native_gate_hint_viewed", hint)
                    status = "Native gate: $hint"
                },
                modifier = Modifier.fillMaxWidth(),
            ) { Text("Show Native Gate") }

            // --- Send Integrity Report ---------------------------------------------
            OutlinedButton(
                onClick = {
                    val token = store.getToken()
                    if (token.isNullOrEmpty()) {
                        status = "Sessão ausente. Faça login primeiro."
                        return@OutlinedButton
                    }
                    val report = store.getLastAppIntegrityReport()
                    if (report.isNullOrEmpty()) {
                        status = "Execute 'Run Integrity Check' primeiro."
                        return@OutlinedButton
                    }
                    status = "Enviando integrity report..."
                    cache.addEvent("integrity_report_sent", "sending")
                    scope.launch {
                        val responseJson = withContext(Dispatchers.IO) {
                            apiClient.sendAppIntegrityReport(token, report)
                        }
                        cache.saveAppIntegrityResponse(responseJson)
                        cache.addEvent("integrity_report_cached", "received")
                        integrityResponseText = responseJson
                        status = "Integrity report enviado."
                    }
                },
                modifier = Modifier.fillMaxWidth(),
            ) { Text("Send Integrity Report") }

            // --- Clear Integrity State ---------------------------------------------
            OutlinedButton(
                onClick = {
                    store.clearIntegrityState()
                    cache.addEvent("integrity_state_cleared", "screen")
                    nativeGateText = ""
                    tamperScoreText = ""
                    sigHashText = ""
                    integrityReportText = ""
                    integrityResponseText = ""
                    status = "Integrity state limpo."
                },
                modifier = Modifier.fillMaxWidth(),
            ) { Text("Clear Integrity State") }

            // --- Status & response display ----------------------------------------
            if (status.isNotBlank()) Text(status)

            if (integrityReportText.isNotBlank()) {
                Divider(modifier = Modifier.padding(vertical = 4.dp))
                Text("Último relatório local:", fontSize = 12.sp)
                IntegrityMono(integrityReportText)
            }

            if (integrityResponseText.isNotBlank()) {
                Divider(modifier = Modifier.padding(vertical = 4.dp))
                Text("Última resposta do backend:", fontSize = 12.sp)
                IntegrityMono(integrityResponseText)
            }

            // --- Bypass hints (didactic) ------------------------------------------
            val bypassHints = NativeGate.buildNativeBypassHints() +
                TamperCheck.buildTamperBypassHints()
            if (bypassHints.isNotEmpty()) {
                Divider(modifier = Modifier.padding(vertical = 4.dp))
                Text("Bypass hint IDs (análise estática / dinâmica):", fontSize = 12.sp)
                bypassHints.distinct().forEach { hint ->
                    IntegrityMono("• $hint")
                }
            }
        }
    }
}

@Composable
private fun IntegrityMono(text: String) {
    Text(
        text = text,
        fontFamily = FontFamily.Monospace,
        fontSize = 11.sp,
        modifier = Modifier.padding(start = 4.dp),
    )
}
