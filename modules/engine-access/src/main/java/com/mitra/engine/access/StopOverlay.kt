package com.mitra.engine.access

import android.content.Context
import android.graphics.PixelFormat
import android.util.Log
import android.view.Gravity
import android.view.WindowManager
import android.widget.Button
import com.mitra.engine.Rect

/**
 * The red Stop button. Drawn as an accessibility overlay so it sits above every app.
 * Pressing it must stop the task within 300 ms (rule R1).
 */
object StopOverlay {
    @Volatile
    var onStop: () -> Unit = {}
    private var button: Button? = null
    private var layoutParams: WindowManager.LayoutParams? = null

    val currentBounds: Rect?
        get() {
            val b = button ?: return null
            val loc = IntArray(2)
            b.getLocationOnScreen(loc)
            return Rect(loc[0], loc[1], loc[0] + b.width, loc[1] + b.height)
        }

    fun show(context: Context) {
        if (button != null) return
        val wm = context.getSystemService(WindowManager::class.java) ?: return
        val b =
            Button(context).apply {
                text = context.getString(R.string.mitra_stop)
                setBackgroundColor(0xFFD32F2F.toInt())
                setTextColor(0xFFFFFFFF.toInt())
                setOnClickListener {
                    val startNs = System.nanoTime()
                    onStop()
                    val endNs = System.nanoTime()
                    val latencyMs = (endNs - startNs) / 1_000_000.0
                    Log.i("MitraStop", "OVERLAY_STOP_TAP: startNs=$startNs, endNs=$endNs, latencyMs=$latencyMs")
                }
            }
        val lp =
            WindowManager
                .LayoutParams(
                    WindowManager.LayoutParams.WRAP_CONTENT,
                    WindowManager.LayoutParams.WRAP_CONTENT,
                    WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
                    WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                    PixelFormat.TRANSLUCENT,
                ).apply {
                    gravity = Gravity.TOP or Gravity.END
                    y = 200
                }
        wm.addView(b, lp)
        button = b
        layoutParams = lp
    }

    fun avoid(
        targetBounds: Rect,
        context: Context,
    ) {
        val b = button ?: return
        val lp = layoutParams ?: return
        val cur = currentBounds ?: return
        if (cur.intersects(targetBounds)) {
            val wm = context.getSystemService(WindowManager::class.java) ?: return
            lp.gravity =
                if (lp.gravity and Gravity.END != 0) {
                    Gravity.TOP or Gravity.START
                } else {
                    Gravity.TOP or Gravity.END
                }
            wm.updateViewLayout(b, lp)
            Log.i("MitraStop", "OVERLAY_AVOID: Moved stop button to avoid bounds $targetBounds")
        }
    }

    fun hide() {
        button?.let {
            val wm = it.context.getSystemService(WindowManager::class.java)
            wm?.removeView(it)
        }
        button = null
        layoutParams = null
    }
}
