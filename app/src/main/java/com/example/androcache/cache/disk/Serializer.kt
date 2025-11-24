package com.example.androcache.cache.disk

interface Serializer<V : Any> {
    fun serialize(value: V): ByteArray
    fun deserialize(bytes: ByteArray): V
}
