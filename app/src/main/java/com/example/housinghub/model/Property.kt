package com.example.housinghub.model.viewmodel

data class Property(
    val id: String = "",
    val title: String = "",
    val location: String = "",
    val price: Int = 0,
    val description: String = "",
    val images: List<String>? = null,
    val agreementUrl: String? = null,
    var isBookmarked: Boolean = false
)
