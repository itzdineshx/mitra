package com.mitra.web

import java.net.URI

/** Result of checking the browser's current URL against a task's allowed domains (rule R4). */
sealed interface DomainCheck {
    data object Allowed : DomainCheck

    data class Refused(
        val reason: String,
    ) : DomainCheck
}

/**
 * Domain allow-list, HTTPS and look-alike checks for web tasks.
 * Patterns: "irctc.co.in" (exact host or its sub-domains) or "*.gov.in" (any sub-domain of gov.in).
 * The URL must include its scheme; AddressBarReader is responsible for normalising what the browser shows.
 */
class DomainPolicy(
    private val lookAlike: LookAlikeDetector = LookAlikeDetector(),
) {
    fun check(
        url: String?,
        allowedPatterns: Set<String>,
    ): DomainCheck {
        if (url.isNullOrBlank()) return DomainCheck.Refused("no address could be read")
        val uri = runCatching { URI(url.trim()) }.getOrNull() ?: return DomainCheck.Refused("unreadable address")
        val scheme = uri.scheme?.lowercase() ?: return DomainCheck.Refused("address has no scheme")
        if (scheme != "https") return DomainCheck.Refused("not a secure (https) page")
        val host = uri.host?.lowercase()?.trimEnd('.') ?: return DomainCheck.Refused("address has no host")
        if (allowedPatterns.isEmpty()) return DomainCheck.Refused("this task is not allowed to use websites")
        if (lookAlike.isLookAlike(host, allowedPatterns)) return DomainCheck.Refused("looks like a fake copy of an allowed site")
        return if (allowedPatterns.any {
                matches(
                    host,
                    it,
                )
            }
        ) {
            DomainCheck.Allowed
        } else {
            DomainCheck.Refused("$host is not allowed for this task")
        }
    }

    companion object {
        fun matches(
            host: String,
            pattern: String,
        ): Boolean {
            val p = pattern.lowercase().trim()
            val base = p.removePrefix("*.")
            return host == base || host.endsWith(".$base")
        }
    }
}

/** Flags hosts that imitate an allowed site: punycode, non-ASCII, embedded names, small spelling changes. */
class LookAlikeDetector {
    fun isLookAlike(
        host: String,
        allowedPatterns: Set<String>,
    ): Boolean {
        if (host.split('.').any { it.startsWith("xn--") }) return true
        if (host.any { it.code > 127 }) return true
        for (pattern in allowedPatterns) {
            val base = pattern.lowercase().removePrefix("*.")
            if (DomainPolicy.matches(host, pattern)) continue
            // allowed name used inside another domain, e.g. irctc.co.in.example.com
            if (host.contains(base)) return true
            // small spelling change of an exact allowed host, e.g. irctcc.co.in
            if (!pattern.startsWith("*.")) {
                val d = levenshtein(host.removePrefix("www."), base.removePrefix("www."))
                if (d in 1..2) return true
            }
        }
        return false
    }

    private fun levenshtein(
        a: String,
        b: String,
    ): Int {
        val dp = IntArray(b.length + 1) { it }
        for (i in 1..a.length) {
            var prev = dp[0]
            dp[0] = i
            for (j in 1..b.length) {
                val tmp = dp[j]
                dp[j] = minOf(dp[j] + 1, dp[j - 1] + 1, prev + if (a[i - 1] == b[j - 1]) 0 else 1)
                prev = tmp
            }
        }
        return dp[b.length]
    }
}
