package com.example.housinghub.adapters

import android.content.Context
import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.housinghub.R

class VideoPreviewAdapter(
    private val context: Context,
    private val onVideoClick: (Int) -> Unit
) : ListAdapter<Uri, VideoPreviewAdapter.VideoViewHolder>(VideoDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VideoViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_video_preview, parent, false)
        return VideoViewHolder(view)
    }

    override fun onBindViewHolder(holder: VideoViewHolder, position: Int) {
        val videoUri = getItem(position)
        holder.bind(videoUri)
        holder.itemView.setOnClickListener { onVideoClick(position) }
    }

    inner class VideoViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val thumbnailView: ImageView = itemView.findViewById(R.id.ivVideoThumbnail)
        private val playButton: ImageView = itemView.findViewById(R.id.ivPlayButton)

        fun bind(videoUri: Uri) {
            // Load video thumbnail using Glide
            Glide.with(context)
                .load(videoUri)
                .thumbnail(0.1f)
                .centerCrop()
                .into(thumbnailView)

            playButton.visibility = View.VISIBLE
        }
    }

    private class VideoDiffCallback : DiffUtil.ItemCallback<Uri>() {
        override fun areItemsTheSame(oldItem: Uri, newItem: Uri): Boolean {
            return oldItem == newItem
        }

        override fun areContentsTheSame(oldItem: Uri, newItem: Uri): Boolean {
            return oldItem == newItem
        }
    }
}
