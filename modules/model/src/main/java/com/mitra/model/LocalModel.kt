package com.mitra.model

/** One on-device model (Gemma 4 E2B on LiteRT-LM) shared by all six agent roles. */
interface LocalModel {
    val isLoaded: Boolean

    suspend fun load(modelPath: String)

    /** Returns the raw model text. Callers parse it as JSON (see modules/agents). */
    suspend fun generate(
        systemPrompt: String,
        userPrompt: String,
        maxTokens: Int = 512,
    ): String

    fun close()
}

/** Device tiers (docs/ARCHITECTURE.md): A = 6 GB+ RAM, B = 3–5 GB (smaller model), C = under 3 GB (cloud with consent). */
enum class DeviceTier { A, B, C }

/**
 * Placeholder until task T06 wires LiteRT-LM.
 * Lets the app run end to end with scripted answers.
 */
class FakeModel(
    private val reply: (String) -> String = { """{"type":"DONE","reason":"model not wired yet"}""" },
) : LocalModel {
    override var isLoaded = false
        private set

    override suspend fun load(modelPath: String) {
        isLoaded = true
    }

    override suspend fun generate(
        systemPrompt: String,
        userPrompt: String,
        maxTokens: Int,
    ) = reply(userPrompt)

    override fun close() {
        isLoaded = false
    }
}
