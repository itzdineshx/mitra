package com.mitra.app

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mitra.model.ModelDownloadState
import com.mitra.model.ModelDownloader
import com.mitra.ui.MitraColors
import com.mitra.ui.MitraShapes

@Composable
fun OnDeviceAiCard(
    modifier: Modifier = Modifier,
    onRunModelTest: () -> Unit = {},
) {
    val context = LocalContext.current
    val downloader = remember { ModelDownloader.instance }
    val downloadState by downloader.state.collectAsState()

    var wifiOnly by remember { mutableStateOf(false) }
    var showDeleteConfirm by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        downloader.checkStatus(context)
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text(stringResource(R.string.model_delete_dialog_title)) },
            text = { Text(stringResource(R.string.model_delete_dialog_body)) },
            confirmButton = {
                Button(
                    onClick = {
                        downloader.deleteModel(context)
                        showDeleteConfirm = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MitraColors.Danger),
                ) {
                    Text(stringResource(R.string.model_delete_confirm))
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) {
                    Text(stringResource(R.string.model_delete_cancel))
                }
            },
        )
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = MitraShapes.Card,
        colors = CardDefaults.cardColors(containerColor = MitraColors.Night),
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            // Header: Model name, subtitle, and Status Pill
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.model_name),
                        style =
                            MaterialTheme.typography.titleLarge.copy(
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                            ),
                    )
                    Text(
                        text = stringResource(R.string.model_desc),
                        style =
                            MaterialTheme.typography.bodyMedium.copy(
                                color = MitraColors.Faint,
                            ),
                    )
                }
                StatusBadge(downloadState = downloadState)
            }

            // Body depending on state
            when (val state = downloadState) {
                is ModelDownloadState.Downloading -> {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        LinearProgressIndicator(
                            progress = { state.progressFraction },
                            modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(4.dp)),
                            color = MitraColors.Cyan,
                            trackColor = Color(0xFF1E293B),
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            val downloadedMb = state.bytesDownloaded / (1024 * 1024)
                            val totalMb = state.totalBytes / (1024 * 1024)
                            Text(
                                text = "$downloadedMb MB / $totalMb MB (${state.progressPercent}%)",
                                color = Color.White,
                                fontSize = 13.sp,
                            )
                            val speedMb = String.format("%.1f", state.speedBytesPerSec / (1024 * 1024.0))
                            Text(
                                text = "$speedMb MB/s",
                                color = MitraColors.Cyan,
                                fontSize = 13.sp,
                            )
                        }

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.padding(top = 4.dp),
                        ) {
                            if (state.isPaused) {
                                Button(
                                    onClick = { downloader.resumeDownload(context, wifiOnly = wifiOnly) },
                                    colors = ButtonDefaults.buttonColors(containerColor = MitraColors.Blue),
                                ) {
                                    Text(stringResource(R.string.model_btn_resume))
                                }
                            } else {
                                OutlinedButton(
                                    onClick = { downloader.pauseDownload() },
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                                ) {
                                    Text(stringResource(R.string.model_btn_pause))
                                }
                            }

                            OutlinedButton(
                                onClick = { downloader.cancelDownload(context) },
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = MitraColors.Danger),
                            ) {
                                Text(stringResource(R.string.model_btn_cancel))
                            }
                        }
                    }
                }

                is ModelDownloadState.Verifying -> {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        LinearProgressIndicator(
                            modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(4.dp)),
                            color = MitraColors.Violet,
                            trackColor = Color(0xFF1E293B),
                        )
                        Text(
                            text = stringResource(R.string.model_status_verifying),
                            color = MitraColors.Warning,
                            fontSize = 13.sp,
                        )
                    }
                }

                is ModelDownloadState.NotDownloaded -> {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(vertical = 2.dp),
                        ) {
                            Checkbox(
                                checked = wifiOnly,
                                onCheckedChange = { wifiOnly = it },
                            )
                            Text(
                                text = stringResource(R.string.model_wifi_only),
                                color = Color.White,
                                fontSize = 13.sp,
                            )
                        }

                        Button(
                            onClick = { downloader.startDownload(context, wifiOnly = wifiOnly) },
                            colors = ButtonDefaults.buttonColors(containerColor = MitraColors.Blue),
                        ) {
                            Text(stringResource(R.string.model_btn_download))
                        }
                    }
                }

                is ModelDownloadState.Error -> {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = state.message,
                            color = MitraColors.Danger,
                            fontSize = 13.sp,
                        )

                        if (state.canRetry) {
                            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                Button(
                                    onClick = { downloader.startDownload(context, wifiOnly = wifiOnly) },
                                    colors = ButtonDefaults.buttonColors(containerColor = MitraColors.Blue),
                                ) {
                                    Text(stringResource(R.string.model_btn_download))
                                }
                                OutlinedButton(
                                    onClick = { downloader.cancelDownload(context) },
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                                ) {
                                    Text(stringResource(R.string.model_btn_cancel))
                                }
                            }
                        }
                    }
                }

                is ModelDownloadState.Ready -> {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Button(
                            onClick = onRunModelTest,
                            colors = ButtonDefaults.buttonColors(containerColor = MitraColors.Blue),
                            modifier = Modifier.weight(1f),
                        ) {
                            Text(stringResource(R.string.model_btn_test))
                        }

                        OutlinedButton(
                            onClick = { showDeleteConfirm = true },
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = MitraColors.Danger),
                        ) {
                            Text(stringResource(R.string.model_btn_delete))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StatusBadge(downloadState: ModelDownloadState) {
    val (bgColor, textColor, text) =
        when (downloadState) {
            is ModelDownloadState.Ready ->
                Triple(MitraColors.SuccessContainer, MitraColors.SuccessText, stringResource(R.string.model_status_ready))
            is ModelDownloadState.Downloading -> {
                if (downloadState.isPaused) {
                    Triple(MitraColors.WarningContainer, MitraColors.WarningText, stringResource(R.string.model_status_paused))
                } else {
                    Triple(MitraColors.BlueSoft, MitraColors.Blue, "${downloadState.progressPercent}%")
                }
            }
            is ModelDownloadState.Verifying ->
                Triple(MitraColors.WarningContainer, MitraColors.WarningText, stringResource(R.string.model_status_verifying))
            is ModelDownloadState.NotDownloaded ->
                Triple(Color(0xFF1E293B), MitraColors.Faint, stringResource(R.string.model_status_not_downloaded))
            is ModelDownloadState.Error ->
                Triple(MitraColors.DangerContainer, MitraColors.DangerText, stringResource(R.string.model_status_error))
        }

    Box(
        modifier =
            Modifier
                .clip(MitraShapes.Pill)
                .background(bgColor)
                .padding(horizontal = 12.dp, vertical = 6.dp),
    ) {
        Text(
            text = text,
            color = textColor,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
        )
    }
}
