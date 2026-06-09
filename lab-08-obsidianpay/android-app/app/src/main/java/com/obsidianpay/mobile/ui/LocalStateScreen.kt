package com.obsidianpay.mobile.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Divider
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.obsidianpay.mobile.ObsidianScaffold
import com.obsidianpay.mobile.storage.LocalCacheManager

/**
 * Internal support / developer tooling screen: shows the device-local app state
 * (session prefs, cached rows, recent debug events and on-disk artifacts) and
 * offers a "clear" action. Presented as a maintenance utility, not as an
 * exploitation surface.
 */
@Composable
fun LocalStateScreen(
    cache: LocalCacheManager,
    onBack: () -> Unit,
) {
    // A simple counter to force a refresh after clearing.
    var refresh by remember { mutableStateOf(0) }
    val debugValues = remember(refresh) { cache.debugValues() }
    val receipts = remember(refresh) { cache.cachedReceipts() }
    val cards = remember(refresh) { cache.cachedCards() }
    val events = remember(refresh) { cache.debugEvents(30) }
    val artifacts = remember(refresh) { cache.listLocalArtifacts() }

    ObsidianScaffold(title = "Local State", onBack = onBack) { modifier ->
        Column(
            modifier = modifier.fillMaxWidth().padding(16.dp).verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Section("Sessão local (SharedPreferences)")
            debugValues.forEach { (k, v) ->
                Mono("$k = ${preview(v)}")
            }

            Section("Recibos em cache (SQLite)")
            if (receipts.isEmpty()) Mono("(vazio)")
            receipts.forEach { Mono("${it.id} · ${it.ownerRole ?: "-"} · ${it.reference ?: "-"}") }

            Section("Cartões em cache (SQLite)")
            if (cards.isEmpty()) Mono("(vazio)")
            cards.forEach { Mono("${it.id} · ${it.ownerRole ?: "-"} · ${it.maskedNumber ?: "-"}") }

            Section("Eventos de debug")
            if (events.isEmpty()) Mono("(vazio)")
            events.forEach { Mono("#${it.id} ${it.eventType}") }

            Section("Artefatos locais (arquivos)")
            if (artifacts.isEmpty()) Mono("(nenhum)")
            artifacts.forEach { Mono(it) }

            Divider(modifier = Modifier.padding(vertical = 8.dp))
            OutlinedButton(
                onClick = {
                    cache.clearAll()
                    refresh++
                },
                modifier = Modifier.fillMaxWidth(),
            ) { Text("Clear Local Data") }
        }
    }
}

@Composable
private fun Section(title: String) {
    Text(title, modifier = Modifier.padding(top = 10.dp))
}

@Composable
private fun Mono(text: String) {
    Text(text = text, fontFamily = FontFamily.Monospace, fontSize = 12.sp)
}

private fun preview(v: String?): String {
    if (v == null) return "null"
    return if (v.length > 80) v.substring(0, 80) + "…" else v
}
