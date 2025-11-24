package com.example.androcache.cache.api

import kotlinx.coroutines.flow.Flow

interface CacheInterface<K : Any, V : Any> {

    /**
     * Put an item into the cache.
     * Suspend because it may involve disk IO.
     */
    suspend fun put(key: K, value: V)

    /**
     * Retrieve an item from the cache.
     * Flow allows:
     *  - Memory first
     *  - Disk second
     *  - Fetcher (if provided)
     *  - Realtime update
     */
    fun get(
        key: K,
        fetcher: (suspend () -> V)? = null
    ): Flow<V?>

    /**
     * Remove a specific key.
     */
    suspend fun remove(key: K): Boolean

    /**
     * Clear entire cache.
     */
    suspend fun clear()

    /**
     * Cache size (sum of memory + disk size).
     */
    suspend fun size(): Long
}
