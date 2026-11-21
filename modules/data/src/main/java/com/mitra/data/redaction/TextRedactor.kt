package com.mitra.data.redaction

import java.security.MessageDigest

object TextRedactor {
    fun sha256(text: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val bytes = digest.digest(text.toByteArray(Charsets.UTF_8))
        return bytes.joinToString("") { "%02x".format(it) }
    }

    /**
     * Redacts or truncates text according to docs/DATA_SPEC.md §4:
     * - Field is secure (password/OTP/PIN/CAPTCHA) -> null or "[REDACTED_SECURE]"
     * - Normal field -> at most first 40 characters + SHA-256 hash.
     */
    fun redactFieldText(
        text: String?,
        isSecure: Boolean,
    ): String? {
        if (text == null) return null
        if (isSecure) return null
        val truncated = text.take(40)
        val hash = sha256(text)
        return if (text.length <= 40) {
            "$truncated [sha256:$hash]"
        } else {
            "$truncated... [sha256:$hash]"
        }
    }
}
