package com.example.housinghub.model

import com.google.firebase.Timestamp
import java.io.Serializable

data class Property(
    var id: String = "",
    var title: String = "",
    var type: String = "",
    // price represented as double; many files parse/show it as number
    var price: Double = 0.0,
    var address: String = "",
    var location: String = "",
    var latitude: Double = 0.0,
    var longitude: Double = 0.0,
    var images: List<String> = emptyList(),
    var videos: List<String> = emptyList(),
    var ownerId: String = "",
    var createdAt: Timestamp? = null,
    var isAvailable: Boolean = true,
    var isBookmarked: Boolean = false,
    // Property specific fields
    var bedrooms: Int = 0,
    var bathrooms: Int = 0,
    // Extra fields used in parts of the app; kept for compatibility
    var description: String = "",
    var agreementUrl: String = "",
    var propertyType: String = "",
    var timestamp: String = "",
    var propertyName: String = "",
    var rating: Double? = null
)
: Serializable