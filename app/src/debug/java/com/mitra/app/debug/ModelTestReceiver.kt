package com.mitra.app.debug

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class ModelTestReceiver : BroadcastReceiver() {
    override fun onReceive(
        context: Context,
        intent: Intent?,
    ) {
        Log.i("MitraModel", "Received debug MODEL_TEST broadcast")
        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val result = runModelBenchmark(context)
                Log.i("MitraModel", "MODEL_TEST completed:\n$result")
            } catch (e: Throwable) {
                Log.e("MitraModel", "MODEL_TEST failed with exception", e)
            } finally {
                pendingResult.finish()
            }
        }
    }
}
