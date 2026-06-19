package com.obsidianpay.mobile.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.SupportAgent
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.obsidianpay.mobile.Screen
import com.obsidianpay.mobile.api.ApiClient
import com.obsidianpay.mobile.api.ApiResult
import com.obsidianpay.mobile.api.Receipt
import com.obsidianpay.mobile.api.UserProfile
import com.obsidianpay.mobile.storage.InsecureSessionStore
import com.obsidianpay.mobile.storage.LocalCacheManager
import com.obsidianpay.mobile.ui.components.ActivityRow
import com.obsidianpay.mobile.ui.components.QuickAction
import com.obsidianpay.mobile.ui.components.SectionHeader
import com.obsidianpay.mobile.ui.components.StatusPill
import com.obsidianpay.mobile.ui.components.StatusTone
import com.obsidianpay.mobile.ui.components.SurfaceCard
import com.obsidianpay.mobile.ui.components.VSpace
import com.obsidianpay.mobile.ui.components.formatBrl
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject

@Composable
fun HomeScreen(
    modifier: Modifier,
    apiClient: ApiClient,
    store: InsecureSessionStore,
    cache: LocalCacheManager,
    onNavigate: (Screen) -> Unit,
) {
    var profile by remember { mutableStateOf<UserProfile?>(null) }
    var receipts by remember { mutableStateOf<List<Receipt>>(emptyList()) }
    var refreshing by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val token = store.getToken()

    fun refresh() {
        if (token == null || refreshing) return
        refreshing = true
        scope.launch {
            val pr = withContext(Dispatchers.IO) { apiClient.getProfile(token) }
            if (pr is ApiResult.Success) {
                profile = pr.data
                cache.cacheProfile(pr.rawBody)
            }
            val rc = withContext(Dispatchers.IO) { apiClient.getReceipts(token) }
            if (rc is ApiResult.Success) {
                receipts = rc.data
                cache.cacheReceiptList(rc.rawBody)
            }
            refreshing = false
        }
    }

    // Hydrate from the local cache immediately, then refresh from the network.
    LaunchedEffect(Unit) {
        val cached = store.getRawProfileJson()
        if (cached != null && profile == null) {
            runCatching { profile = UserProfile.fromJson(JSONObject(cached)) }
        }
        refresh()
    }

    val greetingName = profile?.displayName ?: profile?.username ?: store.getUsername() ?: "cliente"

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp)
            .padding(bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        VSpace(4)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column {
                Text("Olá,", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(greetingName, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
            }
            IconButton(onClick = { refresh() }) {
                Icon(Icons.Filled.Refresh, contentDescription = "Atualizar", tint = MaterialTheme.colorScheme.secondary)
            }
        }

        // --- Balance card -------------------------------------------------------
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(22.dp),
            color = MaterialTheme.colorScheme.primaryContainer,
            tonalElevation = 4.dp,
        ) {
            Column(Modifier.padding(20.dp)) {
                Text(
                    "Saldo disponível",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f),
                )
                VSpace(6)
                Text(
                    formatBrl(profile?.balanceBRL),
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                )
                VSpace(14)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    StatusPill(
                        text = "Plano ${profile?.plan ?: store.getPlan() ?: "—"}",
                        tone = StatusTone.NEUTRAL,
                    )
                    val kyc = profile?.kycApproved
                    StatusPill(
                        text = if (kyc == true) "Conta verificada" else "Verificação pendente",
                        tone = if (kyc == true) StatusTone.POSITIVE else StatusTone.CAUTION,
                    )
                }
            }
        }

        // --- Quick actions ------------------------------------------------------
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            QuickAction(Icons.Filled.SwapHoriz, "Transferir", { onNavigate(Screen.Transfer) }, Modifier.weight(1f))
            QuickAction(Icons.Filled.QrCodeScanner, "Pagar", { onNavigate(Screen.Qr) }, Modifier.weight(1f))
            QuickAction(Icons.Filled.CreditCard, "Cartões", { onNavigate(Screen.Cards) }, Modifier.weight(1f))
            QuickAction(Icons.Filled.SupportAgent, "Ajuda", { onNavigate(Screen.Support) }, Modifier.weight(1f))
        }

        // --- Security status shortcut ------------------------------------------
        Surface(
            onClick = { onNavigate(Screen.Security) },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surfaceContainer,
        ) {
            Row(
                Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                Icon(Icons.Filled.Shield, contentDescription = null, tint = MaterialTheme.colorScheme.secondary)
                Column(Modifier.weight(1f)) {
                    Text("Central de segurança", style = MaterialTheme.typography.titleMedium)
                    Text(
                        "Revise o status do dispositivo, o cofre e a proteção da conta.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                StatusPill("Ativa", StatusTone.POSITIVE)
            }
        }

        // --- Recent activity ----------------------------------------------------
        SectionHeader(
            title = "Atividade recente",
            actionLabel = "Ver tudo",
            onAction = { onNavigate(Screen.Receipts) },
        )
        SurfaceCard {
            if (receipts.isEmpty()) {
                Text(
                    if (refreshing) "Carregando sua atividade..." else "Nenhuma movimentação recente.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                receipts.take(4).forEachIndexed { i, r ->
                    val credit = r.type?.contains("credit", true) == true ||
                        r.type?.contains("received", true) == true
                    ActivityRow(
                        title = r.counterparty ?: "Movimentação",
                        subtitle = "${r.type ?: "transação"} · ${(r.createdAt ?: "").take(10)}",
                        amount = formatBrl(r.amountBRL),
                        amountPositive = credit,
                        statusLabel = r.status,
                    )
                    if (i < minOf(3, receipts.size - 1)) {
                        androidx.compose.material3.HorizontalDivider(color = MaterialTheme.colorScheme.outline)
                    }
                }
            }
        }
    }
}
