package com.mitra.data.db

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "tasks",
    indices = [
        Index("created_at"),
        Index("status"),
    ],
)
data class TaskEntity(
    @PrimaryKey
    val id: String,
    @ColumnInfo(name = "created_at")
    val createdAt: Long,
    @ColumnInfo(name = "ended_at")
    val endedAt: Long? = null,
    @ColumnInfo(name = "user_text")
    val userText: String,
    @ColumnInfo(name = "language")
    val language: String = "en-IN",
    @ColumnInfo(name = "source")
    val source: String,
    @ColumnInfo(name = "status")
    val status: String,
    @ColumnInfo(name = "summary")
    val summary: String,
    @ColumnInfo(name = "scope_json")
    val scopeJson: String,
    @ColumnInfo(name = "engine")
    val engine: String,
    @ColumnInfo(name = "step_count")
    val stepCount: Int = 0,
)

@Entity(
    tableName = "steps",
    foreignKeys = [
        ForeignKey(
            entity = TaskEntity::class,
            parentColumns = ["id"],
            childColumns = ["task_id"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [
        Index("task_id"),
        Index("created_at"),
    ],
)
data class StepEntity(
    @PrimaryKey
    @ColumnInfo(name = "step_id")
    val stepId: String,
    @ColumnInfo(name = "task_id")
    val taskId: String,
    @ColumnInfo(name = "idx")
    val idx: Int,
    @ColumnInfo(name = "agent")
    val agent: String,
    @ColumnInfo(name = "engine")
    val engine: String,
    @ColumnInfo(name = "route")
    val route: String,
    @ColumnInfo(name = "action_json")
    val actionJson: String,
    @ColumnInfo(name = "verdict")
    val verdict: String,
    @ColumnInfo(name = "rule_id")
    val ruleId: String? = null,
    @ColumnInfo(name = "reason")
    val reason: String,
    @ColumnInfo(name = "user_response")
    val userResponse: String? = null,
    @ColumnInfo(name = "result")
    val result: String? = null,
    @ColumnInfo(name = "screenshot_hash")
    val screenshotHash: String? = null,
    @ColumnInfo(name = "duration_ms")
    val durationMs: Long = 0L,
    @ColumnInfo(name = "created_at")
    val createdAt: Long,
)

@Entity(
    tableName = "permissions",
    indices = [
        Index(value = ["kind", "value"], unique = true),
    ],
)
data class PermissionEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    @ColumnInfo(name = "kind")
    val kind: String, // APP, DOMAIN, ASK_BEFORE
    @ColumnInfo(name = "value")
    val value: String,
    @ColumnInfo(name = "mode")
    val mode: String, // ALLOW, BLOCK, ALWAYS_ASK
    @ColumnInfo(name = "locked")
    val locked: Int = 0, // 1 = user cannot change it
    @ColumnInfo(name = "created_at")
    val createdAt: Long,
)

@Entity(tableName = "rules")
data class RuleEntity(
    @PrimaryKey
    val id: String,
    @ColumnInfo(name = "kind")
    val kind: String, // AUTO_REPLY
    @ColumnInfo(name = "description")
    val description: String,
    @ColumnInfo(name = "target_app")
    val targetApp: String,
    @ColumnInfo(name = "config_json")
    val configJson: String,
    @ColumnInfo(name = "enabled")
    val enabled: Boolean,
    @ColumnInfo(name = "created_at")
    val createdAt: Long,
    @ColumnInfo(name = "pin_confirmed_at")
    val pinConfirmedAt: Long? = null,
    @ColumnInfo(name = "last_fired_at")
    val lastFiredAt: Long? = null,
)

@Entity(tableName = "memory")
data class MemoryEntity(
    @PrimaryKey
    val key: String,
    @ColumnInfo(name = "value")
    val value: String,
    @ColumnInfo(name = "source")
    val source: String, // USER_SAID, SCREEN
    @ColumnInfo(name = "source_task_id")
    val sourceTaskId: String? = null,
    @ColumnInfo(name = "consent_at")
    val consentAt: Long,
    @ColumnInfo(name = "created_at")
    val createdAt: Long,
    @ColumnInfo(name = "updated_at")
    val updatedAt: Long,
)

@Entity(tableName = "plugins")
data class PluginEntity(
    @PrimaryKey
    val id: String,
    @ColumnInfo(name = "version")
    val version: String,
    @ColumnInfo(name = "signer_fingerprint")
    val signerFingerprint: String,
    @ColumnInfo(name = "sha256")
    val sha256: String,
    @ColumnInfo(name = "source")
    val source: String, // BUILTIN, FILE
    @ColumnInfo(name = "enabled")
    val enabled: Boolean,
    @ColumnInfo(name = "installed_at")
    val installedAt: Long,
)

@Entity(tableName = "family_links")
data class FamilyLinkEntity(
    @PrimaryKey
    val id: String,
    @ColumnInfo(name = "display_name")
    val displayName: String,
    @ColumnInfo(name = "channel")
    val channel: String, // FCM, UNIFIEDPUSH, SAME_PHONE
    @ColumnInfo(name = "endpoint_hash")
    val endpointHash: String,
    @ColumnInfo(name = "status")
    val status: String, // LINKED, UNLINKED
    @ColumnInfo(name = "linked_at")
    val linkedAt: Long,
    @ColumnInfo(name = "last_seen_at")
    val lastSeenAt: Long,
)

@Entity(tableName = "model_runs")
data class ModelRunEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    @ColumnInfo(name = "ts")
    val ts: Long,
    @ColumnInfo(name = "model_id")
    val modelId: String,
    @ColumnInfo(name = "first_token_ms")
    val firstTokenMs: Long,
    @ColumnInfo(name = "tokens_per_s")
    val tokensPerS: Double,
    @ColumnInfo(name = "peak_ram_mb")
    val peakRamMb: Long,
    @ColumnInfo(name = "device_tier")
    val deviceTier: String,
)

@Entity(tableName = "send_events")
data class SendEventEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    @ColumnInfo(name = "ts")
    val ts: Long,
    @ColumnInfo(name = "task_id")
    val taskId: String,
    @ColumnInfo(name = "kind")
    val kind: String,
)
