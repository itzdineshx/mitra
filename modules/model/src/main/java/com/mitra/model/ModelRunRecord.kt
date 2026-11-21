package com.mitra.model

import kotlinx.serialization.Serializable

/**
 * Diagnostic metrics from a single model execution.
 * Preserved in-memory until Room database (task T60).
 */
@Serializable
data class ModelRunRecord(
    val timestampMillis: Long,
    val timeToFirstTokenMs: Long,
    val totalTimeMs: Long,
    val tokensGenerated: Int,
    val tokensPerSecond: Double,
    val memoryUsedMb: Long,
    val isValidJson: Boolean,
    val actionType: String?,
)
