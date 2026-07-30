package com.sharearchivevault.util

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters

/**
 * Background WorkManager worker that wipes the vault extraction cache.
 * Enqueued from onDestroy() so the wipe completes even if the process is killed.
 */
class CacheWipeWorker(
    private val appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    companion object {
        const val TAG = "CacheWipeWorker"
        const val WORK_NAME = "vault_cache_wipe"
    }

    override suspend fun doWork(): Result {
        return try {
            CacheManager.clearAll(appContext)
            Log.d(TAG, "Background cache wipe completed.")
            Result.success()
        } catch (e: Exception) {
            Log.e(TAG, "Cache wipe failed: ${e.message}")
            Result.retry()
        }
    }
}
