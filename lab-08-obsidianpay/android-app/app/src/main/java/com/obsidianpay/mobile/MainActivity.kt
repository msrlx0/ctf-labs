package com.obsidianpay.mobile

import android.content.Intent
import android.net.Uri
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
import androidx.compose.runtime.LaunchedEffect
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
import com.obsidianpay.mobile.deeplink.DeepLinkData
import com.obsidianpay.mobile.deeplink.DeepLinkRouter
import com.obsidianpay.mobile.deeplink.DeepLinkType
import com.obsidianpay.mobile.storage.InsecureSessionStore
import com.obsidianpay.mobile.storage.LocalCacheManager
import com.obsidianpay.mobile.storage.ObsidianLocalDb
import com.obsidianpay.mobile.ui.CardsScreen
import com.obsidianpay.mobile.ui.DeviceTrustScreen
import com.obsidianpay.mobile.ui.HomeScreen
import com.obsidianpay.mobile.ui.LocalStateScreen
import com.obsidianpay.mobile.ui.LoginScreen
import com.obsidianpay.mobile.ui.QrInputScreen
import com.obsidianpay.mobile.ui.ReceiptsScreen
import com.obsidianpay.mobile.ui.SupportScreen
import com.obsidianpay.mobile.ui.TransferPreviewScreen
import com.obsidianpay.mobile.ui.WebViewSupportScreen

/** Top-level destinations. A tiny enum-based nav keeps the app dependency-free. */
enum class Screen { Login, Home, Receipts, Cards, Support, Transfer, Qr, WebSupport, LocalState, DeviceTrust }

class MainActivity : ComponentActivity() {

    // Holds the most recent deep link Uri so Compose can react to it.
    private val deepLinkUri = mutableStateOf<Uri?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        deepLinkUri.value = intent?.data
        setContent {
            MaterialTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    ObsidianPayApp(
                        deepLink = deepLinkUri.value,
                        onDeepLinkConsumed = { deepLinkUri.value = null },
                    )
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        deepLinkUri.value = intent.data
    }
}

@Composable
fun ObsidianPayApp(
    deepLink: Uri? = null,
    onDeepLinkConsumed: () -> Unit = {},
) {
    val context = LocalContext.current
    val apiClient = remember { ApiClient() }
    val store = remember { InsecureSessionStore(context) }
    val db = remember { ObsidianLocalDb(context) }
    val cache = remember { LocalCacheManager(context.applicationContext, store, db) }

    var screen by remember {
        mutableStateOf(if (store.isLoggedIn()) Screen.Home else Screen.Login)
    }
    // Prefill carried into Transfer/Receipts/WebSupport from a deep link or QR.
    var prefill by remember { mutableStateOf<DeepLinkData?>(null) }
    var prefillSource by remember { mutableStateOf<String?>(null) }
    // Deep link arriving before login is parked here and applied after login.
    var pending by remember { mutableStateOf<DeepLinkData?>(null) }

    fun applyRoute(data: DeepLinkData, source: String) {
        prefill = data
        prefillSource = source
        screen = when (data.type) {
            DeepLinkType.TRANSFER -> Screen.Transfer
            DeepLinkType.SUPPORT -> Screen.WebSupport
            DeepLinkType.RECEIPT -> Screen.Receipts
            DeepLinkType.UNKNOWN -> Screen.Home
        }
    }

    // React to an incoming deep link.
    LaunchedEffect(deepLink) {
        val uri = deepLink ?: return@LaunchedEffect
        val data = DeepLinkRouter.parse(uri)
        cache.saveLastDeepLink(uri.toString(), data.type.name)
        if (store.isLoggedIn()) applyRoute(data, "deeplink") else pending = data
        onDeepLinkConsumed()
    }

    when (screen) {
        Screen.Login -> LoginScreen(
            apiClient = apiClient,
            store = store,
            cache = cache,
            onLoggedIn = {
                val p = pending
                pending = null
                if (p != null) applyRoute(p, "deeplink") else screen = Screen.Home
            },
        )

        Screen.Home -> HomeScreen(
            apiClient = apiClient,
            store = store,
            cache = cache,
            onNavigate = { prefill = null; prefillSource = null; screen = it },
            onLogout = {
                cache.clearAll()
                screen = Screen.Login
            },
        )

        Screen.Receipts -> {
            val r = prefill?.takeIf { it.type == DeepLinkType.RECEIPT }
            ReceiptsScreen(
                apiClient, store, cache,
                initialReceiptId = r?.receiptId,
                prefillSource = if (r != null) prefillSource else null,
            ) { screen = Screen.Home }
        }

        Screen.Cards -> CardsScreen(apiClient, store, cache) { screen = Screen.Home }

        Screen.Support -> SupportScreen(
            apiClient, store, cache,
            onOpenWebSupport = { topic, message ->
                prefill = DeepLinkData(DeepLinkType.SUPPORT, "support-button", topic = topic, message = message)
                prefillSource = "support"
                screen = Screen.WebSupport
            },
        ) { screen = Screen.Home }

        Screen.Transfer -> {
            val t = prefill?.takeIf { it.type == DeepLinkType.TRANSFER }
            TransferPreviewScreen(
                apiClient, store, cache,
                initialToUserId = t?.toUserId,
                initialAmount = t?.amount,
                initialMemo = t?.memo,
                prefillSource = if (t != null) prefillSource else null,
            ) { screen = Screen.Home }
        }

        Screen.Qr -> QrInputScreen(
            cache = cache,
            onRoute = { applyRoute(it, "qr") },
        ) { screen = Screen.Home }

        Screen.WebSupport -> {
            val s = prefill?.takeIf { it.type == DeepLinkType.SUPPORT }
            WebViewSupportScreen(
                store = store,
                cache = cache,
                topic = s?.topic,
                message = s?.message,
            ) { screen = Screen.Home }
        }

        Screen.LocalState -> LocalStateScreen(cache) { screen = Screen.Home }

        Screen.DeviceTrust -> DeviceTrustScreen(apiClient, store, cache) { screen = Screen.Home }
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
