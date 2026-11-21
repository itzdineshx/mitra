package com.mitra.app.debug

import android.app.Activity
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.mitra.engine.access.AccessibilityEngine
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Debug-only broadcast receiver for reading and logging screen elements.
 * Usage:
 * adb shell am broadcast -a com.mitra.app.debug.DUMP_SCREEN -n com.mitra.app/.debug.ScreenDumpReceiver
 */
class ScreenDumpReceiver : BroadcastReceiver() {
    override fun onReceive(
        context: Context,
        intent: Intent?,
    ) {
        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.Main).launch {
            try {
                val engine = AccessibilityEngine(context)
                val snapshot = engine.readScreen()
                ScreenDebugState.lastSnapshot = snapshot

                Log.i("MitraScreen", "--- SNAPSHOT START ---")
                Log.i("MitraScreen", "FOREGROUND_PKG=${snapshot.foregroundPackage}")
                Log.i("MitraScreen", "CURRENT_URL=${snapshot.currentUrl ?: "none"}")
                Log.i("MitraScreen", "ELEMENT_COUNT=${snapshot.elements.size}")

                snapshot.elements.forEach { elem ->
                    val textDisplay = if (elem.secure) "[SECURE/REDACTED]" else (elem.text ?: "")
                    Log.i(
                        "MitraScreen",
                        "[${elem.id}] role=${elem.role} prov=${elem.provenance} secure=${elem.secure} " +
                            "text=\"$textDisplay\" desc=\"${elem.contentDescription ?: ""}\" " +
                            "bounds=(${elem.bounds.left},${elem.bounds.top},${elem.bounds.right},${elem.bounds.bottom}) " +
                            "clickable=${elem.clickable}",
                    )
                }
                Log.i("MitraScreen", "--- SNAPSHOT END ---")

                pendingResult.setResultCode(Activity.RESULT_OK)
                pendingResult.setResultData("Elements: ${snapshot.elements.size}, Pkg: ${snapshot.foregroundPackage}")
            } catch (t: Throwable) {
                Log.e("MitraScreen", "Failed to dump screen", t)
                pendingResult.setResultCode(Activity.RESULT_CANCELED)
                pendingResult.setResultData("Error: ${t.message}")
            } finally {
                pendingResult.finish()
            }
        }
    }
}
