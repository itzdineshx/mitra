package com.mitra.voice

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.util.Log
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Android [SpeechRecognizer] wrapper for en-IN speech input (T07).
 */
class AndroidSpeechRecognizer(
    private val context: Context,
    private val onPartialResult: ((String) -> Unit)? = null,
    private val onEndOfSpeech: ((Long) -> Unit)? = null,
) : SpeechInput {
    override suspend fun listen(languageTag: String): String? =
        withContext(Dispatchers.Main) {
            if (!SpeechRecognizer.isRecognitionAvailable(context)) {
                Log.w("MitraVoice", "Speech recognition not available on device")
                return@withContext null
            }

            val deferred = CompletableDeferred<String?>()
            val recognizer = SpeechRecognizer.createSpeechRecognizer(context)

            val intent =
                Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE, languageTag)
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, languageTag)
                    putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
                    putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3)
                }

            recognizer.setRecognitionListener(
                object : RecognitionListener {
                    override fun onReadyForSpeech(params: Bundle?) {
                        Log.i("MitraVoice", "RECOGNIZER_READY")
                    }

                    override fun onBeginningOfSpeech() {
                        Log.i("MitraVoice", "SPEECH_BEGIN")
                    }

                    override fun onRmsChanged(rmsdB: Float) = Unit

                    override fun onBufferReceived(buffer: ByteArray?) = Unit

                    override fun onEndOfSpeech() {
                        val endNs = System.nanoTime()
                        Log.i("MitraVoice", "SPEECH_END: timestampNs=$endNs")
                        onEndOfSpeech?.invoke(endNs)
                    }

                    override fun onError(error: Int) {
                        Log.w("MitraVoice", "RECOGNITION_ERROR: code=$error")
                        if (!deferred.isCompleted) deferred.complete(null)
                        recognizer.destroy()
                    }

                    override fun onResults(results: Bundle?) {
                        val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                        val text = matches?.firstOrNull()?.trim()
                        Log.i("MitraVoice", "SPEECH_RESULTS: text=\"$text\"")
                        if (!deferred.isCompleted) deferred.complete(text)
                        recognizer.destroy()
                    }

                    override fun onPartialResults(partialResults: Bundle?) {
                        val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                        val partial = matches?.firstOrNull()?.trim().orEmpty()
                        if (partial.isNotBlank()) {
                            Log.d("MitraVoice", "SPEECH_PARTIAL: \"$partial\"")
                            onPartialResult?.invoke(partial)
                        }
                    }

                    override fun onEvent(
                        eventType: Int,
                        params: Bundle?,
                    ) = Unit
                },
            )

            recognizer.startListening(intent)
            deferred.await()
        }
}
