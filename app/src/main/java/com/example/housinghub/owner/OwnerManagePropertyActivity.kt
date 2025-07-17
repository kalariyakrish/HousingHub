package com.example.housinghub.owner

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.housinghub.databinding.ActivityOwnerManagePropertyBinding
import com.example.housinghub.model.Property
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
                    return@addSnapshotListener
                }

                val properties = snapshot?.documents?.mapNotNull { doc ->
                    doc.toObject(Property::class.java)
                } ?: listOf()
                
                adapter.updateData(properties)
            }
    }

    private fun updatePropertyAvailability(propertyId: String, isAvailable: Boolean) {
        db.collection("Properties")
            .document(propertyId)
            .update("isAvailable", isAvailable)
    }
}