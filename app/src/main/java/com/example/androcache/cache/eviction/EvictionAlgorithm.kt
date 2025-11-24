package com.example.androcache.cache.eviction

interface EvictionAlgorithm<K> {
    fun onEntryAccess(key: K)            // Update usage info
    fun onEntryAdd(key: K)               // Track new entry
    fun onEntryRemove(key: K)            // Remove from algorithm tracking
    fun selectEvictionCandidate(): K?    // Which key should be evicted
}
