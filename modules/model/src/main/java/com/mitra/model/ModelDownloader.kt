package com.mitra.model

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.util.Log
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL

sealed interface ModelDownloadState {
    object NotDownloaded : ModelDownloadState

    data class Downloading(
        val bytesDownloaded: Long,
        val totalBytes: Long,
        val speedBytesPerSec: Long,
        val isPaused: Boolean = false,
    ) : ModelDownloadState {
        val progressFraction: Float
            get() = if (totalBytes > 0) bytesDownloaded.toFloat() / totalBytes else 0f
        val progressPercent: Int
            get() = (progressFraction * 100).toInt().coerceIn(0, 100)
    }

    data class Verifying(
        val progressFraction: Float = 0f,
    ) : ModelDownloadState

    object Ready : ModelDownloadState

    data class Error(
        val message: String,
        val canRetry: Boolean = true,
    ) : ModelDownloadState
}

class ModelDownloader(
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + ioDispatcher),
    private val freeSpaceProvider: (File) -> Long = { it.usableSpace },
) {
    companion object {
        private const val TAG = "ModelDownloader"
        private const val BUFFER_SIZE = 64 * 1024 // 64 KB
        private const val MAX_REDIRECTS = 6

        val instance: ModelDownloader by lazy { ModelDownloader() }
    }

    private val _state = MutableStateFlow<ModelDownloadState>(ModelDownloadState.NotDownloaded)
    val state: StateFlow<ModelDownloadState> = _state.asStateFlow()

    @Volatile
    private var isPaused = false

    @Volatile
    private var downloadJob: Job? = null

    fun checkStatus(
        context: Context,
        filename: String = ModelFiles.GEMMA_E2B_FILENAME,
    ): ModelDownloadState {
        if (_state.value is ModelDownloadState.Downloading || _state.value is ModelDownloadState.Verifying) {
            return _state.value
        }
        val file = ModelFiles.findModelFile(context, filename)
        val newState =
            if (file != null && file.exists() && file.length() > 0) {
                ModelDownloadState.Ready
            } else {
                val part = ModelFiles.partFile(context, filename)
                if (part.exists() && part.length() > 0) {
                    ModelDownloadState.Downloading(
                        bytesDownloaded = part.length(),
                        totalBytes = ModelFiles.GEMMA_E2B_SIZE_BYTES,
                        speedBytesPerSec = 0L,
                        isPaused = true,
                    )
                } else {
                    ModelDownloadState.NotDownloaded
                }
            }
        _state.value = newState
        return newState
    }

    fun startDownload(
        context: Context,
        modelUrl: String = ModelFiles.GEMMA_E2B_URL,
        filename: String = ModelFiles.GEMMA_E2B_FILENAME,
        expectedSha256: String = ModelFiles.GEMMA_E2B_SHA256,
        expectedSize: Long = ModelFiles.GEMMA_E2B_SIZE_BYTES,
        wifiOnly: Boolean = false,
    ) {
        if (_state.value is ModelDownloadState.Downloading && !(_state.value as ModelDownloadState.Downloading).isPaused) {
            return
        }

        isPaused = false
        downloadJob?.cancel()

        downloadJob =
            scope.launch {
                runDownload(
                    context = context.applicationContext,
                    modelUrl = modelUrl,
                    filename = filename,
                    expectedSha256 = expectedSha256,
                    expectedSize = expectedSize,
                    wifiOnly = wifiOnly,
                )
            }
    }

    fun pauseDownload() {
        isPaused = true
    }

    fun resumeDownload(
        context: Context,
        modelUrl: String = ModelFiles.GEMMA_E2B_URL,
        filename: String = ModelFiles.GEMMA_E2B_FILENAME,
        expectedSha256: String = ModelFiles.GEMMA_E2B_SHA256,
        expectedSize: Long = ModelFiles.GEMMA_E2B_SIZE_BYTES,
        wifiOnly: Boolean = false,
    ) {
        startDownload(context, modelUrl, filename, expectedSha256, expectedSize, wifiOnly)
    }

    fun cancelDownload(
        context: Context,
        filename: String = ModelFiles.GEMMA_E2B_FILENAME,
    ) {
        isPaused = false
        downloadJob?.cancel()
        downloadJob = null
        val part = ModelFiles.partFile(context, filename)
        if (part.exists()) {
            part.delete()
        }
        _state.value = ModelDownloadState.NotDownloaded
    }

    fun deleteModel(
        context: Context,
        filename: String = ModelFiles.GEMMA_E2B_FILENAME,
    ): Boolean {
        cancelDownload(context, filename)
        val deleted = ModelFiles.deleteModel(context, filename)
        _state.value = ModelDownloadState.NotDownloaded
        return deleted
    }

    private suspend fun runDownload(
        context: Context,
        modelUrl: String,
        filename: String,
        expectedSha256: String,
        expectedSize: Long,
        wifiOnly: Boolean,
    ) = withContext(ioDispatcher) {
        try {
            val targetDir = ModelFiles.modelDirectory(context)
            targetDir.mkdirs()

            // 1. Storage check
            val freeSpace = freeSpaceProvider(targetDir)
            if (freeSpace < ModelFiles.REQUIRED_FREE_SPACE_BYTES) {
                _state.value =
                    ModelDownloadState.Error(
                        message = "Not enough storage space. Mitra needs at least 3 GB free.",
                        canRetry = false,
                    )
                return@withContext
            }

            // 2. Wi-Fi check
            if (wifiOnly && !isWifiConnected(context)) {
                _state.value =
                    ModelDownloadState.Error(
                        message = "Waiting for Wi-Fi (Wi-Fi only is enabled).",
                        canRetry = true,
                    )
                return@withContext
            }

            // 3. Prepare part file
            val partFile = ModelFiles.partFile(context, filename)
            partFile.parentFile?.mkdirs()
            val existingBytes = if (partFile.exists()) partFile.length() else 0L

            // 4. Connect with redirect support
            var currentUrl = modelUrl
            var connection: HttpURLConnection? = null
            var redirects = 0
            var responseCode = 0

            while (redirects < MAX_REDIRECTS) {
                val url = URL(currentUrl)
                connection =
                    (url.openConnection() as HttpURLConnection).apply {
                        connectTimeout = 30_000
                        readTimeout = 60_000
                        instanceFollowRedirects = false
                        setRequestProperty("User-Agent", "Mitra-Android/0.1.0")
                        if (existingBytes > 0) {
                            setRequestProperty("Range", "bytes=$existingBytes-")
                        }
                    }

                responseCode = connection.responseCode
                if (responseCode in 301..308) {
                    val location =
                        connection.getHeaderField("Location")
                            ?: throw IllegalStateException("Redirect without Location header")
                    currentUrl = if (location.startsWith("http")) location else URL(url, location).toString()
                    connection.disconnect()
                    redirects++
                } else {
                    break
                }
            }

            val conn = connection ?: throw IllegalStateException("Failed to establish connection")

            val isPartial = (responseCode == HttpURLConnection.HTTP_PARTIAL)
            if (responseCode != HttpURLConnection.HTTP_OK && responseCode != HttpURLConnection.HTTP_PARTIAL) {
                if (responseCode == 416) {
                    partFile.delete()
                    _state.value =
                        ModelDownloadState.Error(
                            message = "Download range reset needed. Tap Resume to restart.",
                            canRetry = true,
                        )
                } else {
                    _state.value =
                        ModelDownloadState.Error(
                            message = "Server returned HTTP $responseCode: ${conn.responseMessage}",
                            canRetry = true,
                        )
                }
                conn.disconnect()
                return@withContext
            }

            val totalBytes =
                if (isPartial) {
                    existingBytes + conn.contentLengthLong
                } else {
                    conn.contentLengthLong.takeIf { it > 0 } ?: expectedSize
                }

            var bytesDownloaded = if (isPartial) existingBytes else 0L
            val append = isPartial && existingBytes > 0

            _state.value =
                ModelDownloadState.Downloading(
                    bytesDownloaded = bytesDownloaded,
                    totalBytes = totalBytes,
                    speedBytesPerSec = 0L,
                    isPaused = false,
                )

            var lastSampleTime = System.currentTimeMillis()
            var bytesInSample = 0L
            var currentSpeed = 0L

            conn.inputStream.use { input ->
                FileOutputStream(partFile, append).use { output ->
                    val buffer = ByteArray(BUFFER_SIZE)
                    var read: Int

                    while (input.read(buffer).also { read = it } != -1) {
                        if (!isActive) {
                            Log.d(TAG, "Download job cancelled")
                            return@withContext
                        }

                        if (isPaused) {
                            Log.d(TAG, "Download paused at $bytesDownloaded bytes")
                            output.flush()
                            _state.value =
                                ModelDownloadState.Downloading(
                                    bytesDownloaded = bytesDownloaded,
                                    totalBytes = totalBytes,
                                    speedBytesPerSec = 0L,
                                    isPaused = true,
                                )
                            return@withContext
                        }

                        output.write(buffer, 0, read)
                        bytesDownloaded += read
                        bytesInSample += read

                        val now = System.currentTimeMillis()
                        val elapsed = now - lastSampleTime
                        if (elapsed >= 500) {
                            currentSpeed = (bytesInSample * 1000) / elapsed
                            lastSampleTime = now
                            bytesInSample = 0L

                            _state.value =
                                ModelDownloadState.Downloading(
                                    bytesDownloaded = bytesDownloaded,
                                    totalBytes = totalBytes,
                                    speedBytesPerSec = currentSpeed,
                                    isPaused = false,
                                )
                        }
                    }
                    output.flush()
                }
            }

            // 5. Verification
            _state.value = ModelDownloadState.Verifying(progressFraction = 0f)
            Log.d(TAG, "Download complete. Verifying SHA-256 for ${partFile.absolutePath}...")

            val isValid = ModelFiles.verifySha256(partFile, expectedSha256)
            if (isValid) {
                val targetFile = ModelFiles.targetFile(context, filename)
                targetFile.parentFile?.mkdirs()
                if (targetFile.exists()) {
                    targetFile.delete()
                }

                val moved = partFile.renameTo(targetFile)
                if (!moved) {
                    partFile.copyTo(targetFile, overwrite = true)
                    partFile.delete()
                }

                Log.d(TAG, "Model installed to ${targetFile.absolutePath} successfully.")
                _state.value = ModelDownloadState.Ready
            } else {
                Log.e(TAG, "SHA-256 checksum mismatch for downloaded model file!")
                partFile.delete()
                _state.value =
                    ModelDownloadState.Error(
                        message = "SHA-256 checksum mismatch. File was corrupted. Please try again.",
                        canRetry = true,
                    )
            }
        } catch (e: Throwable) {
            Log.e(TAG, "Model download error", e)
            _state.value =
                ModelDownloadState.Error(
                    message = e.localizedMessage ?: "Network error during download",
                    canRetry = true,
                )
        }
    }

    private fun isWifiConnected(context: Context): Boolean {
        val cm =
            context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
                ?: return false
        val network = cm.activeNetwork ?: return false
        val capabilities = cm.getNetworkCapabilities(network) ?: return false
        return capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)
    }
}
