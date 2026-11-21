package com.mitra.engine

import kotlinx.serialization.Serializable

enum class EngineKind { ACCESSIBILITY, GUIDE }

@Serializable
enum class GlobalAction { BACK, HOME, RECENTS, NOTIFICATIONS, QUICK_SETTINGS }

@Serializable
enum class Direction { UP, DOWN, LEFT, RIGHT }

@Serializable
data class IntentSpec(
    val action: String,
    val data: String? = null,
    val packageName: String? = null,
    val extras: Map<String, String> = emptyMap(),
)

enum class FailureReason { SECURE_FIELD, TARGET_NOT_FOUND, TIMEOUT, NOT_SUPPORTED, ENGINE_UNAVAILABLE, CANCELLED, UNKNOWN }

sealed interface ActionResult {
    data class Ok(
        val detail: String? = null,
    ) : ActionResult

    data class Failed(
        val reason: FailureReason,
        val message: String = "",
    ) : ActionResult
}

/**
 * The "hands" of Mitra. Implemented by engine-access (main) and engine-guide (fallback).
 *
 * RULE: only TrustedExecutor (modules/trust) may call the mutating methods.
 * Agents may call [readScreen] directly.
 */
interface ExecutionEngine {
    val kind: EngineKind

    suspend fun isAvailable(): Boolean

    suspend fun readScreen(): ScreenSnapshot

    suspend fun launchApp(packageName: String): ActionResult

    suspend fun sendIntent(intent: IntentSpec): ActionResult

    suspend fun tap(elementId: String): ActionResult

    suspend fun longPress(elementId: String): ActionResult

    suspend fun type(
        elementId: String,
        text: String,
    ): ActionResult

    suspend fun scroll(
        direction: Direction,
        elementId: String? = null,
    ): ActionResult

    suspend fun swipe(
        from: Point,
        to: Point,
        durationMs: Long = 300,
    ): ActionResult

    suspend fun globalAction(action: GlobalAction): ActionResult

    /** Cancel whatever the engine is doing. Must return quickly (Stop button, ≤ 300 ms end to end). */
    fun stop()
}
