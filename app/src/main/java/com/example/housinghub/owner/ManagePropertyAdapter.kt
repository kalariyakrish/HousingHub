package com.example.housinghub.owner

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.housinghub.databinding.ItemManagePropertyBinding
import com.example.housinghub.model.Property

class ManagePropertyAdapter(
    private val onAvailabilityToggled: (Property, Boolean) -> Unit
) : RecyclerView.Adapter<ManagePropertyAdapter.PropertyViewHolder>() {

    private val properties = mutableListOf<Property>()

    inner class PropertyViewHolder(private val binding: ItemManagePropertyBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(property: Property) {
            binding.tvPropertyTitle.text = property.title
            binding.tvPropertyLocation.text = property.location
            binding.tvPropertyPrice.text = "₹${property.price}/month"

            // Set button text based on availability
            binding.btnToggleAvailability.text = if (property.isAvailable) "Mark Unavailable" else "Mark Available"
            
            binding.btnToggleAvailability.setOnClickListener {
                onAvailabilityToggled(property, !property.isAvailable)
            }

            binding.btnManageProperty.setOnClickListener {
                // Handle manage property click
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PropertyViewHolder {
        val binding = ItemManagePropertyBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
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
