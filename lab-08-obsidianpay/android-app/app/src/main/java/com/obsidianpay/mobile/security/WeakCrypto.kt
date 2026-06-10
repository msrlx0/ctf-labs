package com.obsidianpay.mobile.security

import android.util.Base64
import java.security.MessageDigest

/**
 * Deliberately weak / didactic "crypto" helpers.
 *
 * NOTE (instructor): everything here is intentionally insecure and exists so the
 * lab can teach why these are NOT real protections:
 *  - Base64 is encoding, not encryption (treated as fake "protection" elsewhere);
 *  - a repeating-key XOR is trivially reversible;
 *  - SHA-1 / MD5 are broken for security use and used here on purpose.
 * Do not copy any of this into real software.
 */
object WeakCrypto {

    fun base64Encode(input: String): String =
        Base64.encodeToString(input.toByteArray(Charsets.UTF_8), Base64.NO_WRAP)

    fun base64Decode(input: String): String =
        try {
            String(Base64.decode(input, Base64.NO_WRAP), Charsets.UTF_8)
        } catch (e: IllegalArgumentException) {
            ""
        }

    /** Repeating-key XOR over raw bytes. Intentionally reversible/weak. */
    fun weakXor(input: String, key: String): String {
        val out = xorBytes(input.toByteArray(Charsets.UTF_8), key.toByteArray(Charsets.UTF_8))
        return String(out, Charsets.ISO_8859_1)
    }

    /** XOR then Base64 — a fake "encrypt + protect" combo. */
    fun weakXorToBase64(input: String, key: String): String {
        val out = xorBytes(input.toByteArray(Charsets.UTF_8), key.toByteArray(Charsets.UTF_8))
        return Base64.encodeToString(out, Base64.NO_WRAP)
    }

    /** Reverse of [weakXorToBase64]. */
    fun weakXorFromBase64(input: String, key: String): String =
        try {
            val raw = Base64.decode(input, Base64.NO_WRAP)
            val out = xorBytes(raw, key.toByteArray(Charsets.UTF_8))
            String(out, Charsets.UTF_8)
        } catch (e: IllegalArgumentException) {
            ""
        }

    /** SHA-1 hex digest. Intentionally weak hash (no salt, no HMAC). */
    fun sha1Hex(input: String): String = hashHex(input, "SHA-1")

    /** MD5 hex digest. Also intentionally weak — provided for completeness. */
    fun md5Hex(input: String): String = hashHex(input, "MD5")

    private fun xorBytes(data: ByteArray, key: ByteArray): ByteArray {
        if (key.isEmpty()) return data.copyOf()
        return ByteArray(data.size) { i ->
            (data[i].toInt() xor key[i % key.size].toInt()).toByte()
        }
    }

    private fun hashHex(input: String, algorithm: String): String {
        val digest = MessageDigest.getInstance(algorithm)
        val bytes = digest.digest(input.toByteArray(Charsets.UTF_8))
        val sb = StringBuilder(bytes.size * 2)
        for (b in bytes) sb.append("%02x".format(b))
        return sb.toString()
    }
}
