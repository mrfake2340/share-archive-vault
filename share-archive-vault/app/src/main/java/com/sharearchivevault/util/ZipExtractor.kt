package com.sharearchivevault.util

import android.content.Context
import android.net.Uri
import android.util.Log
import com.sharearchivevault.model.MediaItem
import java.io.File
import java.util.zip.ZipInputStream

/**
 * Handles unzipping the incoming ZIP stream and categorising files into
 * photos and videos. Applies SHA-256 duplicate detection during extraction.
 */
object ZipExtractor {

    private const val TAG = "ZipExtractor"

    // Supported media extensions
    private val PHOTO_EXTS = setOf("jpg", "jpeg", "png", "webp")
    private val VIDEO_EXTS = setOf("mp4", "mkv", "3gp", "mov", "avi")

    // Max uncompressed file size guard (200 MB per file)
    private const val MAX_FILE_SIZE = 200L * 1024 * 1024

    /**
     * Extracts the ZIP at [uri] into the cache directory and returns categorised [MediaItem] lists.
     *
     * @param context  App context for cache directory resolution.
     * @param uri      Content URI of the incoming ZIP file.
     * @return Pair of (photos list, videos list) – duplicates already removed.
     */
    suspend fun extract(context: Context, uri: Uri): Pair<List<MediaItem>, List<MediaItem>> {
        val outputDir = CacheManager.getCacheDir(context)
        val photos = mutableListOf<MediaItem>()
        val videos = mutableListOf<MediaItem>()
        val seenHashes = mutableSetOf<String>()

        try {
            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                ZipInputStream(inputStream.buffered()).use { zip ->
                    var entry = zip.nextEntry
                    while (entry != null) {
                        val entryName = entry.name
                        val fileName = File(entryName).name  // strip path traversal
                        val ext = fileName.substringAfterLast('.', "").lowercase()

                        val isPhoto = ext in PHOTO_EXTS
                        val isVideo = ext in VIDEO_EXTS

                        if (!entry.isDirectory && (isPhoto || isVideo)) {
                            val outFile = File(outputDir, fileName)

                            // Write entry to cache (bounded to MAX_FILE_SIZE)
                            try {
                                outFile.outputStream().use { fos ->
                                    val buffer = ByteArray(8192)
                                    var total = 0L
                                    var bytesRead: Int
                                    while (zip.read(buffer).also { bytesRead = it } != -1) {
                                        total += bytesRead
                                        if (total > MAX_FILE_SIZE) {
                                            Log.w(TAG, "File $fileName exceeds size limit; skipping.")
                                            outFile.delete()
                                            break
                                        }
                                        fos.write(buffer, 0, bytesRead)
                                    }
                                }
                            } catch (e: Exception) {
                                Log.e(TAG, "Failed to write $fileName: ${e.message}")
                                outFile.delete()
                                zip.closeEntry()
                                entry = zip.nextEntry
                                continue
                            }

                            if (!outFile.exists() || outFile.length() == 0L) {
                                outFile.delete()
                                zip.closeEntry()
                                entry = zip.nextEntry
                                continue
                            }

                            // Duplicate Prevention: compute hash and skip duplicates
                            val hash = HashUtil.sha256(outFile)
                            if (hash == null || hash in seenHashes) {
                                Log.d(TAG, "Duplicate or unreadable file skipped: $fileName")
                                outFile.delete()
                                zip.closeEntry()
                                entry = zip.nextEntry
                                continue
                            }
                            seenHashes.add(hash)

                            val item = MediaItem(file = outFile, hash = hash, isVideo = isVideo)
                            if (isPhoto) photos.add(item) else videos.add(item)
                        }

                        zip.closeEntry()
                        entry = zip.nextEntry
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "ZIP extraction failed: ${e.message}", e)
        }

        Log.d(TAG, "Extracted ${photos.size} photos and ${videos.size} videos.")
        return Pair(photos, videos)
    }
}
