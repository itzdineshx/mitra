package com.mitra.engine.access

import android.content.Context
import android.graphics.PixelFormat
import android.view.Gravity
import android.view.WindowManager
import android.widget.Button

/**
 * The red Stop button. Drawn as an accessibility overlay so it sits above every app.
 * Pressing it must stop the task within 300 ms (rule R1).
 */
object StopOverlay {
    @Volatile var onStop: () -> Unit = {}
    private var button: Button? = null

    fun show(context: Context) {
        if (button != null) return
        val wm = context.getSystemService(WindowManager::class.java)
        val b =
            Button(context).apply {
                text = context.getString(R.string.mitra_stop)
                setBackgroundColor(0xFFD32F2F.toInt())
                setTextColor(0xFFFFFFFF.toInt())
                setOnClickListener { onStop() }
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
    }

    fun hide() {
        button?.let { it.context.getSystemService(WindowManager::class.java).removeView(it) }
        button = null
    }
}
