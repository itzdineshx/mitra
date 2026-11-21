package com.mitra.engine.access

import android.accessibilityservice.AccessibilityService
import android.view.accessibility.AccessibilityEvent
import kotlinx.coroutines.delay

/**
 * The system-bound service. It only holds the connection; all logic lives in [AccessibilityEngine].
 * This module is the ONLY place allowed to import android.accessibilityservice.* (AGENTS.md §4).
 */
class MitraAccessibilityService : AccessibilityService() {
    @Volatile
    var lastEventTimeMillis: Long = System.currentTimeMillis()
        internal set

    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this
        StopOverlay.onStop = { TaskRunner.cancelTask("overlay stop pressed") }
        StopOverlay.show(this)
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null) return
        when (event.eventType) {
            AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED,
            AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED,
            AccessibilityEvent.TYPE_WINDOWS_CHANGED,
            AccessibilityEvent.TYPE_VIEW_SCROLLED,
            -> {
                lastEventTimeMillis = System.currentTimeMillis()
            }
        }
    }

    override fun onInterrupt() = Unit

    override fun onDestroy() {
        StopOverlay.hide()
        instance = null
        super.onDestroy()
    }

    suspend fun awaitStableScreen(
        quietPeriodMs: Long = 250L,
        timeoutMs: Long = 2000L,
    ): Boolean {
        val start = System.currentTimeMillis()
        while (System.currentTimeMillis() - start < timeoutMs) {
            val quiet = System.currentTimeMillis() - lastEventTimeMillis
            if (quiet >= quietPeriodMs) {
                return true
            }
            delay(50L)
        }
        return false // Timed out waiting for silence; proceed anyway
    }

    companion object {
        @Volatile
        var instance: MitraAccessibilityService? = null
            internal set
    }
}
