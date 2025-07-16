package com.example.housinghub.model

data class Room(
    val title: String = "",
    val description: String = "",
    val price: Double = 0.0,
    val imageUrl: String = "",
    val timestamp: Long = 0L
)
