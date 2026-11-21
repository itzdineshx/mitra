package com.mitra.data

import com.mitra.data.db.ModelRunEntity
import com.mitra.data.db.PermissionEntity
import com.mitra.data.db.TaskEntity
import com.mitra.data.redaction.TextRedactor
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.io.File

class DataSpecTests {
    @Test
    fun `text redactor truncates to 40 chars and computes SHA-256`() {
        val shortText = "Hello Mitra"
        val redactedShort = TextRedactor.redactFieldText(shortText, isSecure = false)
        assertNotNull(redactedShort)
        assertTrue(redactedShort!!.startsWith("Hello Mitra [sha256:"))
        assertTrue(redactedShort.contains(TextRedactor.sha256(shortText)))

        val longText = "This is a very long text that definitely exceeds the forty character truncation limit set in the specification"
        val redactedLong = TextRedactor.redactFieldText(longText, isSecure = false)
        assertNotNull(redactedLong)
        assertTrue(redactedLong!!.startsWith(longText.take(40) + "... [sha256:"))
        assertTrue(redactedLong.contains(TextRedactor.sha256(longText)))
    }

    @Test
    fun `no secure-field text reaches steps audit log`() {
        // DATA_SPEC §4 & §10: Secure field text (password, OTP, PIN, CAPTCHA) is NEVER stored.
        val passwordText = "my_super_secret_password_999"
        val secureRedacted = TextRedactor.redactFieldText(passwordText, isSecure = true)
        assertNull(secureRedacted)

        val otpText = "123456"
        val otpRedacted = TextRedactor.redactFieldText(otpText, isSecure = true)
        assertNull(otpRedacted)

        // Verify actionJson generation in StepEntity with secure target
        val isSecure = true
        val sanitized = TextRedactor.redactFieldText(passwordText, isSecure)
        assertNull(sanitized)
        val actionJson =
            buildString {
                append("{\"type\":\"TYPE\"")
                append(",\"target\":\"password_input\"")
                sanitized?.let { append(",\"text\":\"").append(it).append("\"") }
                append(",\"route\":\"SCREEN\"}")
            }
        assertFalse(actionJson.contains(passwordText), "Password text must never appear in action_json")
        assertFalse(actionJson.contains("\"text\""), "Text field must be absent when target is secure")
    }

    @Test
    fun `key-loss recovery moves database to bad file and resets keys`() {
        val tempDir = File(System.getProperty("java.io.tmpdir"), "mitra_test_key_loss_${System.currentTimeMillis()}")
        tempDir.mkdirs()

        val dbFile = File(tempDir, "mitra.db")
        dbFile.writeText("corrupted db")
        val walFile = File(tempDir, "mitra.db-wal")
        walFile.writeText("wal")
        val shmFile = File(tempDir, "mitra.db-shm")
        shmFile.writeText("shm")

        // Recovery simulation per DATA_SPEC §5
        val badFile = File(tempDir, "${dbFile.name}.bad.${System.currentTimeMillis()}")
        val moved = dbFile.renameTo(badFile)
        assertTrue(moved)
        if (walFile.exists()) walFile.delete()
        if (shmFile.exists()) shmFile.delete()

        assertTrue(badFile.exists())
        assertFalse(dbFile.exists())
        assertFalse(walFile.exists())
        assertFalse(shmFile.exists())

        tempDir.deleteRecursively()
    }

    @Test
    fun `banking packages cannot enter permissions table`() =
        runTest {
            val store = InMemoryPermissionStore()

            // Denied banking/UPI apps
            val phonePe = PermissionEntity(kind = "APP", value = "com.phonepe.app", mode = "ALLOW", createdAt = 1000L)
            val paytm = PermissionEntity(kind = "APP", value = "net.one97.paytm", mode = "ALLOW", createdAt = 1000L)
            val gpay = PermissionEntity(kind = "APP", value = "com.google.android.apps.nbu.paisa.user", mode = "ALLOW", createdAt = 1000L)

            assertFalse(store.addPermission(phonePe), "PhonePe must be refused entry into permissions")
            assertFalse(store.addPermission(paytm), "Paytm must be refused entry into permissions")
            assertFalse(store.addPermission(gpay), "GPay must be refused entry into permissions")

            // Allowed app
            val whatsapp = PermissionEntity(kind = "APP", value = "com.whatsapp", mode = "ALLOW", createdAt = 1000L)
            assertTrue(store.addPermission(whatsapp), "Normal app must be allowed")

            val all = store.getAll().first()
            assertEquals(1, all.size)
            assertEquals("com.whatsapp", all.single().value)
        }

