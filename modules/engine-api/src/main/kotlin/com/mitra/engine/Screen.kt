package com.mitra.engine

import kotlinx.serialization.Serializable

/** Where a screen element came from. Only SYSTEM_UI and APP_LAYOUT are trusted (docs/TRUST_ENGINE_SPEC.md §2). */
@Serializable
enum class Provenance {
    SYSTEM_UI,
    APP_LAYOUT,
    WEB_CONTENT,
    USER_CONTENT,
    OVERLAY,
    UNKNOWN,
    ;

    val isTrusted: Boolean get() = this == SYSTEM_UI || this == APP_LAYOUT
}

@Serializable
data class Rect(
    val left: Int,
    val top: Int,
    val right: Int,
    val bottom: Int,
) {
    fun intersects(other: Rect): Boolean = left < other.right && other.left < right && top < other.bottom && other.top < bottom

    val centerX: Int get() = (left + right) / 2
    val centerY: Int get() = (top + bottom) / 2
}

@Serializable
data class Point(
    val x: Int,
    val y: Int,
)

/** One element of the screen, converted from an accessibility node. Never holds platform objects. */
@Serializable
data class ScreenElement(
    val id: String,
    val role: String,
    val text: String? = null, // always null when [secure] is true
    val contentDescription: String? = null,
    val viewId: String? = null,
    val bounds: Rect,
    val packageName: String,
    val windowType: String, // application, system, overlay, ime, accessibility_overlay
    val provenance: Provenance,
    val clickable: Boolean = false,
    val editable: Boolean = false,
    val secure: Boolean = false,
) {
    /** Text the agents may read; secure elements never expose text. */
    val label: String get() = listOfNotNull(text, contentDescription).joinToString(" ").trim()
}

@Serializable
data class ScreenSnapshot(
    val foregroundPackage: String,
    val currentUrl: String? = null, // from the browser address bar, with scheme (null if not a browser)
    val elements: List<ScreenElement> = emptyList(),
    val takenAtMillis: Long = 0L,
    val screenshotHash: String? = null,
) {
    fun find(id: String?): ScreenElement? = elements.firstOrNull { it.id == id }
}
