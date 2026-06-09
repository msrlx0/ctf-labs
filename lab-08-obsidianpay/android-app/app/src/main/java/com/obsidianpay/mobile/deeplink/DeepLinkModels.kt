package com.obsidianpay.mobile.deeplink

/** Kind of deep link the app understands. */
enum class DeepLinkType { TRANSFER, SUPPORT, RECEIPT, UNKNOWN }

/**
 * Normalized representation of an incoming deep link / QR payload.
 *
 * Values are kept loosely typed (strings) and only lightly normalized on
 * purpose: this is the untrusted-input surface a later phase will examine.
 */
data class DeepLinkData(
    val type: DeepLinkType,
    val rawUri: String,
    val toUserId: String? = null,
    val amount: String? = null,
    val memo: String? = null,
    val topic: String? = null,
    val message: String? = null,
    val receiptId: String? = null,
)