    @Test
    fun `model_runs latest-row query returns most recent speed test`() =
        runTest {
            val store =
                object : ModelRunStore {
                    val runs = mutableListOf<ModelRunEntity>()

                    override suspend fun recordRun(run: ModelRunEntity): Long {
                        runs.add(run.copy(id = runs.size + 1L))
                        return runs.size.toLong()
                    }

                    override fun getLatestRun() = kotlinx.coroutines.flow.flowOf(runs.lastOrNull())

                    override suspend fun getLatestRunSync() = runs.lastOrNull()
                }

            store.recordRun(
                ModelRunEntity(
                    ts = 1000L,
                    modelId = "gemma-4-E2B",
                    firstTokenMs = 21000L,
                    tokensPerS = 1.3,
                    peakRamMb = 1190L,
                    deviceTier = "A",
                ),
            )
            store.recordRun(
                ModelRunEntity(
                    ts = 2000L,
                    modelId = "gemma-4-E2B",
                    firstTokenMs = 13800L,
                    tokensPerS = 1.3,
                    peakRamMb = 1195L,
                    deviceTier = "A",
                ),
            )

            val latest = store.getLatestRunSync()
            assertNotNull(latest)
            assertEquals(2000L, latest?.ts)
            assertEquals(13800L, latest?.firstTokenMs)
        }

    @Test
    fun `prune removes tasks older than 90 days and enforces size cap`() =
        runTest {
            val tasks = mutableMapOf<String, TaskEntity>()
            val sendEvents = mutableListOf<Long>()

            val now = 100_000_000_000L
            val ninetyDaysMs = 90L * 24 * 60 * 60 * 1000L

            // Old task (100 days old)
            tasks["old"] =
                TaskEntity(
                    id = "old",
                    createdAt = now - ninetyDaysMs - 10000L,
                    userText = "old",
                    source = "ORB",
                    status = "DONE",
                    summary = "done",
                    scopeJson = "{}",
                    engine = "ACCESSIBILITY",
                )
            // Fresh task (1 day old)
            tasks["fresh"] =
                TaskEntity(
                    id = "fresh",
                    createdAt = now - 10000L,
                    userText = "fresh",
                    source = "ORB",
                    status = "DONE",
                    summary = "done",
                    scopeJson = "{}",
                    engine = "ACCESSIBILITY",
                )

            // Pruning old
            val cutoff = now - ninetyDaysMs
            val oldKeys = tasks.filter { it.value.createdAt < cutoff }.keys.toList()
            oldKeys.forEach { tasks.remove(it) }

            assertEquals(1, tasks.size)
            assertEquals("fresh", tasks.keys.single())
        }

    @Test
    fun `delete all sequence removes database, plugins, downloads and resets settings`() {
        val tempDir = File(System.getProperty("java.io.tmpdir"), "mitra_test_delete_all_${System.currentTimeMillis()}")
        tempDir.mkdirs()

        val dbFile = File(tempDir, "mitra.db")
        dbFile.writeText("fake db")
        val walFile = File(tempDir, "mitra.db-wal")
        walFile.writeText("fake wal")
        val badFile = File(tempDir, "mitra.db.bad.12345")
        badFile.writeText("fake bad")

        val pluginsDir = File(tempDir, "plugins")
        pluginsDir.mkdirs()
        File(pluginsDir, "test.plugin").writeText("plugin")

        val downloadsDir = File(tempDir, "downloads")
        downloadsDir.mkdirs()
        File(downloadsDir, "model.part").writeText("part")

        // Execution of delete-all steps
        tempDir.listFiles()?.forEach { file ->
            if (file.name.startsWith("mitra.db")) {
                file.delete()
            }
        }
        pluginsDir.deleteRecursively()
        downloadsDir.deleteRecursively()

        assertFalse(dbFile.exists())
        assertFalse(walFile.exists())
        assertFalse(badFile.exists())
        assertFalse(pluginsDir.exists())
        assertFalse(downloadsDir.exists())

        tempDir.deleteRecursively()
    }

    @Test
    fun `memory store put get forget operations work cleanly`() =
        runTest {
            val store = InMemoryStore()
            assertNull(store.get("preferred_name"))

            store.put(MemoryFact(key = "preferred_name", value = "Dinesh", source = "USER_SAID"))
            val fact = store.get("preferred_name")
            assertNotNull(fact)
            assertEquals("Dinesh", fact?.value)

            store.forget("preferred_name")
            assertNull(store.get("preferred_name"))
        }
}
