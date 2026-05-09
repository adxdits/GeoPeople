package com.example.geopeople.viewmodel

import android.app.Application
import android.location.Location
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.geopeople.data.CardRepository
import com.example.geopeople.data.CaptureManager
import com.example.geopeople.location.DistanceUtils
import com.example.geopeople.location.LocationService
import com.example.geopeople.model.GeoCard
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class GameViewModel(application: Application) : AndroidViewModel(application) {
    private val locationService = LocationService(application)
    private val cardRepository = CardRepository()
    val captureManager = CaptureManager()

    val playerLocation: StateFlow<Location?> = locationService.location
    val allCards: StateFlow<List<GeoCard>> = cardRepository.cards
    val inventory: StateFlow<List<GeoCard>> = captureManager.inventory

    private val _selectedCard = MutableStateFlow<GeoCard?>(null)
    val selectedCard: StateFlow<GeoCard?> = _selectedCard.asStateFlow()

    private val _captureSuccess = MutableStateFlow(false)
    val captureSuccess: StateFlow<Boolean> = _captureSuccess.asStateFlow()

    private var lastFetchLat: Double? = null
    private var lastFetchLon: Double? = null

    init {
        viewModelScope.launch {
            playerLocation.filterNotNull().collect { loc ->
                val fLat = lastFetchLat
                val fLon = lastFetchLon
                if (fLat == null || fLon == null ||
                    DistanceUtils.haversine(loc.latitude, loc.longitude, fLat, fLon) > 5000
                ) {
                    lastFetchLat = loc.latitude
                    lastFetchLon = loc.longitude
                    cardRepository.loadCardsAround(loc.latitude, loc.longitude)
                }
            }
        }
    }

    fun startTracking() = locationService.startTracking()

    fun selectCard(card: GeoCard?) {
        _selectedCard.value = card
    }

    fun captureSelected() {
        val card = _selectedCard.value ?: return
        val loc = playerLocation.value ?: return
        if (captureManager.canCapture(loc.latitude, loc.longitude, card)) {
            captureManager.capture(card)
            _captureSuccess.value = true
            _selectedCard.value = null
        }
    }

    fun dismissCaptureSuccess() {
        _captureSuccess.value = false
    }

    override fun onCleared() {
        super.onCleared()
        locationService.stopTracking()
    }
}
