package com.mitra.engine

import kotlinx.serialization.json.Json
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class ScreenTest {
    @Test
    fun `snapshot survives a JSON round trip`() {
        val snap =
            ScreenSnapshot(
                foregroundPackage = "com.android.settings",
                elements =
                    listOf(
                        ScreenElement(
                            id = "e1",
                            role = "button",
                            text = "Display",
                            bounds = Rect(0, 0, 100, 50),
                            packageName = "com.android.settings",
                            windowType = "application",
                            provenance = Provenance.APP_LAYOUT,
                            clickable = true,
                        ),
                    ),
            )
        val json = Json.encodeToString(ScreenSnapshot.serializer(), snap)
        assertEquals(snap, Json.decodeFromString(ScreenSnapshot.serializer(), json))
    }

    @Test
    fun `only system and app layout are trusted`() {
        assertTrue(Provenance.SYSTEM_UI.isTrusted)
        assertTrue(Provenance.APP_LAYOUT.isTrusted)
        assertFalse(Provenance.WEB_CONTENT.isTrusted)
        assertFalse(Provenance.USER_CONTENT.isTrusted)
        assertFalse(Provenance.OVERLAY.isTrusted)
        assertFalse(Provenance.UNKNOWN.isTrusted)
    }

    @Test
    fun `rect intersection`() {
        assertTrue(Rect(0, 0, 10, 10).intersects(Rect(5, 5, 15, 15)))
        assertFalse(Rect(0, 0, 10, 10).intersects(Rect(10, 10, 20, 20)))
    }
}
