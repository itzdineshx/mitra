package com.mitra.voice

import android.content.Context
import android.speech.tts.TextToSpeech
import kotlinx.coroutines.CompletableDeferred
import java.util.Locale

/** Android TextToSpeech wrapper. Speech recognition (SpeechInput) is added in task T07. */
class AndroidTts(
    context: Context,
) : SpeechOutput {
    private val ready = CompletableDeferred<Boolean>()
    private val tts = TextToSpeech(context) { status -> ready.complete(status == TextToSpeech.SUCCESS) }

    override suspend fun speak(
        text: String,
        languageTag: String,
    ) {
        if (!ready.await()) return
        tts.language = Locale.forLanguageTag(languageTag)
        tts.speak(text, TextToSpeech.QUEUE_ADD, null, text.hashCode().toString())
    }

    override fun stop() {
        tts.stop()
    }
}
