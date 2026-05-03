package com.example.geopeople.data

import com.example.geopeople.location.DistanceUtils
import com.example.geopeople.model.GeoCard
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class CardRepository {
    private val _cards = MutableStateFlow<List<GeoCard>>(emptyList())
    val cards: StateFlow<List<GeoCard>> = _cards.asStateFlow()

    suspend fun loadCardsAround(lat: Double, lon: Double) {
        val places = WikidataService.fetchNearbyPlaces(lat, lon, 20)
        _cards.value = places
    }

    fun getCardsInRange(playerLat: Double, playerLon: Double, rangeMeters: Double): List<GeoCard> {
        return _cards.value.filter {
            DistanceUtils.haversine(playerLat, playerLon, it.latitude, it.longitude) <= rangeMeters
        }
    }
}
