package com.example.androcache.cache.eviction

import java.io.File


/** Because disk eviction always works like this:

👉 “Remove oldest files until directory size ≤ maxSize”

This is universal and NOT customizable in the same way memory eviction is
 **/
/**
 * Disk eviction policy that evicts files from the cache directory
 * when total size exceeds [maxSizeBytes].
 *
 * Eviction strategy: delete oldest files first (based on lastModified),
 * which gives a simple LRU-on-disk behavior.
 */
class DiskEvictionPolicy(
    private val directory: File,
    private val maxSizeBytes: Long
) {

    init {
        if (!directory.exists()) {
            directory.mkdirs()
        }
    }

    /**
     * Enforce the maxSize constraint by deleting the oldest files first
     * until total size <= maxSizeBytes.
     *
     * This is safe to call concurrently if caller holds appropriate locks.
     */
    fun enforce() {
        val files = directory.listFiles() ?: return
        var total = files.sumOf { it.length() }
        if (total <= maxSizeBytes) return

        // Sort by lastModified ascending -> oldest first
        val sorted = files.sortedBy { it.lastModified() }

        for (file in sorted) {
            if (total <= maxSizeBytes) break
            val len = file.length()
            if (file.delete()) {
                total -= len
            }
        }
    }

    /**
     * Remove a specific file (if present) and return whether deletion occurred.
     * Caller should ensure locking as necessary.
     */
    fun removeFile(filename: String): Boolean {
        val f = File(directory, filename)
        return if (f.exists()) f.delete() else false
    }

    /**
     * Return total size under directory in bytes.
     */
    fun totalSize(): Long {
        return directory.listFiles()?.sumOf { it.length() } ?: 0L
    }

    /**
     * Clear all files.
     */
    fun clearAll() {
        directory.listFiles()?.forEach { it.delete() }
    }
}