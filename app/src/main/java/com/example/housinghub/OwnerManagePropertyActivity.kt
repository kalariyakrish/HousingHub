package com.example.housinghub

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.housinghub.databinding.ActivityOwnerManagePropertyBinding
import com.example.housinghub.model.Property
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class OwnerManagePropertyActivity : AppCompatActivity() {

    private lateinit var binding: ActivityOwnerManagePropertyBinding
    private lateinit var adapter: OwnerManagePropertyAdapter
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private var allProperties = mutableListOf<Property>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityOwnerManagePropertyBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupToolbar()
        setupRecyclerView()
        loadProperties()
    }

    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(true)
            setDisplayShowHomeEnabled(true)
            title = "Manage Properties"
        }
        binding.toolbar.setNavigationOnClickListener {
            onBackPressed()
        }
    }

    private fun setupRecyclerView() {
        adapter = OwnerManagePropertyAdapter(
            properties = allProperties,
            onToggleAvailability = { property -> togglePropertyAvailability(property) },
            onEditProperty = { property -> editProperty(property) },
            onDeleteProperty = { property -> deleteProperty(property) },
            onViewDetails = { property -> viewPropertyDetails(property) }
        )
        
        binding.recyclerProperties.apply {
            layoutManager = LinearLayoutManager(this@OwnerManagePropertyActivity)
            adapter = this@OwnerManagePropertyActivity.adapter
        }
    }

    private fun loadProperties() {
        showLoading(true)
        val currentUser = auth.currentUser?.uid
        
        if (currentUser == null) {
            showError("User not authenticated")
            return
        }

        Log.d("ManageProperty", "Loading properties for user: $currentUser")

        CoroutineScope(Dispatchers.Main).launch {
            try {
                allProperties.clear()
                
                // Load from Available collection
                val availableProperties = loadPropertiesFromCollection(currentUser, "Available", true)
                Log.d("ManageProperty", "Found ${availableProperties.size} available properties")
                
                // Load from Unavailable collection  
                val unavailableProperties = loadPropertiesFromCollection(currentUser, "Unavailable", false)
                Log.d("ManageProperty", "Found ${unavailableProperties.size} unavailable properties")
                
                // Combine both lists
                allProperties.addAll(availableProperties)
                allProperties.addAll(unavailableProperties)
                
                // Sort by timestamp (most recent first)
                allProperties.sortByDescending { it.timestamp }
                
                Log.d("ManageProperty", "Total properties loaded: ${allProperties.size}")
                
                // Update UI
                showLoading(false)
                updateUI()
                
            } catch (e: Exception) {
                Log.e("ManageProperty", "Error loading properties", e)
                showLoading(false)
                showError("Failed to load properties: ${e.message}")
            }
        }
    }

    private suspend fun loadPropertiesFromCollection(ownerId: String, collection: String, isAvailable: Boolean): List<Property> {
        return try {
            val snapshot = db.collection("Properties")
                .document(ownerId)
                .collection(collection)
                .get()
                .await()
            
            snapshot.documents.mapNotNull { doc ->
                doc.toObject(Property::class.java)?.apply {
                    id = doc.id
                    this.ownerId = ownerId
                    this.isAvailable = isAvailable
                }
            }
        } catch (e: Exception) {
            Log.e("ManageProperty", "Error loading $collection properties", e)
            emptyList()
        }
    }

    private fun updateUI() {
        if (allProperties.isEmpty()) {
            binding.recyclerProperties.visibility = View.GONE
            binding.layoutEmpty.visibility = View.VISIBLE
            binding.tvEmptyMessage.text = "No properties found. Add your first property!"
            binding.btnAddProperty.setOnClickListener {
                startActivity(Intent(this, OwnerAddPropertyActivity::class.java))
            }
        } else {
            binding.recyclerProperties.visibility = View.VISIBLE
            binding.layoutEmpty.visibility = View.GONE
            adapter.notifyDataSetChanged()
        }
        
        // Update statistics
        val availableCount = allProperties.count { it.isAvailable }
        val unavailableCount = allProperties.count { !it.isAvailable }
        
        binding.tvStats.text = "Total: ${allProperties.size} | Available: $availableCount | Unavailable: $unavailableCount"
    }

    private fun togglePropertyAvailability(property: Property) {
        Log.d("ManageProperty", "Toggling availability for property: ${property.id}")
        
        CoroutineScope(Dispatchers.Main).launch {
            try {
                val newAvailability = !property.isAvailable
                val currentUser = auth.currentUser?.uid ?: return@launch
                
                // Source and destination collections
                val sourceCollection = if (property.isAvailable) "Available" else "Unavailable"
                val destCollection = if (property.isAvailable) "Unavailable" else "Available"
                
                Log.d("ManageProperty", "Moving from $sourceCollection to $destCollection")
                
                // References
                val sourceRef = db.collection("Properties")
                    .document(currentUser)
                    .collection(sourceCollection)
                    .document(property.id)
                
                val destRef = db.collection("Properties")
                    .document(currentUser)
                    .collection(destCollection)
                    .document(property.id)
                
                // Run transaction
                db.runTransaction { transaction ->
                    // Get the property from source
                    val propertyDoc = transaction.get(sourceRef)
                    if (!propertyDoc.exists()) {
                        throw Exception("Property not found in $sourceCollection")
                    }
                    
                    // Get property data and update availability
                    val propertyData = propertyDoc.data ?: throw Exception("Invalid property data")
                    val updatedData = propertyData.toMutableMap()
                    updatedData["isAvailable"] = newAvailability
                    updatedData["updatedAt"] = System.currentTimeMillis()
                    
                    // Move to new collection
                    transaction.set(destRef, updatedData)
                    transaction.delete(sourceRef)
                }.await()
                
                Log.d("ManageProperty", "Property moved successfully")
                
                // Update local property
                property.isAvailable = newAvailability
                adapter.notifyDataSetChanged()
                updateUI()
                
                val statusText = if (newAvailability) "available" else "unavailable"
                Toast.makeText(this@OwnerManagePropertyActivity, "Property marked as $statusText", Toast.LENGTH_SHORT).show()
                
            } catch (e: Exception) {
                Log.e("ManageProperty", "Error toggling availability", e)
                Toast.makeText(this@OwnerManagePropertyActivity, "Failed to update property: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun editProperty(property: Property) {
        // TODO: Implement property editing functionality
        Toast.makeText(this, "Edit property feature coming soon", Toast.LENGTH_SHORT).show()
    }

    private fun deleteProperty(property: Property) {
        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("Delete Property")
            .setMessage("Are you sure you want to delete \"${property.title}\"? This action cannot be undone.")
            .setPositiveButton("Delete") { _, _ ->
                performDeleteProperty(property)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun performDeleteProperty(property: Property) {
        CoroutineScope(Dispatchers.Main).launch {
            try {
                val currentUser = auth.currentUser?.uid ?: return@launch
                val collection = if (property.isAvailable) "Available" else "Unavailable"
                
                db.collection("Properties")
                    .document(currentUser)
                    .collection(collection)
                    .document(property.id)
                    .delete()
                    .await()
                
                // Remove from local list
                allProperties.remove(property)
                adapter.notifyDataSetChanged()
                updateUI()
                
                Toast.makeText(this@OwnerManagePropertyActivity, "Property deleted successfully", Toast.LENGTH_SHORT).show()
                
            } catch (e: Exception) {
                Log.e("ManageProperty", "Error deleting property", e)
                Toast.makeText(this@OwnerManagePropertyActivity, "Failed to delete property: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun viewPropertyDetails(property: Property) {
        PropertyDetailsActivity.start(this, property.ownerId, property.id)
    }

    private fun showLoading(isLoading: Boolean) {
        binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
        binding.recyclerProperties.visibility = if (isLoading) View.GONE else View.VISIBLE
    }

    private fun showError(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
    }

    override fun onResume() {
        super.onResume()
        // Refresh properties when returning to this activity
        loadProperties()
    }
}
