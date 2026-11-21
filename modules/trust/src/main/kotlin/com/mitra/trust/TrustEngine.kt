package com.mitra.trust

import com.mitra.engine.Provenance
import com.mitra.web.DomainCheck
import com.mitra.web.DomainPolicy
import java.util.UUID

/**
 * Deterministic safety check run before EVERY action. No model calls in here.
 * Rules R1–R14 are evaluated in order; the first match wins (docs/TRUST_ENGINE_SPEC.md §4).
 */
class TrustEngine(
    private val risk: RiskDeriver = RiskDeriver(),
    private val domains: DomainPolicy = DomainPolicy(),
    private val browserPackages: Set<String> = DEFAULT_BROWSERS,
    private val isPluginTrusted: (String) -> Boolean = { false },
    private val newId: () -> String = { UUID.randomUUID().toString() },
) {
    fun check(
        action: ProposedAction,
        scope: TaskScope,
        ctx: DeviceContext,
        acknowledged: Set<String> = emptySet(), // PAUSE rules (R7, R12, R13) the user already said "continue" to
    ): Decision {
        val ack = acknowledged.intersect(PAUSE_RULES)
        val derived = risk.derive(action, ctx)
        val checks = mutableMapOf("risk" to derived.name, "agentRisk" to action.risk.name)

        fun d(
            v: Verdict,
            rule: String?,
            reason: String,
        ) = Decision(v, rule, checks.toMap(), reason, newId())

        // ---- R1 Stop
        if (ctx.stopPressed) return d(Verdict.ABORT, "R1", "Stopped.")
        if (ctx.nowMillis > scope.expiresAtMillis) return d(Verdict.REFUSE, "EXPIRED", "This task has expired. Please ask again.")

        // ---- Scope
        val pkg = if (action.type == ActionType.OPEN_APP) action.target.orEmpty() else ctx.foregroundPackage
        if (action.type.touchesApp() && pkg !in scope.allowedPackages) {
            checks["scope"] = "fail:package"
            return d(Verdict.REFUSE, "R2", "This task is not allowed to use $pkg.")
        }
        if (action.type !in scope.allowedActions && action.type.touchesApp()) {
            checks["scope"] = "fail:action"
            return d(Verdict.REFUSE, "R3", "This task is not allowed to ${action.type.name.lowercase()}.")
        }
        if (ctx.foregroundPackage in browserPackages && action.type.touchesApp()) {
            val c = domains.check(ctx.currentUrl, scope.allowedDomains)
            if (c is DomainCheck.Refused) {
                checks["scope"] = "fail:domain"
                return d(Verdict.REFUSE, "R4", "I won't continue on this website: ${c.reason}.")
            }
        }
        if (changesSecurity(action, ctx)) {
            checks["scope"] = "fail:security"
            return d(
                Verdict.REFUSE,
                "R5",
                "I never install apps or change security settings. Please do this yourself if you really want to.",
            )
        }
        checks["scope"] = "pass"

        // ---- Provenance
        val target = ctx.screen.find(action.target)
        if (target != null) {
            val overlay =
                target.provenance == Provenance.OVERLAY ||
                    ctx.screen.elements.any {
                        it.provenance == Provenance.OVERLAY && it.packageName != target.packageName && it.bounds.intersects(target.bounds)
                    }
            if (overlay) {
                checks["provenance"] = "fail:overlay"
                return d(Verdict.REFUSE, "R6", "Another app is drawing over the screen. I stopped to keep you safe.")
            }
        }
        val untrustedEvidence =
            action.evidence.any { id ->
                ctx.screen
                    .find(id)
                    ?.provenance
                    ?.isTrusted != true
            } ||
                action.text?.isTrusted == false
        if ("R7" !in ack && untrustedEvidence && !entailedByGoal(action, scope, derived)) {
            checks["provenance"] = "fail:untrusted"
            return d(Verdict.PAUSE, "R7", "Something on the screen is asking me to do a step you didn't ask for. Should I continue?")
        }
        checks["provenance"] = "pass"

        // ---- Risk
        if (derived == Risk.SECRET) return d(Verdict.USER_ONLY, "R8", "Please type this yourself. I never enter passwords or OTPs.")
        if (derived == Risk.PAYMENT) return d(Verdict.USER_ONLY, "R9", "This is a payment screen. Please complete it yourself.")
        if (derived == Risk.SEND && ctx.sendsLastHour >= MAX_SENDS_PER_HOUR) {
            return d(Verdict.REFUSE, "R10", "I've sent many messages in the last hour, so I'm pausing sends for now.")
        }
        if ((derived == Risk.SEND || derived == Risk.SUBMIT) && !ctx.standingRuleCoversAction) {
            val strong = derived == Risk.SUBMIT || action.newRecipient || derived > scope.maxRisk
            val label = target?.label?.ifBlank { null } ?: "this"
            return d(if (strong) Verdict.CONFIRM_STRONG else Verdict.CONFIRM, "R11", "Shall I tap \"$label\" now?")
        }
        checks["risk"] = "pass:${derived.name}"

        // ---- Context
        if ("R12" !in ack && ctx.callActive && !ctx.callerInContacts) {
            return d(Verdict.PAUSE, "R12", "You are on a call with an unknown number. Scammers often do this. Should I continue?")
        }
        if ("R13" !in ack && (ctx.screenShareActive || ctx.recentRemoteAccessInstall)) {
            return d(Verdict.PAUSE, "R13", "Your screen may be shared with someone else. Should I continue?")
        }
        checks["context"] = "pass"

        // ---- Plugin (second line; PluginLoader also refuses at load time)
        val plugin = action.pluginId
        if (plugin != null && !isPluginTrusted(plugin)) return d(Verdict.REFUSE, "R14", "This plugin isn't signed, so I won't use it.")

        return d(Verdict.ALLOW, null, "ok")
    }

    /**
     * Is this action something the user's own words already asked for?
     * Starter version: typed text must appear in the goal, and committing taps need the user. Refine in task T12.
     */
    private fun entailedByGoal(
        action: ProposedAction,
        scope: TaskScope,
        derived: Risk,
    ): Boolean =
        when {
            action.type == ActionType.TYPE -> action.text?.value?.let { scope.goal.contains(it, ignoreCase = true) } ?: true
            derived >= Risk.SEND -> false
            action.type == ActionType.SEND_INTENT || action.type == ActionType.OPEN_APP -> false
            else -> true
        }

    private fun changesSecurity(
        action: ProposedAction,
        ctx: DeviceContext,
    ): Boolean {
        val label =
            ctx.screen
                .find(action.target)
                ?.label
                ?.lowercase()
                .orEmpty()
        val onSecurityScreen = ctx.foregroundPackage in INSTALLER_PACKAGES || SECURITY_WORDS.any { label.contains(it) }
        val targetText = action.target.orEmpty().lowercase()
        return (action.type.touchesApp() && onSecurityScreen && action.type != ActionType.SCROLL) ||
            SECURITY_INTENTS.any { targetText.contains(it) }
    }

    private fun ActionType.touchesApp() = this !in setOf(ActionType.SPEAK, ActionType.ASK_USER, ActionType.DONE)

    companion object {
        const val MAX_SENDS_PER_HOUR = 20
        val PAUSE_RULES = setOf("R7", "R12", "R13")
        val DEFAULT_BROWSERS = setOf("com.android.chrome", "org.mozilla.firefox", "com.sec.android.app.sbrowser", "com.brave.browser")
        private val INSTALLER_PACKAGES = setOf("com.android.packageinstaller", "com.google.android.packageinstaller")
        private val SECURITY_WORDS =
            listOf("unknown sources", "install unknown apps", "device admin", "developer options", "screen lock", "accessibility")
        private val SECURITY_INTENTS =
            listOf("manage_unknown_app_sources", "add_device_admin", "accessibility_settings", "security_settings", "install_package")
    }
}
