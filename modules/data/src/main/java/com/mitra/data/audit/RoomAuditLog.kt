package com.mitra.data.audit

import com.mitra.data.db.MitraDatabase
import com.mitra.data.db.StepEntity
import com.mitra.data.redaction.TextRedactor
import com.mitra.engine.ActionResult
import com.mitra.trust.AuditLog
import com.mitra.trust.Decision
import com.mitra.trust.ProposedAction
import com.mitra.trust.Risk

/**
 * Room-backed encrypted AuditLog (docs/TRUST_ENGINE_SPEC.md §7, DATA_SPEC.md §3).
 */
class RoomAuditLog(
    private val db: MitraDatabase,
    private val now: () -> Long = { System.currentTimeMillis() },
) : AuditLog {
    private val stepStartTimes = mutableMapOf<String, Long>()

    override suspend fun record(
        action: ProposedAction,
        decision: Decision,
    ) {
        stepStartTimes[decision.auditId] = now()

        // Redaction per docs/DATA_SPEC.md §4:
        // NEVER store passwords, OTPs, PINs, or text from secure fields.
        val isSecure =
            action.risk == Risk.SECRET ||
                action.target?.lowercase()?.contains("password") == true ||
                action.target?.lowercase()?.contains("otp") == true ||
                action.target?.lowercase()?.contains("pin") == true

        val sanitizedText =
            if (action.text != null) {
                TextRedactor.redactFieldText(action.text?.value, isSecure)
            } else {
                null
            }

        val actionJson =
            buildString {
                append("{\"type\":\"").append(action.type.name).append("\"")
                action.target?.let { append(",\"target\":\"").append(it).append("\"") }
                sanitizedText?.let { append(",\"text\":\"").append(it.replace("\"", "\\\"")).append("\"") }
                append(",\"route\":\"").append(action.route.name).append("\"")
                append(",\"agent\":\"").append(action.agent.name).append("\"")
                append(",\"step\":").append(action.step)
                append(",\"claimedRisk\":\"").append(action.risk.name).append("\"}")
            }

        val entity =
            StepEntity(
                stepId = decision.auditId,
                taskId = action.taskId,
                idx = action.step,
                agent = action.agent.name,
                engine = "ACCESSIBILITY",
                route = action.route.name,
                actionJson = actionJson,
                verdict = decision.verdict.name,
                ruleId = decision.rule,
                reason = decision.reason,
                userResponse = null,
                result = null,
                screenshotHash = null,
                durationMs = 0L,
                createdAt = now(),
            )
        db.stepDao().insert(entity)
    }

    override suspend fun result(
        auditId: String,
        result: ActionResult,
    ) {
        val startTime = stepStartTimes.remove(auditId) ?: now()
        val duration = (now() - startTime).coerceAtLeast(0L)
        val resultStr =
            when (result) {
                is ActionResult.Ok -> "OK"
                is ActionResult.Failed -> result.reason.name
            }
        val userResponse =
            when {
                result is ActionResult.Failed && result.message.contains("user said no") -> "NO"
                result is ActionResult.Ok && result.detail?.contains("user completed") == true -> "CONTINUE"
                else -> null
            }
        db.stepDao().updateResult(auditId, resultStr, userResponse, duration)
    }
}
