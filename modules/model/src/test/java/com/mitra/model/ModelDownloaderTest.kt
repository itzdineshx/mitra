package com.mitra.model

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File

@OptIn(ExperimentalCoroutinesApi::class)
class ModelDownloaderTest {
    @Test
    fun `storage check fails when free space is below 3 GB`(
        @TempDir tempDir: File,
    ) = runTest {
        val testDispatcher = StandardTestDispatcher(testScheduler)
        val testScope = TestScope(testDispatcher)

        // Downloader with simulated free space below 3 GB (e.g. 1 GB)
        val downloader =
            ModelDownloader(
                ioDispatcher = testDispatcher,
                scope = testScope,
                freeSpaceProvider = { 1_000_000_000L },
            )

        // Mock context using tempDir
        val context = TestModelContext(tempDir)
        downloader.startDownload(context = context)
        testScheduler.advanceUntilIdle()

        val state = downloader.state.value
        assertTrue(state is ModelDownloadState.Error)
        val errorState = state as ModelDownloadState.Error
        assertTrue(errorState.message.contains("Not enough storage space"))
        assertFalse(errorState.canRetry)
    }

    @Test
    fun `cancelDownload deletes part file and resets state to NotDownloaded`(
        @TempDir tempDir: File,
    ) {
        val context = TestModelContext(tempDir)
        val partFile = ModelFiles.partFile(context)
        partFile.parentFile?.mkdirs()
        partFile.writeText("partial download dummy data")
        assertTrue(partFile.exists())

        val downloader = ModelDownloader()
        downloader.cancelDownload(context)

        assertFalse(partFile.exists())
        assertEquals(ModelDownloadState.NotDownloaded, downloader.state.value)
    }

    @Test
    fun `deleteModel removes target file and resets state`(
        @TempDir tempDir: File,
    ) {
        val context = TestModelContext(tempDir)
        val targetFile = ModelFiles.targetFile(context)
        targetFile.parentFile?.mkdirs()
        targetFile.writeText("installed model dummy")
        assertTrue(targetFile.exists())

        val downloader = ModelDownloader()
        val deleted = downloader.deleteModel(context)

        assertTrue(deleted)
        assertFalse(targetFile.exists())
        assertEquals(ModelDownloadState.NotDownloaded, downloader.state.value)
    }
}

/**
 * Minimal context wrapper for testing ModelDownloader file operations without Robolectric.
 */
private class TestModelContext(
    private val baseDir: File,
) : android.content.ContextWrapper(null) {
    private val internalFiles = File(baseDir, "files")
    private val internalCache = File(baseDir, "cache")

    override fun getFilesDir(): File {
        internalFiles.mkdirs()
        return internalFiles
    }

    override fun getCacheDir(): File {
        internalCache.mkdirs()
        return internalCache
    }

    override fun getExternalFilesDir(type: String?): File? {
        val ext = File(baseDir, "external/$type")
        ext.mkdirs()
        return ext
    }

    override fun getApplicationContext(): android.content.Context = this
}
