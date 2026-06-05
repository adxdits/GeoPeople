package com.example.geopeople.viewmodel

import android.app.Application
import android.content.Context
import android.location.Location
import android.util.Log
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
    companion object {
        private const val TAG = "GeoPeopleVM"
    }

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
                Log.d(TAG, "No stored playerId, registering a new player")
                val player = ApiService.registerPlayer("Joueur_${System.currentTimeMillis() % 10000}")
                if (player != null) {
                    playerId = player.id
                    prefs.edit().putString("playerId", player.id).apply()
                    _isConnected.value = true
                    _playerScore.value = player.score
                    Log.d(TAG, "Registered player id=${player.id} score=${player.score}")
                } else {
                    Log.w(TAG, "Player registration failed")
                }
            } else {
                Log.d(TAG, "Restoring playerId=$playerId")
                val player = ApiService.getPlayer(playerId!!)
                if (player != null) {
                    _isConnected.value = true
                    _playerScore.value = player.score
                    Log.d(TAG, "Restored player id=${player.id} score=${player.score}")
                } else {
                    Log.w(TAG, "Could not restore playerId=$playerId")
                }
            }
        }

        // GPS tracking + card loading + location sync
        viewModelScope.launch {
            playerLocation.filterNotNull().collect { loc ->
                Log.d(TAG, "GPS lat=${loc.latitude} lon=${loc.longitude} accuracy=${loc.accuracy}")
                val fLat = lastFetchLat
                val fLon = lastFetchLon

                // Load cards from backend when moved > 5km
                val fetchDistance = if (fLat == null || fLon == null) {
                    null
                } else {
                    DistanceUtils.haversine(loc.latitude, loc.longitude, fLat, fLon)
                }
                val hasPlayableCard = cardRepository.getCardsInRange(
                    loc.latitude,
                    loc.longitude,
                    500.0
                ).isNotEmpty()
                if (fetchDistance == null || fetchDistance > 5000 || !hasPlayableCard) {
                    Log.d(TAG, "Loading cards around lat=${loc.latitude} lon=${loc.longitude}, moved=${fetchDistance ?: "first load"}m")
                    // Try backend first, fallback to Wikidata
                    val backendCards = ApiService.getNearbyCards(loc.latitude, loc.longitude)
                    val playableBackendCards = backendCards.filter {
                        DistanceUtils.haversine(loc.latitude, loc.longitude, it.latitude, it.longitude) <= 500.0
                    }
                    if (playableBackendCards.isNotEmpty()) {
                        lastFetchLat = loc.latitude
                        lastFetchLon = loc.longitude
                        Log.d(TAG, "Using backend cards count=${backendCards.size}")
                        cardRepository.setCards(backendCards)
                    } else {
                        Log.w(TAG, "Backend returned ${backendCards.size} cards but ${playableBackendCards.size} playable cards, falling back to local/Wikidata cards")
                        cardRepository.loadCardsAround(loc.latitude, loc.longitude)
                        val playableFallbackCards = cardRepository.getCardsInRange(
                            loc.latitude,
                            loc.longitude,
                            500.0
                        )
                        if (playableFallbackCards.isNotEmpty()) {
                            lastFetchLat = loc.latitude
                            lastFetchLon = loc.longitude
                        } else {
                            Log.w(TAG, "Fallback also returned no playable cards; next GPS update will retry")
                        }
                    }
                } else {
                    Log.d(TAG, "Skipping card reload, moved=${fetchDistance}m, playable cards already available")
                }

                // Sync location to backend every 100m
                val sLat = lastSyncLat
                val sLon = lastSyncLon
                val syncDistance = if (sLat == null || sLon == null) {
                    null
                } else {
                    DistanceUtils.haversine(loc.latitude, loc.longitude, sLat, sLon)
                }
                if (syncDistance == null || syncDistance > 100) {
                    lastSyncLat = loc.latitude
                    lastSyncLon = loc.longitude
                    playerId?.let { id ->
                        val synced = ApiService.updatePlayerLocation(id, loc.latitude, loc.longitude)
                        Log.d(TAG, "Location sync playerId=$id success=$synced moved=${syncDistance ?: "first sync"}m")
                    } ?: Log.w(TAG, "Skipping location sync, playerId is null")
                } else {
                    Log.d(TAG, "Skipping location sync, moved=${syncDistance}m")
                }
            }
        }

        viewModelScope.launch {
            allCards.collect { cards ->
                val loc = playerLocation.value
                if (loc == null) {
                    Log.d(TAG, "Cards updated count=${cards.size}, no GPS yet")
                } else {
                    val closest = cards.minOfOrNull {
                        DistanceUtils.haversine(loc.latitude, loc.longitude, it.latitude, it.longitude)
                    }
                    Log.d(TAG, "Cards updated count=${cards.size}, closest=${closest?.let { "${it.toInt()}m" } ?: "none"}")
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
                if (card.id.startsWith("local-demo-")) {
                    Log.d(TAG, "Capturing local fallback card id=${card.id}")
                    captureManager.capture(card)
                    _captureSuccess.value = true
                    _selectedCard.value = null
                    return@launch
                }

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
