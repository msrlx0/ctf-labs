package com.obsidianpay.mobile.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.obsidianpay.mobile.api.ApiClient
import com.obsidianpay.mobile.api.ApiResult
import com.obsidianpay.mobile.storage.InsecureSessionStore
import com.obsidianpay.mobile.storage.LocalCacheManager
import com.obsidianpay.mobile.ui.components.PrimaryButton
import com.obsidianpay.mobile.ui.components.StatusBanner
import com.obsidianpay.mobile.ui.components.StatusTone
import com.obsidianpay.mobile.ui.components.VSpace
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Credible fintech sign-in screen.
 *
 * NOTE (instructor): the public guest credentials (guest / guest123) remain
 * pre-filled to keep local testing quick; no private analyst/operator credentials
 * are shown. Input validation here is cosmetic only and does NOT change the login
 * contract — the same username/password are sent to /api/mobile/login.
 */
@Composable
fun LoginScreen(
    apiClient: ApiClient,
    store: InsecureSessionStore,
    cache: LocalCacheManager,
    onLoggedIn: () -> Unit,
) {
    var username by remember { mutableStateOf("guest") }
    var password by remember { mutableStateOf("guest123") }
    var showPassword by remember { mutableStateOf(false) }
    var status by remember { mutableStateOf("") }
    var tone by remember { mutableStateOf(StatusTone.NEUTRAL) }
    var loading by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    fun submit() {
        if (loading) return
        if (username.isBlank() || password.isBlank()) {
            tone = StatusTone.CAUTION
            status = "Informe usuário e senha para continuar."
            return
        }
        loading = true
        tone = StatusTone.NEUTRAL
        status = "Entrando..."
        scope.launch {
            val res = withContext(Dispatchers.IO) { apiClient.login(username.trim(), password) }
            when (res) {
                is ApiResult.Success -> {
                    val login = res.data
                    val profile = withContext(Dispatchers.IO) { apiClient.getProfile(login.token) }
                    val profileRaw = (profile as? ApiResult.Success)?.rawBody
                    val profileData = (profile as? ApiResult.Success)?.data
                    store.saveLoginSession(
                        token = login.token,
                        username = login.username,
                        userId = login.userId,
                        role = login.role,
                        plan = login.plan ?: profileData?.plan,
                        dailyLimit = profileData?.dailyLimit,
                        kycApproved = profileData?.kycApproved,
                        rawProfileJson = profileRaw,
                    )
                    cache.addEvent("login_success", "user=${login.username}")
                    loading = false
                    onLoggedIn()
                }
                is ApiResult.Error -> {
                    loading = false
                    tone = StatusTone.NEGATIVE
                    status = "Não foi possível entrar. Verifique suas credenciais e a conexão e tente novamente."
                }
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .safeDrawingPadding()
            .imePadding(),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .navigationBarsPadding()
                .padding(horizontal = 24.dp, vertical = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            VSpace(24)
            // Brand mark.
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(MaterialTheme.colorScheme.primary),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    Icons.Filled.Lock,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimary,
                )
            }
            VSpace(20)
            Text(
                "ObsidianPay",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
            )
            VSpace(6)
            Text(
                "Sua conta digital, segura e no seu bolso.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            VSpace(36)

            OutlinedTextField(
                value = username,
                onValueChange = { username = it },
                label = { Text("Usuário") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
            )
            VSpace(14)
            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                label = { Text("Senha") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { submit() }),
                trailingIcon = {
                    IconButton(onClick = { showPassword = !showPassword }) {
                        Icon(
                            if (showPassword) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                            contentDescription = if (showPassword) "Ocultar senha" else "Mostrar senha",
                        )
                    }
                },
            )
            VSpace(24)
            PrimaryButton(text = "Entrar", onClick = { submit() }, loading = loading)

            if (status.isNotBlank()) {
                VSpace(16)
                StatusBanner(text = status, tone = tone)
            }

            VSpace(28)
            Text(
                "Acesso de demonstração: guest / guest123",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
