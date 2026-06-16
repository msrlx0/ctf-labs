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
import com.obsidianpay.mobile.environment.EnvironmentRiskEngine
import com.obsidianpay.mobile.storage.InsecureSessionStore
import com.obsidianpay.mobile.storage.LocalCacheManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * SecurityCheckScreen — local environment check (root + emulator detection).
 *
 * NOTE (instructor): this screen deliberately shows every signal and the
 * computed risk level. The checks run entirely on-device; the server only
 * records what the client reports. A student reversing the APK will find
 * the bypassHintId values in EnvironmentRiskEngine and can use them to
 * understand how to defeat the check (hooks / patching). The app does NOT
 * block on risk level — it only reports it.
 */
@Composable
fun SecurityCheckScreen(
    apiClient: ApiClient,
    store: InsecureSessionStore,
    cache: LocalCacheManager,
    onBack: () -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val token = store.getToken()

    var status by remember { mutableStateOf("") }
    var reportJson by remember { mutableStateOf("") }
    var responseJson by remember { mutableStateOf("") }
    var localSignals by remember { mutableStateOf("") }

    ObsidianScaffold(title = "Security Check", onBack = onBack) { modifier ->
        Column(
            modifier = modifier.fillMaxWidth().padding(16.dp).verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("Verificação de ambiente do dispositivo")
                    Text(
                        "Detecta indicadores de root e emulador para fins de análise de risco.",
                        fontSize = 12.sp,
                        fontFamily = FontFamily.Monospace,
                    )
                }
            }

            // Run Security Check
            Button(
                onClick = {
                    status = "Executando verificação..."
                    cache.addEvent("environment_check_started", "screen=SecurityCheckScreen")
                    scope.launch {
                        val report = withContext(Dispatchers.IO) {
                            EnvironmentRiskEngine.run(context)
                        }
                        cache.addEvent("root_detection_completed", "isRooted=${report.root} score=${report.rootScore}")
                        cache.addEvent("emulator_detection_completed", "isEmulator=${report.emulator} score=${report.emulatorScore}")
                        val json = EnvironmentRiskEngine.toJson(report)
                        cache.saveLastEnvironmentReportJson(json)
                        cache.addEvent("environment_risk_calculated", "riskLevel=${report.riskLevel} bypassHintId=${report.bypassHintId}")
                        reportJson = json
                        status = "Verificação concluída. Nível de risco: ${report.riskLevel}"
                    }
                },
                modifier = Modifier.fillMaxWidth(),
            ) { Text("Run Security Check") }

            // Send Environment Report
            OutlinedButton(
                onClick = {
                    if (token == null) { status = "Token ausente. Faça login primeiro."; return@OutlinedButton }
                    if (reportJson.isBlank()) { status = "Execute o check primeiro."; return@OutlinedButton }
                    status = "Enviando report..."
                    scope.launch {
                        val raw = withContext(Dispatchers.IO) {
                            apiClient.sendEnvironmentReport(token, reportJson)
                        }
                        cache.saveLastEnvironmentResponseJson(raw)
                        cache.addEvent("environment_report_sent", "token_prefix=${token.take(20)}")
                        cache.addEvent("environment_report_cached", "length=${raw.length}")
                        responseJson = raw
                        status = "Report enviado."
                    }
                },
                modifier = Modifier.fillMaxWidth(),
            ) { Text("Send Environment Report") }

            // Show Local Signals
            OutlinedButton(
                onClick = {
                    val cached = store.getLastEnvironmentReportJson()
                    localSignals = cached ?: "(nenhum report local encontrado)"
                    status = "Sinais locais carregados."
                },
                modifier = Modifier.fillMaxWidth(),
            ) { Text("Show Local Signals") }

            // Clear Local Report
            OutlinedButton(
                onClick = {
                    cache.clearEnvironmentReport()
                    reportJson = ""
                    responseJson = ""
                    localSignals = ""
                    status = "Report local limpo."
                },
                modifier = Modifier.fillMaxWidth(),
            ) { Text("Clear Local Report") }

            if (status.isNotBlank()) {
                Text(status, fontSize = 13.sp)
            }

            if (reportJson.isNotBlank()) {
                Text("Report local:", fontSize = 12.sp)
                Text(
                    text = reportJson,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 11.sp,
                )
            }

            if (responseJson.isNotBlank()) {
                Text("Resposta do servidor:", fontSize = 12.sp)
                Text(
                    text = responseJson,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 11.sp,
                )
            }

            if (localSignals.isNotBlank()) {
                Text("Sinais em cache local:", fontSize = 12.sp)
                Text(
                    text = localSignals,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 11.sp,
                )
            }
        }
    }
}
