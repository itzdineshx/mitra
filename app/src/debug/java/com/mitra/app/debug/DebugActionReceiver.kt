package com.mitra.app.debug

import android.app.Activity
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.provider.Settings
import android.util.Log
import com.mitra.app.AndroidConfirmer
import com.mitra.app.MainActivity
import com.mitra.data.audit.RoomAuditLog
import com.mitra.data.crypto.DatabaseKeyManager
import com.mitra.data.db.TaskEntity
import com.mitra.engine.Direction
import com.mitra.engine.GlobalAction
import com.mitra.engine.Provenance
import com.mitra.engine.Rect
import com.mitra.engine.ScreenElement
import com.mitra.engine.ScreenSnapshot
import com.mitra.engine.access.AccessibilityEngine
import com.mitra.engine.access.TaskRunner
import com.mitra.trust.ActionType
import com.mitra.trust.AgentRole
import com.mitra.trust.DeviceContext
import com.mitra.trust.ProposedAction
import com.mitra.trust.Risk
import com.mitra.trust.TaskScope
import com.mitra.trust.TrustEngine
import com.mitra.trust.TrustedExecutor
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
        val text = intent?.getStringExtra("text") ?: ""
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
                    "test_trust_executor" -> {
                        val db = DatabaseKeyManager.buildEncryptedDatabase(context)
                        val taskId = "task_${System.currentTimeMillis()}"
                        db.taskDao().insert(
                            TaskEntity(
                                id = taskId,
                                createdAt = System.currentTimeMillis(),
                                userText = "Send hi on WhatsApp",
                                source = "ORB",
                                status = "RUNNING",
                                summary = "testing trust engine",
                                scopeJson = "{}",
                                engine = "ACCESSIBILITY",
                            ),
                        )
                        val auditLog = RoomAuditLog(db)
                        val tts = AndroidTts(context)
                        val confirmer =
                            AndroidConfirmer(
                                context = context,
                                activityProvider = { MainActivity.currentActivity },
                                tts = tts,
                            )
                        val trustEngine = TrustEngine()
                        val dummyElement =
                            ScreenElement(
                                id = elementId,
                                role = "button",
                                text = "Send",
                                bounds = Rect(100, 100, 300, 200),
                                packageName = "com.whatsapp",
                                windowType = "application",
                                provenance = Provenance.APP_LAYOUT,
                                clickable = true,
                            )
                        val screen =
                            ScreenSnapshot(
                                foregroundPackage = "com.whatsapp",
                                elements = listOf(dummyElement),
                            )
                        val devContext =
                            DeviceContext(
                                foregroundPackage = "com.whatsapp",
                                screen = screen,
                                standingRuleCoversAction = true, // Standing rule covers so it proceeds to ALLOW and executes
                            )
                        val executor =
                            TrustedExecutor(
                                engine = engine,
                                trust = trustEngine,
                                confirmer = confirmer,
                                audit = auditLog,
                            ) { devContext }

                        val scope =
                            TaskScope(
                                taskId = taskId,
                                goal = "Send hi on WhatsApp",
                                allowedPackages = setOf("com.whatsapp"),
                                allowedActions = setOf(ActionType.TAP),
                                maxRisk = Risk.SEND,
                                expiresAtMillis = System.currentTimeMillis() + 600_000,
                            )
                        val proposed =
                            ProposedAction(
                                taskId = taskId,
                                step = 1,
                                agent = AgentRole.APP,
                                type = ActionType.TAP,
                                target = elementId,
                                evidence = listOf(elementId),
                                risk = Risk.READ,
                            )
                        val execResult = executor.execute(proposed, scope)
                        val steps = db.stepDao().getStepsForTaskSync(taskId)
                        val recorded = steps.firstOrNull()
                        resultMessage =
                            "TrustedExecutor executed: result=$execResult, recordedStepId=${recorded?.stepId}, rule=${recorded?.ruleId}, verdict=${recorded?.verdict}, actionJson=${recorded?.actionJson}"
                        Log.i("MitraTrust", "TRUST_AUDIT_LOG_SUCCESS: $resultMessage")
                    }
                    "dump_audit_log" -> {
                        val db = DatabaseKeyManager.buildEncryptedDatabase(context)
                        val allSteps =
                            if (text.isNotBlank() && text != "all" && text != "last") {
                                db.stepDao().getStepsForTaskSync(text)
                            } else {
                                db.stepDao().getRecentStepsSync()
                            }
                        resultMessage = "Found ${allSteps.size} steps in encrypted audit log"
                        allSteps.forEach { step ->
                            Log.i(
                                "MitraAudit",
                                "STEP_LOG: id=${step.stepId} task=${step.taskId} rule=${step.ruleId} verdict=${step.verdict} result=${step.result} action=${step.actionJson}",
                            )
                        }
                    }
                    "test_biometric" -> {
                        val tts = AndroidTts(context)
                        val confirmer =
                            AndroidConfirmer(
                                context = context,
                                activityProvider = { MainActivity.currentActivity },
                                tts = tts,
                            )
                        val ok = confirmer.biometric(text.ifBlank { "Authorize payment or security action" })
                        resultMessage = "Biometric prompt result: $ok"
                        Log.i("MitraTrust", "BIOMETRIC_RESULT: $resultMessage")
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
