package com.example.housinghub.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.example.housinghub.databinding.ItemPropertyImageCarouselBinding

class PropertyImageCarouselAdapter(
    private val onImageClick: (Int) -> Unit = {}
) : RecyclerView.Adapter<PropertyImageCarouselAdapter.ImageViewHolder>() {

    private val images = mutableListOf<String>()

    inner class ImageViewHolder(private val binding: ItemPropertyImageCarouselBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(imageUrl: String, position: Int) {
            Glide.with(binding.root.context)
                .load(imageUrl)
                .centerCrop()
                .diskCacheStrategy(DiskCacheStrategy.AUTOMATIC)
                .into(binding.imageView)

            binding.root.setOnClickListener { onImageClick(position) }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ImageViewHolder {
        val binding = ItemPropertyImageCarouselBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ImageViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ImageViewHolder, position: Int) {
        holder.bind(images[position], position)
    }

    override fun getItemCount(): Int = images.size

    fun submitList(list: List<String>) {
        images.clear()
        images.addAll(list)
        notifyDataSetChanged()
    }

    fun getCurrentList(): List<String> = images.toList()
}
