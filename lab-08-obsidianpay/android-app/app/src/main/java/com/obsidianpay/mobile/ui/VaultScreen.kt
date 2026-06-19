package com.obsidianpay.mobile.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.obsidianpay.mobile.api.ApiClient
import com.obsidianpay.mobile.auth.BiometricGate
import com.obsidianpay.mobile.auth.LocalAuthState
import com.obsidianpay.mobile.storage.InsecureSessionStore
import com.obsidianpay.mobile.storage.LocalCacheManager
import com.obsidianpay.mobile.ui.components.DetailMonoCard
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
import org.json.JSONObject

/**
 * Secure Vault — a local authentication gate protecting sensitive in-app content.
 *
 * NOTE (instructor): the intended bypass design is fully preserved. Auth decisions
 * are client-side only: the biometric "result" is a scaffold value the app
 * controls (hook target below), the fallback PIN is weak and hardcoded in
 * LocalAuthState, and the backend vault endpoint trusts whatever the app reports
 * (see BiometricGate.BYPASS_HINT_*). The UI only changed visually.
 */
@Composable
fun VaultScreen(
    modifier: Modifier,
    apiClient: ApiClient,
    store: InsecureSessionStore,
    cache: LocalCacheManager,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val token = store.getToken()

    var vaultUnlocked by remember { mutableStateOf(LocalAuthState.isVaultUnlocked(store)) }
    var lastReason by remember { mutableStateOf(LocalAuthState.getLastUnlockReason(store)) }
    var pinInput by remember { mutableStateOf("") }
    var status by remember { mutableStateOf("") }
    var tone by remember { mutableStateOf(StatusTone.NEUTRAL) }
    var responseText by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        vaultUnlocked = LocalAuthState.isVaultUnlocked(store)
        lastReason = LocalAuthState.getLastUnlockReason(store)
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
        // --- Status hero --------------------------------------------------------
        SurfaceCard {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                Icon(
                    if (vaultUnlocked) Icons.Filled.LockOpen else Icons.Filled.Lock,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.size(32.dp),
                )
                Column(Modifier.weight(1f)) {
                    Text(
                        if (vaultUnlocked) "Cofre disponível" else "Cofre bloqueado",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                        if (vaultUnlocked) "Acesso liberado via ${lastReason ?: "autenticação"}."
                        else "Autentique-se para acessar seus dados protegidos.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }

        if (!vaultUnlocked) {
            PrimaryButton(text = "Desbloquear com biometria", onClick = {
                // Capability check (scaffold: always available in this lab).
                val available = BiometricGate.canUseBiometric(context)
                cache.addEvent("biometric_capability_checked", "available=$available")
                cache.addEvent("biometric_prompt_started", BiometricGate.buildPromptTitle())

                // Scaffold biometric result controlled by the app. A future Frida hook
                // would intercept here (BYPASS_HINT_BIOMETRIC).
                val scaffoldResult = true // hook target: biometric-result-hook
                val decision = LocalAuthState.buildAuthDecision(
                    method = "biometric",
                    success = scaffoldResult,
                    reason = BiometricGate.buildBypassHintId(),
                )
                cache.addEvent("biometric_auth_result", "success=${decision.success}")
                if (decision.success) {
                    LocalAuthState.markVaultUnlocked(store, "biometric")
                    cache.saveVaultUnlocked(true, "biometric")
                    cache.addEvent("local_auth_success", "method=biometric")
                    vaultUnlocked = true; lastReason = "biometric"
                    tone = StatusTone.POSITIVE; status = "Cofre desbloqueado."
                } else {
                    cache.addEvent("local_auth_failed", "method=biometric")
                    tone = StatusTone.NEGATIVE; status = "Não foi possível confirmar sua biometria."
                }
            })

            SurfaceCard {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Ou use seu PIN de acesso", style = MaterialTheme.typography.bodyMedium)
                    OutlinedTextField(
                        value = pinInput,
                        onValueChange = { pinInput = it },
                        label = { Text("PIN de acesso") },
                        singleLine = true,
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                    )
                    SecondaryButton(text = "Desbloquear com PIN", onClick = {
                        val ok = LocalAuthState.validateFallbackPin(pinInput)
                        if (ok) {
                            LocalAuthState.markVaultUnlocked(store, "fallback-pin")
                            cache.saveVaultUnlocked(true, "fallback-pin")
                            cache.addEvent("weak_pin_fallback_used", "success=true")
                            cache.addEvent("local_auth_success", "method=fallback-pin")
                            vaultUnlocked = true; lastReason = "fallback-pin"
                            tone = StatusTone.POSITIVE; status = "Cofre desbloqueado."
                        } else {
                            cache.addEvent("local_auth_failed", "method=fallback-pin")
                            tone = StatusTone.NEGATIVE; status = "PIN incorreto."
                        }
                        pinInput = ""
                    })
                }
            }
        } else {
            // Unlocked: present protected content.
            SurfaceCard {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Icon(Icons.Filled.Fingerprint, contentDescription = null, tint = MaterialTheme.colorScheme.secondary)
                        Text("Dados protegidos", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    }
                    Text(
                        "Chaves de recuperação e documentos sensíveis da sua conta ficam disponíveis enquanto o cofre está aberto.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            SecondaryButton(text = "Bloquear cofre", onClick = {
                LocalAuthState.markVaultLocked(store)
                cache.saveVaultUnlocked(false, "")
                cache.addEvent("vault_locked_local", null)
                vaultUnlocked = false; lastReason = null
                tone = StatusTone.NEUTRAL; status = "Cofre bloqueado."
            })
        }

        StatusBanner(text = status, tone = tone)

        // --- Advanced: server-side vault session --------------------------------
        SectionHeader(title = "Sessão do cofre")
        SurfaceCard {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                SecondaryButton(text = "Verificar status no servidor", onClick = {
                    if (token == null) { tone = StatusTone.NEGATIVE; status = "Sua sessão expirou."; return@SecondaryButton }
                    status = "Consultando..."
                    scope.launch {
                        val raw = withContext(Dispatchers.IO) { apiClient.getMobileVaultStatus(token) }
                        responseText = raw
                        cache.saveLastVaultStatusJson(raw)
                        tone = StatusTone.NEUTRAL; status = "Status atualizado."
                    }
                })
                SecondaryButton(text = "Solicitar acesso ao cofre", onClick = {
                    if (token == null) { tone = StatusTone.NEGATIVE; status = "Sua sessão expirou."; return@SecondaryButton }
                    status = "Solicitando acesso..."
                    val localAuth = LocalAuthState.isVaultUnlocked(store)
                    val method = LocalAuthState.getLastUnlockReason(store) ?: "unknown"
                    val bypassHintId = BiometricGate.buildBypassHintId()
                    scope.launch {
                        val raw = withContext(Dispatchers.IO) {
                            apiClient.requestMobileVaultUnlock(
                                token = token,
                                localAuth = localAuth,
                                method = method,
                                bypassHintId = bypassHintId,
                            )
                        }
                        responseText = raw
                        cache.saveLastVaultUnlockJson(raw)
                        val granted = runCatching {
                            JSONObject(raw).optString("status") == "vault-access-granted"
                        }.getOrDefault(false)
                        tone = if (granted) StatusTone.POSITIVE else StatusTone.CAUTION
                        status = if (granted) "Acesso ao cofre concedido pelo servidor." else "O servidor não concedeu acesso."
                    }
                })
            }
        }

        DetailMonoCard("Detalhes da sessão", responseText)
    }
}
