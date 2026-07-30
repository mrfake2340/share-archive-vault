package com.sharearchivevault.util

import android.content.Context
import android.util.Log
import java.io.File

/**
 * Manages the app's temporary extraction directory.
 *
 * Strict rule: ALL extracted files live only inside [getCacheDir].
 * Nothing is ever written to external/shared storage.
 */
object CacheManager {

    private const val TAG = "CacheManager"
    private const val EXTRACT_DIR = "vault_extract"

    /** Returns (and creates if needed) the vault extraction subdirectory. */
    fun getCacheDir(context: Context): File {
        val dir = File(context.cacheDir, EXTRACT_DIR)
        if (!dir.exists()) dir.mkdirs()
        return dir
    }

    /**
     * Wipes every file inside the extraction directory recursively.
     * Called from onDestroy() and from the background [CacheWipeWorker].
     */
    fun clearAll(context: Context) {
        try {
            val dir = File(context.cacheDir, EXTRACT_DIR)
            if (dir.exists()) {
                dir.deleteRecursively()
                Log.d(TAG, "Cache cleared successfully.")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error clearing cache: ${e.message}")
        }
    }
}
