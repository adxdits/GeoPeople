package com.example.geopeople.ui.map

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import com.example.geopeople.location.DistanceUtils
import com.example.geopeople.ui.capture.CaptureOverlay
import com.example.geopeople.ui.capture.CaptureSuccessAnimation
import com.example.geopeople.viewmodel.GameViewModel

@Composable
fun GameScreen(viewModel: GameViewModel) {
    val playerLocation by viewModel.playerLocation.collectAsState()
    val cards by viewModel.allCards.collectAsState()
    val selectedCard by viewModel.selectedCard.collectAsState()
    val captureSuccess by viewModel.captureSuccess.collectAsState()
    val inventory by viewModel.inventory.collectAsState()

    val capturedIds = remember(inventory) { inventory.map { it.id }.toSet() }

    val visibleCards = remember(playerLocation, cards) {
        val loc = playerLocation ?: return@remember emptyList()
        cards.filter {
            DistanceUtils.haversine(loc.latitude, loc.longitude, it.latitude, it.longitude) <= 500.0
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        MapScreen(
            playerLocation = playerLocation,
            visibleCards = visibleCards,
            capturedIds = capturedIds,
            onCardClick = { viewModel.selectCard(it) }
        )

        selectedCard?.let { card ->
            val distance = playerLocation?.let { loc ->
                DistanceUtils.haversine(loc.latitude, loc.longitude, card.latitude, card.longitude)
            } ?: Double.MAX_VALUE

            CaptureOverlay(
                card = card,
                distance = distance,
                canCapture = distance <= 50.0 && !capturedIds.contains(card.id),
                alreadyCaptured = capturedIds.contains(card.id),
                onCapture = { viewModel.captureSelected() },
                onDismiss = { viewModel.selectCard(null) }
            )
        }

        if (captureSuccess) {
            CaptureSuccessAnimation(onDismiss = { viewModel.dismissCaptureSuccess() })
        }
    }
}
