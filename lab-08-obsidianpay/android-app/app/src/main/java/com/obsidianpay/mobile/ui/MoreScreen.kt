package com.obsidianpay.mobile.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.SupportAgent
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.obsidianpay.mobile.Screen
import com.obsidianpay.mobile.storage.InsecureSessionStore
import com.obsidianpay.mobile.ui.components.NavRow
import com.obsidianpay.mobile.ui.components.SecondaryButton
import com.obsidianpay.mobile.ui.components.SectionHeader
import com.obsidianpay.mobile.ui.components.SurfaceCard
import com.obsidianpay.mobile.ui.components.VSpace

/**
 * "Conta" — profile + account hub. Groups secondary destinations and the sign-out
 * action. Every entry opens a real, meaningful screen.
 */
@Composable
fun MoreScreen(
    modifier: Modifier,
    store: InsecureSessionStore,
    onNavigate: (Screen) -> Unit,
    onLogout: () -> Unit,
) {
    val username = store.getUsername() ?: "cliente"
    val role = store.getRole() ?: "Conta pessoal"

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp)
            .padding(bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        VSpace(4)
        // Profile header.
        SurfaceCard {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                Box(
                    Modifier.size(52.dp).background(MaterialTheme.colorScheme.primary, CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        username.take(1).uppercase(),
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onPrimary,
                        fontWeight = FontWeight.Bold,
                    )
                }
                Column {
                    Text(username, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
                    Text(role, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }

        SectionHeader(title = "Minha conta")
        NavRow(Icons.Filled.ReceiptLong, "Atividade", { onNavigate(Screen.Receipts) }, subtitle = "Seus recibos e movimentações")
        NavRow(Icons.Filled.Shield, "Segurança", { onNavigate(Screen.Security) }, subtitle = "Proteção da conta e do dispositivo")
        NavRow(Icons.Filled.SupportAgent, "Central de ajuda", { onNavigate(Screen.Support) }, subtitle = "Suporte e artigos de ajuda")
        NavRow(Icons.Filled.Settings, "Configurações", { onNavigate(Screen.Settings) }, subtitle = "Preferências, conexão e privacidade")

        VSpace(8)
        SecondaryButton(text = "Sair da conta", onClick = onLogout)

        VSpace(4)
        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(14.dp))
            Text(
                "  ObsidianPay · versão 1.0.0-rc2",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
