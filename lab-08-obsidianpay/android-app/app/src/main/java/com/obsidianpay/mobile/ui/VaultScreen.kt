package com.obsidianpay.mobile.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.obsidianpay.mobile.ObsidianScaffold
import com.obsidianpay.mobile.ResponseBox
import com.obsidianpay.mobile.api.ApiClient
import com.obsidianpay.mobile.auth.BiometricGate
import com.obsidianpay.mobile.auth.LocalAuthState
import com.obsidianpay.mobile.storage.InsecureSessionStore
import com.obsidianpay.mobile.storage.LocalCacheManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject

/**
 * Secure Vault screen — local authentication gate for in-app vault access.
 *
 * Displays vault lock/unlock state, exposes a biometric scaffold flow and a
 * fallback PIN input, and connects to the backend vault-mobile endpoints.
 * The auth decisions are client-side only; the server trusts whatever the
 * app reports (intentional teaching seam — see BiometricGate.BYPASS_HINT_*).
 */
@Composable
fun VaultScreen(
    apiClient: ApiClient,
    store: InsecureSessionStore,
    cache: LocalCacheManager,
    onBack: () -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val token = store.getToken()

    var vaultUnlocked by remember { mutableStateOf(LocalAuthState.isVaultUnlocked(store)) }
    var lastReason by remember { mutableStateOf(LocalAuthState.getLastUnlockReason(store)) }
    var pinInput by remember { mutableStateOf("") }
    var status by remember { mutableStateOf("") }
    var responseText by remember { mutableStateOf("") }
    var biometricAvailable by remember { mutableStateOf<Boolean?>(null) }

    // Hydrate state from local store on first composition.
    LaunchedEffect(Unit) {
        vaultUnlocked = LocalAuthState.isVaultUnlocked(store)
        lastReason = LocalAuthState.getLastUnlockReason(store)
    }

    ObsidianScaffold(title = "Secure Vault", onBack = onBack) { modifier ->
        Column(
            modifier = modifier
                .fillMaxWidth()
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            // --- Status card ------------------------------------------------
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("Status: ${if (vaultUnlocked) "unlocked" else "locked"}")
                    Text("Último método: ${lastReason ?: "-"}")
                    if (biometricAvailable != null) {
                        Text("Biometria disponível: ${biometricAvailable}")
                    }
                }
            }

            // --- Biometric capability check ---------------------------------
            OutlinedButton(
                onClick = {
                    val available = BiometricGate.canUseBiometric(context)
                    biometricAvailable = available
                    cache.addEvent("biometric_capability_checked", "available=$available")
                    status = "Biometric disponível: $available"
                },
                modifier = Modifier.fillMaxWidth(),
            ) { Text("Check Biometric") }

            // --- Scaffold biometric unlock -----------------------------------
            Button(
                onClick = {
                    cache.addEvent("biometric_prompt_started", BiometricGate.buildPromptTitle())
                    status = "${BiometricGate.buildPromptTitle()} — ${BiometricGate.buildPromptSubtitle()}"

                    // Scaffold: simulates a biometric result controlled by the app.
                    // A future Frida hook would intercept here (BYPASS_HINT_BIOMETRIC).
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
                        vaultUnlocked = true
                        lastReason = "biometric"
                        status = "Vault desbloqueado via biometria."
                    } else {
                        cache.addEvent("local_auth_failed", "method=biometric")
                        status = "Biometria falhou."
                    }
                },
                modifier = Modifier.fillMaxWidth(),
            ) { Text("Unlock with Biometric") }

            // --- Fallback PIN -----------------------------------------------
            OutlinedTextField(
                value = pinInput,
                onValueChange = { pinInput = it },
                label = { Text("PIN de acesso") },
                visualTransformation = PasswordVisualTransformation(),
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )

            OutlinedButton(
                onClick = {
                    val ok = LocalAuthState.validateFallbackPin(pinInput)
                    if (ok) {
                        LocalAuthState.markVaultUnlocked(store, "fallback-pin")
                        cache.saveVaultUnlocked(true, "fallback-pin")
                        cache.addEvent("weak_pin_fallback_used", "success=true")
                        cache.addEvent("local_auth_success", "method=fallback-pin")
                        vaultUnlocked = true
                        lastReason = "fallback-pin"
                        status = "Vault desbloqueado via PIN."
                    } else {
                        cache.addEvent("local_auth_failed", "method=fallback-pin")
                        status = "PIN incorreto."
                    }
                    pinInput = ""
                },
                modifier = Modifier.fillMaxWidth(),
            ) { Text("Unlock with PIN") }

            // --- Lock vault -------------------------------------------------
            OutlinedButton(
                onClick = {
                    LocalAuthState.markVaultLocked(store)
                    cache.saveVaultUnlocked(false, "")
                    cache.addEvent("vault_locked_local", null)
                    vaultUnlocked = false
                    lastReason = null
                    status = "Vault bloqueado."
                },
                modifier = Modifier.fillMaxWidth(),
            ) { Text("Lock Vault") }

            // --- Backend: Fetch Vault Status ---------------------------------
            OutlinedButton(
                onClick = {
                    if (token == null) { status = "Sessão ausente."; return@OutlinedButton }
                    status = "Consultando status do vault..."
                    scope.launch {
                        val raw = withContext(Dispatchers.IO) {
                            apiClient.getMobileVaultStatus(token)
                        }
                        responseText = raw
                        cache.saveLastVaultStatusJson(raw)
                        status = "Status carregado."
                    }
                },
                modifier = Modifier.fillMaxWidth(),
            ) { Text("Fetch Vault Status") }

            // --- Backend: Request Vault Unlock -------------------------------
            Button(
                onClick = {
                    if (token == null) { status = "Sessão ausente."; return@Button }
                    status = "Solicitando unlock do vault..."
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
                        status = if (granted) "Vault access granted pelo servidor." else "Servidor negou ou erro."
                    }
                },
                modifier = Modifier.fillMaxWidth(),
            ) { Text("Request Vault Unlock") }

            if (status.isNotBlank()) Text(status)
            ResponseBox(responseText)
        }
    }
}
