package com.example.housinghub.adapters

import android.content.Context
import android.net.Uri
import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.housinghub.databinding.ItemVideoPreviewBinding
import kotlinx.coroutines.*

class VideoPreviewAdapter(
    private val context: Context,
    private val onVideoClick: (Int) -> Unit,
    private val onDeleteClick: (Int) -> Unit
) : RecyclerView.Adapter<VideoPreviewAdapter.VideoViewHolder>() {

    private val videos = mutableListOf<Uri>()
    private val coroutineScope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    inner class VideoViewHolder(private val binding: ItemVideoPreviewBinding) :
        RecyclerView.ViewHolder(binding.root) {
        private var thumbnailJob: Job? = null

        fun bind(videoUri: Uri, position: Int) {
            // Cancel previous job if exists
            thumbnailJob?.cancel()
            
            // Use Glide to load video thumbnail directly
            thumbnailJob = coroutineScope.launch {
                try {
                    // Load the video thumbnail using Glide
                    Glide.with(context)
                        .load(videoUri)
                        .centerCrop()
                        .into(binding.ivVideoThumbnail)
                } catch (e: Exception) {
                    Log.e("VideoPreviewAdapter", "Error loading thumbnail: ${e.message}")
                }
            }

            // Set click listeners for the video item and delete button
            binding.ivVideoThumbnail.setOnClickListener { onVideoClick(position) }
            binding.ivPlayButton.setOnClickListener { onVideoClick(position) }
            binding.btnDelete.setOnClickListener { onDeleteClick(position) }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VideoViewHolder {
        val binding = ItemVideoPreviewBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return VideoViewHolder(binding)
    }

    override fun onBindViewHolder(holder: VideoViewHolder, position: Int) {
        if (position < videos.size) {
            holder.bind(videos[position], position)
        }
    }

    override fun getItemCount(): Int = videos.size

    fun submitList(newVideos: List<Uri>) {
        videos.clear()
        videos.addAll(newVideos)
        notifyDataSetChanged()
    }
    
    fun onDestroy() {
        coroutineScope.cancel()
    }
}
