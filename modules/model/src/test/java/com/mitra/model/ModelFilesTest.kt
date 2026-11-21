package com.mitra.model

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File
import java.security.MessageDigest

class ModelFilesTest {
    @Test
    fun `computes and verifies SHA-256 correctly`(
        @TempDir tempDir: File,
    ) {
        val sampleFile = File(tempDir, "sample.litertlm")
        val content = "Gemma 4 model test content"
        sampleFile.writeText(content)

        val expectedSha256 =
            MessageDigest
                .getInstance("SHA-256")
                .digest(content.toByteArray())
                .joinToString("") { "%02x".format(it) }

        val computed = ModelFiles.computeSha256(sampleFile)
        assertEquals(expectedSha256, computed)

        assertTrue(ModelFiles.verifySha256(sampleFile, expectedSha256))
        assertFalse(ModelFiles.verifySha256(sampleFile, "0000000000000000000000000000000000000000000000000000000000000000"))
    }

    @Test
    fun `verifySha256 returns false for non-existent file`() {
        assertFalse(ModelFiles.verifySha256(File("non_existent_path.litertlm"), "abc"))
    }
}
