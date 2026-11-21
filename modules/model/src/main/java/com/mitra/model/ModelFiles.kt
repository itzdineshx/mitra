package com.mitra.model

import android.content.Context
import java.io.File
import java.io.FileInputStream
import java.security.MessageDigest

object ModelFiles {
    const val GEMMA_E2B_FILENAME = "gemma-4-E2B-it.litertlm"
    const val GEMMA_E2B_URL =
        "https://huggingface.co/litert-community/gemma-4-E2B-it-litert-lm/resolve/main/gemma-4-E2B-it.litertlm"

    // Pinned official SHA-256 (Git LFS OID from litert-community/gemma-4-E2B-it-litert-lm)
    const val GEMMA_E2B_SHA256 = "181938105e0eefd105961417e8da75903eacda102c4fce9ce90f50b97139a63c"
    const val GEMMA_E2B_SIZE_BYTES = 2588147712L // ~2.58 GB
    const val REQUIRED_FREE_SPACE_BYTES = 3000000000L // ~3 GB (file size + 10%)

    /**
     * Primary directory where models reside:
     * App internal files dir (`filesDir/models`) per DATA_SPEC §2.
     */
    fun modelDirectory(context: Context): File = File(context.filesDir, "models")

    fun partFile(
        context: Context,
        filename: String = GEMMA_E2B_FILENAME,
    ): File {
        val downloadDir = File(context.cacheDir, "downloads")
        return File(downloadDir, "$filename.part")
    }

    fun targetFile(
        context: Context,
        filename: String = GEMMA_E2B_FILENAME,
    ): File {
        val dir = modelDirectory(context)
        return File(dir, filename)
    }

    /**
     * Finds model file in either internal or external filesDir.
     */
    fun findModelFile(
        context: Context,
        filename: String = GEMMA_E2B_FILENAME,
    ): File? {
        val internal = targetFile(context, filename)
        if (internal.exists() && internal.isFile && internal.length() > 0) return internal

        val external = context.getExternalFilesDir("models")
        if (external != null) {
            val extFile = File(external, filename)
            if (extFile.exists() && extFile.isFile && extFile.length() > 0) return extFile
        }
        return null
    }

    fun hasEnoughSpace(
        targetDir: File,
        neededBytes: Long = REQUIRED_FREE_SPACE_BYTES,
    ): Boolean {
        targetDir.mkdirs()
        return targetDir.usableSpace >= neededBytes
    }

    fun deleteModel(
        context: Context,
        filename: String = GEMMA_E2B_FILENAME,
    ): Boolean {
        var deleted = false
        val internal = targetFile(context, filename)
        if (internal.exists()) {
            deleted = internal.delete() || deleted
        }
        val external = context.getExternalFilesDir("models")
        if (external != null) {
            val extFile = File(external, filename)
            if (extFile.exists()) {
                deleted = extFile.delete() || deleted
            }
        }
        val part = partFile(context, filename)
        if (part.exists()) {
            part.delete()
        }
        return deleted
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
