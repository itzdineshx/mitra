package com.mitra.data.db

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [
        TaskEntity::class,
        StepEntity::class,
        PermissionEntity::class,
        RuleEntity::class,
        MemoryEntity::class,
        PluginEntity::class,
        FamilyLinkEntity::class,
        ModelRunEntity::class,
        SendEventEntity::class,
    ],
    version = 1,
    exportSchema = true,
)
abstract class MitraDatabase : RoomDatabase() {
    abstract fun taskDao(): TaskDao

    abstract fun stepDao(): StepDao

    abstract fun permissionDao(): PermissionDao

    abstract fun ruleDao(): RuleDao

    abstract fun memoryDao(): MemoryDao

    abstract fun pluginDao(): PluginDao

    abstract fun familyLinkDao(): FamilyLinkDao

    abstract fun modelRunDao(): ModelRunDao

    abstract fun sendEventDao(): SendEventDao
}
