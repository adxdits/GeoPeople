package com.example.geopeople.model

data class GeoCard(
    val id: String,
    val name: String,
    val description: String,
    val latitude: Double,
    val longitude: Double,
    val imageUrl: String? = null,
    val power: Int = (1..10).random()
)
