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
import androidx.compose.material.icons.filled.PhonelinkLock
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.obsidianpay.mobile.api.ApiClient
import com.obsidianpay.mobile.environment.EnvironmentRiskEngine
import com.obsidianpay.mobile.storage.InsecureSessionStore
import com.obsidianpay.mobile.storage.LocalCacheManager
import com.obsidianpay.mobile.ui.components.DetailMonoCard
import com.obsidianpay.mobile.ui.components.PrimaryButton
import com.obsidianpay.mobile.ui.components.SecondaryButton
import com.obsidianpay.mobile.ui.components.StatusBanner
import com.obsidianpay.mobile.ui.components.StatusPill
import com.obsidianpay.mobile.ui.components.StatusTone
import com.obsidianpay.mobile.ui.components.SurfaceCard
import com.obsidianpay.mobile.ui.components.VSpace
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Device status screen — runs the local environment review (root + emulator
 * detection) and reports a friendly status.
 *
 * NOTE (instructor): the detection logic, the report shape, every cache event and
 * the monitor-only backend report call are unchanged. The checks still run only
 * on-device and the app still does NOT block on risk level — it only reports it
 * (a student reversing the APK still finds the EnvironmentRiskEngine signals and
 * bypass anchors). Only the presentation/labels changed: no "root", "emulator",
 * "bypass" or challenge wording is shown in the primary UI.
 */
@Composable
fun SecurityCheckScreen(
    modifier: Modifier,
    apiClient: ApiClient,
    store: InsecureSessionStore,
    cache: LocalCacheManager,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val token = store.getToken()

    var status by remember { mutableStateOf("") }
    var tone by remember { mutableStateOf(StatusTone.NEUTRAL) }
    var reportJson by remember { mutableStateOf("") }
    var responseJson by remember { mutableStateOf("") }
    var verified by remember { mutableStateOf<Boolean?>(null) }
    var headline by remember { mutableStateOf("Toque para verificar este dispositivo") }

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
                Icon(Icons.Filled.PhonelinkLock, contentDescription = null, tint = MaterialTheme.colorScheme.secondary)
                Column(Modifier.weight(1f)) {
                    Text("Status do dispositivo", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    Text(
                        headline,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                verified?.let {
                    StatusPill(
                        text = if (it) "Verificado" else "Atenção",
                        tone = if (it) StatusTone.POSITIVE else StatusTone.CAUTION,
                    )
                }
            }
        }

        PrimaryButton(text = "Verificar dispositivo", onClick = {
            tone = StatusTone.NEUTRAL
            status = "Verificando este dispositivo..."
            cache.addEvent("environment_check_started", "screen=SecurityCheckScreen")
            scope.launch {
                val report = withContext(Dispatchers.IO) { EnvironmentRiskEngine.run(context) }
                cache.addEvent("root_detection_completed", "isRooted=${report.root} score=${report.rootScore}")
                cache.addEvent("emulator_detection_completed", "isEmulator=${report.emulator} score=${report.emulatorScore}")
                val json = EnvironmentRiskEngine.toJson(report)
                cache.saveLastEnvironmentReportJson(json)
                cache.addEvent("environment_risk_calculated", "riskLevel=${report.riskLevel} bypassHintId=${report.bypassHintId}")
                reportJson = json
                val clean = !report.root && !report.emulator
                verified = clean
                if (clean) {
                    tone = StatusTone.POSITIVE
                    headline = "Este dispositivo está verificado."
                    status = "Tudo certo. Nenhuma ação necessária."
                } else {
                    tone = StatusTone.CAUTION
                    headline = "Verificação adicional recomendada."
                    status = "Recomendamos revisar a segurança deste dispositivo."
                }
            }
        })

        StatusBanner(text = status, tone = tone)

        // Advanced — share the technical report with support, or review the detail.
        SecondaryButton(text = "Enviar relatório de segurança", onClick = {
            if (token == null) { tone = StatusTone.NEGATIVE; status = "Sua sessão expirou. Entre novamente."; return@SecondaryButton }
            if (reportJson.isBlank()) { tone = StatusTone.CAUTION; status = "Verifique o dispositivo primeiro."; return@SecondaryButton }
            status = "Enviando relatório..."
            scope.launch {
                val raw = withContext(Dispatchers.IO) { apiClient.sendEnvironmentReport(token, reportJson) }
                cache.saveLastEnvironmentResponseJson(raw)
                cache.addEvent("environment_report_sent", "token_prefix=${token.take(20)}")
                cache.addEvent("environment_report_cached", "length=${raw.length}")
                responseJson = raw
                tone = StatusTone.POSITIVE
                status = "Relatório enviado."
            }
        })

        SecondaryButton(text = "Limpar dados desta verificação", onClick = {
            cache.clearEnvironmentReport()
            reportJson = ""; responseJson = ""; verified = null
            headline = "Toque para verificar este dispositivo"
            tone = StatusTone.NEUTRAL; status = "Dados da verificação removidos."
        })

        DetailMonoCard("Detalhes técnicos", reportJson)
        DetailMonoCard("Resposta do serviço", responseJson)
    }
}
