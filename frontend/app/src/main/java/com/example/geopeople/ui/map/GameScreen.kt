package com.example.geopeople.ui.map

import android.app.Activity
import android.content.Intent
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.geopeople.R
import androidx.compose.ui.platform.LocalContext
import com.example.geopeople.data.CaptureManager
import com.example.geopeople.location.DistanceUtils
import com.example.geopeople.model.GeoCard
import com.example.geopeople.ui.capture.CaptureOverlay
import com.example.geopeople.ui.capture.CaptureSuccessAnimation
import com.example.geopeople.viewmodel.GameViewModel
import com.example.mini_game.MainActivity as PokeballActivity
import com.example.minijeu.MainActivity as BallrunActivity
import com.example.tp4.MainActivity as MemoryActivity
import org.osmdroid.views.MapView
import tp4.uge.snake.SnakeActivity
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

private val captureMiniGames = listOf(
    SnakeActivity::class.java,
    PokeballActivity::class.java,
    BallrunActivity::class.java,
    MemoryActivity::class.java
)

private const val TAG = "GeoPeopleScreen"

@Composable
fun GameScreen(
    viewModel: GameViewModel,
    mapView: MapView
) {
    val context = LocalContext.current
    val playerLocation by viewModel.playerLocation.collectAsState()
    val cards by viewModel.allCards.collectAsState()
    val selectedCard by viewModel.selectedCard.collectAsState()
    val captureSuccess by viewModel.captureSuccess.collectAsState()
    val captureMessage by viewModel.captureMessage.collectAsState()
    val serverConnectionMessage by viewModel.serverConnectionMessage.collectAsState()
    val inventory by viewModel.inventory.collectAsState()
    var pendingCaptureCard by remember { mutableStateOf<GeoCard?>(null) }
    var previousDistanceByCard by remember { mutableStateOf<Map<String, Double>>(emptyMap()) }
    val miniGameLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val card = pendingCaptureCard
        pendingCaptureCard = null
        if (card == null) return@rememberLauncherForActivityResult

        if (result.resultCode == Activity.RESULT_OK) {
            viewModel.captureAfterMiniGame(card)
        } else {
            viewModel.reportCaptureFailure(context.getString(R.string.capture_minigame_lost))
        }
    }

    val capturedIds = remember(inventory, cards) {
        inventory.map { it.id }.toSet() + cards.filter { it.capturedBy != null }.map { it.id }
    }

    val visibleCards = remember(playerLocation, cards) {
        val loc = playerLocation ?: return@remember emptyList()
        cards.filter {
            DistanceUtils.haversine(loc.latitude, loc.longitude, it.latitude, it.longitude) <= 500.0
        }
    }

    LaunchedEffect(playerLocation, cards, visibleCards) {
        val loc = playerLocation
        if (loc == null) {
            Log.d(TAG, "Render state: no GPS, loadedCards=${cards.size}")
        } else {
            val closest = cards.minOfOrNull {
                DistanceUtils.haversine(loc.latitude, loc.longitude, it.latitude, it.longitude)
            }
            Log.d(
                TAG,
                "Render state: lat=${loc.latitude} lon=${loc.longitude} loadedCards=${cards.size} visibleCards=${visibleCards.size} closest=${closest?.let { "${it.toInt()}m" } ?: "none"}"
            )
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        MapScreen(
            mapView = mapView,
            playerLocation = playerLocation,
            visibleCards = visibleCards,
            capturedIds = capturedIds,
            onCardClick = { viewModel.selectCard(it) }
        )

        MapStatusPanel(
            hasLocation = playerLocation != null,
            loadedCards = cards.size,
            visibleCards = visibleCards.size,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(14.dp)
        )

        selectedCard?.let { card ->
            val distance = playerLocation?.let { loc ->
                DistanceUtils.haversine(loc.latitude, loc.longitude, card.latitude, card.longitude)
            } ?: Double.MAX_VALUE
            val previousDistance = previousDistanceByCard[card.id]
            val approachState = remember(card.id, distance, previousDistance) {
                when {
                    previousDistance == null -> ApproachState.Unknown
                    distance < previousDistance - 2.0 -> ApproachState.Closer
                    distance > previousDistance + 2.0 -> ApproachState.Farther
                    else -> ApproachState.Stable
                }
            }
            val bearing = playerLocation?.let { loc ->
                bearingDegrees(loc.latitude, loc.longitude, card.latitude, card.longitude)
            }

            LaunchedEffect(card.id, distance) {
                if (distance.isFinite()) {
                    previousDistanceByCard = previousDistanceByCard + (card.id to distance)
                }
            }

            CaptureOverlay(
                card = card,
                distance = distance,
                bearingDegrees = bearing,
                directionLabel = bearing?.let(::compassLabel) ?: stringResource(R.string.capture_direction_gps),
                approachMessage = approachState.message(),
                approachSignal = approachState.signal,
                canCapture = distance <= CaptureManager.CAPTURE_RANGE && !capturedIds.contains(card.id),
                alreadyCaptured = capturedIds.contains(card.id),
                onCapture = {
                    pendingCaptureCard = card
                    viewModel.selectCard(null)
                    val miniGameClass = captureMiniGames.random(Random(System.nanoTime()))
                    miniGameLauncher.launch(Intent(context, miniGameClass))
                },
                onDismiss = { viewModel.selectCard(null) }
            )
        }

        if (captureSuccess) {
            CaptureSuccessAnimation(onDismiss = { viewModel.dismissCaptureSuccess() })
        }

        captureMessage?.let { message ->
            androidx.compose.material3.AlertDialog(
                onDismissRequest = { viewModel.dismissCaptureMessage() },
                confirmButton = {
                    androidx.compose.material3.TextButton(onClick = { viewModel.dismissCaptureMessage() }) {
                        Text(stringResource(R.string.action_ok))
                    }
                },
                title = { Text(stringResource(R.string.capture_dialog_title)) },
                text = { Text(message) }
            )
        }

        serverConnectionMessage?.let { message ->
            androidx.compose.material3.AlertDialog(
                onDismissRequest = { viewModel.dismissServerConnectionMessage() },
                confirmButton = {
                    androidx.compose.material3.TextButton(onClick = { viewModel.dismissServerConnectionMessage() }) {
                        Text(stringResource(R.string.action_ok))
                    }
                },
                title = { Text(stringResource(R.string.server_unavailable)) },
                text = {
                    Text(
                        stringResource(R.string.server_retry_message, message)
                    )
                }
            )
        }
    }
}

