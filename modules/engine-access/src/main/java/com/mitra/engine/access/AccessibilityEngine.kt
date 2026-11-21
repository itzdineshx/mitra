package com.mitra.engine.access

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.content.Context
import android.content.Intent
import android.graphics.Path
import android.net.Uri
import android.os.Bundle
import android.view.accessibility.AccessibilityNodeInfo
import com.mitra.engine.ActionResult
import com.mitra.engine.Direction
import com.mitra.engine.EngineKind
import com.mitra.engine.ExecutionEngine
import com.mitra.engine.FailureReason
import com.mitra.engine.GlobalAction
import com.mitra.engine.IntentSpec
import com.mitra.engine.Point
import com.mitra.engine.ScreenElement
import com.mitra.engine.ScreenSnapshot
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

/** Main engine: drives any app through the Accessibility Service. Called only by TrustedExecutor. */
class AccessibilityEngine(
    private val context: Context,
) : ExecutionEngine {
    override val kind = EngineKind.ACCESSIBILITY

    @Volatile private var stopped = false
    private var lastNodes: Map<String, AccessibilityNodeInfo> = emptyMap()

    private val service get() = MitraAccessibilityService.instance

    override suspend fun isAvailable() = service != null

    override suspend fun readScreen(): ScreenSnapshot {
        val s = service ?: return ScreenSnapshot(foregroundPackage = "")
        val fg =
            s.rootInActiveWindow
                ?.packageName
                ?.toString()
                .orEmpty()
        val mapper = NodeMapper(targetPackage = fg)
        val elements = mutableListOf<ScreenElement>()
        s.windows.forEach { mapper.map(it, elements) }
        lastNodes = mapper.byId
        return ScreenSnapshot(
            foregroundPackage = fg,
            currentUrl = AddressBar.read(elements, fg),
            elements = elements,
            takenAtMillis = System.currentTimeMillis(),
        )
    }

    override suspend fun launchApp(packageName: String): ActionResult {
        val intent =
            context.packageManager.getLaunchIntentForPackage(packageName) ?: return fail(FailureReason.TARGET_NOT_FOUND, packageName)
        context.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        return ActionResult.Ok()
    }

    override suspend fun sendIntent(intent: IntentSpec): ActionResult {
        val i = Intent(intent.action).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        intent.data?.let { i.data = Uri.parse(it) }
        intent.packageName?.let { i.setPackage(it) }
        intent.extras.forEach { (k, v) -> i.putExtra(k, v) }
        return runCatching {
            context.startActivity(
                i,
            )
        }.fold({ ActionResult.Ok() }, { fail(FailureReason.NOT_SUPPORTED, it.message.orEmpty()) })
    }

    override suspend fun tap(elementId: String) = nodeAction(elementId, AccessibilityNodeInfo.ACTION_CLICK)

    override suspend fun longPress(elementId: String) = nodeAction(elementId, AccessibilityNodeInfo.ACTION_LONG_CLICK)

    override suspend fun type(
        elementId: String,
        text: String,
    ): ActionResult {
        val node = lastNodes[elementId] ?: return fail(FailureReason.TARGET_NOT_FOUND, elementId)
        if (node.isPassword) return fail(FailureReason.SECURE_FIELD, "never types into secure fields")
        val args = Bundle().apply { putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, text) }
        return if (node.performAction(
                AccessibilityNodeInfo.ACTION_SET_TEXT,
                args,
            )
        ) {
            ActionResult.Ok()
        } else {
            fail(FailureReason.UNKNOWN, "set text failed")
        }
    }

    override suspend fun scroll(
        direction: Direction,
        elementId: String?,
    ): ActionResult {
        val node = elementId?.let { lastNodes[it] } ?: lastNodes.values.firstOrNull { it.isScrollable }
        val action =
            if (direction == Direction.UP ||
                direction == Direction.LEFT
            ) {
                AccessibilityNodeInfo.ACTION_SCROLL_BACKWARD
            } else {
                AccessibilityNodeInfo.ACTION_SCROLL_FORWARD
            }
        return if (node?.performAction(action) == true) ActionResult.Ok() else fail(FailureReason.TARGET_NOT_FOUND, "nothing to scroll")
    }

    override suspend fun swipe(
        from: Point,
        to: Point,
        durationMs: Long,
    ): ActionResult {
        val s = service ?: return fail(FailureReason.ENGINE_UNAVAILABLE, "service off")
        val path =
            Path().apply {
                moveTo(from.x.toFloat(), from.y.toFloat())
                lineTo(to.x.toFloat(), to.y.toFloat())
            }
        val gesture = GestureDescription.Builder().addStroke(GestureDescription.StrokeDescription(path, 0, durationMs)).build()
        val ok =
            suspendCancellableCoroutine { cont ->
                s.dispatchGesture(
                    gesture,
                    object : AccessibilityService.GestureResultCallback() {
                        override fun onCompleted(d: GestureDescription?) = cont.resume(true)

                        override fun onCancelled(d: GestureDescription?) = cont.resume(false)
                    },
                    null,
                )
            }
        return if (ok) ActionResult.Ok() else fail(FailureReason.CANCELLED, "gesture cancelled")
    }

    override suspend fun globalAction(action: GlobalAction): ActionResult {
        val s = service ?: return fail(FailureReason.ENGINE_UNAVAILABLE, "service off")
        val code =
            when (action) {
                GlobalAction.BACK -> AccessibilityService.GLOBAL_ACTION_BACK
                GlobalAction.HOME -> AccessibilityService.GLOBAL_ACTION_HOME
                GlobalAction.RECENTS -> AccessibilityService.GLOBAL_ACTION_RECENTS
                GlobalAction.NOTIFICATIONS -> AccessibilityService.GLOBAL_ACTION_NOTIFICATIONS
                GlobalAction.QUICK_SETTINGS -> AccessibilityService.GLOBAL_ACTION_QUICK_SETTINGS
            }
        return if (s.performGlobalAction(code)) ActionResult.Ok() else fail(FailureReason.UNKNOWN, "global action failed")
    }

    override fun stop() {
        stopped = true
        lastNodes = emptyMap()
    }

    private fun nodeAction(
        id: String,
        action: Int,
    ): ActionResult {
        if (stopped) return fail(FailureReason.CANCELLED, "stopped")
        val node = lastNodes[id] ?: return fail(FailureReason.TARGET_NOT_FOUND, id)
        var n: AccessibilityNodeInfo? = node
        while (n != null && !n.isClickable) n = n.parent // tap the nearest clickable ancestor
        return if ((n ?: node).performAction(action)) ActionResult.Ok() else fail(FailureReason.UNKNOWN, "action failed")
    }

    private fun fail(
        r: FailureReason,
        m: String,
    ) = ActionResult.Failed(r, m)
}

/** Reads the URL from the browser's address bar (needed for rule R4). Extend per browser in task T17. */
internal object AddressBar {
    private val urlBarIds =
        mapOf(
            "com.android.chrome" to "com.android.chrome:id/url_bar",
            "org.mozilla.firefox" to "org.mozilla.firefox:id/mozac_browser_toolbar_url_view",
            "com.sec.android.app.sbrowser" to "com.sec.android.app.sbrowser:id/location_bar_edit_text",
        )

    fun read(
        elements: List<ScreenElement>,
        pkg: String,
    ): String? {
        val id = urlBarIds[pkg] ?: return null
        val raw = elements.firstOrNull { it.viewId == id }?.text?.trim() ?: return null
        // Chrome hides "https://" for secure pages; anything shown without a scheme is treated as https only
        // when the browser shows its secure lock. Starter: assume https if no scheme. Harden in task T17.
        return if (raw.contains("://")) raw else "https://$raw"
    }
}
