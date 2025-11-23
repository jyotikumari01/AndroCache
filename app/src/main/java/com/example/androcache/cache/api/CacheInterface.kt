package com.example.androcache.cache.api

interface CacheInterface<K,V> {

    fun add(key: K, value: V) {

    }

    fun retrieve(key: K): V {

    }

    fun remove(key: K) {

    }

    fun clear() {

    }

}