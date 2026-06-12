package com.obsidianpay.mobile.integrity

/**
 * NativeGate — Phase 12 scaffold.
 *
 * Attempts to load an optional native library ("obsidian_native_gate") that
 * would normally host a JNI gate function. If the library is absent (no NDK
 * build required), the object falls back gracefully to pure Kotlin values.
 *
 * Teaching anchors (instructor):
 *   - JNI boundary: the native method signature is what Frida/jadx would hook.
 *   - Patch target: patch the JNI return value or replace the Kotlin fallback.
 *   - strings tool: run `strings libobsidian_native_gate.so` to find embedded hints.
 *   - Frida hook: hook the JNI method or the Kotlin fallback path.
 */
object NativeGate {

    // --- State ------------------------------------------------------------------

    private var nativeLibraryLoaded: Boolean = false

    init {
        nativeLibraryLoaded = try {
            System.loadLibrary("obsidian_native_gate")
            true
        } catch (_: UnsatisfiedLinkError) {
            // Library absent — fallback to Kotlin scaffold. Not an error for the lab.
            false
        } catch (_: SecurityException) {
            false
        }
    }

    // --- Public API -------------------------------------------------------------

    fun isNativeLibraryLoaded(): Boolean = nativeLibraryLoaded

    fun getNativeGateStatus(): NativeGateResult {
        return if (nativeLibraryLoaded) {
            // In a real build the JNI method would be called here.
            // Scaffold: treat as if JNI returned the "open" status.
            NativeGateResult(
                loaded = true,
                statusCode = 1,
                statusLabel = "native-gate-open-scaffold",
                bypassHintId = "jni-return-value-hook",
                patchTargetName = getPatchTargetName(),
                fallbackUsed = false,
            )
        } else {
            NativeGateResult(
                loaded = false,
                statusCode = 0,
                statusLabel = "native-library-missing-fallback",
                bypassHintId = "native-gate-kotlin-fallback",
                patchTargetName = getPatchTargetName(),
                fallbackUsed = true,
            )
        }
    }

    /**
     * Didactic hint for the native gate boundary.
     * Does not contain a FLAG or real credentials.
     */
    fun getNativeSecretHint(): String =
        "native-hint: inspect fallback and JNI boundary"

    /**
     * The name of the JNI function a student would target when patching.
     * Matches the expected mangled name for `com.obsidianpay.mobile.integrity.NativeGate.nativeGateCheck`.
     */
    fun getPatchTargetName(): String =
        "Java_com_obsidianpay_mobile_integrity_NativeGate_nativeGateCheck"

    fun buildNativeBypassHints(): List<String> = listOf(
        "jni-return-value-hook",
        "patch-native-gate-result",
        "strings-libobsidian-native",
        "native-gate-kotlin-fallback",
        "native-library-missing-fallback",
    )
}

// --- Result model ---------------------------------------------------------------

data class NativeGateResult(
    val loaded: Boolean,
    val statusCode: Int,
    val statusLabel: String,
    val bypassHintId: String,
    val patchTargetName: String,
    val fallbackUsed: Boolean,
) {
    fun toJson(): String = buildString {
        append("{")
        append("\"loaded\":$loaded,")
        append("\"statusCode\":$statusCode,")
        append("\"statusLabel\":\"$statusLabel\",")
        append("\"bypassHintId\":\"$bypassHintId\",")
        append("\"patchTargetName\":\"$patchTargetName\",")
        append("\"fallbackUsed\":$fallbackUsed")
        append("}")
    }
}
