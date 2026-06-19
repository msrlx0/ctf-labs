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
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.GppGood
import androidx.compose.material.icons.filled.PhonelinkLock
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.obsidianpay.mobile.Screen
import com.obsidianpay.mobile.api.ApiClient
import com.obsidianpay.mobile.auth.LocalAuthState
import com.obsidianpay.mobile.storage.InsecureSessionStore
import com.obsidianpay.mobile.storage.LocalCacheManager
import com.obsidianpay.mobile.ui.components.NavRow
import com.obsidianpay.mobile.ui.components.SectionHeader
import com.obsidianpay.mobile.ui.components.StatusPill
import com.obsidianpay.mobile.ui.components.StatusTone
import com.obsidianpay.mobile.ui.components.SurfaceCard
import com.obsidianpay.mobile.ui.components.VSpace

/**
 * Security Center hub. Presents the account's security features with legitimate,
 * customer-facing labels and routes to the underlying screens.
 *
 * NOTE (instructor): this is purely an information-architecture layer. It does
 * NOT change any detection, vault, attestation or network behavior — it only
 * groups the existing surfaces (device status, secure vault, device verification,
 * app integrity, network protection) under friendly names. No vulnerability,
 * bypass or challenge terminology is shown.
 */
@Composable
fun SecurityCenterScreen(
    modifier: Modifier,
    @Suppress("UNUSED_PARAMETER") apiClient: ApiClient,
    store: InsecureSessionStore,
    @Suppress("UNUSED_PARAMETER") cache: LocalCacheManager,
    onNavigate: (Screen) -> Unit,
) {
    val vaultUnlocked = remember { mutableStateOf(LocalAuthState.isVaultUnlocked(store)) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp)
            .padding(bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        VSpace(4)
        // Overall protection summary.
        SurfaceCard {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                Icon(Icons.Filled.GppGood, contentDescription = null, tint = MaterialTheme.colorScheme.secondary)
                Column(Modifier.weight(1f)) {
                    Text("Proteção da conta", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    Text(
                        "Sua conta está protegida. Revise as recomendações abaixo para manter tudo em dia.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }

        SectionHeader(title = "Recursos de segurança")

        NavRow(
            icon = Icons.Filled.PhonelinkLock,
            title = "Status do dispositivo",
            subtitle = "Revise a integridade do ambiente deste aparelho.",
            onClick = { onNavigate(Screen.SecurityCheck) },
        )
        NavRow(
            icon = Icons.Filled.Fingerprint,
            title = "Cofre seguro",
            subtitle = "Guarde dados sensíveis com autenticação adicional.",
            trailing = {
                StatusPill(
                    text = if (vaultUnlocked.value) "Disponível" else "Bloqueado",
                    tone = if (vaultUnlocked.value) StatusTone.POSITIVE else StatusTone.NEUTRAL,
                )
            },
            onClick = { onNavigate(Screen.Vault) },
        )
        NavRow(
            icon = Icons.Filled.VerifiedUser,
            title = "Verificação do dispositivo",
            subtitle = "Confirme que este é um dispositivo confiável.",
            onClick = { onNavigate(Screen.DeviceTrust) },
        )
        NavRow(
            icon = Icons.Filled.GppGood,
            title = "Integridade do aplicativo",
            subtitle = "Verifique a integridade da instalação do app.",
            onClick = { onNavigate(Screen.Integrity) },
        )
        NavRow(
            icon = Icons.Filled.Wifi,
            title = "Proteção de rede",
            subtitle = "Ajuste a conexão e revise a proteção do tráfego.",
            onClick = { onNavigate(Screen.ApiHost) },
        )
    }
}
