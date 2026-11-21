package com.mitra.app.debug

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
fun DebugSection(modifier: Modifier = Modifier) {
    // Zero-overhead no-op in release builds
}

suspend fun runModelBenchmark(context: Context): String = ""
