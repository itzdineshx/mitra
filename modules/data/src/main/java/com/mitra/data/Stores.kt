package com.mitra.data

/**
 * Local storage contracts. Room (encrypted) implementations arrive in task T09.
 * Nothing here leaves the phone.
 */
data class MemoryFact(
    val key: String,
    val value: String,
    val updatedAtMillis: Long,
)

interface MemoryStore {
    suspend fun put(fact: MemoryFact)

    suspend fun get(key: String): MemoryFact?

    suspend fun forget(key: String)
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
