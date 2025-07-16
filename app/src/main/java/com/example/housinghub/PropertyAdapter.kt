package com.example.housinghub

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.housinghub.R.color.green_500
import com.example.housinghub.SharedViewModel.Viewmodel.SharedViewModel
import com.example.housinghub.databinding.ItemPropertyBinding
import com.example.housinghub.model.Property // Updated import

class PropertyAdapter(
    private val sharedViewModel: SharedViewModel? = null,
    private val bookmarkClickListener: BookmarkClickListener? = null
) : RecyclerView.Adapter<PropertyAdapter.PropertyViewHolder>() {

    private val properties = mutableListOf<Property>()

    inner class PropertyViewHolder(private val binding: ItemPropertyBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(property: Property) {
            binding.titleText.text = property.title
            binding.locationText.text = property.location
            binding.priceText.text = "₹${property.price}/month"

            // Show owner information if available
            binding.ownerText.text = "Owner: ${if (property.ownerId.isNotEmpty()) property.ownerId else "Not specified"}"

            // Show availability status
            binding.availabilityText.text = if (property.isAvailable) "Available" else "Not Available"
            binding.availabilityText.setTextColor(
                binding.root.context.getColor(
                    if (property.isAvailable) green_500 else R.color.red_500
                )
            )

            // Load first image if available
            if (property.images.isNotEmpty()) {
                Glide.with(binding.propertyImage.context)
                    .load(property.images[0])
                    .placeholder(R.drawable.placeholder_image)
                    .into(binding.propertyImage)
            } else {
                binding.propertyImage.setImageResource(R.drawable.placeholder_image)
            }

            // Handle bookmarks
            if (sharedViewModel != null && bookmarkClickListener != null) {
                binding.bookmarkIcon.setImageResource(
                    if (property.isBookmarked) R.drawable.ic_bookmark_filled
                    else R.drawable.ic_bookmark_border
                )

                binding.bookmarkIcon.setOnClickListener {
                    property.isBookmarked = !property.isBookmarked
                    sharedViewModel.toggleBookmark(property)
                    bookmarkClickListener.onBookmarkClicked(property, adapterPosition)
                }

                binding.bookmarkIcon.visibility = ViewGroup.VISIBLE
            } else {
                binding.bookmarkIcon.visibility = ViewGroup.GONE
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PropertyViewHolder {
        val binding = ItemPropertyBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return PropertyViewHolder(binding)
    }

    override fun onBindViewHolder(holder: PropertyViewHolder, position: Int) {
        holder.bind(properties[position])
    }

    override fun getItemCount(): Int = properties.size

    fun updateData(newList: List<Property>) {
        properties.clear()
        properties.addAll(newList)
        notifyDataSetChanged()
    }
}
