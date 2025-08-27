package com.example.housinghub.managers

import android.content.Context
import android.util.Log
import com.example.housinghub.model.Property
import com.example.housinghub.utils.UserSessionManager
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

class BookmarkManager(private val context: Context) {
    
    private val firestore = FirebaseFirestore.getInstance()
    private val userSessionManager = UserSessionManager(context)
    
    companion object {
        private const val TAG = "BookmarkManager"
    }
    
    /**
     * Add property to bookmarks in Firestore
     * Structure: bookmarked/{tenantEmail}/properties/{propertyId}
     */
    suspend fun addBookmark(property: Property): Result<Unit> {
        return try {
            val tenantEmail = userSessionManager.getEmail()
            Log.d(TAG, "Adding bookmark for property: ${property.id}, tenant: $tenantEmail")
            
            if (tenantEmail.isEmpty()) {
                Log.e(TAG, "User not logged in")
                return Result.failure(Exception("User not logged in"))
            }
            
            // Create a copy of the property with bookmark flag set
            val bookmarkedProperty = property.copy().apply {
                isBookmarked = true
            }
            
            firestore.collection("bookmarked")
                .document(tenantEmail)
                .collection("properties")
                .document(property.id)
                .set(bookmarkedProperty)
                .await()
            
            Log.d(TAG, "Successfully added bookmark for property: ${property.id}")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to add bookmark: ${e.message}", e)
            Result.failure(e)
        }
    }
    
    /**
     * Remove property from bookmarks in Firestore
     */
    suspend fun removeBookmark(propertyId: String): Result<Unit> {
        return try {
            val tenantEmail = userSessionManager.getEmail()
            Log.d(TAG, "Removing bookmark for property: $propertyId, tenant: $tenantEmail")
            
            if (tenantEmail.isEmpty()) {
                Log.e(TAG, "User not logged in")
                return Result.failure(Exception("User not logged in"))
            }
            
            firestore.collection("bookmarked")
                .document(tenantEmail)
                .collection("properties")
                .document(propertyId)
                .delete()
                .await()
            
            Log.d(TAG, "Successfully removed bookmark for property: $propertyId")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to remove bookmark: ${e.message}", e)
            Result.failure(e)
        }
    }
    
    /**
     * Check if property is bookmarked
     */
    suspend fun isBookmarked(propertyId: String): Boolean {
        return try {
            val tenantEmail = userSessionManager.getEmail()
            if (tenantEmail.isEmpty()) {
                Log.w(TAG, "User not logged in, property not bookmarked")
                return false
            }
            
            val document = firestore.collection("bookmarked")
                .document(tenantEmail)
                .collection("properties")
                .document(propertyId)
                .get()
                .await()
            
            val exists = document.exists()
            Log.d(TAG, "Property $propertyId bookmark status: $exists")
            exists
        } catch (e: Exception) {
            Log.e(TAG, "Error checking bookmark status: ${e.message}", e)
            false
        }
    }
    
    /**
     * Get all bookmarked properties for current tenant
     */
    suspend fun getBookmarkedProperties(): Result<List<Property>> {
        return try {
            val tenantEmail = userSessionManager.getEmail()
            Log.d(TAG, "Loading bookmarked properties for tenant: $tenantEmail")
            
            if (tenantEmail.isEmpty()) {
                Log.e(TAG, "User not logged in")
                return Result.failure(Exception("User not logged in"))
            }
            
            val querySnapshot = firestore.collection("bookmarked")
                .document(tenantEmail)
                .collection("properties")
                .get()
                .await()
            
            val properties = querySnapshot.documents.mapNotNull { doc ->
                doc.toObject(Property::class.java)?.apply {
                    id = doc.id
                    isBookmarked = true // Mark as bookmarked since it's from bookmarks collection
                }
            }
            
            Log.d(TAG, "Loaded ${properties.size} bookmarked properties")
            Result.success(properties)
        } catch (e: Exception) {
            Log.e(TAG, "Error loading bookmarked properties: ${e.message}", e)
            Result.failure(e)
        }
    }
    
    /**
     * Toggle bookmark status for a property
     */
    suspend fun toggleBookmark(property: Property): Result<Boolean> {
        return try {
            Log.d(TAG, "Toggling bookmark for property: ${property.id}, current state: ${property.isBookmarked}")
            
            // Always check current state from Firestore to be sure
            val isCurrentlyBookmarked = isBookmarked(property.id)
            
            if (isCurrentlyBookmarked) {
                val result = removeBookmark(property.id)
                if (result.isSuccess) {
                    property.isBookmarked = false
                    Log.d(TAG, "Property ${property.id} removed from bookmarks")
                    Result.success(false)
                } else {
                    Result.failure(result.exceptionOrNull() ?: Exception("Failed to remove bookmark"))
                }
            } else {
                val result = addBookmark(property)
                if (result.isSuccess) {
                    property.isBookmarked = true
                    Log.d(TAG, "Property ${property.id} added to bookmarks")
                    Result.success(true)
                } else {
                    Result.failure(result.exceptionOrNull() ?: Exception("Failed to add bookmark"))
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error toggling bookmark: ${e.message}", e)
            Result.failure(e)
        }
    }
    
    /**
     * Update bookmark states for a list of properties
     */
    suspend fun updateBookmarkStates(properties: List<Property>): List<Property> {
        return try {
            val tenantEmail = userSessionManager.getEmail()
            if (tenantEmail.isEmpty()) {
                return properties.map { it.apply { isBookmarked = false } }
            }
            
            // Get all bookmarked property IDs for this tenant
            val bookmarkedIds = mutableSetOf<String>()
            try {
                val querySnapshot = firestore.collection("bookmarked")
                    .document(tenantEmail)
                    .collection("properties")
                    .get()
                    .await()
                
                bookmarkedIds.addAll(querySnapshot.documents.map { it.id })
            } catch (e: Exception) {
                Log.e(TAG, "Error getting bookmarked IDs: ${e.message}")
            }
            
            // Update bookmark states
            properties.map { property ->
                property.apply {
                    isBookmarked = bookmarkedIds.contains(property.id)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error updating bookmark states: ${e.message}", e)
            properties.map { it.apply { isBookmarked = false } }
        }
    }
}
