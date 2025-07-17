package com.example.housinghub

import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide

class ImageSliderAdapter(private val images: MutableList<Uri> = mutableListOf(),
                        private val onImageClick: (Uri) -> Unit = {},
                        private val onDeleteClick: (Int) -> Unit = {}) : 
    RecyclerView.Adapter<ImageSliderAdapter.ImageViewHolder>() {

    inner class ImageViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val imageView: ImageView = view.findViewById(R.id.sliderImage)
        val btnDelete: ImageButton = view.findViewById(R.id.btnDelete)

        init {
            imageView.setOnClickListener {
                val position = adapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    onImageClick(images[position])
                }
            }
            
            btnDelete.setOnClickListener {
                val position = adapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    onDeleteClick(position)
                }
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ImageViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.slider_image, parent, false)
        return ImageViewHolder(view)
    }

    override fun onBindViewHolder(holder: ImageViewHolder, position: Int) {
        Glide.with(holder.itemView.context)
            .load(images[position])
            .centerCrop()
            .into(holder.imageView)
    }

    override fun getItemCount(): Int = images.size

    fun submitList(newList: List<String>) {
        val oldSize = images.size
        images.clear()
        // Convert each string to Uri before adding to the images list
        val uriList = newList.map { Uri.parse(it) }
        images.addAll(uriList)
        
        // Use more specific change events as recommended
        if (oldSize == 0 && uriList.isNotEmpty()) {
            notifyItemRangeInserted(0, uriList.size)
        } else if (uriList.isEmpty() && oldSize > 0) {
            notifyItemRangeRemoved(0, oldSize)
        } else if (uriList.size != oldSize) {
            // Different sizes, use specific notifications
            if (oldSize > 0) {
                notifyItemRangeRemoved(0, oldSize)
            }
            if (uriList.isNotEmpty()) {
                notifyItemRangeInserted(0, uriList.size)
            }
        } else {
            // Same size but potentially different content
            notifyItemRangeChanged(0, uriList.size)
        }
    }
}
