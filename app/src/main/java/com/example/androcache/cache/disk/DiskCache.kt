package com.example.androcache.cache.disk

import com.example.androcache.cache.api.CacheInterface
import com.example.androcache.cache.eviction.DiskEvictionPolicy
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withContext
import java.io.File
import java.security.MessageDigest
import java.util.concurrent.locks.ReadWriteLock

class DiskCache<K : Any, V : Any>(
    private val directory: File,
    private val maxSize: Long,
    private val serializer: Serializer<V>,
    private val locker: ReadWriteLock
) : CacheInterface<K, V> {

    private val evictionPolicy = DiskEvictionPolicy(directory, maxSize)

    init {
        if (!directory.exists()) directory.mkdirs()
    }

    override suspend fun put(key: K, value: V) {
        val bytes = serializer.serialize(value)

        withContext(Dispatchers.IO) {
            locker.writeLock().run {
                val filename = keyToFileName(key)
                val file = File(directory, filename)
                file.writeBytes(bytes)
                file.setLastModified(System.currentTimeMillis())
                evictionPolicy.enforce()
            }
        }
    }

    override fun get(key: K, fetcher: (suspend () -> V)?): Flow<V?> {

           val result = locker.readLock().run read@{
               val filename = keyToFileName(key)
               val f = File(directory, filename)
               if (!f.exists()) return@read null

               // Read bytes
               val bytes = try {
                   f.readBytes()
               } catch (ex: Exception) {
                   // Could not read; treat as missing
                   return@read null
               }

               // Update lastModified to mark as recently-used (supports LRU-on-disk)
               try {
                   f.setLastModified(System.currentTimeMillis())
               } catch (_: Exception) { /* ignore if not supported */
               }

               // Deserialize; if deserialize fails, treat as missing
               return@read try {
                   serializer.deserialize(bytes)
               } catch (ex: Exception) {
                   null
               }
           }
        return flow { emit(result) }
    }

    override suspend fun remove(key: K): Boolean {
        return withContext(Dispatchers.IO) {
            locker.writeLock().run {
                val filename = keyToFileName(key)
                evictionPolicy.removeFile(filename)
            }
        }
    }

    override suspend fun clear() {
        withContext(Dispatchers.IO) {
            locker.writeLock().run {
                evictionPolicy.clearAll()
            }
        }
    }

    override suspend fun size(): Long {
        return withContext(Dispatchers.IO) {
            locker.readLock().run {
                evictionPolicy.totalSize()
            }
        }
    }

    // ---------------------------
    // Helper: key -> safe filename
    // ---------------------------
    private fun keyToFileName(key: K): String {
        // Simple deterministic hashing (hex of SHA-256). Keeps filename length bounded and safe.
        val s = key.toString().toByteArray(Charsets.UTF_8)
        val md = MessageDigest.getInstance("SHA-256")
        val digest = md.digest(s)
        return digest.toHex()
    }

    private fun ByteArray.toHex(): String {
        val sb = StringBuilder(size * 2)
        forEach {
            sb.append(String.format("%02x", it))
        }
        return sb.toString()
    }
}