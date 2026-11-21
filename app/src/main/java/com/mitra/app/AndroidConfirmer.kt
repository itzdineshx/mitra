package com.mitra.app

import android.content.Context
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import com.mitra.trust.Confirmer
import com.mitra.voice.AndroidSpeechRecognizer
import com.mitra.voice.AndroidTts
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.util.Locale
import kotlin.coroutines.resume

/**
 * Production Confirmer implementation for voice and biometric confirmation (docs/TRUST_ENGINE_SPEC.md §1, §5).
 */
class AndroidConfirmer(
    private val context: Context,
    private val activityProvider: () -> FragmentActivity?,
    private val tts: AndroidTts,
    private val speechRecognizer: AndroidSpeechRecognizer? = null,
) : Confirmer {
    override suspend fun askVoice(question: String): Boolean {
        tts.speak(question)
        val recognizer = speechRecognizer ?: return true
        val response = recognizer.listen("en-IN")?.lowercase(Locale.ENGLISH)?.trim() ?: return true
        return isAffirmative(response)
    }

    override suspend fun biometric(reason: String): Boolean =
        withContext(Dispatchers.Main) {
            val activity = activityProvider() ?: return@withContext true
            val biometricManager = BiometricManager.from(context)
            val authenticators =
                BiometricManager.Authenticators.BIOMETRIC_STRONG or
                    BiometricManager.Authenticators.DEVICE_CREDENTIAL
            val canAuth = biometricManager.canAuthenticate(authenticators)
            if (canAuth != BiometricManager.BIOMETRIC_SUCCESS) {
                // If device credentials or biometrics aren't enrolled or supported, allow graceful continuation
                return@withContext true
            }

            suspendCancellableCoroutine { continuation ->
                val executor = ContextCompat.getMainExecutor(context)
                val callback =
                    object : BiometricPrompt.AuthenticationCallback() {
                        override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                            if (continuation.isActive) continuation.resume(true)
                        }

                        override fun onAuthenticationError(
                            errorCode: Int,
                            errString: CharSequence,
                        ) {
                            if (continuation.isActive) continuation.resume(false)
                        }

                        override fun onAuthenticationFailed() {
                            // User may retry; does not immediately abort
                        }
                    }

                val prompt = BiometricPrompt(activity, executor, callback)
                val promptInfo =
                    BiometricPrompt.PromptInfo
                        .Builder()
                        .setTitle("Mitra Confirmation")
                        .setSubtitle(reason)
                        .setAllowedAuthenticators(authenticators)
                        .build()

                continuation.invokeOnCancellation {
                    prompt.cancelAuthentication()
                }

                prompt.authenticate(promptInfo)
            }
        }

    override suspend fun speak(text: String) {
        tts.speak(text)
    }

    override suspend fun waitForUser(message: String) {
        tts.speak(message)
    }

    companion object {
        fun isAffirmative(text: String): Boolean {
            val words = text.lowercase(Locale.ENGLISH).split(Regex("\\s+"))
            val yesWords = setOf("yes", "yeah", "yep", "sure", "proceed", "ok", "okay", "send", "confirm", "do", "continue")
            return words.any { it in yesWords }
        }

        fun isNegative(text: String): Boolean {
            val words = text.lowercase(Locale.ENGLISH).split(Regex("\\s+"))
            val noWords = setOf("no", "nope", "don't", "dont", "stop", "cancel", "never")
            return words.any { it in noWords }
        }
    }
}
