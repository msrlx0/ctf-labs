package com.obsidianpay.mobile.auth

import com.obsidianpay.mobile.storage.InsecureSessionStore

data class AuthDecision(
    val method: String,
    val success: Boolean,
    val reason: String,
)

/**
 * Local authentication state manager for the Secure Vault flow.
 *
 * NOTE (instructor): all auth decisions here are client-side only. The fallback
 * PIN is hardcoded and weak by design — the teaching point is that client-side
 * auth state can be tampered with via hook or binary patch. The backend vault
 * endpoint trusts whatever the app claims (see BiometricGate.BYPASS_HINT_*).
 *
 * Bypass hints (scaffold for future Frida/patching exercises):
 *   patch-local-auth-state  — overwrite vault_unlocked in SharedPreferences.
 *   force-auth-decision-true — hook validateFallbackPin / isVaultUnlocked to return true.
 */
object LocalAuthState {

    // Weak, hardcoded fallback PIN for lab use only.
    private const val WEAK_FALLBACK_PIN = "0420"

    fun isVaultUnlocked(store: InsecureSessionStore): Boolean =
        store.getVaultUnlocked()

    fun markVaultUnlocked(store: InsecureSessionStore, reason: String) {
        store.saveVaultUnlocked(true, reason)
    }

    fun markVaultLocked(store: InsecureSessionStore) {
        store.saveVaultUnlocked(false, "")
    }

    fun getLastUnlockReason(store: InsecureSessionStore): String? =
        store.getLastVaultUnlockReason()

    fun getWeakFallbackPin(): String = WEAK_FALLBACK_PIN

    fun validateFallbackPin(input: String): Boolean = input == WEAK_FALLBACK_PIN

    fun buildAuthDecision(method: String, success: Boolean, reason: String): AuthDecision =
        AuthDecision(method = method, success = success, reason = reason)
}
