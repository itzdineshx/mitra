package com.mitra.model

/**
 * Detects device memory tiers with tolerance for OS/hardware reserved RAM.
 *
 * Advertised RAM vs realistic `ActivityManager.MemoryInfo.totalMem`:
 * - Advertised 6 GB+  reads ~5.4 - 5.8 GB -> Tier A (threshold: >= 5.2 GiB)
 * - Advertised 3-5 GB reads ~2.7 - 4.8 GB -> Tier B (threshold: >= 2.6 GiB)
 * - Advertised < 3 GB reads < 2.6 GB      -> Tier C (under 2.6 GiB)
 */
object DeviceTierDetector {
    // 5.2 GiB (5.2 * 1024^3)
    const val TIER_A_MIN_BYTES: Long = 5_583_457_485L

    // 2.6 GiB (2.6 * 1024^3)
    const val TIER_B_MIN_BYTES: Long = 2_791_728_742L

    fun detect(totalMemBytes: Long): DeviceTier =
        when {
            totalMemBytes >= TIER_A_MIN_BYTES -> DeviceTier.A
            totalMemBytes >= TIER_B_MIN_BYTES -> DeviceTier.B
            else -> DeviceTier.C
        }
}
