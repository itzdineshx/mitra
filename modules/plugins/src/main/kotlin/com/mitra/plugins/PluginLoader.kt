package com.mitra.plugins

import kotlinx.serialization.json.Json
import java.security.KeyFactory
import java.security.PublicKey
import java.security.Signature
import java.security.spec.X509EncodedKeySpec

fun interface SignatureVerifier {
    fun verify(
        data: ByteArray,
        signature: ByteArray,
    ): Boolean
}

/**
 * JDK Ed25519 verifier (JVM 15+, Android API 33+).
 * On Android 10–12 swap in a Tink or BouncyCastle implementation (task T13).
 */
class Ed25519Verifier(
    publicKeyX509: ByteArray,
) : SignatureVerifier {
    private val key: PublicKey = KeyFactory.getInstance("Ed25519").generatePublic(X509EncodedKeySpec(publicKeyX509))

    override fun verify(
        data: ByteArray,
        signature: ByteArray,
    ): Boolean =
        runCatching {
            Signature.getInstance("Ed25519").run {
                initVerify(key)
                update(data)
                verify(signature)
            }
        }.getOrDefault(false)
}

sealed interface LoadResult {
    data class Loaded(
        val plugin: Plugin,
    ) : LoadResult

    data class Rejected(
        val reason: String,
    ) : LoadResult
}

/** Rule R14 at load time: a plugin with a missing or bad signature never loads. */
class PluginLoader(
    private val verifier: SignatureVerifier,
) {
    private val json = Json { ignoreUnknownKeys = false }
    private val loaded = mutableMapOf<String, Plugin>()

    fun load(signed: SignedPlugin): LoadResult {
        if (!verifier.verify(signed.json, signed.signature)) return LoadResult.Rejected("bad signature")
        val plugin =
            runCatching { json.decodeFromString(Plugin.serializer(), signed.json.decodeToString()) }
                .getOrElse { return LoadResult.Rejected("invalid JSON: ${it.message}") }
        validate(plugin)?.let { return LoadResult.Rejected(it) }
        loaded[plugin.id] = plugin
        return LoadResult.Loaded(plugin)
    }

    /** Author rules from docs/PLUGIN_SPEC.md §2. Returns a reason, or null when the plugin is valid. */
    private fun validate(p: Plugin): String? {
        if (p.schema != 1) return "unsupported schema ${p.schema}"
        if (p.app.packageName.isBlank()) return "plugin declares no app package"
        for (a in p.actions) {
            if (a.risk !in RISKS) return "action ${a.id}: unknown risk ${a.risk}"
            if (a.risk in setOf("send", "submit") && !a.confirm) return "action ${a.id}: ${a.risk} actions need \"confirm\": true"
            if (a.risk != "read" && a.verify == null) return "action ${a.id}: state-changing actions need a verify block"
            if (a.steps.any { it.size != 1 }) return "action ${a.id}: each step must have exactly one key"
        }
        return null
    }

    /** Pass to TrustEngine(isPluginTrusted = loader::isTrusted). */
    fun isTrusted(id: String): Boolean = id in loaded

    fun all(): List<Plugin> = loaded.values.toList()

    private companion object {
        val RISKS = setOf("read", "write", "send", "submit")
    }
}
