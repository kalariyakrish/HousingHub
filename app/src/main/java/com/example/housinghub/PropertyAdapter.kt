package com.example.housinghub

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.housinghub.R
import com.example.housinghub.SharedViewModel.Viewmodel.SharedViewModel
import com.example.housinghub.databinding.ItemPropertyBinding
import com.example.housinghub.model.Property

class PropertyAdapter(
    private val sharedViewModel: SharedViewModel? = null,
    private val bookmarkClickListener: BookmarkClickListener? = null,
    private val itemClickListener: ((Property) -> Unit)? = null
) : RecyclerView.Adapter<PropertyAdapter.PropertyViewHolder>() {

    private val properties = mutableListOf<Property>()

    // Expose items for fragments to read (used by HomeFragment search)
    val items: List<Property> get() = properties

    inner class PropertyViewHolder(private val binding: ItemPropertyBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(property: Property) {
            binding.titleText.text = property.title
            // support older field name `address` as fallback
            binding.locationText.text = if (property.location.isNotEmpty()) property.location else property.address
            binding.priceText.text = "₹${property.price}/mo"

            // Owner info
            binding.ownerText.text = "Owner: ${if (property.ownerId.isNotEmpty()) property.ownerId else "Not specified"}"

            // Availability label and color
            binding.availabilityText.text = if (property.isAvailable) "Available" else "Not Available"
            val color = if (property.isAvailable) R.color.green_500 else R.color.red_500
            binding.availabilityText.setTextColor(binding.root.context.getColor(color))

            // Load image
            if (property.images.isNotEmpty()) {
                Glide.with(binding.propertyImage.context)
                    .load(property.images[0])
                    .placeholder(R.drawable.placeholder_image)
                    .into(binding.propertyImage)
            } else {
                binding.propertyImage.setImageResource(R.drawable.placeholder_image)
            }

            // Bookmark
            if (sharedViewModel != null && bookmarkClickListener != null) {
                binding.bookmarkIcon.setImageResource(
                    if (property.isBookmarked) R.drawable.ic_bookmark_filled else R.drawable.ic_bookmark_border
                )

                binding.bookmarkIcon.setOnClickListener {
                    val pos = adapterPosition
                    if (pos != RecyclerView.NO_POSITION) {
                        property.isBookmarked = !property.isBookmarked
                        sharedViewModel.toggleBookmark(property)
                        bookmarkClickListener.onBookmarkClicked(property, pos)
                        notifyItemChanged(pos)
                    }
                }

                binding.bookmarkIcon.visibility = View.VISIBLE
            } else {
                binding.bookmarkIcon.visibility = View.GONE
            }

            // Item click -> open details
            binding.root.setOnClickListener {
                val pos = adapterPosition
                if (pos != RecyclerView.NO_POSITION) {
                    itemClickListener?.invoke(property)
                }
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
