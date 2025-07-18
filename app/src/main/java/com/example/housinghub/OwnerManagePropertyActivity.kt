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

    private fun loadProperties() {
        val currentUser = auth.currentUser?.email ?: return

        db.collection("Properties")
            .whereEqualTo("ownerId", currentUser)
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    // Handle error
                    return@addSnapshotListener
                }

                val properties = snapshot?.documents?.mapNotNull { doc ->
                    val property = doc.toObject(Property::class.java)
                    property?.id = doc.id // Make sure to set the document ID
                    property
                } ?: listOf()

                adapter.updateData(properties)
            }
    }

    private fun updatePropertyAvailability(propertyId: String, isAvailable: Boolean) {
        db.collection("Properties")
            .document(propertyId)
            .update("isAvailable", isAvailable)
            .addOnSuccessListener {
                // Property availability updated successfully
            }
            .addOnFailureListener { e ->
                // Handle the error
            }
    }
}
