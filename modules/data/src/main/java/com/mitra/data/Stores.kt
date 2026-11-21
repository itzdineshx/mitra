package com.mitra.data

import com.mitra.data.db.FamilyLinkEntity
import com.mitra.data.db.MemoryEntity
import com.mitra.data.db.MitraDatabase
import com.mitra.data.db.ModelRunEntity
import com.mitra.data.db.PermissionEntity
import com.mitra.data.db.PluginEntity
import com.mitra.data.db.RuleEntity
import com.mitra.data.db.TaskEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonPrimitive

/**
 * Local storage contracts (docs/DATA_SPEC.md §7).
 * Pure interfaces with Room implementations and in-memory test fakes.
 */
data class MemoryFact(
    val key: String,
    val value: String,
    val source: String = "USER_SAID",
    val consentAt: Long = System.currentTimeMillis(),
    val updatedAtMillis: Long = System.currentTimeMillis(),
)

interface MemoryStore {
    suspend fun put(fact: MemoryFact)

    suspend fun get(key: String): MemoryFact?

    suspend fun forget(key: String)
}

class RoomMemoryStore(
    private val db: MitraDatabase,
) : MemoryStore {
    override suspend fun put(fact: MemoryFact) {
        db.memoryDao().insert(
            MemoryEntity(
                key = fact.key,
                value = fact.value,
                source = fact.source,
                consentAt = fact.consentAt,
                createdAt = fact.consentAt,
                updatedAt = fact.updatedAtMillis,
            ),
        )
    }

    override suspend fun get(key: String): MemoryFact? =
        db.memoryDao().get(key)?.let {
            MemoryFact(
                key = it.key,
                value = it.value,
                source = it.source,
                consentAt = it.consentAt,
                updatedAtMillis = it.updatedAt,
            )
        }

    override suspend fun forget(key: String) {
        db.memoryDao().delete(key)
    }
}

class InMemoryStore : MemoryStore {
    private val map = mutableMapOf<String, MemoryFact>()

    override suspend fun put(fact: MemoryFact) {
        map[fact.key] = fact
    }

    override suspend fun get(key: String) = map[key]

    override suspend fun forget(key: String) {
        map.remove(key)
    }
}

interface PermissionStore {
    suspend fun addPermission(permission: PermissionEntity): Boolean

    fun getAll(): Flow<List<PermissionEntity>>

    suspend fun get(
        kind: String,
        value: String,
    ): PermissionEntity?

    suspend fun remove(
        kind: String,
        value: String,
    )
}

class RoomPermissionStore(
    private val db: MitraDatabase,
    private val denylist: Set<String> = loadDenylistPackages(),
) : PermissionStore {
    override suspend fun addPermission(permission: PermissionEntity): Boolean {
        if (permission.kind == "APP" && denylist.contains(permission.value)) {
            // Banking/UPI packages are completely refused entry per DATA_SPEC.md §3
            return false
        }
        db.permissionDao().insert(permission)
        return true
    }

    override fun getAll(): Flow<List<PermissionEntity>> = db.permissionDao().getAll()

    override suspend fun get(
        kind: String,
        value: String,
    ): PermissionEntity? = db.permissionDao().get(kind, value)

    override suspend fun remove(
        kind: String,
        value: String,
    ) {
        db.permissionDao().delete(kind, value)
    }

    companion object {
        fun loadDenylistPackages(): Set<String> =
            try {
                val stream = RoomPermissionStore::class.java.getResourceAsStream("/denylist_packages.json")
                if (stream != null) {
                    val text = stream.bufferedReader().use { it.readText() }
                    val arr = Json.parseToJsonElement(text).jsonArray
                    arr.map { it.jsonPrimitive.content }.toSet()
                } else {
                    FALLBACK_DENYLIST
                }
            } catch (_: Exception) {
                FALLBACK_DENYLIST
            }

        private val FALLBACK_DENYLIST =
            setOf(
                "com.phonepe.app",
                "net.one97.paytm",
                "com.google.android.apps.nbu.paisa.user",
                "in.org.npci.upiapp",
                "com.sbi.lotusintouch",
                "com.snapwork.hdfc",
                "com.csam.icici.bank.imobile",
                "com.axis.mobile",
                "com.msf.kbank.mobile",
                "com.zerodha.kite3",
                "com.nextbillion.groww",
            )
    }
}

