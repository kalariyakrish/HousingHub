package com.example.housinghub.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.housinghub.R
import com.example.housinghub.SharedViewModel.Viewmodel.SharedViewModel
import com.example.housinghub.databinding.ItemPropertyEnhancedBinding
import com.example.housinghub.model.Property
import com.google.android.material.button.MaterialButton
import com.google.android.material.chip.Chip
import com.google.android.material.floatingactionbutton.FloatingActionButton

interface PropertyInteractionListener {
    fun onPropertyClicked(property: Property)
    fun onBookmarkClicked(property: Property, position: Int)
    fun onViewDetailsClicked(property: Property)
    fun onContactClicked(property: Property)
}

class EnhancedPropertyAdapter(
    private val sharedViewModel: SharedViewModel? = null,
    private val interactionListener: PropertyInteractionListener? = null
) : RecyclerView.Adapter<EnhancedPropertyAdapter.EnhancedPropertyViewHolder>() {

    private val properties = mutableListOf<Property>()

    // Expose items for fragments to read (used by HomeFragment search)
    val items: List<Property> get() = properties

    inner class EnhancedPropertyViewHolder(private val binding: ItemPropertyEnhancedBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(property: Property) {
            // Basic property information
            binding.titleText.text = property.title
            binding.locationText.text = if (property.location.isNotEmpty()) property.location else property.address
            binding.priceText.text = "₹${String.format("%.0f", property.price)}/mo"

            // Property type chip
            binding.chipPropertyType.text = property.type

            // Owner information
            val ownerName = if (property.ownerId.isNotEmpty()) {
                property.ownerId.substringBefore("@").replace(".", " ")
                    .split(" ").joinToString(" ") { 
                        it.replaceFirstChar { char -> char.uppercaseChar() } 
                    }
            } else "Owner"
            binding.ownerText.text = ownerName

            // Availability status
            updateAvailabilityChip(property.isAvailable)

            // Load property image
            loadPropertyImage(property)

            // Update bookmark state
            updateBookmarkIcon(property.isBookmarked)

            // Rating (if available)
            updateRating(property)

            // Set click listeners
            setupClickListeners(property)
        }

        private fun updateAvailabilityChip(isAvailable: Boolean) {
            binding.chipAvailability.text = if (isAvailable) "Available" else "Not Available"
            val colorRes = if (isAvailable) R.color.primary_green else R.color.red_500
            binding.chipAvailability.setChipBackgroundColorResource(colorRes)
        }

        private fun loadPropertyImage(property: Property) {
            if (property.images.isNotEmpty()) {
                Glide.with(binding.propertyImage.context)
                    .load(property.images[0])
                    .placeholder(R.drawable.placeholder_image)
                    .centerCrop()
                    .into(binding.propertyImage)
            } else {
                binding.propertyImage.setImageResource(R.drawable.placeholder_image)
            }
        }

        private fun updateBookmarkIcon(isBookmarked: Boolean) {
            val iconRes = if (isBookmarked) {
                R.drawable.ic_bookmark_filled
            } else {
                R.drawable.ic_bookmark_border
            }
            binding.bookmarkIcon.setImageResource(iconRes)
        }


        private fun updateRating(property: Property) {
            if (property.rating != null && property.rating!! > 0) {
                binding.layoutRating.visibility = View.VISIBLE
                binding.tvRating.text = String.format("%.1f", property.rating)
            } else {
                binding.layoutRating.visibility = View.GONE
            }
        }

        private fun setupClickListeners(property: Property) {
            // Main card click
            binding.root.setOnClickListener {
                interactionListener?.onPropertyClicked(property)
            }

            // Bookmark click
            binding.bookmarkIcon.setOnClickListener {
                val pos = adapterPosition
                if (pos != RecyclerView.NO_POSITION) {
                    // Only notify the listener, don't handle the toggle here
                    // The fragment/activity will handle the actual toggle
                    interactionListener?.onBookmarkClicked(property, pos)
                }
            }

            // View details button
            binding.btnViewDetails.setOnClickListener {
                interactionListener?.onViewDetailsClicked(property)
            }

            // Contact button
            binding.btnContact.setOnClickListener {
                interactionListener?.onContactClicked(property)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): EnhancedPropertyViewHolder {
        val binding = ItemPropertyEnhancedBinding.inflate(
            LayoutInflater.from(parent.context), 
            parent, 
            false
        )
        return EnhancedPropertyViewHolder(binding)
    }

    override fun onBindViewHolder(holder: EnhancedPropertyViewHolder, position: Int) {
        holder.bind(properties[position])
    }

    override fun getItemCount(): Int = properties.size

    fun updateData(newList: List<Property>) {
        properties.clear()
        properties.addAll(newList)
        notifyDataSetChanged()
    }

    fun addProperty(property: Property) {
        properties.add(0, property)
        notifyItemInserted(0)
    }

    fun removeProperty(position: Int) {
        if (position >= 0 && position < properties.size) {
            properties.removeAt(position)
            notifyItemRemoved(position)
        }
    }

    fun updateProperty(position: Int, property: Property) {
        if (position >= 0 && position < properties.size) {
            properties[position] = property
            notifyItemChanged(position)
        }
    }
}
