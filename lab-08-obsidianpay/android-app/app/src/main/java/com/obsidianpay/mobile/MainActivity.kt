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
import com.obsidianpay.mobile.network.NetworkSecurityProfile
import com.obsidianpay.mobile.storage.InsecureSessionStore
import com.obsidianpay.mobile.storage.LocalCacheManager
import com.obsidianpay.mobile.storage.ObsidianLocalDb
import com.obsidianpay.mobile.ui.ApiHostOverrideScreen
import com.obsidianpay.mobile.ui.CardsScreen
import com.obsidianpay.mobile.ui.DeviceTrustScreen
import com.obsidianpay.mobile.ui.HomeScreen
import com.obsidianpay.mobile.ui.IntegrityScreen
import com.obsidianpay.mobile.ui.LocalStateScreen
import com.obsidianpay.mobile.ui.LoginScreen
import com.obsidianpay.mobile.ui.QrInputScreen
import com.obsidianpay.mobile.ui.ReceiptsScreen
import com.obsidianpay.mobile.ui.SecurityCheckScreen
import com.obsidianpay.mobile.ui.SupportScreen
import com.obsidianpay.mobile.ui.TransferPreviewScreen
import com.obsidianpay.mobile.ui.VaultScreen
import com.obsidianpay.mobile.ui.WebViewSupportScreen

/** Top-level destinations. A tiny enum-based nav keeps the app dependency-free. */
enum class Screen { Login, Home, Receipts, Cards, Support, Transfer, Qr, WebSupport, LocalState, DeviceTrust, SecurityCheck, Vault, ApiHost, Integrity }

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
    val store = remember { InsecureSessionStore(context) }
    // Phase 11: restore persisted base URL override on startup so the app
    // continues reaching the correct host (emulator or physical device) after
    // a restart without asking the user to re-enter the URL.
    val apiClient = remember {
        // Single source of truth for the effective host (override ?: emulator default).
        ApiClient(NetworkSecurityProfile.effectiveBaseUrl(store.getApiBaseUrlOverride()))
    }
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
                apiClient = apiClient,
                store = store,
                cache = cache,
                topic = s?.topic,
                message = s?.message,
            ) { screen = Screen.Home }
        }

        Screen.LocalState -> LocalStateScreen(cache) { screen = Screen.Home }

        Screen.DeviceTrust -> DeviceTrustScreen(apiClient, store, cache) { screen = Screen.Home }

        Screen.SecurityCheck -> SecurityCheckScreen(apiClient, store, cache) { screen = Screen.Home }

        Screen.Vault -> VaultScreen(apiClient, store, cache) { screen = Screen.Home }

        // Phase 11 — API host configuration (emulator ↔ physical device).
        Screen.ApiHost -> ApiHostOverrideScreen(apiClient, store, cache) { screen = Screen.Home }

        // Phase 12 — App integrity / NativeGate / TamperCheck.
        Screen.Integrity -> IntegrityScreen(apiClient, store, cache) { screen = Screen.Home }
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

/**
 * Monospace box for showing raw API responses.
 *
 * NOTE (Phase 20): this box must NOT own a vertical scroll. Every caller renders
 * it inside a parent `Column(...).verticalScroll(...)`, and a scrollable child
 * inside a scrollable parent is measured with an infinite max height, which
 * crashes with:
 *   "Vertically scrollable component was measured with an infinity maximum height".
 * The single scroll owner is the screen's outer Column; this box simply lays its
 * text out at natural height and scrolls together with the rest of the screen.
 * Do NOT re-add `Modifier.verticalScroll(...)` here — see scripts/validate-phase20.sh.
 */
@Composable
fun ResponseBox(text: String, modifier: Modifier = Modifier) {
    if (text.isBlank()) return
    Box(modifier = modifier.fillMaxWidth().padding(top = 8.dp)) {
        Text(
            text = text,
            fontFamily = FontFamily.Monospace,
            fontSize = 12.sp,
        )
    }
}
