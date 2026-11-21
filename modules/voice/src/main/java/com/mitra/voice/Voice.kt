package com.mitra.voice

/** Speech in and out. English first ("en-IN"); Hindi and Tamil come in v1.0, so always pass the language tag. */
interface SpeechInput {
    suspend fun listen(languageTag: String = "en-IN"): String?
}

interface SpeechOutput {
    suspend fun speak(
        text: String,
        languageTag: String = "en-IN",
    )

    fun stop()
}

/** Spoken yes/no for CONFIRM. Anything unclear counts as "no". */
object YesNo {
    private val yes = setOf("yes", "yeah", "yep", "ok", "okay", "sure", "go ahead", "do it", "confirm")

    fun isYes(heard: String?): Boolean {
        val t = heard?.lowercase()?.trim()?.trimEnd('.', '!') ?: return false
        return t in yes
    }
}
