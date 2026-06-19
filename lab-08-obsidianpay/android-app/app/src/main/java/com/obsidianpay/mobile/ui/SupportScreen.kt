package com.obsidianpay.mobile.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.obsidianpay.mobile.ResponseBox
import com.obsidianpay.mobile.api.ApiClient
import com.obsidianpay.mobile.api.ApiResult
import com.obsidianpay.mobile.storage.InsecureSessionStore
import com.obsidianpay.mobile.storage.LocalCacheManager
import com.obsidianpay.mobile.ui.components.NavRow
import com.obsidianpay.mobile.ui.components.PrimaryButton
import com.obsidianpay.mobile.ui.components.SecondaryButton
import com.obsidianpay.mobile.ui.components.SectionHeader
import com.obsidianpay.mobile.ui.components.StatusBanner
import com.obsidianpay.mobile.ui.components.StatusTone
import com.obsidianpay.mobile.ui.components.SurfaceCard
import com.obsidianpay.mobile.ui.components.VSpace
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Support / help center.
 *
 * NOTE (instructor): the support surfaces are preserved — the in-app help portal
 * still opens via the WebView (onOpenWebSupport), and the backend support
 * endpoints (support/sync, support/diagnostics with and without the debug header,
 * legacy/routes) plus local-artifact listing remain reachable under an advanced
 * "diagnostics" framing. Behavior and cache events are unchanged.
 */
@Composable
fun SupportScreen(
    modifier: Modifier,
    apiClient: ApiClient,
    store: InsecureSessionStore,
    cache: LocalCacheManager,
    onOpenWebSupport: (topic: String?, message: String?) -> Unit,
) {
    var response by remember { mutableStateOf("") }
    var status by remember { mutableStateOf("") }
    var tone by remember { mutableStateOf(StatusTone.NEUTRAL) }
    val scope = rememberCoroutineScope()
    val token = store.getToken()

    fun run(label: String, block: suspend () -> ApiResult<String>, onCache: (String) -> Unit) {
        status = "$label..."
        tone = StatusTone.NEUTRAL
        scope.launch {
            val res = withContext(Dispatchers.IO) { block() }
            when (res) {
                is ApiResult.Success -> {
                    tone = StatusTone.POSITIVE; status = "$label: concluído."
                    response = res.data; onCache(res.rawBody)
                }
                is ApiResult.Error -> {
                    tone = StatusTone.NEGATIVE; status = "$label: não foi possível concluir."
                    response = res.message
                }
            }
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp)
            .padding(bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        VSpace(4)
        Text("Como podemos ajudar?", style = MaterialTheme.typography.titleMedium)

        // Help articles — each opens the support portal scoped to the topic.
        HelpArticle(Icons.Filled.SwapHoriz, "Transferências e pagamentos", "Como enviar dinheiro e usar o pagamento por QR.") {
            onOpenWebSupport("mobile", "Transferências e pagamentos")
        }
        HelpArticle(Icons.Filled.CreditCard, "Cartões", "Gerencie seus cartões e veja os detalhes.") {
            onOpenWebSupport("mobile", "Cartões")
        }
        HelpArticle(Icons.Filled.Security, "Segurança da conta", "Proteja sua conta e seu dispositivo.") {
            onOpenWebSupport("mobile", "Segurança da conta")
        }

        SectionHeader(title = "Atendimento")
        PrimaryButton(text = "Abrir central de ajuda", onClick = { onOpenWebSupport("mobile", null) })
        SecondaryButton(text = "Enviar mensagem ao suporte", onClick = { onOpenWebSupport("mobile", "hello from support") })

        // Advanced diagnostics (kept for support/automation).
        SectionHeader(title = "Diagnóstico")
        SurfaceCard {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                SecondaryButton(text = "Sincronizar suporte", onClick = {
                    run("Sincronização", { apiClient.supportSync(token, "ping do app") }) { cache.cacheSupportSync(it) }
                })
                SecondaryButton(text = "Executar diagnóstico", onClick = {
                    if (token == null) { tone = StatusTone.NEGATIVE; status = "Sua sessão expirou."; return@SecondaryButton }
                    run("Diagnóstico", { apiClient.getSupportDiagnostics(token, includeDebugHeader = false) }) { cache.cacheDiagnostics(it) }
                })
                SecondaryButton(text = "Diagnóstico detalhado", onClick = {
                    if (token == null) { tone = StatusTone.NEGATIVE; status = "Sua sessão expirou."; return@SecondaryButton }
                    run("Diagnóstico detalhado", { apiClient.getSupportDiagnostics(token, includeDebugHeader = true) }) { cache.cacheDiagnostics(it) }
                })
                SecondaryButton(text = "Serviços disponíveis", onClick = {
                    if (token == null) { tone = StatusTone.NEGATIVE; status = "Sua sessão expirou."; return@SecondaryButton }
                    run("Serviços", { apiClient.getLegacyRoutes(token) }) { cache.cacheLegacyRoutes(it) }
                })
                SecondaryButton(text = "Arquivos locais de suporte", onClick = {
                    val artifacts = cache.listLocalArtifacts()
                    response = if (artifacts.isEmpty()) "Nenhum arquivo local ainda." else artifacts.joinToString("\n")
                    tone = StatusTone.NEUTRAL; status = "Arquivos locais: ${artifacts.size}"
                })
            }
        }

        StatusBanner(text = status, tone = tone)
        ResponseBox(response)
    }
}

@Composable
private fun HelpArticle(icon: ImageVector, title: String, subtitle: String, onClick: () -> Unit) {
    NavRow(icon = icon, title = title, subtitle = subtitle, onClick = onClick)
}