private enum class ApproachState(val signal: Int) {
    Unknown(0),
    Closer(1),
    Farther(-1),
    Stable(0)
}

@Composable
private fun ApproachState.message(): String = when (this) {
    ApproachState.Unknown -> stringResource(R.string.approach_unknown)
    ApproachState.Closer -> stringResource(R.string.approach_closer)
    ApproachState.Farther -> stringResource(R.string.approach_farther)
    ApproachState.Stable -> stringResource(R.string.approach_stable)
}

private fun bearingDegrees(fromLat: Double, fromLon: Double, toLat: Double, toLon: Double): Double {
    val lat1 = Math.toRadians(fromLat)
    val lat2 = Math.toRadians(toLat)
    val deltaLon = Math.toRadians(toLon - fromLon)
    val y = sin(deltaLon) * cos(lat2)
    val x = cos(lat1) * sin(lat2) - sin(lat1) * cos(lat2) * cos(deltaLon)
    return (Math.toDegrees(atan2(y, x)) + 360.0) % 360.0
}

private fun compassLabel(degrees: Double): String {
    val directions = listOf("N", "NE", "E", "SE", "S", "SO", "O", "NO")
    val index = (((degrees + 22.5) % 360.0) / 45.0).toInt()
    return directions[index]
}

@Composable
private fun MapStatusPanel(
    hasLocation: Boolean,
    loadedCards: Int,
    visibleCards: Int,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFF9FBFF).copy(alpha = 0.94f)),
        elevation = CardDefaults.cardElevation(6.dp)
    ) {
        androidx.compose.foundation.layout.Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.Transparent)
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Image(
                painter = painterResource(id = R.drawable.pikachu),
                contentDescription = null,
                modifier = Modifier.size(52.dp)
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.map_status_title),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF17212B)
                )
                Text(
                    text = when {
                        !hasLocation -> stringResource(R.string.map_status_no_gps)
                        visibleCards > 0 -> stringResource(R.string.map_status_visible_cards, visibleCards)
                        loadedCards > 0 -> stringResource(R.string.map_status_loaded_no_nearby)
                        else -> stringResource(R.string.map_status_loading)
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFF40515F)
                )
            }
        }
    }
}
