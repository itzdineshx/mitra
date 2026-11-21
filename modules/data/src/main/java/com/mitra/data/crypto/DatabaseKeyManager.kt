package com.mitra.data.crypto

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import androidx.room.Room
import com.mitra.data.db.MitraDatabase
import net.zetetic.database.sqlcipher.SupportOpenHelperFactory
import java.io.File
import java.security.KeyStore
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/**
 * Android Keystore wrapper for database encryption key and key-loss recovery (docs/DATA_SPEC.md §5).
 */
object DatabaseKeyManager {
    private const val KEYSTORE_PROVIDER = "AndroidKeyStore"
    private const val KEY_ALIAS = "mitra_db_key"
    private const val GCM_IV_LENGTH = 12
    private const val GCM_TAG_LENGTH_BITS = 128
    private const val PASSPHRASE_FILE = "db_passphrase.bin"

    fun getOrCreatePassphrase(context: Context): ByteArray {
        val passFile = File(context.noBackupFilesDir, PASSPHRASE_FILE)
        if (passFile.exists()) {
            return try {
                decryptPassphrase(passFile.readBytes())
            } catch (e: Exception) {
                // Key unreadable: re-generate
                val newPass = generateRandomBytes(32)
                passFile.writeBytes(encryptPassphrase(newPass))
                newPass
            }
        }
        val newPass = generateRandomBytes(32)
        passFile.parentFile?.mkdirs()
        passFile.writeBytes(encryptPassphrase(newPass))
        return newPass
    }

    fun deleteKeyAndPassphrase(context: Context) {
        val passFile = File(context.noBackupFilesDir, PASSPHRASE_FILE)
        if (passFile.exists()) {
            passFile.delete()
        }
        try {
            val ks = KeyStore.getInstance(KEYSTORE_PROVIDER).apply { load(null) }
            if (ks.containsAlias(KEY_ALIAS)) {
                ks.deleteEntry(KEY_ALIAS)
            }
        } catch (_: Exception) {
        }
    }

    fun buildEncryptedDatabase(
        context: Context,
        dbName: String = "mitra.db",
        onKeyLoss: (() -> Unit)? = null,
    ): MitraDatabase {
        cleanOldBadFiles(context)
        val dbFile = context.getDatabasePath(dbName)
        val passphrase = getOrCreatePassphrase(context)
        val factory = SupportOpenHelperFactory(passphrase)

        return try {
            val db =
                Room
                    .databaseBuilder(context, MitraDatabase::class.java, dbName)
                    .openHelperFactory(factory)
                    .build()
            // Force an initial query to verify decryptability
            db.openHelper.writableDatabase
            db
        } catch (e: Exception) {
            // Key loss or unreadable recovery per DATA_SPEC.md §5:
            // Rename to mitra.db.bad, reset key/passphrase, notify
            recoverCorruptDatabase(context, dbFile)
            onKeyLoss?.invoke()

            val freshPass = getOrCreatePassphrase(context)
            val freshFactory = SupportOpenHelperFactory(freshPass)
            Room
                .databaseBuilder(context, MitraDatabase::class.java, dbName)
                .openHelperFactory(freshFactory)
                .build()
        }
    }

    private fun recoverCorruptDatabase(
        context: Context,
        dbFile: File,
    ) {
        if (dbFile.exists()) {
            val badFile = File(dbFile.parentFile, "${dbFile.name}.bad.${System.currentTimeMillis()}")
            dbFile.renameTo(badFile)
            val wal = File("${dbFile.path}-wal")
            if (wal.exists()) wal.delete()
            val shm = File("${dbFile.path}-shm")
            if (shm.exists()) shm.delete()
        }
        deleteKeyAndPassphrase(context)
    }

    private fun cleanOldBadFiles(context: Context) {
        val dbDir = context.getDatabasePath("mitra.db").parentFile ?: return
        val sevenDaysAgo = System.currentTimeMillis() - (7 * 86_400_000L)
        dbDir.listFiles()?.forEach { file ->
            if (file.name.contains(".bad") && file.lastModified() < sevenDaysAgo) {
                file.delete()
            }
        }
    }

    private fun getOrCreateKey(): SecretKey {
        val ks = KeyStore.getInstance(KEYSTORE_PROVIDER).apply { load(null) }
        if (!ks.containsAlias(KEY_ALIAS)) {
            val keyGen = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, KEYSTORE_PROVIDER)
            keyGen.init(
                KeyGenParameterSpec
                    .Builder(
                        KEY_ALIAS,
                        KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT,
                    ).setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                    .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                    .setKeySize(256)
                    .build(),
            )
            return keyGen.generateKey()
        }
        return (ks.getEntry(KEY_ALIAS, null) as KeyStore.SecretKeyEntry).secretKey
    }

    private fun encryptPassphrase(plain: ByteArray): ByteArray {
        val key = getOrCreateKey()
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, key)
        val iv = cipher.iv
        val encrypted = cipher.doFinal(plain)
        return iv + encrypted
    }

    private fun decryptPassphrase(blob: ByteArray): ByteArray {
        val iv = blob.copyOfRange(0, GCM_IV_LENGTH)
        val ciphertext = blob.copyOfRange(GCM_IV_LENGTH, blob.size)
        val key = getOrCreateKey()
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.DECRYPT_MODE, key, GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv))
        return cipher.doFinal(ciphertext)
    }

    private fun generateRandomBytes(size: Int): ByteArray {
        val bytes = ByteArray(size)
        SecureRandom().nextBytes(bytes)
        return bytes
    }
}
