package com.mitra.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface TaskDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(task: TaskEntity)

    @Update
    suspend fun update(task: TaskEntity): Int

    @Query("SELECT * FROM tasks WHERE id = :id")
    suspend fun getById(id: String): TaskEntity?

    @Query("SELECT * FROM tasks ORDER BY created_at DESC LIMIT :limit")
    fun getRecent(limit: Int): Flow<List<TaskEntity>>

    @Query("SELECT * FROM tasks ORDER BY created_at DESC")
    fun getAll(): Flow<List<TaskEntity>>

    @Query("DELETE FROM tasks WHERE id = :id")
    suspend fun deleteById(id: String): Int

    @Query("DELETE FROM tasks WHERE created_at < :cutoffMillis")
    suspend fun deleteOlderThan(cutoffMillis: Long): Int

    @Query("SELECT COUNT(*) FROM tasks")
    suspend fun count(): Int

    @Query("SELECT * FROM tasks ORDER BY created_at ASC")
    suspend fun getOldestTasks(): List<TaskEntity>

    @Query("DELETE FROM tasks")
    suspend fun deleteAll(): Int
}

@Dao
interface StepDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(step: StepEntity)

    @Query("UPDATE steps SET result = :result, user_response = :userResponse, duration_ms = :durationMs WHERE step_id = :stepId")
    suspend fun updateResult(
        stepId: String,
        result: String?,
        userResponse: String?,
        durationMs: Long,
    ): Int

    @Query("SELECT * FROM steps WHERE task_id = :taskId ORDER BY idx ASC")
    fun getStepsForTask(taskId: String): Flow<List<StepEntity>>

    @Query("SELECT * FROM steps WHERE task_id = :taskId ORDER BY idx ASC")
    suspend fun getStepsForTaskSync(taskId: String): List<StepEntity>

    @Query("SELECT * FROM steps ORDER BY created_at DESC LIMIT 50")
    suspend fun getRecentStepsSync(): List<StepEntity>

    @Query("DELETE FROM steps")
    suspend fun deleteAll(): Int
}

@Dao
interface PermissionDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(permission: PermissionEntity): Long

    @Query("SELECT * FROM permissions ORDER BY created_at DESC")
    fun getAll(): Flow<List<PermissionEntity>>

    @Query("SELECT * FROM permissions WHERE kind = :kind AND value = :value LIMIT 1")
    suspend fun get(
        kind: String,
        value: String,
    ): PermissionEntity?

    @Query("DELETE FROM permissions WHERE kind = :kind AND value = :value")
    suspend fun delete(
        kind: String,
        value: String,
    ): Int

    @Query("DELETE FROM permissions")
    suspend fun deleteAll(): Int
}

@Dao
interface RuleDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(rule: RuleEntity)

    @Query("SELECT * FROM rules ORDER BY created_at DESC")
    fun getAll(): Flow<List<RuleEntity>>

    @Query("SELECT * FROM rules WHERE id = :id")
    suspend fun getById(id: String): RuleEntity?

    @Query("DELETE FROM rules WHERE id = :id")
    suspend fun delete(id: String): Int

    @Query("DELETE FROM rules")
    suspend fun deleteAll(): Int
}

@Dao
interface MemoryDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(memory: MemoryEntity)

    @Query("SELECT * FROM memory WHERE `key` = :key")
    suspend fun get(key: String): MemoryEntity?

    @Query("SELECT * FROM memory ORDER BY updated_at DESC")
    fun getAll(): Flow<List<MemoryEntity>>

    @Query("DELETE FROM memory WHERE `key` = :key")
    suspend fun delete(key: String): Int

    @Query("DELETE FROM memory")
    suspend fun deleteAll(): Int
}

@Dao
interface PluginDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(plugin: PluginEntity)

    @Query("SELECT * FROM plugins ORDER BY installed_at DESC")
    fun getAll(): Flow<List<PluginEntity>>

    @Query("SELECT * FROM plugins WHERE id = :id")
    suspend fun getById(id: String): PluginEntity?

    @Query("DELETE FROM plugins WHERE id = :id")
    suspend fun delete(id: String): Int

    @Query("DELETE FROM plugins")
    suspend fun deleteAll(): Int
}

@Dao
interface FamilyLinkDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(link: FamilyLinkEntity)

    @Query("SELECT * FROM family_links ORDER BY linked_at DESC")
    fun getAll(): Flow<List<FamilyLinkEntity>>

    @Query("DELETE FROM family_links WHERE id = :id")
    suspend fun delete(id: String): Int

    @Query("DELETE FROM family_links")
    suspend fun deleteAll(): Int
}

@Dao
interface ModelRunDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(run: ModelRunEntity): Long

    @Query("SELECT * FROM model_runs ORDER BY id DESC LIMIT 1")
    fun getLatest(): Flow<ModelRunEntity?>

    @Query("SELECT * FROM model_runs ORDER BY id DESC LIMIT 1")
    suspend fun getLatestSync(): ModelRunEntity?

    @Query("DELETE FROM model_runs")
    suspend fun deleteAll()
}

@Dao
interface SendEventDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(event: SendEventEntity): Long

    @Query("SELECT COUNT(*) FROM send_events WHERE ts >= :sinceMillis AND kind = 'SEND'")
    suspend fun countSendsInWindow(sinceMillis: Long): Int

    @Query("DELETE FROM send_events WHERE ts < :cutoffMillis")
    suspend fun deleteOlderThan(cutoffMillis: Long): Int

    @Query("DELETE FROM send_events")
    suspend fun deleteAll()
}
