package com.example.housinghub.property

import com.google.firebase.Timestamp
import java.util.Date

data class Property(
    var id: String = "",
    val title: String = "",
    val type: String = "",
    val price: String = "0.0",
    val address: String = "",
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val images: List<String> = emptyList(),
    val videos: List<String> = emptyList(),
    var ownerId: String = "",
    val createdAt: Timestamp = Timestamp.now(),
    var isAvailable: Boolean = true,
    var isBookmarked: Boolean = false,
    val description: String = "",
    val agreementUrl: String = "",
    val propertyType: String = "",
    val timestamp: String = Date().toString(),
    var location: String = "",
    val propertyName: String = "",
    val rating: Double? = null
)
