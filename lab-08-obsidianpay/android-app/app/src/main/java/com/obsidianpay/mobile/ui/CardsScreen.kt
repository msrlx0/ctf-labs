package com.obsidianpay.mobile.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Contactless
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import com.obsidianpay.mobile.ResponseBox
import com.obsidianpay.mobile.api.ApiClient
import com.obsidianpay.mobile.api.ApiResult
import com.obsidianpay.mobile.api.Card as CardModel
import com.obsidianpay.mobile.storage.InsecureSessionStore
import com.obsidianpay.mobile.storage.LocalCacheManager
import com.obsidianpay.mobile.ui.components.LoadingState
import com.obsidianpay.mobile.ui.components.PrimaryButton
import com.obsidianpay.mobile.ui.components.SectionHeader
import com.obsidianpay.mobile.ui.components.StatusBanner
import com.obsidianpay.mobile.ui.components.StatusTone
import com.obsidianpay.mobile.ui.components.SurfaceCard
import com.obsidianpay.mobile.ui.components.VSpace
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun CardsScreen(
    modifier: Modifier,
    apiClient: ApiClient,
    store: InsecureSessionStore,
    cache: LocalCacheManager,
) {
    var cards by remember { mutableStateOf<List<CardModel>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var status by remember { mutableStateOf("") }
    var tone by remember { mutableStateOf(StatusTone.NEUTRAL) }
    var lookupId by remember { mutableStateOf("") }
    var detail by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()
    val token = store.getToken()

    LaunchedEffect(Unit) {
        if (token == null) { loading = false; tone = StatusTone.NEGATIVE; status = "Sua sessão expirou. Entre novamente."; return@LaunchedEffect }
        val res = withContext(Dispatchers.IO) { apiClient.getCards(token) }
        loading = false
        when (res) {
            is ApiResult.Success -> { cards = res.data; cache.cacheCardList(res.rawBody) }
            is ApiResult.Error -> { tone = StatusTone.NEGATIVE; status = "Não foi possível carregar seus cartões." }
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
        Text("Seus cartões", style = MaterialTheme.typography.titleMedium)

        if (loading) {
            LoadingState("Carregando seus cartões...")
        } else {
            cards.forEach { c -> VirtualCard(c) }
            if (cards.isEmpty() && status.isBlank()) {
                Text(
                    "Você ainda não tem cartões ativos.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        StatusBanner(text = status, tone = tone)

        // Card details lookup — fetch a specific card's details by its id.
        SectionHeader(title = "Detalhes do cartão")
        SurfaceCard {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = lookupId,
                    onValueChange = { lookupId = it },
                    label = { Text("Número de referência do cartão") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                )
                PrimaryButton(text = "Ver detalhes", onClick = {
                    val id = lookupId.trim()
                    if (token == null || id.isEmpty()) { detail = "Informe a referência do cartão."; return@PrimaryButton }
                    detail = "Carregando..."
                    scope.launch {
                        val res = withContext(Dispatchers.IO) { apiClient.getCard(token, id) }
                        detail = when (res) {
                            is ApiResult.Success -> { cache.cacheCard(id, res.rawBody); res.rawBody }
                            is ApiResult.Error -> "Não foi possível abrir o cartão (${res.httpCode ?: "?"})."
                        }
                    }
                })
                ResponseBox(detail)
            }
        }
    }
}

@Composable
private fun VirtualCard(c: CardModel) {
    Surface(
        modifier = Modifier.fillMaxWidth().height(190.dp),
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.primaryContainer,
        tonalElevation = 4.dp,
    ) {
        Box(
            Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.primaryContainer)
                .padding(20.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().align(Alignment.TopStart),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    "ObsidianPay",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                )
                Icon(
                    Icons.Filled.Contactless,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.85f),
                )
            }
            Column(Modifier.align(Alignment.BottomStart), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    c.maskedNumber ?: "•••• •••• •••• ••••",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Column {
                        Text(
                            "Titular",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f),
                        )
                        Text(
                            c.holder ?: "—",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                        )
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            "Validade",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f),
                        )
                        Text(
                            c.expiry ?: "—",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                        )
                    }
                    Text(
                        c.brand ?: "",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                    )
                }
            }
        }
    }
}
