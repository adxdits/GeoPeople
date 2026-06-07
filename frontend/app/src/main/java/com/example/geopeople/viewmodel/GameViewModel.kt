package com.example.geopeople.viewmodel

import android.app.Application
import android.content.Context
import android.location.Location
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.geopeople.R
import com.example.geopeople.data.ApiService
import com.example.geopeople.data.CardRepository
import com.example.geopeople.data.CaptureManager
import com.example.geopeople.data.LeaderboardPlayerResponse
import com.example.geopeople.data.TradeProposalResponse
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
    val captureLocks = captureManager.captureLocks

    private val _selectedCard = MutableStateFlow<GeoCard?>(null)
    val selectedCard: StateFlow<GeoCard?> = _selectedCard.asStateFlow()

    private val _captureSuccess = MutableStateFlow(false)
    val captureSuccess: StateFlow<Boolean> = _captureSuccess.asStateFlow()

    private val _captureMessage = MutableStateFlow<String?>(null)
    val captureMessage: StateFlow<String?> = _captureMessage.asStateFlow()

    private val _serverConnectionMessage = MutableStateFlow<String?>(null)
    val serverConnectionMessage: StateFlow<String?> = _serverConnectionMessage.asStateFlow()

    private val _playerScore = MutableStateFlow(0)
    val playerScore: StateFlow<Int> = _playerScore.asStateFlow()

    private val _playerName = MutableStateFlow(application.getString(R.string.player_default_name))
    val playerName: StateFlow<String> = _playerName.asStateFlow()

    private val _currentPlayerId = MutableStateFlow<String?>(null)
    val currentPlayerId: StateFlow<String?> = _currentPlayerId.asStateFlow()

    private val _needsPlayerName = MutableStateFlow(false)
    val needsPlayerName: StateFlow<Boolean> = _needsPlayerName.asStateFlow()

    private val _leaderboard = MutableStateFlow<List<LeaderboardPlayerResponse>>(emptyList())
    val leaderboard: StateFlow<List<LeaderboardPlayerResponse>> = _leaderboard.asStateFlow()

    private val _tradeTargetInventory = MutableStateFlow<List<GeoCard>>(emptyList())
    val tradeTargetInventory: StateFlow<List<GeoCard>> = _tradeTargetInventory.asStateFlow()

    private val _tradeMessage = MutableStateFlow<String?>(null)
    val tradeMessage: StateFlow<String?> = _tradeMessage.asStateFlow()

    private val _trades = MutableStateFlow<List<TradeProposalResponse>>(emptyList())
    val trades: StateFlow<List<TradeProposalResponse>> = _trades.asStateFlow()

    private val _isConnected = MutableStateFlow(false)
    val isConnected: StateFlow<Boolean> = _isConnected.asStateFlow()

    private var playerId: String? = null
    private var lastFetchLat: Double? = null
    private var lastFetchLon: Double? = null
    private var lastSyncLat: Double? = null
    private var lastSyncLon: Double? = null
    private var serverConnectionPopupDismissed = false

    private val prefs = application.getSharedPreferences("geopeople", Context.MODE_PRIVATE)

    init {
        ApiService.initialize(application)

        // Restore player ID from prefs
        playerId = prefs.getString("playerId", null)
        _currentPlayerId.value = playerId
        _needsPlayerName.value = playerId == null

        // Register or restore player session
        viewModelScope.launch {
            if (playerId == null) {
                Log.d(TAG, "No stored playerId, waiting for player name")
                _needsPlayerName.value = true
            } else {
                Log.d(TAG, "Restoring playerId=$playerId")
                val player = ApiService.getPlayer(playerId!!)
                if (player != null) {
                    _isConnected.value = true
                    _playerName.value = player.name
                    _playerScore.value = player.score
                    restoreInventoryFromServer(player.id)
                    Log.d(TAG, "Restored player id=${player.id} score=${player.score}")
                    refreshLeaderboard()
                } else {
                    Log.w(TAG, "Could not restore playerId=$playerId")
                    _isConnected.value = false
                    _needsPlayerName.value = true
                    if (!serverConnectionPopupDismissed) {
                        _serverConnectionMessage.value = getApplication<Application>().getString(R.string.restore_player_failed)
                    }
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
                    val backendResponse = ApiService.getNearbyCardsResult(loc.latitude, loc.longitude)
                    if (!backendResponse.success) {
                        _isConnected.value = false
                        if (!serverConnectionPopupDismissed) {
                            _serverConnectionMessage.value = backendResponse.message.ifBlank {
                                getApplication<Application>().getString(R.string.backend_reconnect_retry)
                            }
                        }
                        Log.w(TAG, "Backend unavailable: ${backendResponse.message}")
                        return@collect
                    }

                    _isConnected.value = true
                    serverConnectionPopupDismissed = false
                    _serverConnectionMessage.value = null
                    val backendCards = backendResponse.cards
                    val playableBackendCards = backendCards.filter {
                        DistanceUtils.haversine(loc.latitude, loc.longitude, it.latitude, it.longitude) <= 500.0
                    }
                    if (playableBackendCards.isNotEmpty()) {
                        lastFetchLat = loc.latitude
                        lastFetchLon = loc.longitude
                        Log.d(TAG, "Using backend cards count=${backendCards.size}")
                        cardRepository.setCards(backendCards)
                    } else {
                        Log.w(TAG, "Backend returned ${backendCards.size} cards but ${playableBackendCards.size} playable cards, falling back to Wikidata cards")
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
                val id = playerId
                if (id == null) {
                    _captureMessage.value = getApplication<Application>().getString(R.string.player_not_connected)
                    return@launch
                }

                val result = ApiService.captureCard(id, card.id, loc.latitude, loc.longitude, miniGameSuccess = true)
                if (result.success) {
                    captureManager.capture(card)
                    _captureSuccess.value = true
                    _selectedCard.value = null

                    val player = ApiService.getPlayer(id)
                    if (player != null) {
                        _playerName.value = player.name
                        _playerScore.value = player.score
                        refreshLeaderboard()
                    }
                } else {
                    _captureMessage.value = result.message.ifBlank {
                        getApplication<Application>().getString(R.string.capture_denied_backend)
                    }
                }
            }
        }
    }

    fun reportCaptureFailure(card: GeoCard) {
        val lock = captureManager.registerFailedAttempt(card.id)
        val remainingSeconds = ((lock.lockedUntilMillis - System.currentTimeMillis()) / 1000L).coerceAtLeast(1L)
        _captureMessage.value = getApplication<Application>().getString(
            R.string.capture_minigame_lost_with_lock,
            formatLockDuration(remainingSeconds),
            lock.attempts
        )

        val loc = playerLocation.value ?: return
        val id = playerId ?: return
        viewModelScope.launch {
            ApiService.captureCard(id, card.id, loc.latitude, loc.longitude, miniGameSuccess = false)
        }
    }

    fun clearExpiredCaptureLocks() {
        captureManager.clearExpiredLocks()
    }

    fun dismissCaptureMessage() {
        _captureMessage.value = null
    }

    fun dismissServerConnectionMessage() {
        serverConnectionPopupDismissed = true
        _serverConnectionMessage.value = null
    }

    fun dismissCaptureSuccess() {
        _captureSuccess.value = false
    }

    fun refreshLeaderboard() {
        viewModelScope.launch {
            _leaderboard.value = ApiService.getLeaderboard()
        }
    }

    fun refreshTrades() {
        val id = playerId ?: return
        viewModelScope.launch {
            _trades.value = ApiService.getPlayerTrades(id)
        }
    }

    fun loadTradeTargetInventory(targetPlayerId: String) {
        viewModelScope.launch {
            _tradeTargetInventory.value = ApiService.getPlayerInventoryCards(targetPlayerId)
        }
    }

    fun clearTradeTargetInventory() {
        _tradeTargetInventory.value = emptyList()
    }

    fun dismissTradeMessage() {
        _tradeMessage.value = null
    }

    fun createTradeProposal(targetPlayerId: String, myCardId: String, targetCardId: String) {
        val id = playerId
        if (id == null) {
            _tradeMessage.value = getApplication<Application>().getString(R.string.player_not_connected)
            return
        }

        viewModelScope.launch {
            val result = ApiService.createTradeProposal(
                fromPlayerId = id,
                toPlayerId = targetPlayerId,
                fromCardId = myCardId,
                toCardId = targetCardId
            )
            _tradeMessage.value = result.message.ifBlank {
                if (result.success) "Proposition envoyee" else "Proposition refusee"
            }

            if (result.success) {
                refreshTrades()
            }
        }
    }

    fun acceptTrade(tradeId: String) {
        val id = playerId ?: return
        viewModelScope.launch {
            val result = ApiService.acceptTrade(tradeId, id)
            _tradeMessage.value = result.message.ifBlank {
                if (result.success) "Echange accepte" else "Echange refuse"
            }
            refreshAfterTradeResolution(id)
        }
    }

    fun rejectTrade(tradeId: String) {
        val id = playerId ?: return
        viewModelScope.launch {
            val result = ApiService.rejectTrade(tradeId, id)
            _tradeMessage.value = result.message.ifBlank {
                if (result.success) "Echange refuse" else "Action impossible"
            }
            refreshTrades()
        }
    }

    private suspend fun refreshAfterTradeResolution(id: String) {
        val player = ApiService.getPlayer(id)
        if (player != null) {
            _playerScore.value = player.score
            _playerName.value = player.name
        }
        restoreInventoryFromServer(id)
        refreshLeaderboard()
        refreshTrades()
    }

    fun createNewPlayer(name: String) {
        val cleanName = name.trim().ifBlank {
            getApplication<Application>().getString(R.string.player_generated_name, System.currentTimeMillis() % 10000)
        }
        viewModelScope.launch {
            val player = ApiService.registerPlayer(cleanName)
            if (player == null) {
                _serverConnectionMessage.value = getApplication<Application>().getString(R.string.create_player_failed)
                return@launch
            }

            playerId = player.id
            _currentPlayerId.value = player.id
            prefs.edit().putString("playerId", player.id).apply()
            captureManager.clear()
            _selectedCard.value = null
            _captureSuccess.value = false
            _captureMessage.value = null
            _playerName.value = player.name
            _playerScore.value = player.score
            _isConnected.value = true
            _needsPlayerName.value = false
            restoreInventoryFromServer(player.id)
            refreshLeaderboard()
        }
    }

    private suspend fun restoreInventoryFromServer(id: String) {
        val cards = ApiService.getPlayerInventoryCards(id)
        captureManager.replaceInventory(cards)
        Log.d(TAG, "Restored inventory from server playerId=$id count=${cards.size}")
    }

    fun logoutPlayer() {
        playerId = null
        prefs.edit().remove("playerId").apply()
        _currentPlayerId.value = null
        _playerName.value = getApplication<Application>().getString(R.string.player_default_name)
        _playerScore.value = 0
        _isConnected.value = false
        _needsPlayerName.value = true
        _selectedCard.value = null
        _captureSuccess.value = false
        _captureMessage.value = null
        _serverConnectionMessage.value = null
        serverConnectionPopupDismissed = false
        lastFetchLat = null
        lastFetchLon = null
        lastSyncLat = null
        lastSyncLon = null
        captureManager.clear()
    }

    override fun onCleared() {
        super.onCleared()
        locationService.stopTracking()
    }

    private fun formatLockDuration(totalSeconds: Long): String {
        val minutes = totalSeconds / 60
        val seconds = totalSeconds % 60
        return if (minutes > 0) {
            getApplication<Application>().getString(R.string.duration_minutes_seconds, minutes, seconds)
        } else {
            getApplication<Application>().getString(R.string.duration_seconds, seconds)
        }
    }
}
