package com.obsidianpay.mobile.auth

import android.content.Context

/**
 * Scaffold wrapper for biometric / local authentication capability checks.
 *
 * NOTE (instructor): this is a didactic stub — no real BiometricPrompt is
 * wired here. In a future phase, BiometricPrompt / BiometricManager would be
 * integrated. For now, the methods return controlled values that a student can
 * bypass by Frida-hooking the return value or patching the local auth state.
 *
 * Integration note: add `androidx.biometric:biometric:1.2.0-alpha05` (or newer)
 * to app/build.gradle when wiring the real BiometricPrompt flow. The method
 * signatures below are already aligned with that API surface.
 *
 * Bypass entry points (for future Frida/patching study):
 *   biometric-result-hook   — hook canUseBiometric() or the scaffold auth result.
 *   force-auth-decision-true — hook the auth callback to always return success.
 *   patch-local-auth-state  — patch LocalAuthState.markVaultUnlocked() directly.
 */
object BiometricGate {

    // Bypass hint identifiers exposed to VaultScreen and the backend request.
    const val BYPASS_HINT_BIOMETRIC = "biometric-result-hook"
    const val BYPASS_HINT_FORCE_AUTH = "force-auth-decision-true"
    const val BYPASS_HINT_PATCH_STATE = "patch-local-auth-state"

    /**
     * Returns true if the device can use biometric authentication.
     *
     * Scaffold: always reports available in this lab. A real implementation
     * would call BiometricManager.canAuthenticate(BIOMETRIC_STRONG or DEVICE_CREDENTIAL).
     */
    fun canUseBiometric(context: Context): Boolean = true

    fun buildPromptTitle(): String = "ObsidianPay Vault"

    fun buildPromptSubtitle(): String = "Confirm your identity"

    fun buildPromptDescription(): String = "Authenticate to access your Secure Vault"

    fun shouldAllowFallback(): Boolean = true

    fun buildBypassHintId(): String = BYPASS_HINT_BIOMETRIC
}
