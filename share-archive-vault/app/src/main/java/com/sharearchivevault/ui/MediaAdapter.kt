package com.sharearchivevault.ui

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.sharearchivevault.R
import com.sharearchivevault.databinding.ItemMediaBinding
import com.sharearchivevault.model.MediaItem

/**
 * RecyclerView adapter for the dark-themed media grid with multi-select support.
 */
class MediaAdapter(
    private val onItemClick: (MediaItem) -> Unit
) : ListAdapter<MediaItem, MediaAdapter.MediaViewHolder>(DiffCallback) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MediaViewHolder {
        val binding = ItemMediaBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return MediaViewHolder(binding)
    }

    override fun onBindViewHolder(holder: MediaViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class MediaViewHolder(private val binding: ItemMediaBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(item: MediaItem) {
            val ctx = binding.root.context

            // Load thumbnail via Glide (works for both images and video frames)
            Glide.with(ctx)
                .load(item.file)
                .diskCacheStrategy(DiskCacheStrategy.NONE)
                .skipMemoryCache(true)
                .centerCrop()
                .placeholder(R.drawable.bg_media_placeholder)
                .into(binding.imgThumbnail)

            // Video badge visibility
            binding.videoBadge.visibility =
                if (item.isVideo) android.view.View.VISIBLE else android.view.View.GONE

            // Checkbox state
            binding.checkboxSelect.isChecked = item.isSelected

            // Overlay tint when selected
            binding.selectionOverlay.visibility =
                if (item.isSelected) android.view.View.VISIBLE else android.view.View.GONE

            binding.root.setOnClickListener { onItemClick(item) }
            binding.checkboxSelect.setOnClickListener { onItemClick(item) }
        }
    }

    private object DiffCallback : DiffUtil.ItemCallback<MediaItem>() {
        override fun areItemsTheSame(oldItem: MediaItem, newItem: MediaItem) =
            oldItem.hash == newItem.hash

        override fun areContentsTheSame(oldItem: MediaItem, newItem: MediaItem) =
            oldItem.isSelected == newItem.isSelected && oldItem.hash == newItem.hash
    }
}
