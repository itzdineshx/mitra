package com.mitra.app

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.mitra.engine.access.TaskRunner

class StopActionReceiver : BroadcastReceiver() {
    override fun onReceive(
        context: Context,
        intent: Intent?,
    ) {
        TaskRunner.cancelTask("notification stop pressed")
    }
}
