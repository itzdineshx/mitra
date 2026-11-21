package com.mitra.data.eraser

import android.content.Context
import com.mitra.data.crypto.DatabaseKeyManager
import com.mitra.data.db.MitraDatabase
import com.mitra.data.settings.SettingsStore
import java.io.File

class DataEraser(
    private val db: MitraDatabase?,
    private val settingsStore: SettingsStore?,
) {
    /**
     * Delete all sequence per docs/DATA_SPEC.md §6:
     * 1. Close database.
     * 2. Delete mitra.db, -wal, -shm, .bad files.
     * 3. Delete Keystore alias and wrapped passphrase.
     * 4. Delete plugins/, cacheDir/downloads/, and DataStore preferences.
     * 5. If removeModel == true, delete models/.
     */
    suspend fun eraseAll(
        context: Context,
        removeModel: Boolean = false,
    ) {
        // 1. Close database
        if (db != null && db.isOpen) {
            db.close()
        }

        // 2. Delete database files
        val dbFile = context.getDatabasePath("mitra.db")
        val dbDir = dbFile.parentFile
        if (dbDir != null && dbDir.exists()) {
            dbDir.listFiles()?.forEach { file ->
                if (file.name.startsWith("mitra.db")) {
                    file.delete()
                }
            }
        }

        // 3. Delete Keystore key & passphrase
        DatabaseKeyManager.deleteKeyAndPassphrase(context)

        // 4. Delete plugins, downloads, datastore
        File(context.filesDir, "plugins").deleteRecursively()
        File(context.cacheDir, "downloads").deleteRecursively()
        settingsStore?.clear()

        // 5. Remove models if requested
        if (removeModel) {
            File(context.filesDir, "models").deleteRecursively()
        }
    }
}
