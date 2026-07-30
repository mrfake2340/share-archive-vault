package com.sharearchivevault.model

import java.io.File

/**
 * Represents a single extracted media file (photo or video).
 *
 * @param file     The temporary File reference inside cache directory.
 * @param hash     SHA-256 hash used for duplicate detection.
 * @param isVideo  True if this is a video file; false for photos.
 */
data class MediaItem(
    val file: File,
    val hash: String,
    val isVideo: Boolean,
    var isSelected: Boolean = false
) {
    val name: String get() = file.name
    val uri get() = file
}
