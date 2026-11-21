package com.mitra.engine.access

import android.view.accessibility.AccessibilityNodeInfo
import android.view.accessibility.AccessibilityWindowInfo
import com.mitra.engine.Provenance
import com.mitra.engine.Rect
import com.mitra.engine.ScreenElement
import android.graphics.Rect as AndroidRect

/** Converts accessibility nodes into [ScreenElement]s and tags provenance (docs/TRUST_ENGINE_SPEC.md §2). */
internal class NodeMapper(
    private val targetPackage: String,
    private val userContentViewIds: Set<String> = emptySet(),
) {
    private val idCounter = intArrayOf(0)
    val byId = mutableMapOf<String, AccessibilityNodeInfo>()

    fun map(
        window: AccessibilityWindowInfo,
        out: MutableList<ScreenElement>,
    ) {
        val root = window.root ?: return
        walk(root, window, insideWeb = false, insideUserContent = false, out)
    }

    private fun walk(
        node: AccessibilityNodeInfo,
        window: AccessibilityWindowInfo,
        insideWeb: Boolean,
        insideUserContent: Boolean,
        out: MutableList<ScreenElement>,
    ) {
        val isWeb = insideWeb || node.className?.toString() == "android.webkit.WebView"
        val isUser = insideUserContent || node.viewIdResourceName in userContentViewIds
        val r = AndroidRect().also { node.getBoundsInScreen(it) }
        val id = "e${idCounter[0]++}"
        byId[id] = node
        out +=
            ScreenElement(
                id = id,
                role = node.className?.toString()?.substringAfterLast('.') ?: "view",
                text = if (node.isPassword) null else node.text?.toString(),
                contentDescription = node.contentDescription?.toString(),
                viewId = node.viewIdResourceName,
                bounds = Rect(r.left, r.top, r.right, r.bottom),
                packageName = node.packageName?.toString().orEmpty(),
                windowType = windowType(window),
                provenance = provenance(window, node, isWeb, isUser),
                clickable = node.isClickable,
                editable = node.isEditable,
                secure = node.isPassword,
            )
        for (i in 0 until node.childCount) node.getChild(i)?.let { walk(it, window, isWeb, isUser, out) }
    }

    private fun provenance(
        window: AccessibilityWindowInfo,
        node: AccessibilityNodeInfo,
        web: Boolean,
        user: Boolean,
    ): Provenance {
        val pkg = node.packageName?.toString()
        return when {
            window.type == AccessibilityWindowInfo.TYPE_SYSTEM ||
                window.type == AccessibilityWindowInfo.TYPE_INPUT_METHOD -> Provenance.SYSTEM_UI
            pkg != null && pkg != targetPackage && pkg != "com.android.systemui" -> Provenance.OVERLAY
            web -> Provenance.WEB_CONTENT
            user -> Provenance.USER_CONTENT
            window.type == AccessibilityWindowInfo.TYPE_APPLICATION -> Provenance.APP_LAYOUT
            else -> Provenance.UNKNOWN
        }
    }

    private fun windowType(w: AccessibilityWindowInfo) =
        when (w.type) {
            AccessibilityWindowInfo.TYPE_APPLICATION -> "application"
            AccessibilityWindowInfo.TYPE_SYSTEM -> "system"
            AccessibilityWindowInfo.TYPE_INPUT_METHOD -> "ime"
            AccessibilityWindowInfo.TYPE_ACCESSIBILITY_OVERLAY -> "accessibility_overlay"
            else -> "overlay"
        }
}
