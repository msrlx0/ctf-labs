package com.obsidianpay.mobile.api

import org.json.JSONArray
import org.json.JSONObject

/**
 * Lightweight data classes for the ObsidianPay mobile contracts, parsed from the
 * backend JSON with org.json. Fields are nullable/optional on purpose: this is a
 * lab client and the server shape may evolve across phases.
 */

private fun JSONObject.optStringOrNull(key: String): String? =
    if (has(key) && !isNull(key)) optString(key) else null

private fun JSONObject.optIntOrNull(key: String): Int? =
    if (has(key) && !isNull(key)) optInt(key) else null

private fun JSONObject.optDoubleOrNull(key: String): Double? =
    if (has(key) && !isNull(key)) optDouble(key) else null

private fun JSONObject.optBoolOrNull(key: String): Boolean? =
    if (has(key) && !isNull(key)) optBoolean(key) else null

data class LoginRequest(
    val username: String,
    val password: String,
)

data class LoginResponse(
    val token: String,
    val tokenType: String?,
    val username: String?,
    val userId: Int?,
    val role: String?,
    val plan: String?,
) {
    companion object {
        fun fromJson(o: JSONObject): LoginResponse {
            val profile = o.optJSONObject("profile")
            return LoginResponse(
                token = o.optString("token"),
                tokenType = o.optStringOrNull("tokenType"),
                username = profile?.optStringOrNull("username"),
                userId = profile?.optIntOrNull("id"),
                role = profile?.optStringOrNull("role"),
                plan = profile?.optStringOrNull("plan"),
            )
        }
    }
}

data class UserProfile(
    val id: Int?,
    val username: String?,
    val displayName: String?,
    val phone: String?,
    val role: String?,
    val plan: String?,
    val walletId: String?,
    val dailyLimit: Double?,
    val kycApproved: Boolean?,
    val supportTier: String?,
    val balanceBRL: Double?,
) {
    companion object {
        fun fromJson(o: JSONObject) = UserProfile(
            id = o.optIntOrNull("id"),
            username = o.optStringOrNull("username"),
            displayName = o.optStringOrNull("displayName"),
            phone = o.optStringOrNull("phone"),
            role = o.optStringOrNull("role"),
            plan = o.optStringOrNull("plan"),
            walletId = o.optStringOrNull("walletId"),
            dailyLimit = o.optDoubleOrNull("dailyLimit"),
            kycApproved = o.optBoolOrNull("kycApproved"),
            supportTier = o.optStringOrNull("supportTier"),
            balanceBRL = o.optDoubleOrNull("balanceBRL"),
        )
    }
}

data class Receipt(
    val receiptId: Int?,
    val type: String?,
    val status: String?,
    val amountBRL: Double?,
    val currency: String?,
    val counterparty: String?,
    val createdAt: String?,
    val reference: String?,
    val ownerRole: String?,
) {
    companion object {
        fun fromJson(o: JSONObject) = Receipt(
            receiptId = o.optIntOrNull("receiptId"),
            type = o.optStringOrNull("type"),
            status = o.optStringOrNull("status"),
            amountBRL = o.optDoubleOrNull("amountBRL"),
            currency = o.optStringOrNull("currency"),
            counterparty = o.optStringOrNull("counterparty"),
            createdAt = o.optStringOrNull("createdAt"),
            reference = o.optStringOrNull("reference"),
            ownerRole = o.optStringOrNull("ownerRole"),
        )
    }
}

data class Card(
    val cardId: String?,
    val ownerRole: String?,
    val brand: String?,
    val maskedNumber: String?,
    val expiry: String?,
    val holder: String?,
    val internalReference: String?,
) {
    companion object {
        fun fromJson(o: JSONObject) = Card(
            cardId = o.optStringOrNull("cardId"),
            ownerRole = o.optStringOrNull("ownerRole"),
            brand = o.optStringOrNull("brand"),
            maskedNumber = o.optStringOrNull("maskedNumber"),
            expiry = o.optStringOrNull("expiry"),
            holder = o.optStringOrNull("holder"),
            internalReference = o.optStringOrNull("internalReference"),
        )
    }
}

data class MobileConfig(
    val apiVersion: String?,
    val baseUrlHint: String?,
    val supportSyncMode: String?,
    val qrTransferScheme: String?,
    val supportDeepLinkScheme: String?,
    val webViewSupportPath: String?,
    val raw: String,
) {
    companion object {
        fun fromJson(o: JSONObject) = MobileConfig(
            apiVersion = o.optStringOrNull("apiVersion"),
            baseUrlHint = o.optStringOrNull("baseUrlHint"),
            supportSyncMode = o.optStringOrNull("supportSyncMode"),
            qrTransferScheme = o.optStringOrNull("qrTransferScheme"),
            supportDeepLinkScheme = o.optStringOrNull("supportDeepLinkScheme"),
            webViewSupportPath = o.optStringOrNull("webViewSupportPath"),
            raw = o.toString(2),
        )
    }
}

data class TransferPreviewRequest(
    val toUserId: Int,
    val amount: String,
    val memo: String,
)

data class TransferPreviewResponse(
    val willExecute: Boolean?,
    val amount: Double?,
    val currency: String?,
    val recipientKnown: Boolean?,
    val recipientDisplayName: String?,
    val memo: String?,
    val raw: String,
) {
    companion object {
        fun fromJson(o: JSONObject): TransferPreviewResponse {
            val p = o.optJSONObject("normalizedPreview")
            return TransferPreviewResponse(
                willExecute = o.optBoolOrNull("willExecute"),
                amount = p?.optDoubleOrNull("amount"),
                currency = p?.optStringOrNull("currency"),
                recipientKnown = p?.optBoolOrNull("recipientKnown"),
                recipientDisplayName = p?.optStringOrNull("recipientDisplayName"),
                memo = p?.optStringOrNull("memo"),
                raw = o.toString(2),
            )
        }
    }
}

data class GenericApiError(
    val error: String?,
    val message: String?,
) {
    companion object {
        fun fromJson(o: JSONObject) = GenericApiError(
            error = o.optStringOrNull("error"),
            message = o.optStringOrNull("message"),
        )
    }
}

/** Parses a JSON array of receipt objects (the `receipts` field of the list). */
fun parseReceiptList(arr: JSONArray?): List<Receipt> {
    if (arr == null) return emptyList()
    return (0 until arr.length()).map { Receipt.fromJson(arr.getJSONObject(it)) }
}

/** Parses a JSON array of card objects (the `cards` field of the list). */
fun parseCardList(arr: JSONArray?): List<Card> {
    if (arr == null) return emptyList()
    return (0 until arr.length()).map { Card.fromJson(arr.getJSONObject(it)) }
}
