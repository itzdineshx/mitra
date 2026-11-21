package com.mitra.app.debug

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.mitra.engine.ScreenSnapshot

object ScreenDebugState {
    var lastSnapshot by mutableStateOf<ScreenSnapshot?>(null)
}
