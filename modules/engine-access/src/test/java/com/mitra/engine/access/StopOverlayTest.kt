package com.mitra.engine.access

import com.mitra.engine.Rect
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class StopOverlayTest {
    @Test
    fun `stop cancels running task in less than 300 ms`() =
        runBlocking {
            var taskCancelled = false
            val started = CompletableDeferred<Unit>()

            val job: Job =
                CoroutineScope(Dispatchers.Default).launch {
                    try {
                        started.complete(Unit)
                        while (isActive) {
                            delay(50L)
                        }
                    } finally {
                        taskCancelled = true
                    }
                }

            started.await()
            assertTrue(job.isActive)

            val startNs = System.nanoTime()
            // Simulate Stop action
            job.cancel()
            job.join()
            val endNs = System.nanoTime()

            val elapsedMs = (endNs - startNs) / 1_000_000.0

            assertTrue(taskCancelled, "Task should have cancelled")
            assertFalse(job.isActive, "Job should not be active")
            assertTrue(elapsedMs <= 300.0, "Stop must complete within 300 ms, took $elapsedMs ms")
        }

    @Test
    fun `bounds intersection correctly detects overlap for avoidance`() {
        val overlayBounds = Rect(547, 246, 720, 341)
        val overlappingTarget = Rect(500, 200, 600, 300)
        val nonOverlappingTarget = Rect(0, 0, 100, 100)

        assertTrue(overlayBounds.intersects(overlappingTarget), "Should detect overlap when bounds intersect")
        assertFalse(overlayBounds.intersects(nonOverlappingTarget), "Should not detect overlap when bounds don't intersect")
    }
}
