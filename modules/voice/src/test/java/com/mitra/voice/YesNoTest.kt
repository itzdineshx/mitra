package com.mitra.voice

import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class YesNoTest {
    @Test
    fun `isYes returns true for all standard affirmative responses`() {
        val affirmativeCases = listOf("yes", "yeah", "yep", "ok", "okay", "sure", "go ahead", "do it", "confirm")
        for (case in affirmativeCases) {
            assertTrue(YesNo.isYes(case), "Expected true for '$case'")
            assertTrue(YesNo.isYes(" $case. "), "Expected true for padded/punctuated ' $case. '")
            assertTrue(YesNo.isYes(case.uppercase()), "Expected true for uppercase '${case.uppercase()}'")
        }
    }

    @Test
    fun `isYes returns false for negative and ambiguous responses`() {
        val negativeCases = listOf("no", "nope", "never", "cancel", "stop", "wait", "not now", "maybe", "", "   ")
        for (case in negativeCases) {
            assertFalse(YesNo.isYes(case), "Expected false for '$case'")
        }
        assertFalse(YesNo.isYes(null), "Expected false for null")
    }
}
