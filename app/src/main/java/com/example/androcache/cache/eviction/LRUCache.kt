package com.example.androcache.cache.eviction

import com.example.androcache.cache.api.CacheInterface

class LRUCacheL<K, V>(val key: K, val value: V) : CacheInterface<K, V> {

}