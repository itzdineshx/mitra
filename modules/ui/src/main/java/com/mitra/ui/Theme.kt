package com.mitra.ui

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/**
 * Design tokens from docs/UI_SPEC.md §2 (the "aurora" look).
 * Use these, never ad-hoc colours. Logo and brand files are in /brand.
 */
object MitraColors {
    // Brand
    val Blue = Color(0xFF2F5BFF) // primary
    val BlueSoft = Color(0xFFEAF0FF) // selected tab, tonal fills, how-to boxes
    val Violet = Color(0xFF7C5CFF)
    val Cyan = Color(0xFF22D3EE)
    val Night = Color(0xFF101828)

    // Neutrals
    val Ink = Color(0xFF101828) // titles and body
    val Muted = Color(0xFF667085) // subtitles, helper text
    val Faint = Color(0xFF98A2B3) // captions, inactive icons
    val Line = Color(0xFFEAECF0) // hairlines between list rows
    val Tonal = Color(0xFFF2F4F7) // quoted message, inputs, secondary buttons
    val Background = Color(0xFFF6F7FB) // app pages
    val Surface = Color(0xFFFFFFFF) // cards and sheets

    // Status (container = tint behind the icon, text = readable on a tint)
    val Success = Color(0xFF12B76A)
    val SuccessContainer = Color(0xFFE7F8EF)
    val SuccessText = Color(0xFF067647)
    val Warning = Color(0xFFF79009)
    val WarningContainer = Color(0xFFFEF0C7)
    val WarningText = Color(0xFFB54708)
    val Danger = Color(0xFFF04438) // Stop button square
    val DangerContainer = Color(0xFFFEE4E2)
    val DangerText = Color(0xFFB42318)
}

/** The Mitra orb / gradient: blue → violet → cyan. Used for the orb, progress and the launcher icon. */
object MitraBrush {
    val Aurora: Brush =
        Brush.linearGradient(listOf(MitraColors.Blue, MitraColors.Violet, MitraColors.Cyan))
    val Night: Brush =
        Brush.verticalGradient(listOf(Color(0xFF1B2250), Color(0xFF0B1020)))
}

/** Corner radii (UI_SPEC §2). Cards 24, buttons 20, pills fully rounded. */
object MitraShapes {
    val Card = RoundedCornerShape(24.dp)
    val Sheet = RoundedCornerShape(topStart = 36.dp, topEnd = 36.dp, bottomStart = 36.dp, bottomEnd = 36.dp)
    val Button = RoundedCornerShape(20.dp)
    val Pill = RoundedCornerShape(percent = 50)
}

/** Status colours that Material 3 has no slot for (Done / Waiting / Blocked). */
@Immutable
data class StatusColors(
    val success: Color = MitraColors.Success,
    val successContainer: Color = MitraColors.SuccessContainer,
    val successText: Color = MitraColors.SuccessText,
    val warning: Color = MitraColors.Warning,
    val warningContainer: Color = MitraColors.WarningContainer,
    val warningText: Color = MitraColors.WarningText,
    val danger: Color = MitraColors.Danger,
    val dangerContainer: Color = MitraColors.DangerContainer,
    val dangerText: Color = MitraColors.DangerText,
    val textMuted: Color = MitraColors.Muted,
)

val LocalStatusColors = staticCompositionLocalOf { StatusColors() }

private val LightScheme =
    lightColorScheme(
        primary = MitraColors.Blue,
        onPrimary = Color.White,
        primaryContainer = MitraColors.BlueSoft,
        onPrimaryContainer = MitraColors.Blue,
        secondary = MitraColors.Violet,
        tertiary = MitraColors.Cyan,
        background = MitraColors.Background,
        onBackground = MitraColors.Ink,
        surface = MitraColors.Surface,
        onSurface = MitraColors.Ink,
        surfaceVariant = MitraColors.Tonal,
        onSurfaceVariant = MitraColors.Muted,
        outline = MitraColors.Faint,
        outlineVariant = MitraColors.Line,
        error = MitraColors.DangerText,
        errorContainer = MitraColors.DangerContainer,
    )

private val MitraMaterialShapes =
    Shapes(
        small = RoundedCornerShape(12.dp),
        medium = MitraShapes.Button,
        large = MitraShapes.Card,
    )

@Composable
fun MitraTheme(content: @Composable () -> Unit) {
    CompositionLocalProvider(LocalStatusColors provides StatusColors()) {
        MaterialTheme(colorScheme = LightScheme, shapes = MitraMaterialShapes, content = content)
    }
}
