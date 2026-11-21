package com.mitra.engine.access

import android.view.accessibility.AccessibilityNodeInfo
import android.view.accessibility.AccessibilityWindowInfo
import com.mitra.engine.Provenance
import com.mitra.engine.Rect
import com.mitra.engine.ScreenElement
import android.graphics.Rect as AndroidRect

/** Abstraction over accessibility node to enable testability and decouple from Android runtime. */
internal interface AccessibleNode {
    val className: CharSequence?
    val text: CharSequence?
    val contentDescription: CharSequence?
    val viewIdResourceName: String?
    val packageName: CharSequence?
    val isClickable: Boolean
    val isEditable: Boolean
    val isPassword: Boolean
    val childCount: Int
    val bounds: Rect

    fun getChild(index: Int): AccessibleNode?

    val rawNode: AccessibilityNodeInfo?
}

/** Abstraction over accessibility window. */
internal interface AccessibleWindow {
    val type: Int
    val root: AccessibleNode?
}

internal class RealAccessibleNode(
    private val node: AccessibilityNodeInfo,
) : AccessibleNode {
    override val className: CharSequence? get() = node.className
    override val text: CharSequence? get() = node.text
    override val contentDescription: CharSequence? get() = node.contentDescription
    override val viewIdResourceName: String? get() = node.viewIdResourceName
    override val packageName: CharSequence? get() = node.packageName
    override val isClickable: Boolean get() = node.isClickable
    override val isEditable: Boolean get() = node.isEditable
    override val isPassword: Boolean get() = node.isPassword
    override val childCount: Int get() = node.childCount
    override val bounds: Rect
        get() {
            val r = AndroidRect()
            node.getBoundsInScreen(r)
            return Rect(r.left, r.top, r.right, r.bottom)
        }

    override fun getChild(index: Int): AccessibleNode? = node.getChild(index)?.let { RealAccessibleNode(it) }

    override val rawNode: AccessibilityNodeInfo get() = node
}

internal class RealAccessibleWindow(
    private val window: AccessibilityWindowInfo,
) : AccessibleWindow {
    override val type: Int get() = window.type
    override val root: AccessibleNode? get() = window.root?.let { RealAccessibleNode(it) }
}

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
        map(RealAccessibleWindow(window), out)
    }

    fun map(
        window: AccessibleWindow,
        out: MutableList<ScreenElement>,
    ) {
        val root = window.root ?: return
        walk(root, window, insideWeb = false, insideUserContent = false, out)
    }

    private fun walk(
        node: AccessibleNode,
        window: AccessibleWindow,
        insideWeb: Boolean,
        insideUserContent: Boolean,
        out: MutableList<ScreenElement>,
    ) {
        val isWeb = insideWeb || node.className?.toString() == "android.webkit.WebView"
        val isUser = insideUserContent || node.viewIdResourceName in userContentViewIds
        val r = node.bounds
        val id = "e${idCounter[0]++}"
        node.rawNode?.let { byId[id] = it }
        out +=
            ScreenElement(
                id = id,
                role = node.className?.toString()?.substringAfterLast('.') ?: "view",
                text = if (node.isPassword) null else node.text?.toString(),
                contentDescription = node.contentDescription?.toString(),
                viewId = node.viewIdResourceName,
                bounds = r,
                packageName = node.packageName?.toString().orEmpty(),
                windowType = windowType(window.type),
                provenance = provenance(window.type, node.packageName?.toString(), isWeb, isUser),
                clickable = node.isClickable,
                editable = node.isEditable,
                secure = node.isPassword,
            )
        for (i in 0 until node.childCount) {
            node.getChild(i)?.let { walk(it, window, isWeb, isUser, out) }
        }
    }

    private fun provenance(
        windowType: Int,
        pkg: String?,
        web: Boolean,
        user: Boolean,
    ): Provenance =
        when {
            windowType == AccessibilityWindowInfo.TYPE_SYSTEM ||
                windowType == AccessibilityWindowInfo.TYPE_INPUT_METHOD -> Provenance.SYSTEM_UI
            pkg != null && pkg != targetPackage && pkg != "com.android.systemui" -> Provenance.OVERLAY
            web -> Provenance.WEB_CONTENT
            user -> Provenance.USER_CONTENT
            windowType == AccessibilityWindowInfo.TYPE_APPLICATION -> Provenance.APP_LAYOUT
            else -> Provenance.UNKNOWN
        }

    private fun windowType(type: Int) =
        when (type) {
            AccessibilityWindowInfo.TYPE_APPLICATION -> "application"
            AccessibilityWindowInfo.TYPE_SYSTEM -> "system"
            AccessibilityWindowInfo.TYPE_INPUT_METHOD -> "ime"
            AccessibilityWindowInfo.TYPE_ACCESSIBILITY_OVERLAY -> "accessibility_overlay"
            else -> "overlay"
        }
}
