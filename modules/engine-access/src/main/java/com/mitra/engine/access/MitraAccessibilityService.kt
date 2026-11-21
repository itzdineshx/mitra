package com.mitra.engine.access

import android.accessibilityservice.AccessibilityService
import android.view.accessibility.AccessibilityEvent

/**
 * The system-bound service. It only holds the connection; all logic lives in [AccessibilityEngine].
 * This module is the ONLY place allowed to import android.accessibilityservice.* (AGENTS.md §4).
 */
class MitraAccessibilityService : AccessibilityService() {
    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this
        StopOverlay.show(this)
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        // Screen changes are read on demand by AccessibilityEngine.readScreen(); nothing to do here yet.
    }

    override fun onInterrupt() = Unit

    override fun onDestroy() {
        StopOverlay.hide()
        instance = null
        super.onDestroy()
    }

    companion object {
        @Volatile
        var instance: MitraAccessibilityService? = null
            private set
    }
}
