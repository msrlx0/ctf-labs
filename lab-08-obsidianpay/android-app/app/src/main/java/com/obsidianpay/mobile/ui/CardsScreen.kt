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
import com.obsidianpay.mobile.api.Card as CardModel
import com.obsidianpay.mobile.storage.InsecureSessionStore
import com.obsidianpay.mobile.storage.LocalCacheManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun CardsScreen(
    apiClient: ApiClient,
    store: InsecureSessionStore,
    cache: LocalCacheManager,
    onBack: () -> Unit,
) {
    var cards by remember { mutableStateOf<List<CardModel>>(emptyList()) }
    var status by remember { mutableStateOf("Carregando cartões...") }
    var lookupId by remember { mutableStateOf("") }
    var detail by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()
    val token = store.token

    LaunchedEffect(Unit) {
        if (token == null) {
            status = "Sessão ausente."
            return@LaunchedEffect
        }
        val res = withContext(Dispatchers.IO) { apiClient.getCards(token) }
        when (res) {
            is ApiResult.Success -> {
                cards = res.data
                cache.cacheCardList(res.rawBody)
                status = "${res.data.size} cartão(ões)."
            }
            is ApiResult.Error -> status = "Erro: ${res.message}"
        }
    }

    ObsidianScaffold(title = "Cartões", onBack = onBack) { modifier ->
        Column(
            modifier = modifier.fillMaxWidth().padding(16.dp).verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(status)

            cards.forEach { c ->
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(12.dp)) {
                        Text("${c.cardId ?: "-"} · ${c.brand ?: "-"}")
                        Text(c.maskedNumber ?: "-")
                        Text("Titular: ${c.holder ?: "-"} · Val: ${c.expiry ?: "-"}")
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
                    label = { Text("ID do cartão") },
                    modifier = Modifier.weight(1f),
                )
                Button(onClick = {
                    val id = lookupId.trim()
                    if (token == null || id.isEmpty()) {
                        detail = "Informe um ID."
                        return@Button
                    }
                    detail = "Abrindo cartão $id..."
                    scope.launch {
                        val res = withContext(Dispatchers.IO) { apiClient.getCard(token, id) }
                        when (res) {
                            is ApiResult.Success -> {
                                cache.cacheCard(id, res.rawBody)
                                detail = res.rawBody
                            }
                            is ApiResult.Error -> detail = "Erro (${res.httpCode ?: "?"}): ${res.message}"
                        }
                    }
                }) { Text("Abrir") }
            }

            OutlinedButton(
                onClick = {
                    val cached = cache.cachedCards()
                    detail = if (cached.isEmpty()) {
                        "Nenhum cartão em cache local."
                    } else {
                        cached.joinToString("\n") {
                            "${it.id} · ${it.ownerRole ?: "-"} · ${it.maskedNumber ?: "-"}"
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth(),
            ) { Text("Cached Cards") }

            ResponseBox(detail)
        }
    }
}
