package com.verifyetb.app.activation

import android.annotation.SuppressLint
import android.content.Context
import android.provider.Settings
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

/**
 * Offline activation: device shows a request (tempo) code derived from ANDROID_ID;
 * you derive a matching lifetime activation code with the same secret.
 *
 * Same secret lives in:
 * - tools/generate_activation.py
 * - web/src/server/activation.ts
 */
object ActivationCrypto {

    /** Keep in sync with web + tools scripts. */
    private const val SECRET =
        "9c3dd13201d0d9e88b23876d5f252614a6384f0932f3d7844691cdbecd0660c7"

    private const val ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"

    fun normalize(code: String): String =
        code.uppercase()
            .replace("-", "")
            .replace(" ", "")
            .filter { it in ALPHABET }

    fun formatDisplay(raw: String): String {
        val n = normalize(raw)
        if (n.isEmpty()) return ""
        return n.chunked(4).joinToString("-")
    }

    /**
     * Stable tempo code for this phone. Same ANDROID_ID → same code
     * (survives reinstall / clear data for the same signing key).
     */
    @SuppressLint("HardwareIds")
    fun requestCodeForDevice(context: Context): String {
        val androidId = Settings.Secure.getString(
            context.contentResolver,
            Settings.Secure.ANDROID_ID,
        ).orEmpty().ifBlank { "unknown-device" }
        return hmacToCode("device:$androidId")
    }

    /** Lifetime activation code for a given tempo/request code. */
    fun activationFor(requestCode: String): String {
        val payload = normalize(requestCode)
        require(payload.length == 8) { "Request code must be 8 characters" }
        return hmacToCode(payload)
    }

    fun isValid(requestCode: String, activationCode: String): Boolean {
        val req = normalize(requestCode)
        val act = normalize(activationCode)
        if (req.length != 8 || act.length != 8) return false
        return normalize(activationFor(req)) == act
    }

    private fun hmacToCode(payload: String): String {
        val mac = Mac.getInstance("HmacSHA256")
        mac.init(SecretKeySpec(SECRET.toByteArray(Charsets.UTF_8), "HmacSHA256"))
        val hash = mac.doFinal(payload.toByteArray(Charsets.UTF_8))
        val raw = buildString(8) {
            repeat(8) { i ->
                val b = hash[i].toInt() and 0xFF
                append(ALPHABET[b % ALPHABET.length])
            }
        }
        return formatDisplay(raw)
    }
}
