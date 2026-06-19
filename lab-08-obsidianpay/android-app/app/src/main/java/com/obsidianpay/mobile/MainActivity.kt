package com.obsidianpay.mobile

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.obsidianpay.mobile.deeplink.DeepLinkData
import com.obsidianpay.mobile.deeplink.DeepLinkRouter
import com.obsidianpay.mobile.deeplink.DeepLinkType
import com.obsidianpay.mobile.api.ApiClient
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
import com.obsidianpay.mobile.ui.MoreScreen
import com.obsidianpay.mobile.ui.QrInputScreen
import com.obsidianpay.mobile.ui.ReceiptsScreen
import com.obsidianpay.mobile.ui.SecurityCenterScreen
import com.obsidianpay.mobile.ui.SecurityCheckScreen
import com.obsidianpay.mobile.ui.SettingsScreen
import com.obsidianpay.mobile.ui.SupportScreen
import com.obsidianpay.mobile.ui.TransferPreviewScreen
import com.obsidianpay.mobile.ui.VaultScreen
import com.obsidianpay.mobile.ui.WebViewSupportScreen
import com.obsidianpay.mobile.ui.theme.ObsidianPayTheme

/**
 * Top-level destinations. A small enum-based navigation keeps the app
 * dependency-free while still supporting a bottom-navigation shell, a back
 * stack, deep links and the exported-component entry points.
 *
 * NOTE (instructor): the destination set and the deep-link → destination mapping
 * are unchanged in intent from earlier phases — Transfer/Support/Receipt deep
 * links and QR payloads still reach the same flows. The Phase 23 redesign only
 * reorganises how destinations are presented (bottom tabs + pushed details).
 */
enum class Screen {
    Login,
    // Bottom-navigation tabs.
    Home, Transfer, Cards, Security, More,
    // Pushed detail / secondary destinations.
    Receipts, Support, Qr, WebSupport, LocalState, DeviceTrust,
    SecurityCheck, Vault, ApiHost, Integrity, Settings,
}

private val TAB_SCREENS = listOf(Screen.Home, Screen.Transfer, Screen.Cards, Screen.Security, Screen.More)

class MainActivity : ComponentActivity() {

