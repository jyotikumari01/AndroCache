package com.example.androcache.cache.api

import com.example.androcache.cache.disk.DiskCache
import com.example.androcache.cache.disk.Serializer
import com.example.androcache.cache.eviction.EvictionAlgorithm
import com.example.androcache.cache.memory.MemoryCache
import java.io.File

/**
 * CacheConfig defines all settings required to create a complete
 * multi-layer cache (Memory + Disk) via CacheFactory.
 *
 * It is immutable and built using the Builder pattern.
 */
data class CacheConfig<K : Any, V : Any>(
    val memoryConfig: MemoryConfig<K, V>?,
    val diskConfig: DiskConfig<V>?,
    val defaultTtl: Long,
    val serializer: Serializer<V>,
) {

    /**
     * Configuration specific to RAM-based caching.
     */
    data class MemoryConfig<K : Any, V : Any>(
        val maxItems: Int,
        val evictionAlgorithm: EvictionAlgorithm<K>
    )

    /**
     * Configuration specific to disk-based caching.
     */
    data class DiskConfig<V : Any>(
        val directory: File,
        val maxSizeBytes: Long
    )

    // -------------------------------
    // Builder for typed construction
    // -------------------------------

    class Builder<K : Any, V : Any> {

        private var memoryConfig: MemoryConfig<K, V>? = null
        private var diskConfig: DiskConfig<V>? = null
        private var ttl: Long = 0L
        private var serializer: Serializer<V>? = null

        fun memory(
            maxItems: Int,
            evictionAlgorithm: EvictionAlgorithm<K>
        ) = apply {
            this.memoryConfig = MemoryConfig(maxItems, evictionAlgorithm)
        }

        fun disk(
            directory: File,
            maxSizeBytes: Long
        ) = apply {
            this.diskConfig = DiskConfig(directory, maxSizeBytes)
        }

        fun serializer(serializer: Serializer<V>) = apply {
            this.serializer = serializer
        }

        fun ttl(defaultTtlMillis: Long) = apply {
            this.ttl = defaultTtlMillis
        }

        fun build(): CacheConfig<K, V> {

            val ser = serializer
                ?: throw IllegalStateException("Serializer must be provided")

            // If user provided disk config, ensure directory exists
            diskConfig?.let {
                if (!it.directory.exists()) it.directory.mkdirs()
            }

            return CacheConfig(
                memoryConfig = memoryConfig,
                diskConfig = diskConfig,
                defaultTtl = ttl,
                serializer = ser
            )
        }
    }
}
