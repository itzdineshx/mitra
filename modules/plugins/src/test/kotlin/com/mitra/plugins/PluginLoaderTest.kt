package com.mitra.plugins

import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.security.KeyPairGenerator
import java.security.Signature

class PluginLoaderTest {
    private val keys = KeyPairGenerator.getInstance("Ed25519").generateKeyPair()
    private val loader = PluginLoader(Ed25519Verifier(keys.public.encoded))

    private val json =
        """
        {"schema":1,"id":"mitra.plugin.settings","version":"1.0.0","app":{"package":"com.android.settings"},
         "actions":[{"id":"open_wifi","risk":"read","steps":[{"intent":{"action":"android.settings.WIFI_SETTINGS"}}]}]}
        """.trimIndent().toByteArray()

    private fun sign(data: ByteArray): ByteArray =
        Signature.getInstance("Ed25519").run {
            initSign(keys.private)
            update(data)
            sign()
        }

    @Test
    fun `signed plugin loads`() {
        assertTrue(loader.load(SignedPlugin(json, sign(json))) is LoadResult.Loaded)
        assertTrue(loader.isTrusted("mitra.plugin.settings"))
    }

    @Test
    fun `tampered plugin is rejected`() {
        val sig = sign(json)
        val tampered = json.decodeToString().replace("WIFI_SETTINGS", "SECURITY_SETTINGS").toByteArray()
        assertTrue(loader.load(SignedPlugin(tampered, sig)) is LoadResult.Rejected)
        assertFalse(loader.isTrusted("mitra.plugin.settings"))
    }
}
