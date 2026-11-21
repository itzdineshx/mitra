package com.mitra.model

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class DeviceTierTest {
    @Test
    fun `realistic 6 GB phone readings classify as Tier A`() {
        // Advertised 6 GB phone: reads ~5.4 - 5.8 GB
        val reading56Gb = (5.6 * 1024 * 1024 * 1024).toLong()
        val reading54Gb = (5.4 * 1024 * 1024 * 1024).toLong()
        val reading80Gb = (7.5 * 1024 * 1024 * 1024).toLong()

        assertEquals(DeviceTier.A, DeviceTierDetector.detect(reading56Gb))
        assertEquals(DeviceTier.A, DeviceTierDetector.detect(reading54Gb))
        assertEquals(DeviceTier.A, DeviceTierDetector.detect(reading80Gb))
    }

    @Test
    fun `realistic 4 GB and 3 GB phone readings classify as Tier B`() {
        // Advertised 4 GB phone: reads ~3.6 - 3.8 GB
        val reading37Gb = (3.7 * 1024 * 1024 * 1024).toLong()
        // Advertised 3 GB phone: reads ~2.7 - 2.9 GB
        val reading28Gb = (2.8 * 1024 * 1024 * 1024).toLong()

        assertEquals(DeviceTier.B, DeviceTierDetector.detect(reading37Gb))
        assertEquals(DeviceTier.B, DeviceTierDetector.detect(reading28Gb))
    }

    @Test
    fun `phone with under 2 point 6 GB available classifies as Tier C`() {
        // Advertised 2 GB phone: reads ~1.8 - 2.2 GB
        val reading22Gb = (2.2 * 1024 * 1024 * 1024).toLong()
        val reading15Gb = (1.5 * 1024 * 1024 * 1024).toLong()

        assertEquals(DeviceTier.C, DeviceTierDetector.detect(reading22Gb))
        assertEquals(DeviceTier.C, DeviceTierDetector.detect(reading15Gb))
    }
}