    // Holds the most recent deep link Uri so Compose can react to it.
    private val deepLinkUri = mutableStateOf<Uri?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        deepLinkUri.value = intent?.data
        setContent {
            ObsidianPayTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background,
                ) {
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
    // Restore the persisted base URL override on startup so the app keeps reaching
    // the correct host (emulator or physical device) after a restart without
    // asking the user to re-enter the URL.
    val apiClient = remember {
        // Single source of truth for the effective host (override ?: default).
        ApiClient(NetworkSecurityProfile.effectiveBaseUrl(store.getApiBaseUrlOverride()))
    }
    val db = remember { ObsidianLocalDb(context) }
    val cache = remember { LocalCacheManager(context.applicationContext, store, db) }

    var screen by remember {
        mutableStateOf(if (store.isLoggedIn()) Screen.Home else Screen.Login)
    }
    val backStack = remember { mutableStateListOf<Screen>() }

    // Prefill carried into Transfer/Receipts/WebSupport from a deep link or QR.
    var prefill by remember { mutableStateOf<DeepLinkData?>(null) }
    var prefillSource by remember { mutableStateOf<String?>(null) }
    // Deep link arriving before login is parked here and applied after login.
    var pending by remember { mutableStateOf<DeepLinkData?>(null) }

    fun goTab(target: Screen) {
        backStack.clear()
        prefill = null
        prefillSource = null
        screen = target
    }

    fun push(target: Screen) {
        if (screen != target) backStack.add(screen)
        screen = target
    }

    fun back() {
        screen = if (backStack.isNotEmpty()) backStack.removeAt(backStack.lastIndex) else Screen.Home
    }

    fun navigate(target: Screen) {
        if (target in TAB_SCREENS) goTab(target) else push(target)
    }

    fun applyRoute(data: DeepLinkData, source: String) {
        prefill = data
        prefillSource = source
        val target = when (data.type) {
            DeepLinkType.TRANSFER -> Screen.Transfer
            DeepLinkType.SUPPORT -> Screen.WebSupport
            DeepLinkType.RECEIPT -> Screen.Receipts
            DeepLinkType.UNKNOWN -> Screen.Home
        }
        if (target in TAB_SCREENS) { backStack.clear(); screen = target } else push(target)
    }

    // React to an incoming deep link.
    LaunchedEffect(deepLink) {
        val uri = deepLink ?: return@LaunchedEffect
        val data = DeepLinkRouter.parse(uri)
        cache.saveLastDeepLink(uri.toString(), data.type.name)
        if (store.isLoggedIn()) applyRoute(data, "deeplink") else pending = data
        onDeepLinkConsumed()
    }

    // Hardware back: pop the stack, fall back to Home, else let the system finish.
    BackHandler(enabled = screen != Screen.Login && (backStack.isNotEmpty() || screen != Screen.Home)) {
        if (backStack.isNotEmpty()) back() else screen = Screen.Home
    }

    if (screen == Screen.Login) {
        LoginScreen(
            apiClient = apiClient,
            store = store,
            cache = cache,
            onLoggedIn = {
                val p = pending
                pending = null
                backStack.clear()
                if (p != null) applyRoute(p, "deeplink") else screen = Screen.Home
            },
        )
        return
    }

    val isTab = screen in TAB_SCREENS
    ObsidianScaffold(
        title = titleFor(screen),
        onBack = if (isTab) null else { { back() } },
        bottomBar = {
            if (isTab) ObsidianBottomBar(current = screen, onSelect = { navigate(it) })
        },
    ) { m ->
        when (screen) {
            Screen.Home -> HomeScreen(
                modifier = m,
                apiClient = apiClient,
                store = store,
                cache = cache,
                onNavigate = { navigate(it) },
            )

            Screen.Transfer -> {
                val t = prefill?.takeIf { it.type == DeepLinkType.TRANSFER }
                TransferPreviewScreen(
                    modifier = m,
                    apiClient = apiClient,
                    store = store,
                    cache = cache,
                    initialToUserId = t?.toUserId,
                    initialAmount = t?.amount,
                    initialMemo = t?.memo,
                    prefillSource = if (t != null) prefillSource else null,
                )
            }

            Screen.Cards -> CardsScreen(m, apiClient, store, cache)

            Screen.Security -> SecurityCenterScreen(m, apiClient, store, cache, onNavigate = { navigate(it) })

            Screen.More -> MoreScreen(
                modifier = m,
                store = store,
                onNavigate = { navigate(it) },
                onLogout = {
                    cache.clearAll()
                    backStack.clear()
                    screen = Screen.Login
                },
            )

            Screen.Receipts -> {
                val r = prefill?.takeIf { it.type == DeepLinkType.RECEIPT }
                ReceiptsScreen(
                    m, apiClient, store, cache,
                    initialReceiptId = r?.receiptId,
                    prefillSource = if (r != null) prefillSource else null,
                )
            }

            Screen.Support -> SupportScreen(
                m, apiClient, store, cache,
                onOpenWebSupport = { topic, message ->
                    prefill = DeepLinkData(DeepLinkType.SUPPORT, "support-button", topic = topic, message = message)
                    prefillSource = "support"
                    push(Screen.WebSupport)
                },
            )

            Screen.Qr -> QrInputScreen(
                modifier = m,
                cache = cache,
                onRoute = { applyRoute(it, "qr") },
            )

            Screen.WebSupport -> {
                val s = prefill?.takeIf { it.type == DeepLinkType.SUPPORT }
                WebViewSupportScreen(
                    modifier = m,
                    apiClient = apiClient,
                    store = store,
                    cache = cache,
                    topic = s?.topic,
                    message = s?.message,
                )
            }

            Screen.LocalState -> LocalStateScreen(m, cache)
            Screen.DeviceTrust -> DeviceTrustScreen(m, apiClient, store, cache)
            Screen.SecurityCheck -> SecurityCheckScreen(m, apiClient, store, cache)
            Screen.Vault -> VaultScreen(m, apiClient, store, cache)
            Screen.ApiHost -> ApiHostOverrideScreen(m, apiClient, store, cache)
            Screen.Integrity -> IntegrityScreen(m, apiClient, store, cache)
            Screen.Settings -> SettingsScreen(m, cache, onNavigate = { navigate(it) })
            Screen.Login -> Unit
        }
    }
}

/** Customer-facing title for each destination (no challenge/technical terms). */
private fun titleFor(screen: Screen): String = when (screen) {
    Screen.Home -> "ObsidianPay"
    Screen.Transfer -> "Transferir"
    Screen.Cards -> "Cartões"
    Screen.Security -> "Segurança"
    Screen.More -> "Conta"
    Screen.Receipts -> "Atividade"
    Screen.Support -> "Central de ajuda"
    Screen.Qr -> "Pagar com QR"
    Screen.WebSupport -> "Central de ajuda"
    Screen.LocalState -> "Dados do dispositivo"
    Screen.DeviceTrust -> "Verificação do dispositivo"
    Screen.SecurityCheck -> "Status do dispositivo"
    Screen.Vault -> "Cofre seguro"
    Screen.ApiHost -> "Conexão avançada"
    Screen.Integrity -> "Integridade do app"
    Screen.Settings -> "Configurações"
    Screen.Login -> "ObsidianPay"
}

private data class TabSpec(val screen: Screen, val label: String, val icon: ImageVector)

@Composable
private fun ObsidianBottomBar(current: Screen, onSelect: (Screen) -> Unit) {
    val tabs = listOf(
        TabSpec(Screen.Home, "Início", Icons.Filled.Home),
        TabSpec(Screen.Transfer, "Transferir", Icons.Filled.SwapHoriz),
        TabSpec(Screen.Cards, "Cartões", Icons.Filled.CreditCard),
        TabSpec(Screen.Security, "Segurança", Icons.Filled.Shield),
        TabSpec(Screen.More, "Conta", Icons.Filled.GridView),
    )
    NavigationBar(
        containerColor = MaterialTheme.colorScheme.surface,
        tonalElevation = 0.dp,
    ) {
        tabs.forEach { tab ->
            NavigationBarItem(
                selected = current == tab.screen,
                onClick = { onSelect(tab.screen) },
                icon = { Icon(tab.icon, contentDescription = tab.label) },
                label = { Text(tab.label, fontSize = 11.sp) },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = MaterialTheme.colorScheme.onPrimary,
                    indicatorColor = MaterialTheme.colorScheme.primary,
                    selectedTextColor = MaterialTheme.colorScheme.secondary,
                    unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                ),
            )
        }
    }
}

