package com.mitra.plugins

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject

/** A signed, code-free JSON recipe that teaches Mitra one app (schema v1, docs/PLUGIN_SPEC.md §2). */
@Serializable
data class Plugin(
    val schema: Int,
    val id: String, // reverse-DNS, e.g. "mitra.plugin.settings"
    val version: String,
    val app: AppRef,
    val languages: List<String> = listOf("en"),
    val screens: List<ScreenDef> = emptyList(),
    val actions: List<PluginAction>,
)

@Serializable
data class AppRef(
    @SerialName("package") val packageName: String,
    val minVersionCode: Long = 0,
)

@Serializable
data class ScreenDef(
    val id: String,
    val match: JsonObject,
    @SerialName("user_content") val userContent: List<String> = emptyList(),
)

@Serializable
data class PluginAction(
    val id: String,
    val risk: String, // read | write | send | submit (the engine still re-derives risk)
    val confirm: Boolean = false,
    val params: Map<String, String> = emptyMap(),
    val steps: List<JsonObject>, // each object has exactly one key: launch, intent, tap, type, scroll, global, readText, waitFor ...
    val verify: JsonObject? = null,
)

/** On disk: <name>/plugin.json next to <name>/plugin.sig (Ed25519 signature of the exact JSON bytes). */
class SignedPlugin(
    val json: ByteArray,
    val signature: ByteArray,
)
