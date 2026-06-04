package com.example.geopeople.viewmodel

import android.app.Application
import android.content.Context
import android.location.Location
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.geopeople.data.ApiService
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

    private val _captureMessage = MutableStateFlow<String?>(null)
    val captureMessage: StateFlow<String?> = _captureMessage.asStateFlow()

    private val _playerScore = MutableStateFlow(0)
    val playerScore: StateFlow<Int> = _playerScore.asStateFlow()

    private val _isConnected = MutableStateFlow(false)
    val isConnected: StateFlow<Boolean> = _isConnected.asStateFlow()

    private var playerId: String? = null
    private var lastFetchLat: Double? = null
    private var lastFetchLon: Double? = null
    private var lastSyncLat: Double? = null
    private var lastSyncLon: Double? = null

    private val prefs = application.getSharedPreferences("geopeople", Context.MODE_PRIVATE)

    init {
        // Restore player ID from prefs
        playerId = prefs.getString("playerId", null)

        // Register or restore player session
        viewModelScope.launch {
            if (playerId == null) {
                val player = ApiService.registerPlayer("Joueur_${System.currentTimeMillis() % 10000}")
                if (player != null) {
                    playerId = player.id
                    prefs.edit().putString("playerId", player.id).apply()
                    _isConnected.value = true
                    _playerScore.value = player.score
                }
            } else {
                val player = ApiService.getPlayer(playerId!!)
                if (player != null) {
                    _isConnected.value = true
                    _playerScore.value = player.score
                }
            }
        }

        // GPS tracking + card loading + location sync
        viewModelScope.launch {
            playerLocation.filterNotNull().collect { loc ->
                val fLat = lastFetchLat
                val fLon = lastFetchLon

                // Load cards from backend when moved > 5km
                if (fLat == null || fLon == null ||
                    DistanceUtils.haversine(loc.latitude, loc.longitude, fLat, fLon) > 5000
                ) {
                    lastFetchLat = loc.latitude
                    lastFetchLon = loc.longitude
                    // Try backend first, fallback to Wikidata
                    val backendCards = ApiService.getNearbyCards(loc.latitude, loc.longitude)
                    if (backendCards.isNotEmpty()) {
                        cardRepository.setCards(backendCards)
                    } else {
                        cardRepository.loadCardsAround(loc.latitude, loc.longitude)
                    }
                }

                // Sync location to backend every 100m
                val sLat = lastSyncLat
                val sLon = lastSyncLon
                if (sLat == null || sLon == null ||
                    DistanceUtils.haversine(loc.latitude, loc.longitude, sLat, sLon) > 100
                ) {
                    lastSyncLat = loc.latitude
                    lastSyncLon = loc.longitude
                    playerId?.let { id ->
                        ApiService.updatePlayerLocation(id, loc.latitude, loc.longitude)
                    }
                }
            }
        }
    }

    fun startTracking() = locationService.startTracking()

    fun selectCard(card: GeoCard?) {
        _selectedCard.value = card
    }

    fun captureAfterMiniGame(card: GeoCard) {
        val loc = playerLocation.value ?: return
        if (captureManager.canCapture(loc.latitude, loc.longitude, card)) {
            viewModelScope.launch {
                val id = playerId
                if (id == null) {
                    _captureMessage.value = "Joueur non connecte au backend"
                    return@launch
                }

                val result = ApiService.captureCard(id, card.id, loc.latitude, loc.longitude, miniGameSuccess = true)
                if (result.success) {
                    captureManager.capture(card)
                    _captureSuccess.value = true
                    _selectedCard.value = null

                    val player = ApiService.getPlayer(id)
                    if (player != null) {
                        _playerScore.value = player.score
                    }
                } else {
                    _captureMessage.value = result.message.ifBlank { "Capture refusee par le backend" }
                }
            }
        }
    }

    fun reportCaptureFailure(message: String) {
        _captureMessage.value = message
    }

    fun dismissCaptureMessage() {
        _captureMessage.value = null
    }

    fun dismissCaptureSuccess() {
        _captureSuccess.value = false
    }

    override fun onCleared() {
        super.onCleared()
        locationService.stopTracking()
    }
}
