package com.example.androcache.cache.memory

import com.example.androcache.cache.api.CacheInterface
import com.example.androcache.cache.eviction.MemoryEvictionPolicy
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import java.util.concurrent.locks.ReadWriteLock

class MemoryCache<K : Any, V : Any>(
    private val evictionPolicy: MemoryEvictionPolicy<K,V>,
    private val locker: ReadWriteLock
) : CacheInterface<K, V> {

    override suspend fun put(key: K, value: V) {
        locker.writeLock().run {
            evictionPolicy.put(key, value)
        }
    }

    override fun get(key: K, fetcher: (suspend () -> V)?): Flow<V> {
        val value: V = locker.readLock().run {
            evictionPolicy.get(key) as V
        }
        return flow {
            emit(value)
        }
    }

    override suspend fun remove(key: K): Boolean {
        return locker.writeLock().run {
            evictionPolicy.remove(key)
        }
    }

    override suspend fun clear() {
        locker.writeLock().run { evictionPolicy.clear() }
    }

    override suspend fun size(): Long {
        return locker.readLock().run { evictionPolicy.size().toLong() }
    }
}