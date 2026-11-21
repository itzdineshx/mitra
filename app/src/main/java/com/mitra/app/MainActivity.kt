package com.mitra.app

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.mitra.app.debug.DebugSection
import com.mitra.app.debug.runModelBenchmark
import com.mitra.engine.access.MitraAccessibilityService
import com.mitra.ui.MitraTheme
import com.mitra.voice.AndroidSpeechRecognizer
import com.mitra.voice.AndroidTts
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    private val serviceOn = mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MitraTheme {
                HomeScreen(
                    serviceOn = serviceOn.value,
                    onOpenSettings = { startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)) },
                    onTask = { /* T10: build TaskScope and run Orchestrator */ },
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()
        serviceOn.value = MitraAccessibilityService.instance != null
    }
}

@Composable
private fun HomeScreen(
    serviceOn: Boolean,
    onOpenSettings: () -> Unit,
    onTask: (String) -> Unit,
) {
    var task by remember { mutableStateOf("") }
    var transcript by remember { mutableStateOf<String?>(null) }
    var voiceReply by remember { mutableStateOf<String?>(null) }
    var isListening by remember { mutableStateOf(false) }
    val scrollState = rememberScrollState()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var isModelRunning by remember { mutableStateOf(false) }
    var modelResultText by remember { mutableStateOf<String?>(null) }

    val tts = remember { AndroidTts(context) }

    fun processVoiceGoal(
        goal: String,
        endSpeechNs: Long?,
    ) {
        transcript = goal
        task = goal
        scope.launch {
            if (goal.contains("torch", ignoreCase = true) || goal.contains("flashlight", ignoreCase = true)) {
                // Turn on torch (T04/T07 stub)
                val cm = context.getSystemService(Context.CAMERA_SERVICE) as CameraManager
                val cameraId =
                    cm.cameraIdList.firstOrNull { id ->
                        cm.getCameraCharacteristics(id).get(CameraCharacteristics.FLASH_INFO_AVAILABLE) == true
                    }
                if (cameraId != null) {
                    runCatching { cm.setTorchMode(cameraId, true) }
                }

                val ttsStartNs = System.nanoTime()
                if (endSpeechNs != null) {
                    val latencyMs = (ttsStartNs - endSpeechNs) / 1_000_000.0
                    Log.i("MitraVoice", "END_OF_SPEECH_TO_TTS_LATENCY: ${"%.2f".format(latencyMs)} ms")
                }
                voiceReply = "Torch is on"
                tts.speak("Torch is on", "en-IN")
            } else {
                voiceReply = "I heard: $goal"
                tts.speak("I heard: $goal", "en-IN")
            }
        }
    }

    fun startListening() {
        if (isListening) return
        isListening = true
        scope.launch {
            var endSpeechNs: Long? = null
            val recognizer =
                AndroidSpeechRecognizer(
                    context = context,
                    onPartialResult = { partial -> transcript = partial },
                    onEndOfSpeech = { endNs -> endSpeechNs = endNs },
                )
            val heard = recognizer.listen("en-IN")
            isListening = false
            if (!heard.isNullOrBlank()) {
                processVoiceGoal(heard, endSpeechNs)
            }
        }
    }

    val permissionLauncher =
        rememberLauncherForActivityResult(
            contract = ActivityResultContracts.RequestPermission(),
        ) { isGranted ->
            if (isGranted) {
                startListening()
            }
        }

    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .padding(24.dp)
                .verticalScroll(scrollState),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(stringResource(R.string.home_greeting), style = MaterialTheme.typography.headlineSmall)
        if (!serviceOn) {
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(stringResource(R.string.service_off_title), style = MaterialTheme.typography.titleMedium)
                    Text(stringResource(R.string.service_off_body))
                    Text(stringResource(R.string.restricted_hint), style = MaterialTheme.typography.bodySmall)
                    Button(onClick = onOpenSettings) { Text(stringResource(R.string.service_off_button)) }
                }
            }
        } else {
            Text(stringResource(R.string.service_on), color = MaterialTheme.colorScheme.secondary)
        }
        OutlinedTextField(
            value = task,
            onValueChange = { task = it },
            label = { Text(stringResource(R.string.home_hint)) },
            modifier = Modifier.fillMaxWidth(),
        )
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Button(
                onClick = {
                    onTask(task)
                    processVoiceGoal(task, null)
                },
                enabled = task.isNotBlank(),
            ) {
                Text(stringResource(R.string.home_go))
            }
            OutlinedButton(
                onClick = {
                    val hasPerm =
                        ContextCompat.checkSelfPermission(
                            context,
                            Manifest.permission.RECORD_AUDIO,
                        ) == PackageManager.PERMISSION_GRANTED
                    if (hasPerm) {
                        startListening()
                    } else {
                        permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                    }
                },
            ) {
                Text(if (isListening) "Listening…" else stringResource(R.string.home_mic))
            }
        }

        transcript?.let { t ->
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("Transcript:", style = MaterialTheme.typography.labelMedium)
                    Text("\"$t\"", style = MaterialTheme.typography.bodyMedium)
                    voiceReply?.let { r ->
                        Text("Mitra replied:", style = MaterialTheme.typography.labelMedium)
                        Text("\"$r\"", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary)
                    }
                }
            }
        }

        OnDeviceAiCard(
            onRunModelTest = {
                if (isModelRunning) return@OnDeviceAiCard
                isModelRunning = true
                scope.launch {
                    modelResultText = runModelBenchmark(context)
                    isModelRunning = false
                }
            },
        )
        modelResultText?.let {
            Card(Modifier.fillMaxWidth()) {
                Text(
                    text = it,
                    modifier = Modifier.padding(16.dp),
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        }
        DebugSection()
    }
}
