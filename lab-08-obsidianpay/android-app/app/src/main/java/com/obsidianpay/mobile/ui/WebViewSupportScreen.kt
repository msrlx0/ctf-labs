package com.obsidianpay.mobile.ui

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.net.Uri
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.obsidianpay.mobile.api.ApiClient
import com.obsidianpay.mobile.network.NetworkSecurityProfile
import com.obsidianpay.mobile.storage.InsecureSessionStore
import com.obsidianpay.mobile.storage.LocalCacheManager
import com.obsidianpay.mobile.util.Constants
import com.obsidianpay.mobile.webview.ObsidianSupportBridge
import com.obsidianpay.mobile.ui.components.SecondaryButton
import com.obsidianpay.mobile.ui.components.StatusBanner
import com.obsidianpay.mobile.ui.components.StatusTone

/**
 * WebView-based support portal. Loads the backend's support page and reflects the
 * topic/message that may arrive from a deep link or QR payload.
 *
 * NOTE (instructor): JavaScript and DOM storage are enabled and the
 * `@JavascriptInterface` bridge (`ObsidianBridge`) is attached so the support
 * page can surface internal context without a backend round-trip. This is the
 * intentional, lab-controlled WebView bridge surface — see [ObsidianSupportBridge].
 *
 * NOTE (Phase 20 — physical-device fix): the portal URL is built from the SAME
 * effective base URL the rest of the app uses ([ApiClient.getBaseUrl], seeded from
 * the persisted API-host override). It is NOT hardcoded to the emulator loopback,
 * so the WebView works on the emulator AND on a physical device pointed at the
 * host's LAN IP (or via `adb reverse`).
 */
@SuppressLint("SetJavaScriptEnabled")
@Composable
fun WebViewSupportScreen(
    modifier: Modifier,
    apiClient: ApiClient,
    store: InsecureSessionStore,
    cache: LocalCacheManager,
    topic: String?,
    message: String?,
) {
    // The effective base URL is the single source shared with ApiClient — never a
    // hardcoded constant. An override saved in the connection settings is respected.
    val baseUrl = apiClient.getBaseUrl()

    fun buildUrl(t: String?, m: String?): String {
        // Normalize the trailing slash and append only the support path + query.
        val base = NetworkSecurityProfile.joinUrl(baseUrl, Constants.WEBVIEW_SUPPORT_PATH)
        val b = Uri.parse(base).buildUpon()
        b.appendQueryParameter("topic", t ?: "mobile")
        if (!m.isNullOrEmpty()) b.appendQueryParameter("message", m)
        return b.build().toString()
    }

    var currentUrl by remember { mutableStateOf(buildUrl(topic, message)) }
    var reloadTick by remember { mutableStateOf(0) }
    var loadError by remember { mutableStateOf<String?>(null) }
    val loadKey = "$currentUrl#$reloadTick"

    Column(modifier = modifier.fillMaxSize().padding(horizontal = 12.dp, vertical = 8.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            SecondaryButton(text = "Recarregar", onClick = { loadError = null; reloadTick++ }, modifier = Modifier.weight(1f))
            SecondaryButton(
                text = "Início da ajuda",
                onClick = { loadError = null; currentUrl = buildUrl("mobile", null); reloadTick++ },
                modifier = Modifier.weight(1f),
            )
        }

        loadError?.let { err ->
            StatusBanner(
                text = "Não foi possível conectar à central de ajuda do ObsidianPay. " +
                    "Revise as configurações de conexão e tente novamente.\n($err)",
                tone = StatusTone.NEGATIVE,
                modifier = Modifier.padding(bottom = 8.dp),
            )
        }

        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { ctx ->
                WebView(ctx).apply {
                    webViewClient = object : WebViewClient() {
                        override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                            loadError = null
                        }

                        // Surface a readable error instead of a blank page.
                        override fun onReceivedError(
                            view: WebView?,
                            request: WebResourceRequest?,
                            error: WebResourceError?,
                        ) {
                            if (request?.isForMainFrame == true) {
                                loadError = "${error?.description ?: "erro de rede"} (${error?.errorCode ?: -1})"
                            }
                        }

                        override fun onReceivedHttpError(
                            view: WebView?,
                            request: WebResourceRequest?,
                            errorResponse: WebResourceResponse?,
                        ) {
                            if (request?.isForMainFrame == true) {
                                loadError = "HTTP ${errorResponse?.statusCode ?: -1} ao carregar o portal"
                            }
                        }
                    }
                    settings.javaScriptEnabled = true
                    settings.domStorageEnabled = true
                    // Attach the lab support bridge so the page can read local
                    // context. Exposed to JS as window.ObsidianBridge.
                    val bridge = ObsidianSupportBridge(store, cache)
                    addJavascriptInterface(bridge, "ObsidianBridge")
                    cache.addEvent("webview_bridge_attached", "ObsidianBridge")
                }
            },
            update = { web ->
                // loadKey (currentUrl + reloadTick) drives recomposition.
                loadKey.let { /* referenced to keep the dependency explicit */ }
                cache.saveLastWebViewUrl(currentUrl)
                web.loadUrl(currentUrl)
            },
        )
    }
}
