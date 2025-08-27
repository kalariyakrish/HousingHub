package com.example.housinghub.SharedViewModel.Viewmodel

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.housinghub.managers.BookmarkManager
import com.example.housinghub.model.Property
import kotlinx.coroutines.launch

class SharedViewModel(application: Application) : AndroidViewModel(application) {

    private val bookmarkManager = BookmarkManager(application.applicationContext)
    
    private val _bookmarkedProperties = MutableLiveData<MutableList<Property>>(mutableListOf())
    val bookmarkedProperties: LiveData<MutableList<Property>> get() = _bookmarkedProperties
    
    private val _isLoading = MutableLiveData<Boolean>(false)
    val isLoading: LiveData<Boolean> get() = _isLoading
    
    private val _errorMessage = MutableLiveData<String?>()
    val errorMessage: LiveData<String?> get() = _errorMessage

    companion object {
        private const val TAG = "SharedViewModel"
    }

    init {
        loadBookmarkedProperties()
    }

    fun toggleBookmark(property: Property, onComplete: ((Boolean) -> Unit)? = null) {
        viewModelScope.launch {
            try {
                Log.d(TAG, "Toggling bookmark for property: ${property.id}")
                
                val result = bookmarkManager.toggleBookmark(property)
                result.fold(
                    onSuccess = { isBookmarked ->
                        Log.d(TAG, "Successfully toggled bookmark. New state: $isBookmarked")
                        
                        val currentList = _bookmarkedProperties.value ?: mutableListOf()
                        
                        if (isBookmarked) {
                            // Add to list if not already present
                            if (currentList.none { it.id == property.id }) {
                                currentList.add(property)
                                Log.d(TAG, "Added property to bookmarked list")
                            }
                        } else {
                            // Remove from list
                            currentList.removeAll { it.id == property.id }
                            Log.d(TAG, "Removed property from bookmarked list")
                        }
                        
                        _bookmarkedProperties.value = currentList
                        onComplete?.invoke(isBookmarked)
                    },
                    onFailure = { exception ->
                        Log.e(TAG, "Failed to toggle bookmark: ${exception.message}", exception)
                        _errorMessage.value = "Failed to update bookmark: ${exception.message}"
                        onComplete?.invoke(property.isBookmarked) // Keep original state
                    }
                )
            } catch (e: Exception) {
                Log.e(TAG, "Error toggling bookmark: ${e.message}", e)
                _errorMessage.value = "Error toggling bookmark: ${e.message}"
                onComplete?.invoke(property.isBookmarked) // Keep original state
            }
        }
    }
    
    fun loadBookmarkedProperties() {
        viewModelScope.launch {
            _isLoading.value = true
            
            try {
                Log.d(TAG, "Loading bookmarked properties")
                val result = bookmarkManager.getBookmarkedProperties()
                result.fold(
                    onSuccess = { properties ->
                        Log.d(TAG, "Successfully loaded ${properties.size} bookmarked properties")
                        _bookmarkedProperties.value = properties.toMutableList()
                    },
                    onFailure = { exception ->
                        Log.e(TAG, "Failed to load bookmarks: ${exception.message}", exception)
                        _errorMessage.value = "Failed to load bookmarks: ${exception.message}"
                    }
                )
            } catch (e: Exception) {
                Log.e(TAG, "Error loading bookmarks: ${e.message}", e)
                _errorMessage.value = "Error loading bookmarks: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    fun updatePropertiesBookmarkStatus(properties: List<Property>, onComplete: ((List<Property>) -> Unit)? = null) {
        viewModelScope.launch {
            try {
                Log.d(TAG, "Updating bookmark status for ${properties.size} properties")
                val updatedProperties = bookmarkManager.updateBookmarkStates(properties)
                onComplete?.invoke(updatedProperties)
            } catch (e: Exception) {
                Log.e(TAG, "Error updating bookmark states: ${e.message}", e)
                onComplete?.invoke(properties) // Return original list if error
            }
        }
    }
    
    fun isPropertyBookmarked(propertyId: String): Boolean {
        return _bookmarkedProperties.value?.any { it.id == propertyId } ?: false
    }
    
    fun clearErrorMessage() {
        _errorMessage.value = null
    }
}
