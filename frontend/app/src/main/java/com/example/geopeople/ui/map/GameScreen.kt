package com.example.geopeople.ui.map

import android.app.Activity
import android.content.Intent
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
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.geopeople.R
import androidx.compose.ui.platform.LocalContext
import com.example.geopeople.location.DistanceUtils
import com.example.geopeople.model.GeoCard
import com.example.geopeople.ui.capture.CaptureOverlay
import com.example.geopeople.ui.capture.CaptureSuccessAnimation
import com.example.geopeople.viewmodel.GameViewModel
import com.example.mini_game.MainActivity as PokeballActivity
import com.example.minijeu.MainActivity as BallrunActivity
import com.example.tp4.MainActivity as MemoryActivity
import tp4.uge.snake.SnakeActivity
import kotlin.random.Random

private val captureMiniGames = listOf(
    SnakeActivity::class.java,
    PokeballActivity::class.java,
    BallrunActivity::class.java,
    MemoryActivity::class.java
)

@Composable
fun GameScreen(viewModel: GameViewModel) {
    val context = LocalContext.current
    val playerLocation by viewModel.playerLocation.collectAsState()
    val cards by viewModel.allCards.collectAsState()
    val selectedCard by viewModel.selectedCard.collectAsState()
    val captureSuccess by viewModel.captureSuccess.collectAsState()
    val captureMessage by viewModel.captureMessage.collectAsState()
    val inventory by viewModel.inventory.collectAsState()
    var pendingCaptureCard by remember { mutableStateOf<GeoCard?>(null) }
    val miniGameLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val card = pendingCaptureCard
        pendingCaptureCard = null
        if (card == null) return@rememberLauncherForActivityResult

        if (result.resultCode == Activity.RESULT_OK) {
            viewModel.captureAfterMiniGame(card)
        } else {
            viewModel.reportCaptureFailure("Mini-jeu perdu : la carte reste verrouillee.")
        }
    }

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

        MapStatusPanel(
            hasLocation = playerLocation != null,
            loadedCards = cards.size,
            visibleCards = visibleCards.size,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(14.dp)
        )

        PixelAssetStrip(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(start = 12.dp, bottom = 18.dp)
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
                        androidx.compose.material3.Text("OK")
                    }
                },
                title = { androidx.compose.material3.Text("Capture") },
                text = { androidx.compose.material3.Text(message) }
            )
        }
    }
}

@Composable
private fun PixelAssetStrip(modifier: Modifier = Modifier) {
    androidx.compose.foundation.layout.Row(
        modifier = modifier,
        verticalAlignment = Alignment.Bottom,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Image(
            painter = painterResource(id = R.drawable.house1),
            contentDescription = null,
            modifier = Modifier.size(72.dp)
        )
        Image(
            painter = painterResource(id = R.drawable.tree1),
            contentDescription = null,
            modifier = Modifier.size(64.dp)
        )
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Image(
                painter = painterResource(id = R.drawable.male_spritesheet),
                contentDescription = null,
                modifier = Modifier.size(48.dp)
            )
            Image(
                painter = painterResource(id = R.drawable.tall_grass),
                contentDescription = null,
                modifier = Modifier
                    .width(84.dp)
                    .size(38.dp)
            )
        }
    }
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
                    text = "GeoPeople",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF17212B)
                )
                Text(
                    text = when {
                        !hasLocation -> "GPS en attente. Dans l'emulateur: Location > set position near Googleplex."
                        visibleCards > 0 -> "$visibleCards carte(s) proche(s). Tape un marqueur pour capturer."
                        loadedCards > 0 -> "Cartes chargees, mais aucune a moins de 500 m."
                        else -> "Connexion backend/cartes en cours..."
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFF40515F)
                )
            }
        }
    }
}
