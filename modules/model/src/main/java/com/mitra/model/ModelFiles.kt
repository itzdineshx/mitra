package com.mitra.model

import android.content.Context
import java.io.File
import java.io.FileInputStream
import java.security.MessageDigest

object ModelFiles {
    const val GEMMA_E2B_FILENAME = "gemma-4-E2B-it.litertlm"

    // Source: Hugging Face litert-community/gemma-4-E2B-it-litert-lm (requires login and Gemma license terms)
    // TODO(Q-6): Pin exact official SHA-256 after owner accepts model license terms.
    const val GEMMA_E2B_SHA256_TODO = "TODO_Q6_AWAITING_OWNER_CONFIRMATION"

    /**
     * Directory where on-device models reside:
     * Primary: app external files dir (`/sdcard/Android/data/com.mitra.app/files/models`)
     * Fallback: internal files dir (`/data/data/com.mitra.app/files/models`)
     */
    fun modelDirectory(context: Context): File {
        val ext = context.getExternalFilesDir("models")
        return ext ?: File(context.filesDir, "models")
    }

    fun findModelFile(
        context: Context,
        filename: String = GEMMA_E2B_FILENAME,
    ): File? {
        val dir = modelDirectory(context)
        val file = File(dir, filename)
        return if (file.exists() && file.isFile) file else null
    }

    fun computeSha256(file: File): String {
        val digest = MessageDigest.getInstance("SHA-256")
        FileInputStream(file).use { input ->
            val buffer = ByteArray(8192)
            var bytesRead: Int
            while (input.read(buffer).also { bytesRead = it } != -1) {
                digest.update(buffer, 0, bytesRead)
            }
        }
        return digest.digest().joinToString("") { "%02x".format(it) }
    }

    fun verifySha256(
        file: File,
        expectedSha256: String,
    ): Boolean {
        if (!file.exists() || !file.isFile) return false
        val computed = computeSha256(file)
        return computed.equals(expectedSha256.trim(), ignoreCase = true)
    }
}
