package com.mitra.agents

import com.mitra.engine.ActionResult
import com.mitra.engine.ExecutionEngine
import com.mitra.engine.FailureReason
import com.mitra.model.LocalModel
import com.mitra.trust.ActionType
import com.mitra.trust.AgentRole
import com.mitra.trust.ProposedAction
import com.mitra.trust.TaskScope
import com.mitra.trust.TrustedExecutor
import kotlinx.serialization.json.Json

/**
 * Runs one task: plan → act → verify, one step at a time (docs/ARCHITECTURE.md).
 * Agents may READ the screen directly but every other action goes through [TrustedExecutor].
 */
class Orchestrator(
    private val model: LocalModel,
    private val engine: ExecutionEngine, // read-only use here
    private val executor: TrustedExecutor,
    private val prompts: Prompts = Prompts(),
    private val maxSteps: Int = 25,
) {
    private val json = Json { ignoreUnknownKeys = true }

    suspend fun run(scope: TaskScope): TaskOutcome {
        for (step in 1..maxSteps) {
            val screen = engine.readScreen()
            val raw = model.generate(prompts.forRole(AgentRole.APP), prompts.stepInput(scope, screen, step))
            val plan =
                runCatching { json.decodeFromString(ModelStep.serializer(), raw.trim()) }
                    .getOrElse { return TaskOutcome.Failed("model gave an unreadable answer") }
            if (plan.type == ActionType.DONE) return TaskOutcome.Done(plan.reason)
            val action =
                ProposedAction(
                    taskId = scope.taskId,
                    step = step,
                    agent = AgentRole.APP,
                    type = plan.type,
                    target = plan.target,
                    text = plan.text?.let { prompts.taint(it, screen) },
                    evidence = plan.evidence,
                    reason = plan.reason,
                )
            val result = executor.execute(action, scope)
            if (result is ActionResult.Failed && result.reason == FailureReason.CANCELLED) return TaskOutcome.Cancelled(result.message)
        }
        return TaskOutcome.Failed("too many steps")
    }
}

sealed interface TaskOutcome {
    data class Done(
        val summary: String,
    ) : TaskOutcome

    data class Cancelled(
        val why: String,
    ) : TaskOutcome

    data class Failed(
        val why: String,
    ) : TaskOutcome
}
