package com.obsidianpay.mobile.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.obsidianpay.mobile.Screen
import com.obsidianpay.mobile.storage.LocalCacheManager
import com.obsidianpay.mobile.ui.components.NavRow
import com.obsidianpay.mobile.ui.components.SecondaryButton
import com.obsidianpay.mobile.ui.components.SectionHeader
import com.obsidianpay.mobile.ui.components.StatusBanner
import com.obsidianpay.mobile.ui.components.StatusTone
import com.obsidianpay.mobile.ui.components.SurfaceCard
import com.obsidianpay.mobile.ui.components.VSpace

/**
 * App settings: connection, data & diagnostics, and app information.
 *
 * NOTE (instructor): "Conexão avançada" routes to the existing API-host screen
 * (plaintext override persistence preserved). "Dados do dispositivo" routes to the
 * local-state inspector. "Limpar dados locais" preserves the old clear-local-data
 * behavior and cache event. No settings are migrated to secure/encrypted storage.
 */
@Composable
fun SettingsScreen(
    modifier: Modifier,
    cache: LocalCacheManager,
    onNavigate: (Screen) -> Unit,
) {
    var status by remember { mutableStateOf("") }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp)
            .padding(bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        VSpace(4)
        SectionHeader(title = "Conexão")
        NavRow(
            icon = Icons.Filled.Wifi,
            title = "Conexão avançada",
            subtitle = "Emulador, dispositivo físico ou endpoint personalizado",
            onClick = { onNavigate(Screen.ApiHost) },
        )

        SectionHeader(title = "Dados e privacidade")
        NavRow(
            icon = Icons.Filled.Storage,
            title = "Dados do dispositivo",
            subtitle = "Veja o que o app guarda localmente neste aparelho",
            onClick = { onNavigate(Screen.LocalState) },
        )
        SecondaryButton(text = "Limpar dados locais", onClick = {
            cache.clearLocalArtifacts()
            cache.addEvent("clear_local_data", "settings")
            status = "Dados locais limpos."
        })
        StatusBanner(text = status, tone = StatusTone.POSITIVE)

        SectionHeader(title = "Sobre")
        SurfaceCard {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                androidx.compose.foundation.layout.Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                    androidx.compose.material3.Icon(
                        Icons.Filled.Info,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.secondary,
                    )
                    Text(
                        "  ObsidianPay",
                        style = MaterialTheme.typography.titleMedium,
                    )
                }
                AboutRow("Versão", "1.0.0-rc2")
                AboutRow("Ambiente", "Conta de demonstração")
                Text(
                    "ObsidianPay é uma conta digital. Este build é destinado a avaliação e demonstração.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun AboutRow(label: String, value: String) {
    androidx.compose.foundation.layout.Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.bodyMedium)
    }
}
