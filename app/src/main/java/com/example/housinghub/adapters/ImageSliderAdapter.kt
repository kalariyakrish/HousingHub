package com.example.housinghub.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.housinghub.R

class ImageSliderAdapter(
    private val onImageClick: ((Int) -> Unit)? = null
) : RecyclerView.Adapter<ImageSliderAdapter.SliderViewHolder>() {

    private var imageUrls: List<String> = listOf()

    inner class SliderViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val imageView: ImageView = itemView.findViewById(R.id.sliderImage)

        init {
            imageView.setOnClickListener {
                onImageClick?.invoke(adapterPosition)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SliderViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.slider_image, parent, false)
        return SliderViewHolder(view)
    }

    override fun onBindViewHolder(holder: SliderViewHolder, position: Int) {
        val imageUrl = imageUrls[position]
        
        // Load image using Glide
        Glide.with(holder.itemView.context)
            .load(imageUrl)
            .placeholder(R.drawable.image_placeholder_bg)
            .error(R.drawable.image_placeholder_bg)
            .centerCrop()
            .into(holder.imageView)
    }

    override fun getItemCount(): Int = imageUrls.size

    fun submitList(newImages: List<String>) {
        imageUrls = newImages
        notifyDataSetChanged()
    }
}

