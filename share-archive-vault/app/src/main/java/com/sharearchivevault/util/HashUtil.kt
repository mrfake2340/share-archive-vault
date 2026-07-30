package com.sharearchivevault.util

import java.io.File
import java.security.MessageDigest

/**
 * Utility for computing SHA-256 hashes of files.
 * Used by the Duplicate Prevention Engine to identify and skip duplicate media.
 */
object HashUtil {

    /**
     * Computes the SHA-256 hash of the given file.
     * Returns a hex string, or null if the file cannot be read.
     */
    fun sha256(file: File): String? {
        return try {
            val digest = MessageDigest.getInstance("SHA-256")
            file.inputStream().use { fis ->
                val buffer = ByteArray(8192)
                var bytesRead: Int
                while (fis.read(buffer).also { bytesRead = it } != -1) {
                    digest.update(buffer, 0, bytesRead)
                }
            }
            digest.digest().joinToString("") { "%02x".format(it) }
        } catch (e: Exception) {
            null
        }
    }
}
