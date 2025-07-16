package com.example.housinghub.owner

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.housinghub.R
import com.example.housinghub.model.Property

class ManagePropertyAdapter(
    private val propertyList: List<Property>,
    private val onManageClick: (Property) -> Unit
) : RecyclerView.Adapter<ManagePropertyAdapter.PropertyViewHolder>() {

    inner class PropertyViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val title: TextView = itemView.findViewById(R.id.tvPropertyTitle)
        val location: TextView = itemView.findViewById(R.id.tvPropertyLocation)
        val price: TextView = itemView.findViewById(R.id.tvPropertyPrice)
        val manageButton: Button = itemView.findViewById(R.id.btnManageProperty)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PropertyViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_manage_property, parent, false)
        return PropertyViewHolder(view)
    }

    override fun onBindViewHolder(holder: PropertyViewHolder, position: Int) {
        val property = propertyList[position]
        holder.title.text = property.title
        holder.location.text = property.location
        holder.price.text = "₹${property.price}"
        holder.manageButton.setOnClickListener {
            onManageClick(property)
        }
    }

    override fun getItemCount(): Int = propertyList.size
}
