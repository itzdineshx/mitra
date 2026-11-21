package com.mitra.trust

import com.mitra.engine.ScreenElement
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
data class RiskKeywords(
    val send: List<String>,
    val submit: List<String>,
    val payment: List<String>,
    val secret: List<String>,
)

/**
 * Re-derives the risk of an action from the screen (docs/TRUST_ENGINE_SPEC.md §3).
 * The agent's own `risk` claim is ignored.
 */
class RiskDeriver(
    private val keywords: RiskKeywords = Resources.keywords("en"),
    private val paymentPackages: Set<String> = Resources.denyList(),
) {
    fun derive(
        action: ProposedAction,
        ctx: DeviceContext,
    ): Risk {
        if (ctx.foregroundPackage in paymentPackages) return Risk.PAYMENT
        val el = ctx.screen.find(action.target)
        return when (action.type) {
            ActionType.SCROLL, ActionType.SPEAK, ActionType.ASK_USER, ActionType.DONE,
            ActionType.GLOBAL_ACTION, ActionType.SWIPE,
            -> Risk.READ
            ActionType.OPEN_APP -> if (action.target in paymentPackages) Risk.PAYMENT else Risk.READ
            ActionType.TYPE -> if (el != null && isSecret(el)) Risk.SECRET else Risk.WRITE
            ActionType.TAP, ActionType.LONG_PRESS -> el?.let { fromLabel(it) } ?: Risk.WRITE
            ActionType.SEND_INTENT, ActionType.CALL_FUNCTION -> fromText(action.target.orEmpty()).coerceAtLeast(Risk.WRITE)
        }
    }

    private fun isSecret(el: ScreenElement) = el.secure || has(el.label + " " + el.viewId.orEmpty(), keywords.secret)

    private fun fromLabel(el: ScreenElement): Risk = if (isSecret(el)) Risk.SECRET else fromText(el.label + " " + el.viewId.orEmpty())

    private fun fromText(text: String): Risk =
        when {
            has(text, keywords.payment) -> Risk.PAYMENT
            has(text, keywords.submit) -> Risk.SUBMIT
            has(text, keywords.send) -> Risk.SEND
            else -> Risk.WRITE
        }

    private fun has(
        text: String,
        words: List<String>,
    ): Boolean {
        val t = text.lowercase()
        return words.any { Regex("\\b${Regex.escape(it)}\\b").containsMatchIn(t) }
    }
}

internal object Resources {
    private val json = Json { ignoreUnknownKeys = true }

    private fun read(name: String): String =
        requireNotNull(Resources::class.java.classLoader.getResourceAsStream(name)) { "missing resource $name" }
            .bufferedReader()
            .use { it.readText() }

    fun keywords(lang: String): RiskKeywords = json.decodeFromString(RiskKeywords.serializer(), read("risk_keywords_$lang.json"))

    fun denyList(): Set<String> = json.decodeFromString<List<String>>(read("denylist_packages.json")).toSet()
}
