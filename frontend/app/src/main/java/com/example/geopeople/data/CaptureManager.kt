package com.example.geopeople.data

import com.example.geopeople.location.DistanceUtils
import com.example.geopeople.model.GeoCard
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class CaptureManager {
    private val _inventory = MutableStateFlow<List<GeoCard>>(emptyList())
    val inventory: StateFlow<List<GeoCard>> = _inventory.asStateFlow()
    private val _captureLocks = MutableStateFlow<Map<String, CaptureLock>>(emptyMap())
    val captureLocks: StateFlow<Map<String, CaptureLock>> = _captureLocks.asStateFlow()
    private val capturedIds = mutableSetOf<String>()

    companion object {
        const val CAPTURE_RANGE = 50.0
        private val LOCK_DURATIONS_MS = longArrayOf(
            30_000L,
            2 * 60_000L,
            5 * 60_000L,
            15 * 60_000L,
            30 * 60_000L
        )
    }

    fun canCapture(playerLat: Double, playerLon: Double, card: GeoCard): Boolean {
        if (capturedIds.contains(card.id)) return false
        if (isLocked(card.id)) return false
        return DistanceUtils.haversine(playerLat, playerLon, card.latitude, card.longitude) <= CAPTURE_RANGE
    }

    fun capture(card: GeoCard): Boolean {
        if (capturedIds.contains(card.id)) return false
        capturedIds.add(card.id)
        _captureLocks.value = _captureLocks.value - card.id
        _inventory.value = _inventory.value + card
        return true
    }

    fun registerFailedAttempt(cardId: String): CaptureLock {
        val now = System.currentTimeMillis()
        val previousAttempts = _captureLocks.value[cardId]?.attempts ?: 0
        val attempts = previousAttempts + 1
        val duration = LOCK_DURATIONS_MS[(attempts - 1).coerceAtMost(LOCK_DURATIONS_MS.lastIndex)]
        val lock = CaptureLock(
            cardId = cardId,
            attempts = attempts,
            lockedUntilMillis = now + duration
        )
        _captureLocks.value = _captureLocks.value + (cardId to lock)
        return lock
    }

    fun remainingLockMillis(cardId: String): Long {
        val lock = _captureLocks.value[cardId] ?: return 0L
        return (lock.lockedUntilMillis - System.currentTimeMillis()).coerceAtLeast(0L)
    }

    fun isLocked(cardId: String): Boolean = remainingLockMillis(cardId) > 0L

    fun clearExpiredLocks() {
        val now = System.currentTimeMillis()
        _captureLocks.value = _captureLocks.value.filterValues { it.lockedUntilMillis > now }
    }

    fun replaceInventory(cards: List<GeoCard>) {
        capturedIds.clear()
        capturedIds.addAll(cards.map { it.id })
        _captureLocks.value = _captureLocks.value - cards.map { it.id }.toSet()
        _inventory.value = cards.distinctBy { it.id }
    }

    fun isAlreadyCaptured(cardId: String): Boolean = capturedIds.contains(cardId)

    fun clear() {
        capturedIds.clear()
        _captureLocks.value = emptyMap()
        _inventory.value = emptyList()
    }
}

data class CaptureLock(
    val cardId: String,
    val attempts: Int,
    val lockedUntilMillis: Long
)
