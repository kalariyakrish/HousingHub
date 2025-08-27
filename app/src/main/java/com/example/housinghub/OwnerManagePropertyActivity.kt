package com.example.housinghub

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.housinghub.databinding.ActivityOwnerManagePropertyBinding
import com.example.housinghub.model.Property
import com.example.housinghub.owner.ManagePropertyAdapter
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query

class OwnerManagePropertyActivity : AppCompatActivity() {

    private lateinit var binding: ActivityOwnerManagePropertyBinding
    private lateinit var adapter: ManagePropertyAdapter
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityOwnerManagePropertyBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupRecyclerView()
        loadProperties()
    }

    private fun setupRecyclerView() {
        adapter = ManagePropertyAdapter { property, isAvailable ->
            updatePropertyAvailability(property.id, isAvailable)
        }
        binding.recyclerProperties.apply {
            layoutManager = LinearLayoutManager(this@OwnerManagePropertyActivity)
            adapter = this@OwnerManagePropertyActivity.adapter
        }
    }

    private fun updatePropertyAvailability(id: String, available: Boolean) {
        TODO("Not yet implemented")
    }

    private fun loadProperties() {
        val currentUser = auth.currentUser?.email ?: return
        val allProperties = mutableListOf<Property>()

        // Fetch both Available and Unavailable properties
        val collections = listOf("Available", "Unavailable")
        var completedQueries = 0

        collections.forEach { collection ->
            db.collection("Properties")
                .document(currentUser)
                .collection(collection)
                .get()
                .addOnSuccessListener { snapshot ->
                    val properties = snapshot.documents.mapNotNull { doc ->
                        doc.toObject(Property::class.java)?.apply {
                            id = doc.id
                            ownerId = currentUser
                            isAvailable = collection == "Available"
                        }
                    }
                    allProperties.addAll(properties)

                    completedQueries++
                    if (completedQueries == collections.size) {
                        adapter.updateData(allProperties.sortedByDescending { it.timestamp })
                    }
                }
        }

        fun updatePropertyAvailability(propertyId: String, isAvailable: Boolean) {
            val currentUser = auth.currentUser?.email ?: return

            // Source and destination collections
            val sourceCollection = if (isAvailable) "Unavailable" else "Available"
            val destCollection = if (isAvailable) "Available" else "Unavailable"

            // References for the transaction
            val sourceRef = db.collection("Properties")
                .document(currentUser)
                .collection(sourceCollection)
                .document(propertyId)

            val destRef = db.collection("Properties")
                .document(currentUser)
                .collection(destCollection)
                .document(propertyId)

            db.runTransaction { transaction ->
                // Get the property from source
                val propertyDoc = transaction.get(sourceRef)
                if (!propertyDoc.exists()) {
                    throw Exception("Property not found")
                }

                // Get property data and update availability
                val property = propertyDoc.toObject(Property::class.java)?.copy(
                    isAvailable = isAvailable
                ) ?: throw Exception("Invalid property data")

                // Move to new collection
                transaction.set(destRef, property)
                transaction.delete(sourceRef)
            }
        }
    }
}
