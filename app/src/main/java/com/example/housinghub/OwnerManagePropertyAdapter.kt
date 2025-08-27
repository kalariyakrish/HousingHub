package com.example.housinghub

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.housinghub.model.Property
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import com.google.android.material.chip.Chip
import java.text.SimpleDateFormat
import java.util.*

class OwnerManagePropertyAdapter(
    private val properties: MutableList<Property>,
    private val onToggleAvailability: (Property) -> Unit,
    private val onEditProperty: (Property) -> Unit,
    private val onDeleteProperty: (Property) -> Unit,
    private val onViewDetails: (Property) -> Unit
) : RecyclerView.Adapter<OwnerManagePropertyAdapter.PropertyViewHolder>() {

    inner class PropertyViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val cardView: MaterialCardView = itemView.findViewById(R.id.cardProperty)
        private val ivPropertyImage: ImageView = itemView.findViewById(R.id.ivPropertyImage)
        private val tvTitle: TextView = itemView.findViewById(R.id.tvPropertyTitle)
        private val tvLocation: TextView = itemView.findViewById(R.id.tvPropertyLocation)
        private val tvPrice: TextView = itemView.findViewById(R.id.tvPropertyPrice)
        private val tvType: TextView = itemView.findViewById(R.id.tvPropertyType)
        private val chipStatus: Chip = itemView.findViewById(R.id.chipStatus)
        private val tvDateAdded: TextView = itemView.findViewById(R.id.tvDateAdded)
        private val btnToggleAvailability: MaterialButton = itemView.findViewById(R.id.btnToggleAvailability)
        private val btnEdit: MaterialButton = itemView.findViewById(R.id.btnEdit)
        private val btnDelete: MaterialButton = itemView.findViewById(R.id.btnDelete)
        private val btnViewDetails: MaterialButton = itemView.findViewById(R.id.btnViewDetails)

        fun bind(property: Property) {
            // Set property details
            tvTitle.text = property.title
            tvLocation.text = property.address.ifEmpty { property.location }
            tvPrice.text = "₹${String.format("%.0f", property.price)}/month"
            tvType.text = property.type

            // Set status chip
            if (property.isAvailable) {
                chipStatus.text = "Available"
                chipStatus.setChipBackgroundColorResource(android.R.color.holo_green_light)
                chipStatus.setTextColor(ContextCompat.getColor(itemView.context, android.R.color.white))
            } else {
                chipStatus.text = "Unavailable"
                chipStatus.setChipBackgroundColorResource(android.R.color.holo_red_light)
                chipStatus.setTextColor(ContextCompat.getColor(itemView.context, android.R.color.white))
            }

            // Set date added
            if (property.timestamp!="0") {
                val dateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
                tvDateAdded.text = "Added: ${dateFormat.format(Date(property.timestamp))}"
            } else {
                tvDateAdded.text = "Added: Recently"
            }

            // Load property image
            if (property.images.isNotEmpty()) {
                Glide.with(itemView.context)
                    .load(property.images[0])
                    .placeholder(R.drawable.placeholder_image)
                    .centerCrop()
                    .into(ivPropertyImage)
            } else {
                ivPropertyImage.setImageResource(R.drawable.placeholder_image)
            }

            // Set button states and listeners
            setupButtons(property)
        }

        private fun setupButtons(property: Property) {
            // Toggle availability button
            if (property.isAvailable) {
                btnToggleAvailability.text = "Mark Unavailable"
                btnToggleAvailability.setBackgroundColor(
                    ContextCompat.getColor(itemView.context, android.R.color.holo_orange_light)
                )
            } else {
                btnToggleAvailability.text = "Mark Available"
                btnToggleAvailability.setBackgroundColor(
                    ContextCompat.getColor(itemView.context, android.R.color.holo_green_light)
                )
            }

            btnToggleAvailability.setOnClickListener {
                onToggleAvailability(property)
            }

            btnEdit.setOnClickListener {
                onEditProperty(property)
            }

            btnDelete.setOnClickListener {
                onDeleteProperty(property)
            }

            btnViewDetails.setOnClickListener {
                onViewDetails(property)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PropertyViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_owner_manage_property, parent, false)
        return PropertyViewHolder(view)
    }

    override fun onBindViewHolder(holder: PropertyViewHolder, position: Int) {
        holder.bind(properties[position])
    }

    override fun getItemCount(): Int = properties.size

    fun updateData(newProperties: List<Property>) {
        properties.clear()
        properties.addAll(newProperties)
        notifyDataSetChanged()
    }

    fun removeProperty(property: Property) {
        val position = properties.indexOf(property)
        if (position != -1) {
            properties.removeAt(position)
            notifyItemRemoved(position)
        }
    }
}
