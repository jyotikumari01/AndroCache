package com.example.androcache.cache.eviction

class MemoryEvictionPolicy<K : Any, V : Any>(
    private val maxSize: Int,
    private val algo: EvictionAlgorithm<K>
) {

    private val map = LinkedHashMap<K, V>()

    fun put(key: K, value: V) {
        if (map.size >= maxSize) {
            val toEvict = algo.selectEvictionCandidate()
            if (toEvict != null) {
                map.remove(toEvict)
                algo.onEntryRemove(toEvict)
            }
        }

        map[key] = value
        algo.onEntryAdd(key)
    }

    fun get(key: K): V? {
        val v = map[key]
        if (v != null) algo.onEntryAccess(key)
        return v
    }

    fun remove(key: K): Boolean {
        val removed = map.remove(key) != null
        if (removed) algo.onEntryRemove(key)
        return removed
    }

    fun clear() {
        map.clear()
    }

    fun size(): Int = map.size
}