class InMemoryPermissionStore(
    private val denylist: Set<String> = RoomPermissionStore.loadDenylistPackages(),
) : PermissionStore {
    private val list = MutableStateFlow<List<PermissionEntity>>(emptyList())

    override suspend fun addPermission(permission: PermissionEntity): Boolean {
        if (permission.kind == "APP" && denylist.contains(permission.value)) {
            return false
        }
        list.value = list.value.filterNot { it.kind == permission.kind && it.value == permission.value } + permission
        return true
    }

    override fun getAll(): Flow<List<PermissionEntity>> = list

    override suspend fun get(
        kind: String,
        value: String,
    ): PermissionEntity? = list.value.find { it.kind == kind && it.value == value }

    override suspend fun remove(
        kind: String,
        value: String,
    ) {
        list.value = list.value.filterNot { it.kind == kind && it.value == value }
    }
}

interface TaskStore {
    suspend fun saveTask(task: TaskEntity)

    suspend fun getTask(id: String): TaskEntity?

    fun getRecentTasks(limit: Int): Flow<List<TaskEntity>>

    suspend fun deleteTask(id: String)
}

class RoomTaskStore(
    private val db: MitraDatabase,
) : TaskStore {
    override suspend fun saveTask(task: TaskEntity) {
        db.taskDao().insert(task)
    }

    override suspend fun getTask(id: String): TaskEntity? = db.taskDao().getById(id)

    override fun getRecentTasks(limit: Int): Flow<List<TaskEntity>> = db.taskDao().getRecent(limit)

    override suspend fun deleteTask(id: String) {
        db.taskDao().deleteById(id)
    }
}

interface RuleStore {
    suspend fun saveRule(rule: RuleEntity)

    fun getAllRules(): Flow<List<RuleEntity>>

    suspend fun getRule(id: String): RuleEntity?

    suspend fun deleteRule(id: String)
}

class RoomRuleStore(
    private val db: MitraDatabase,
) : RuleStore {
    override suspend fun saveRule(rule: RuleEntity) {
        db.ruleDao().insert(rule)
    }

    override fun getAllRules(): Flow<List<RuleEntity>> = db.ruleDao().getAll()

    override suspend fun getRule(id: String): RuleEntity? = db.ruleDao().getById(id)

    override suspend fun deleteRule(id: String) {
        db.ruleDao().delete(id)
    }
}

interface PluginStore {
    suspend fun savePlugin(plugin: PluginEntity)

    fun getAllPlugins(): Flow<List<PluginEntity>>

    suspend fun getPlugin(id: String): PluginEntity?

    suspend fun deletePlugin(id: String)
}

class RoomPluginStore(
    private val db: MitraDatabase,
) : PluginStore {
    override suspend fun savePlugin(plugin: PluginEntity) {
        db.pluginDao().insert(plugin)
    }

    override fun getAllPlugins(): Flow<List<PluginEntity>> = db.pluginDao().getAll()

    override suspend fun getPlugin(id: String): PluginEntity? = db.pluginDao().getById(id)

    override suspend fun deletePlugin(id: String) {
        db.pluginDao().delete(id)
    }
}

interface FamilyStore {
    suspend fun saveLink(link: FamilyLinkEntity)

    fun getAllLinks(): Flow<List<FamilyLinkEntity>>

    suspend fun deleteLink(id: String)
}

class RoomFamilyStore(
    private val db: MitraDatabase,
) : FamilyStore {
    override suspend fun saveLink(link: FamilyLinkEntity) {
        db.familyLinkDao().insert(link)
    }

    override fun getAllLinks(): Flow<List<FamilyLinkEntity>> = db.familyLinkDao().getAll()

    override suspend fun deleteLink(id: String) {
        db.familyLinkDao().delete(id)
    }
}

interface ModelRunStore {
    suspend fun recordRun(run: ModelRunEntity): Long

    fun getLatestRun(): Flow<ModelRunEntity?>

    suspend fun getLatestRunSync(): ModelRunEntity?
}

class RoomModelRunStore(
    private val db: MitraDatabase,
) : ModelRunStore {
    override suspend fun recordRun(run: ModelRunEntity): Long = db.modelRunDao().insert(run)

    override fun getLatestRun(): Flow<ModelRunEntity?> = db.modelRunDao().getLatest()

    override suspend fun getLatestRunSync(): ModelRunEntity? = db.modelRunDao().getLatestSync()
}
