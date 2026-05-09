package com.example.geopeople.data

import com.example.geopeople.location.DistanceUtils
import com.example.geopeople.model.GeoCard
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class CaptureManager {
    private val _inventory = MutableStateFlow<List<GeoCard>>(emptyList())
    val inventory: StateFlow<List<GeoCard>> = _inventory.asStateFlow()
    private val capturedIds = mutableSetOf<String>()

    companion object {
        const val CAPTURE_RANGE = 50.0
    }

    fun canCapture(playerLat: Double, playerLon: Double, card: GeoCard): Boolean {
        if (capturedIds.contains(card.id)) return false
        return DistanceUtils.haversine(playerLat, playerLon, card.latitude, card.longitude) <= CAPTURE_RANGE
    }

    fun capture(card: GeoCard): Boolean {
        if (capturedIds.contains(card.id)) return false
        capturedIds.add(card.id)
        _inventory.value = _inventory.value + card
        return true
    }

    fun isAlreadyCaptured(cardId: String): Boolean = capturedIds.contains(cardId)
}
