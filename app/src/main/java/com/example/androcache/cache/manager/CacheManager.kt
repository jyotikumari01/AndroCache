package com.example.androcache.cache.manager

import com.example.androcache.cache.api.CacheInterface
import com.example.androcache.cache.api.CacheConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.withContext

/**
 * CacheManager = Orchestrator for:
 * - MemoryCache (optional)
 * - DiskCache (optional)
 * - TTL expiration
 * - Fetcher fallback
 *
 * Responsibilities:
 *  - Read from memory first
 *  - If miss: read from disk
 *  - If miss: use fetcher
 *  - Sync results back into all layers
 *  - Emit as Flow for reactive updates
 */
class CacheManager<K : Any, V : Any>(
    private val memoryCache: CacheInterface<K, V>?,
    private val diskCache: CacheInterface<K, V>?,
    private val config: CacheConfig<K, V>
) : CacheInterface<K, V> {

    // Helper to get current time quickly
    private fun now() = System.currentTimeMillis()

    // ---------------------------
    // PUT (writes to memory + disk)
    // ---------------------------
    override suspend fun put(key: K, value: V) {
        // Memory write
        memoryCache?.put(key, value)

        // Disk write
        diskCache?.put(key, value)
    }


    // ---------------------------
    // GET (memory → disk → fetcher)
    // ---------------------------
    override fun get(
        key: K,
        fetcher: (suspend () -> V)?
    ): Flow<V?> = flow {

        // 1) Try Memory
        val memValue = memoryCache?.get(key)?.firstOrNull()
        if (memValue != null) {
            emit(memValue)
            return@flow
        }

        // 2) Try Disk
        val diskValue = diskCache?.get(key)?.firstOrNull()
        if (diskValue != null) {

            // Sync back to memory for faster next read
            memoryCache?.put(key, diskValue)

            emit(diskValue)
            return@flow
        }

        // 3) Fetcher fallback (network/db)
        if (fetcher != null) {
            val freshValue = fetcher()

            // Save into both layers
            memoryCache?.put(key, freshValue)
            diskCache?.put(key, freshValue)

            emit(freshValue)
            return@flow
        }

        // 4) Nothing exists
        emit(null)
    }.flowOn(Dispatchers.IO)


    // ---------------------------
    // REMOVE
    // ---------------------------
    override suspend fun remove(key: K): Boolean {
        var removed = false

        memoryCache?.let { removed = it.remove(key) || removed }
        diskCache?.let { removed = it.remove(key) || removed }

        return removed
    }


    // ---------------------------
    // CLEAR
    // ---------------------------
    override suspend fun clear() {
        memoryCache?.clear()
        diskCache?.clear()
    }


    // ---------------------------
    // SIZE = sum of memory + disk
    // ---------------------------
    override suspend fun size(): Long {
        val memSize = memoryCache?.size() ?: 0
        val diskSize = diskCache?.size() ?: 0
        return memSize + diskSize
    }
}
