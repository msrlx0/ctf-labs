package com.obsidianpay.mobile.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.obsidianpay.mobile.ObsidianScaffold
import com.obsidianpay.mobile.api.ApiClient
import com.obsidianpay.mobile.api.ApiResult
import com.obsidianpay.mobile.storage.InsecureSessionStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun LoginScreen(
    apiClient: ApiClient,
    store: InsecureSessionStore,
    onLoggedIn: () -> Unit,
) {
    // Defaults are pre-filled to make local testing quick.
    var username by remember { mutableStateOf("guest") }
    var password by remember { mutableStateOf("guest123") }
    var status by remember { mutableStateOf("") }
    var loading by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    ObsidianScaffold(title = "ObsidianPay") { modifier ->
        Column(
            modifier = modifier.fillMaxWidth().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text("Sua carteira digital, no seu bolso.")

            OutlinedTextField(
                value = username,
                onValueChange = { username = it },
                label = { Text("Usuário") },
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                label = { Text("Senha") },
                visualTransformation = PasswordVisualTransformation(),
                modifier = Modifier.fillMaxWidth(),
            )

            Button(
                enabled = !loading,
                onClick = {
                    loading = true
                    status = "Entrando..."
                    scope.launch {
                        val res = withContext(Dispatchers.IO) {
                            apiClient.login(username.trim(), password)
                        }
                        when (res) {
                            is ApiResult.Success -> {
                                val login = res.data
                                val profile = withContext(Dispatchers.IO) {
                                    apiClient.getProfile(login.token)
                                }
                                val profileCache =
                                    (profile as? ApiResult.Success)?.data?.toString()
                                store.saveSession(
                                    token = login.token,
                                    username = login.username,
                                    userId = login.userId,
                                    role = login.role,
                                    profileJson = profileCache,
                                )
                                loading = false
                                onLoggedIn()
                            }

                            is ApiResult.Error -> {
                                loading = false
                                status = "Falha no login: ${res.message}"
                            }
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Entrar")
            }

            if (status.isNotBlank()) Text(status)
        }
    }
}
