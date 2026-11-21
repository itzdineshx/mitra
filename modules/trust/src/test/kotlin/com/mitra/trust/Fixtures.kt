package com.mitra.trust

import com.mitra.engine.Provenance
import com.mitra.engine.Rect
import com.mitra.engine.ScreenElement
import com.mitra.engine.ScreenSnapshot

/** Small builders so each test reads like a row of docs/TRUST_ENGINE_SPEC.md §6. */
object Fx {
    const val WHATSAPP = "com.whatsapp"
    const val CHROME = "com.android.chrome"
    const val NOW = 1_000_000L

    fun el(
        id: String,
        text: String,
        prov: Provenance = Provenance.APP_LAYOUT,
        pkg: String = WHATSAPP,
        bounds: Rect = Rect(0, 1000, 200, 1100),
        secure: Boolean = false,
        editable: Boolean = false,
    ) = ScreenElement(
        id = id,
        role = if (editable) "edit" else "button",
        text = if (secure) null else text,
        contentDescription = if (secure) text else null,
        bounds = bounds,
        packageName = pkg,
        windowType = if (prov == Provenance.OVERLAY) "overlay" else "application",
        provenance = prov,
        clickable = true,
        editable = editable,
        secure = secure,
    )

    fun scope(
        goal: String = "Send hi to Amma on WhatsApp",
        packages: Set<String> = setOf(WHATSAPP),
        domains: Set<String> = emptySet(),
        maxRisk: Risk = Risk.SEND,
    ) = TaskScope(
        taskId = "t1",
        goal = goal,
        allowedPackages = packages,
        allowedDomains = domains,
        allowedActions = ActionType.entries.toSet(),
        maxRisk = maxRisk,
        expiresAtMillis = NOW + 600_000,
    )

    fun ctx(
        vararg elements: ScreenElement,
        pkg: String = WHATSAPP,
        url: String? = null,
    ) = DeviceContext(
        foregroundPackage = pkg,
        currentUrl = url,
        screen = ScreenSnapshot(foregroundPackage = pkg, currentUrl = url, elements = elements.toList()),
        nowMillis = NOW,
    )

    fun tap(
        target: String,
        evidence: List<String> = listOf(target),
        claimed: Risk = Risk.READ,
    ) = ProposedAction(
        taskId = "t1",
        step = 1,
        agent = AgentRole.APP,
        type = ActionType.TAP,
        target = target,
        evidence = evidence,
        risk = claimed,
    )

    fun type(
        target: String,
        text: TaintedText,
        evidence: List<String> = listOf(target),
    ) = ProposedAction(
        taskId = "t1",
        step = 1,
        agent = AgentRole.APP,
        type = ActionType.TYPE,
        target = target,
        text = text,
        evidence = evidence,
    )
}
