package com.example.housinghub

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.housinghub.R
import com.example.housinghub.model.Property
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.util.Locale

class ManagePropertyAdapter(
    private val propertyList: MutableList<Property>,
    private val onManageClick: (Property) -> Unit
) : RecyclerView.Adapter<ManagePropertyAdapter.PropertyViewHolder>() {

    private val firestore = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    inner class PropertyViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val title: TextView = itemView.findViewById(R.id.tvPropertyTitle)
        val location: TextView = itemView.findViewById(R.id.tvPropertyLocation)
        val price: TextView = itemView.findViewById(R.id.tvPropertyPrice)
        val availabilityButton: Button = itemView.findViewById(R.id.btnToggleAvailability)
        val manageButton: Button = itemView.findViewById(R.id.btnManageProperty)

        fun bind(property: Property) {
            title.text = property.title
            location.text = property.location
            price.text = String.format(Locale.getDefault(), "₹%.0f", property.price)

            availabilityButton.text = if (property.isAvailable) "Mark Unavailable" else "Mark Available"
            availabilityButton.setOnClickListener {
                toggleAvailability(property)
            }

            manageButton.setOnClickListener {
                onManageClick(property)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PropertyViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_manage_property, parent, false)
        return PropertyViewHolder(view)
    }

    override fun onBindViewHolder(holder: PropertyViewHolder, position: Int) {
        holder.bind(propertyList[position])
    }

    override fun getItemCount(): Int = propertyList.size

    private fun toggleAvailability(property: Property) {
        val ownerEmail = auth.currentUser?.email ?: return
        val newAvailability = !property.isAvailable

        // Source and destination collection names
        val sourceCollection = if (property.isAvailable) "Available" else "Unavailable"
        val destinationCollection = if (newAvailability) "Available" else "Unavailable"

        // References to source and destination documents
        val sourceRef = firestore.collection("Properties")
            .document(ownerEmail)
            .collection(sourceCollection)
            .document(property.id)

        val destinationRef = firestore.collection("Properties")
            .document(ownerEmail)
            .collection(destinationCollection)
            .document(property.id)

        // Start a transaction to move the property
        firestore.runTransaction { transaction ->
            // First, get the current property data
            val propertySnapshot = transaction.get(sourceRef)
            if (!propertySnapshot.exists()) {
                throw Exception("Property does not exist in source collection")
            }

            // Create updated property data
            val updatedProperty = propertySnapshot.toObject(Property::class.java)?.copy(
                isAvailable = newAvailability
            ) ?: throw Exception("Could not convert property data")

            // Copy to new location
            transaction.set(destinationRef, updatedProperty)
            // Delete from old location
            transaction.delete(sourceRef)
        }.addOnSuccessListener {
            // Update local list
            val index = propertyList.indexOf(property)
            propertyList[index] = property.copy(isAvailable = newAvailability)
            notifyItemChanged(index)
        }.addOnFailureListener { e ->
            // Show error message if the transaction failed
            // Handle error silently or log it
            e.printStackTrace()
        }
    }

    fun updateData(newList: List<Property>) {
        propertyList.clear()
        propertyList.addAll(newList)
        notifyDataSetChanged()
    }
}
