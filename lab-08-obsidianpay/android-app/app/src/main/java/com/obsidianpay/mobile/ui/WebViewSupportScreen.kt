package com.obsidianpay.mobile.ui

import android.annotation.SuppressLint
import android.net.Uri
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.obsidianpay.mobile.ObsidianScaffold
import com.obsidianpay.mobile.storage.InsecureSessionStore
import com.obsidianpay.mobile.storage.LocalCacheManager
import com.obsidianpay.mobile.util.Constants
import com.obsidianpay.mobile.webview.ObsidianSupportBridge

/**
 * WebView-based support portal. Loads the backend's support page and reflects
 * the topic/message that may arrive from a deep link or QR payload.
 *
 * NOTE (instructor): JavaScript and DOM storage are enabled and an
 * `@JavascriptInterface` bridge (`ObsidianBridge`) is attached so the support
 * page can surface internal context (session summary, cached payloads, local
 * artifacts) without a backend round-trip. This is the intentional,
 * lab-controlled WebView bridge surface — see [ObsidianSupportBridge].
 */
@SuppressLint("SetJavaScriptEnabled")
@Composable
fun WebViewSupportScreen(
    store: InsecureSessionStore,
    cache: LocalCacheManager,
    topic: String?,
    message: String?,
    onBack: () -> Unit,
) {
    fun buildUrl(t: String?, m: String?): String {
        val b = Uri.parse(Constants.DEFAULT_BASE_URL + Constants.WEBVIEW_SUPPORT_PATH).buildUpon()
        b.appendQueryParameter("topic", t ?: "mobile")
        if (!m.isNullOrEmpty()) b.appendQueryParameter("message", m)
        return b.build().toString()
    }

    var currentUrl by remember { mutableStateOf(buildUrl(topic, message)) }
    // A bump value forces the WebView's update block to reload on demand.
    var reloadTick by remember { mutableStateOf(0) }
    // Read both states in composable scope so changes recompose -> update() reruns.
    val loadKey = "$currentUrl#$reloadTick"

    ObsidianScaffold(title = "Web Support", onBack = onBack) { modifier ->
        Column(modifier = modifier.fillMaxSize().padding(8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                OutlinedButton(onClick = { reloadTick++ }, modifier = Modifier.weight(1f)) { Text("Reload") }
                OutlinedButton(
                    onClick = { currentUrl = buildUrl("mobile", null); reloadTick++ },
                    modifier = Modifier.weight(1f),
                ) { Text("Open Default Support") }
                OutlinedButton(onClick = onBack, modifier = Modifier.weight(1f)) { Text("Back") }
            }

            Text("URL: $currentUrl")

            AndroidView(
                modifier = Modifier.fillMaxSize(),
                factory = { ctx ->
                    WebView(ctx).apply {
                        webViewClient = WebViewClient()
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
}