/**
 * Shared scaffold: a themed top app bar (with optional back), an optional bottom
 * navigation bar, and a content slot that receives a Modifier already padded for
 * the system insets and the bars.
 *
 * NOTE (Phase 20/23): this file must NOT own a vertical scroll. Each screen is the
 * single scroll owner via its own `Column(...).verticalScroll(...)`. A scrollable
 * child inside a scrollable parent is measured with an infinite max height and
 * crashes ("Vertically scrollable component was measured with an infinity maximum
 * height"). Do NOT add `verticalScroll(...)` (or its import) here — see
 * scripts/validate-phase20.sh and scripts/validate-phase23.sh.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ObsidianScaffold(
    title: String,
    onBack: (() -> Unit)? = null,
    actions: @Composable RowScope.() -> Unit = {},
    bottomBar: @Composable () -> Unit = {},
    content: @Composable (Modifier) -> Unit,
) {
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text(title, style = MaterialTheme.typography.titleLarge) },
                navigationIcon = {
                    if (onBack != null) {
                        IconButton(onClick = onBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Voltar")
                        }
                    }
                },
                actions = actions,
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.onBackground,
                    navigationIconContentColor = MaterialTheme.colorScheme.onBackground,
                    actionIconContentColor = MaterialTheme.colorScheme.secondary,
                ),
            )
        },
        bottomBar = bottomBar,
    ) { inner ->
        content(Modifier.padding(inner))
    }
}

/**
 * Discreet monospace block for raw service payloads on advanced/diagnostic
 * surfaces. It deliberately does NOT own a vertical scroll: every caller renders
 * it inside a parent `Column(...).verticalScroll(...)`, and a scrollable child
 * inside a scrollable parent crashes with an infinite-height measure. Do NOT
 * re-add `Modifier.verticalScroll(...)` here — see scripts/validate-phase20.sh.
 */
@Composable
fun ResponseBox(text: String, modifier: Modifier = Modifier) {
    if (text.isBlank()) return
    Surface(
        modifier = modifier.fillMaxWidth().padding(top = 8.dp),
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surface,
    ) {
        Text(
            text = text,
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            fontFamily = FontFamily.Monospace,
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
