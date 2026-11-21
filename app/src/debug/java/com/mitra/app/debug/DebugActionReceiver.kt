package com.mitra.app.debug

import android.app.Activity
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.provider.Settings
import android.util.Log
import com.mitra.engine.Direction
import com.mitra.engine.GlobalAction
import com.mitra.engine.access.AccessibilityEngine
import com.mitra.engine.access.TaskRunner
import com.mitra.voice.AndroidTts
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * Debug-only action receiver for executing and verifying accessibility actions over ADB (T04).
 * Usage:
 * adb shell am broadcast -a com.mitra.app.debug.ACTION -n com.mitra.app/.debug.DebugActionReceiver --es action <action>
 */
class DebugActionReceiver : BroadcastReceiver() {
    companion object {
        @Volatile
        var torchEnabled: Boolean = false
    }

    override fun onReceive(
        context: Context,
        intent: Intent?,
    ) {
        val action = intent?.getStringExtra("action") ?: "help"
        val elementId = intent?.getStringExtra("elementId") ?: "e0"
        val text = intent?.getStringExtra("text") ?: "Mitra Test"
        val pendingResult = goAsync()

        CoroutineScope(Dispatchers.Main).launch {
            try {
                val engine = AccessibilityEngine(context)
                var resultMessage = "Action '$action' executed"

                when (action) {
                    "quick_settings" -> {
                        val res = engine.globalAction(GlobalAction.QUICK_SETTINGS)
                        resultMessage = "quick_settings result=$res"
                    }
                    "home" -> {
                        val res = engine.globalAction(GlobalAction.HOME)
                        resultMessage = "home result=$res"
                    }
                    "back" -> {
                        val res = engine.globalAction(GlobalAction.BACK)
                        resultMessage = "back result=$res"
                    }
                    "settings_display" -> {
                        val i = Intent(Settings.ACTION_DISPLAY_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        context.startActivity(i)
                        resultMessage = "launched Settings Display"
                    }
                    "torch" -> {
                        val cm = context.getSystemService(Context.CAMERA_SERVICE) as CameraManager
                        val cameraId =
                            cm.cameraIdList.firstOrNull { id ->
                                cm.getCameraCharacteristics(id).get(CameraCharacteristics.FLASH_INFO_AVAILABLE) == true
                            }
                        if (cameraId != null) {
                            torchEnabled = !torchEnabled
                            cm.setTorchMode(cameraId, torchEnabled)
                            resultMessage = "torch set to $torchEnabled"
                        } else {
                            resultMessage = "torch not available"
                        }
                    }
                    "torch_on" -> {
                        val cm = context.getSystemService(Context.CAMERA_SERVICE) as CameraManager
                        val cameraId =
                            cm.cameraIdList.firstOrNull { id ->
                                cm.getCameraCharacteristics(id).get(CameraCharacteristics.FLASH_INFO_AVAILABLE) == true
                            }
                        if (cameraId != null) {
                            torchEnabled = true
                            cm.setTorchMode(cameraId, true)
                            resultMessage = "torch set to true"
                        } else {
                            resultMessage = "torch not available"
                        }
                    }
                    "torch_off" -> {
                        val cm = context.getSystemService(Context.CAMERA_SERVICE) as CameraManager
                        val cameraId =
                            cm.cameraIdList.firstOrNull { id ->
                                cm.getCameraCharacteristics(id).get(CameraCharacteristics.FLASH_INFO_AVAILABLE) == true
                            }
                        if (cameraId != null) {
                            torchEnabled = false
                            cm.setTorchMode(cameraId, false)
                            resultMessage = "torch set to false"
                        } else {
                            resultMessage = "torch not available"
                        }
                    }
                    "tap" -> {
                        val res = engine.tap(elementId)
                        resultMessage = "tap element $elementId result=$res"
                    }
                    "type" -> {
                        val res = engine.type(elementId, text)
                        resultMessage = "type into $elementId result=$res"
                    }
                    "scroll" -> {
                        val res = engine.scroll(Direction.DOWN, elementId.takeIf { it.isNotBlank() })
                        resultMessage = "scroll down result=$res"
                    }
                    "start_fake_task" -> {
                        TaskRunner.startTask(context, engine) {
                            Log.i("MitraStop", "FAKE_TASK_STARTED")
                            var i = 0
                            while (isActive && i < 200) {
                                delay(100L)
                                i++
                            }
                            Log.i("MitraStop", "FAKE_TASK_ENDED")
                        }
                        resultMessage = "fake task started"
                    }
                    "stop_task" -> {
                        val elapsedNs = TaskRunner.cancelTask("debug action stop")
                        val ms = elapsedNs / 1_000_000.0
                        resultMessage = "task stopped in ${"%.2f".format(ms)} ms"
                    }
                    "simulate_voice" -> {
                        val voiceInput = text.ifBlank { "Turn on the torch" }
                        val endSpeechNs = System.nanoTime()
                        val cm = context.getSystemService(Context.CAMERA_SERVICE) as CameraManager
                        val cameraId =
                            cm.cameraIdList.firstOrNull { id ->
                                cm.getCameraCharacteristics(id).get(CameraCharacteristics.FLASH_INFO_AVAILABLE) == true
                            }
                        if (cameraId != null) {
                            torchEnabled = true
                            cm.setTorchMode(cameraId, true)
                        }
                        val ttsStartNs = System.nanoTime()
                        val latencyMs = (ttsStartNs - endSpeechNs) / 1_000_000.0
                        Log.i("MitraVoice", "END_OF_SPEECH_TO_TTS_LATENCY: ${"%.2f".format(latencyMs)} ms")
                        val tts = AndroidTts(context)
                        tts.speak("Torch is on", "en-IN")
                        resultMessage = "Simulated voice '$voiceInput' -> TTS 'Torch is on' in ${"%.2f".format(latencyMs)} ms"
                    }
                    else -> {
                        resultMessage =
                            "Unknown action '$action'. Available: quick_settings, home, back, settings_display, torch, torch_on, torch_off, tap, type, scroll"
                    }
                }

                Log.i("MitraAction", "ACTION_EXEC: $resultMessage")
                pendingResult.setResultCode(Activity.RESULT_OK)
                pendingResult.setResultData(resultMessage)
            } catch (t: Throwable) {
                Log.e("MitraAction", "ACTION_FAIL: ${t.message}", t)
                pendingResult.setResultCode(Activity.RESULT_CANCELED)
                pendingResult.setResultData("Error: ${t.message}")
            } finally {
                pendingResult.finish()
            }
        }
    }
}
