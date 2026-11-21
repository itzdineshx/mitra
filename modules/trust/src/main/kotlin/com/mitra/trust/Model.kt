package com.mitra.trust

import com.mitra.engine.Provenance
import com.mitra.engine.ScreenSnapshot
import kotlinx.serialization.Serializable

/** Ordered from least to most dangerous; compare with `>`. */
@Serializable
enum class Risk { READ, WRITE, SEND, SUBMIT, SECRET, PAYMENT }

@Serializable
enum class AgentRole { ORCHESTRATOR, APP, WEB, MEMORY, VERIFIER, GUARDIAN }

@Serializable
enum class ActionType { OPEN_APP, CALL_FUNCTION, SEND_INTENT, TAP, LONG_PRESS, TYPE, SCROLL, SWIPE, GLOBAL_ACTION, SPEAK, ASK_USER, DONE }

@Serializable
enum class Route { FUNCTION, INTENT, SCREEN }

enum class Verdict { ALLOW, CONFIRM, CONFIRM_STRONG, USER_ONLY, REFUSE, PAUSE, ABORT }

/** What one task is allowed to do. Created by the orchestrator, fixed for the task's life. */
@Serializable
data class TaskScope(
    val taskId: String,
    val goal: String, // the user's words, verbatim
    val languageTag: String = "en-IN",
    val allowedPackages: Set<String>,
    val allowedDomains: Set<String> = emptySet(), // empty = no web
    val allowedActions: Set<ActionType>,
    val maxRisk: Risk,
    val expiresAtMillis: Long,
)

/** Text that remembers where it came from (cross-agent provenance). Untrusted if any source is untrusted. */
@Serializable
data class TaintedText(
    val value: String,
    val sources: List<Provenance>,
) {
    val isTrusted: Boolean get() = sources.all { it.isTrusted }

    operator fun plus(other: TaintedText) = TaintedText(value + other.value, sources + other.sources)

    companion object {
        fun fromUser(value: String) = TaintedText(value, listOf(Provenance.SYSTEM_UI))
    }
}

@Serializable
data class ProposedAction(
    val taskId: String,
    val step: Int,
    val agent: AgentRole,
    val type: ActionType,
    val target: String? = null, // element id, package name or intent action
    val text: TaintedText? = null, // for TYPE
    val route: Route = Route.SCREEN,
    val evidence: List<String> = emptyList(), // element ids the agent relied on
    val risk: Risk = Risk.READ, // the agent's claim; never trusted
    val reason: String = "",
    val pluginId: String? = null, // set when the step comes from a plugin recipe
    val newRecipient: Boolean = false,
)

@Serializable
data class DeviceContext(
    val foregroundPackage: String,
    val currentUrl: String? = null,
    val screen: ScreenSnapshot,
    val callActive: Boolean = false,
    val callerInContacts: Boolean = false,
    val screenShareActive: Boolean = false,
    val recentRemoteAccessInstall: Boolean = false,
    val sendsLastHour: Int = 0,
    val stopPressed: Boolean = false,
    val standingRuleCoversAction: Boolean = false,
    val nowMillis: Long = System.currentTimeMillis(),
)

@Serializable
data class Decision(
    val verdict: Verdict,
    val rule: String?, // "R11" etc.; null = default allow
    val checks: Map<String, String>,
    val reason: String, // plain English, can be spoken
    val auditId: String,
)
