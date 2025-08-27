package com.example.housinghub.SharedViewModel.Viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.housinghub.model.Property

class SharedViewModel : ViewModel() {

    private val _bookmarkedProperties = MutableLiveData<MutableList<Property>>(mutableListOf())
    val bookmarkedProperties: LiveData<MutableList<Property>> get() = _bookmarkedProperties

    fun toggleBookmark(property: Property) {
        val currentList = _bookmarkedProperties.value ?: mutableListOf()
        if (currentList.any { it.id == property.id }) {
            currentList.removeAll { it.id == property.id }
        } else {
            currentList.add(property)
        }
        _bookmarkedProperties.value = currentList
    }
}
