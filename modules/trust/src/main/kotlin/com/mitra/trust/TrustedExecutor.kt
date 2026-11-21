package com.mitra.trust

import com.mitra.engine.ActionResult
import com.mitra.engine.Direction
import com.mitra.engine.ExecutionEngine
import com.mitra.engine.FailureReason
import com.mitra.engine.GlobalAction
import com.mitra.engine.IntentSpec

/** Asks the user. Implemented in the app (voice "yes" + BiometricPrompt). */
interface Confirmer {
    suspend fun askVoice(question: String): Boolean

    suspend fun biometric(reason: String): Boolean

    suspend fun speak(text: String)

    /** Hand control to the user (R8/R9) and suspend until they say they're done. */
    suspend fun waitForUser(message: String)
}

/** Writes every decision to the local `steps` table (docs/TRUST_ENGINE_SPEC.md §7). */
interface AuditLog {
    suspend fun record(
        action: ProposedAction,
        decision: Decision,
    )

    suspend fun result(
        auditId: String,
        result: ActionResult,
    )
}

/**
 * THE ONLY PATH from an agent to a mutating engine call (AGENTS.md §3, rule 1).
 * Agents build a [ProposedAction]; this class checks, confirms, performs and logs it.
 */
class TrustedExecutor(
    private val engine: ExecutionEngine,
    private val trust: TrustEngine,
    private val confirmer: Confirmer,
    private val audit: AuditLog,
    private val context: suspend () -> DeviceContext,
) {
    suspend fun execute(
        action: ProposedAction,
        scope: TaskScope,
        acknowledged: Set<String> = emptySet(),
    ): ActionResult {
        val decision = trust.check(action, scope, context(), acknowledged)
        audit.record(action, decision)
        val result =
            when (decision.verdict) {
                Verdict.ALLOW -> performIfStillThere(action)
                Verdict.CONFIRM ->
                    if (confirmer.askVoice(decision.reason)) performIfStillThere(action) else cancelled()
                Verdict.CONFIRM_STRONG ->
                    if (confirmer.askVoice(decision.reason) && confirmer.biometric(decision.reason)) {
                        performIfStillThere(action)
                    } else {
                        cancelled()
                    }
                Verdict.USER_ONLY -> {
                    confirmer.waitForUser(decision.reason)
                    ActionResult.Ok("user completed the step")
                }
                Verdict.PAUSE ->
                    if (confirmer.askVoice(decision.reason)) {
                        // Explicit yes: re-check with this warning acknowledged; every other rule still applies.
                        execute(action, scope, acknowledged + decision.rule.orEmpty())
                    } else {
                        cancelled()
                    }
                Verdict.REFUSE -> {
                    confirmer.speak(decision.reason)
                    ActionResult.Failed(FailureReason.CANCELLED, decision.reason)
                }
                Verdict.ABORT -> {
                    engine.stop()
                    ActionResult.Failed(FailureReason.CANCELLED, "stopped")
                }
            }
        audit.result(decision.auditId, result)
        return result
    }

    /** Re-read the screen right before acting: the target must still exist with the same bounds and provenance. */
    private suspend fun performIfStillThere(action: ProposedAction): ActionResult {
        val id = action.target
        if (action.type in ELEMENT_ACTIONS && id != null) {
            val before = context().screen.find(id)
            val now = engine.readScreen().find(id)
            if (now == null || before == null || now.bounds != before.bounds || now.provenance != before.provenance) {
                return ActionResult.Failed(FailureReason.TARGET_NOT_FOUND, "screen changed; re-plan")
            }
        }
        return perform(action)
    }

    private suspend fun perform(a: ProposedAction): ActionResult =
        when (a.type) {
            ActionType.OPEN_APP -> engine.launchApp(a.target.orEmpty())
            ActionType.SEND_INTENT -> engine.sendIntent(IntentSpec(action = a.target.orEmpty()))
            ActionType.TAP -> engine.tap(a.target.orEmpty())
            ActionType.LONG_PRESS -> engine.longPress(a.target.orEmpty())
            ActionType.TYPE -> engine.type(a.target.orEmpty(), a.text?.value.orEmpty())
            ActionType.SCROLL -> engine.scroll(Direction.valueOf(a.text?.value ?: "DOWN"), a.target)
            ActionType.GLOBAL_ACTION -> engine.globalAction(GlobalAction.valueOf(a.target ?: "BACK"))
            ActionType.SPEAK -> {
                confirmer.speak(a.text?.value.orEmpty())
                ActionResult.Ok()
            }
            ActionType.SWIPE, ActionType.CALL_FUNCTION ->
                ActionResult.Failed(FailureReason.NOT_SUPPORTED, "${a.type} is wired in a later task")
            ActionType.ASK_USER, ActionType.DONE -> ActionResult.Ok()
        }

    private fun cancelled() = ActionResult.Failed(FailureReason.CANCELLED, "user said no")

    private companion object {
        val ELEMENT_ACTIONS = setOf(ActionType.TAP, ActionType.LONG_PRESS, ActionType.TYPE)
    }
}
