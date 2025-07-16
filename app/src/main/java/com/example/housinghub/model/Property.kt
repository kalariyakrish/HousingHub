package com.example.housinghub.model.viewmodel

data class Property(
    val id: String = "",
    val title: String = "",
    val location: String = "",
    val price: String = "",
    val description: String = "",
    val images: List<String>? = null,
    val agreementUrl: String? = null,
    var isBookmarked: Boolean = false,
    var isAvailable: Boolean = true
)
