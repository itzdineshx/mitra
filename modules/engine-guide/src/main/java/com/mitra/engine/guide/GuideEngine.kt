package com.mitra.engine.guide

import com.mitra.engine.ActionResult
import com.mitra.engine.Direction
import com.mitra.engine.EngineKind
import com.mitra.engine.ExecutionEngine
import com.mitra.engine.FailureReason
import com.mitra.engine.GlobalAction
import com.mitra.engine.IntentSpec
import com.mitra.engine.Point
import com.mitra.engine.ScreenSnapshot

/**
 * Fallback when the Accessibility Service is off: Mitra speaks each step and the user taps.
 * Only intents and app launches are performed directly. Implemented in task T24.
 */
class GuideEngine(
    private val say: suspend (String) -> Unit,
) : ExecutionEngine {
    override val kind = EngineKind.GUIDE

    override suspend fun isAvailable() = true

    override suspend fun readScreen() = ScreenSnapshot(foregroundPackage = "")

    override suspend fun launchApp(packageName: String) = guide("Please open $packageName.")

    override suspend fun sendIntent(intent: IntentSpec) = ActionResult.Failed(FailureReason.NOT_SUPPORTED, "TODO T24")

    override suspend fun tap(elementId: String) = guide("Please tap the highlighted button.")

    override suspend fun longPress(elementId: String) = guide("Please press and hold the highlighted item.")

    override suspend fun type(
        elementId: String,
        text: String,
    ) = guide("Please type: $text")

    override suspend fun scroll(
        direction: Direction,
        elementId: String?,
    ) = guide("Please scroll ${direction.name.lowercase()}.")

    override suspend fun swipe(
        from: Point,
        to: Point,
        durationMs: Long,
    ) = guide("Please swipe on the screen.")

    override suspend fun globalAction(action: GlobalAction) = guide("Please press ${action.name.lowercase().replace('_', ' ')}.")

    override fun stop() = Unit

    private suspend fun guide(text: String): ActionResult {
        say(text)
        return ActionResult.Ok("guided")
    }
}
