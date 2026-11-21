package com.mitra.data.prune

import com.mitra.data.db.MitraDatabase
import java.io.File

class DatabasePruner(
    private val db: MitraDatabase,
) {
    /**
     * Prunes database per docs/DATA_SPEC.md §6:
     * 1. Delete tasks (and steps via cascade) older than 90 days.
     * 2. If database file is larger than 100 MB, delete oldest tasks until size <= 100 MB.
     * 3. Delete send_events older than 24h.
     */
    suspend fun prune(
        dbFile: File?,
        maxAgeDays: Long = 90,
        maxSizeBytes: Long = 100 * 1024 * 1024,
        nowMillis: Long = System.currentTimeMillis(),
    ): Int {
        val cutoff = nowMillis - (maxAgeDays * 24 * 60 * 60 * 1000L)
        var deleted = db.taskDao().deleteOlderThan(cutoff)

        val sendCutoff = nowMillis - (24 * 60 * 60 * 1000L)
        db.sendEventDao().deleteOlderThan(sendCutoff)

        if (dbFile != null && dbFile.exists() && dbFile.length() > maxSizeBytes) {
            val oldest = db.taskDao().getOldestTasks()
            for (task in oldest) {
                if (dbFile.length() <= maxSizeBytes) break
                db.taskDao().deleteById(task.id)
                deleted++
            }
        }
        return deleted
    }
}
