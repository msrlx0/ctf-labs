package com.obsidianpay.mobile

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.obsidianpay.mobile.api.ApiClient
import com.obsidianpay.mobile.storage.InsecureSessionStore
import com.obsidianpay.mobile.storage.LocalCacheManager
import com.obsidianpay.mobile.storage.ObsidianLocalDb
import com.obsidianpay.mobile.ui.CardsScreen
import com.obsidianpay.mobile.ui.HomeScreen
import com.obsidianpay.mobile.ui.LocalStateScreen
import com.obsidianpay.mobile.ui.LoginScreen
import com.obsidianpay.mobile.ui.ReceiptsScreen
import com.obsidianpay.mobile.ui.SupportScreen
import com.obsidianpay.mobile.ui.TransferPreviewScreen

/** Top-level destinations. A tiny enum-based nav keeps the app dependency-free. */
enum class Screen { Login, Home, Receipts, Cards, Support, Transfer, LocalState }

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    ObsidianPayApp()
                }
            }
        }
    }
}

@Composable
fun ObsidianPayApp() {
    val context = LocalContext.current
    val apiClient = remember { ApiClient() }
    val store = remember { InsecureSessionStore(context) }
    val db = remember { ObsidianLocalDb(context) }
    val cache = remember { LocalCacheManager(context.applicationContext, store, db) }

    var screen by remember {
        mutableStateOf(if (store.isLoggedIn()) Screen.Home else Screen.Login)
    }

    when (screen) {
        Screen.Login -> LoginScreen(
            apiClient = apiClient,
            store = store,
            cache = cache,
            onLoggedIn = { screen = Screen.Home },
        )

        Screen.Home -> HomeScreen(
            apiClient = apiClient,
            store = store,
            cache = cache,
            onNavigate = { screen = it },
            onLogout = {
                cache.clearAll()
                screen = Screen.Login
            },
        )

        Screen.Receipts -> ReceiptsScreen(apiClient, store, cache) { screen = Screen.Home }
        Screen.Cards -> CardsScreen(apiClient, store, cache) { screen = Screen.Home }
        Screen.Support -> SupportScreen(apiClient, store, cache) { screen = Screen.Home }
        Screen.Transfer -> TransferPreviewScreen(apiClient, store, cache) { screen = Screen.Home }
        Screen.LocalState -> LocalStateScreen(cache) { screen = Screen.Home }
    }
}

/** Shared scaffold with a title and optional back action, used by inner screens. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ObsidianScaffold(
    title: String,
    onBack: (() -> Unit)? = null,
    content: @Composable (Modifier) -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(title) },
                navigationIcon = {
                    if (onBack != null) {
                        TextButton(onClick = onBack) { Text("Voltar") }
                    }
                },
            )
        },
    ) { inner ->
        content(Modifier.padding(inner))
    }
}

/** Scrollable monospace box for showing raw API responses. */
@Composable
fun ResponseBox(text: String, modifier: Modifier = Modifier) {
    if (text.isBlank()) return
    Box(modifier = modifier.fillMaxWidth().padding(top = 8.dp)) {
        Text(
            text = text,
            fontFamily = FontFamily.Monospace,
            fontSize = 12.sp,
            modifier = Modifier.verticalScroll(rememberScrollState()),
        )
    }
}
