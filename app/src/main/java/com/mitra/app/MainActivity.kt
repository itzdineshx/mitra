package com.mitra.app

import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.mitra.engine.access.MitraAccessibilityService
import com.mitra.ui.MitraTheme

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
    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
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
            Button(onClick = { onTask(task) }, enabled = task.isNotBlank()) { Text(stringResource(R.string.home_go)) }
            OutlinedButton(onClick = { /* T07: speech input */ }) { Text(stringResource(R.string.home_mic)) }
        }
    }
}
