package com.example.housinghub.model

data class Property(
    var id: String = "",
    val title: String = "",
    val location: String = "",
    val price: String = "",
    val description: String = "",
    var ownerId: String = "",
    val images: List<String> = emptyList(),
    val agreementUrl: String = "",
    val timestamp: Long = 0L,
    var isBookmarked: Boolean = false,
    val isAvailable: Boolean = true
)
