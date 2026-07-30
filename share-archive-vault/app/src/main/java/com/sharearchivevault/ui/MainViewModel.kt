package com.sharearchivevault.ui

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sharearchivevault.model.MediaItem
import com.sharearchivevault.util.ZipExtractor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed class ExtractionState {
    object Idle : ExtractionState()
    object Loading : ExtractionState()
    data class Success(val photos: List<MediaItem>, val videos: List<MediaItem>) : ExtractionState()
    data class Error(val message: String) : ExtractionState()
    object Empty : ExtractionState()
}

/**
 * Survives configuration changes. Drives extraction and selection state.
 */
class MainViewModel : ViewModel() {

    private val _state = MutableStateFlow<ExtractionState>(ExtractionState.Idle)
    val state: StateFlow<ExtractionState> = _state.asStateFlow()

    /** Extract the ZIP and emit state updates on the IO dispatcher. */
    fun processZip(context: Context, uri: Uri) {
        if (_state.value is ExtractionState.Loading) return
        _state.value = ExtractionState.Loading

        viewModelScope.launch(Dispatchers.IO) {
            try {
                val (photos, videos) = ZipExtractor.extract(context.applicationContext, uri)
                _state.value = if (photos.isEmpty() && videos.isEmpty()) {
                    ExtractionState.Empty
                } else {
                    ExtractionState.Success(photos, videos)
                }
            } catch (e: Exception) {
                _state.value = ExtractionState.Error(e.message ?: "Unknown error during extraction.")
            }
        }
    }

    /** Toggle selection of a specific item across photos or videos. */
    fun toggleSelection(item: MediaItem) {
        val current = _state.value as? ExtractionState.Success ?: return
        item.isSelected = !item.isSelected
        // Re-emit to trigger observer updates
        _state.value = current.copy(
            photos = current.photos.toList(),
            videos = current.videos.toList()
        )
    }

    /** Select or deselect all items in the given list. */
    fun setAllSelected(items: List<MediaItem>, selected: Boolean) {
        val current = _state.value as? ExtractionState.Success ?: return
        items.forEach { it.isSelected = selected }
        _state.value = current.copy(
            photos = current.photos.toList(),
            videos = current.videos.toList()
        )
    }

    /** Returns all currently selected items (photos + videos). */
    fun getSelectedItems(): List<MediaItem> {
        val current = _state.value as? ExtractionState.Success ?: return emptyList()
        return (current.photos + current.videos).filter { it.isSelected }
    }
}
