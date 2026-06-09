package com.obsidianpay.mobile.deeplink

import android.net.Uri

/**
 * Parses obsidianpay:// deep links and QR text payloads into [DeepLinkData].
 *
 * Supported shapes:
 *   obsidianpay://transfer?toUserId=2001&amount=10&memo=test
 *   obsidianpay://support?topic=mobile&message=hello
 *   obsidianpay://receipt?id=1002
 *
 * NOTE (instructor): parsing here is intentionally permissive. Inputs are not
 * heavily sanitized so the deep-link / QR surface stays explorable in a later
 * phase. The app treats this data as untrusted.
 */
object DeepLinkRouter {

    private const val SCHEME = "obsidianpay"

    /** Parse a raw string payload (e.g. typed/pasted QR content). */
    fun parse(raw: String): DeepLinkData {
        val trimmed = raw.trim()
        val uri = runCatching { Uri.parse(trimmed) }.getOrNull()
            ?: return DeepLinkData(DeepLinkType.UNKNOWN, trimmed)
        return parse(uri)
    }

    /** Parse a [Uri] (e.g. from an Intent's data). */
    fun parse(uri: Uri): DeepLinkData {
        val raw = uri.toString()
        if (!uri.scheme.equals(SCHEME, ignoreCase = true)) {
            return DeepLinkData(DeepLinkType.UNKNOWN, raw)
        }

        // host carries the action; some launchers surface it via authority.
        val action = (uri.host ?: uri.authority ?: "").lowercase()

        fun q(name: String): String? = runCatching { uri.getQueryParameter(name) }.getOrNull()

        return when (action) {
            "transfer" -> DeepLinkData(
                type = DeepLinkType.TRANSFER,
                rawUri = raw,
                toUserId = q("toUserId"),
                amount = q("amount"),
                memo = q("memo"),
            )

            "support" -> DeepLinkData(
                type = DeepLinkType.SUPPORT,
                rawUri = raw,
                topic = q("topic"),
                message = q("message"),
            )

            "receipt" -> DeepLinkData(
                type = DeepLinkType.RECEIPT,
                rawUri = raw,
                receiptId = q("id") ?: q("receiptId"),
            )

            else -> DeepLinkData(DeepLinkType.UNKNOWN, raw)
        }
    }
}
