package com.mitra.model

import com.google.ai.edge.litertlm.Backend
import com.google.ai.edge.litertlm.Content
import com.google.ai.edge.litertlm.Contents
import com.google.ai.edge.litertlm.Conversation
import com.google.ai.edge.litertlm.ConversationConfig
import com.google.ai.edge.litertlm.Engine
import com.google.ai.edge.litertlm.EngineConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

/**
 * On-device Gemma 4 model runner on LiteRT-LM.
 * Operations run off the main thread. Conversation session reuses system prompt where possible.
 */
class LiteRtLocalModel(
    private val backend: Backend = Backend.CPU(),
) : LocalModel {
    private val mutex = Mutex()
    private var engine: Engine? = null
    private var conversation: Conversation? = null
    private var currentSystemPrompt: String? = null

    override val isLoaded: Boolean
        get() = engine != null && (engine?.isInitialized() == true)

    override suspend fun load(modelPath: String): Unit =
        withContext(Dispatchers.IO) {
            mutex.withLock {
                closeLocked()
                val config =
                    EngineConfig(
                        modelPath = modelPath,
                        backend = backend,
                    )
                val eng = Engine(config)
                eng.initialize()
                engine = eng
            }
        }

    override suspend fun generate(
        systemPrompt: String,
        userPrompt: String,
        maxTokens: Int,
    ): String =
        withContext(Dispatchers.Default) {
            mutex.withLock {
                val eng = engine ?: error("Model not loaded; call load() first")

                val conv =
                    if (conversation != null && currentSystemPrompt == systemPrompt) {
                        conversation!!
                    } else {
                        conversation?.close()
                        val convConfig =
                            ConversationConfig(
                                systemInstruction = Contents.of(systemPrompt),
                            )
                        val newConv = eng.createConversation(convConfig)
                        conversation = newConv
                        currentSystemPrompt = systemPrompt
                        newConv
                    }

                val sb = StringBuilder()
                conv.sendMessageAsync(userPrompt).collect { msg ->
                    msg.contents.contents.filterIsInstance<Content.Text>().forEach { textContent ->
                        sb.append(textContent.text)
                    }
                }
                sb.toString()
            }
        }

    override fun close() {
        closeLocked()
    }

    private fun closeLocked() {
        try {
            conversation?.close()
        } catch (_: Throwable) {
        }
        conversation = null
        currentSystemPrompt = null

        try {
            engine?.close()
        } catch (_: Throwable) {
        }
        engine = null
    }
}
