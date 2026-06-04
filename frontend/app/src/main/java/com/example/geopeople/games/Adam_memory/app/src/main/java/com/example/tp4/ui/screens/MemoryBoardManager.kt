package com.example.tp4.ui.screens

import android.os.SystemClock
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.tp4.model.MemoryCard
import com.example.tp4.ui.components.Chronometer
import com.example.tp4.viewmodel.BoardViewModel
import kotlinx.coroutines.delay

@Composable
fun MemoryBoardManager(
    cards: List<MemoryCard>,
    aspectRatio: Float,
    onGameComplete: (pairsFound: Int, attempts: Int, elapsedMs: Long) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: BoardViewModel = viewModel()
) {
    // Initialize ViewModel with cards if not already done (supports rotation)
    LaunchedEffect(cards) {
        if (viewModel.cards.isEmpty() && cards.isNotEmpty()) {
            viewModel.cards = cards
        }
    }

    val visibility by remember {
        derivedStateOf { viewModel.visibility }
    }

    // Handle two cards flipped: check match or flip back after delay
    LaunchedEffect(viewModel.flippedCards) {
        if (viewModel.flippedCards.size == 2) {
            val match = viewModel.checkMatch()
            if (match == true) {
                viewModel.applyMatch()
                if (viewModel.isGameComplete) {
                    val elapsed = SystemClock.elapsedRealtime() - viewModel.startTime
                    onGameComplete(viewModel.pairsFound, viewModel.attempts, elapsed)
                }
            } else if (match == false) {
                delay(2000L)
                viewModel.hideFlipped()
            }
        }
    }

    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Paires : ${viewModel.pairsFound}/${viewModel.totalPairs}")
            Text("Tentatives : ${viewModel.attempts}")
            Chronometer(startTime = viewModel.startTime)
        }

        MemoryBoardDisplayer(
            cards = viewModel.cards,
            visibility = visibility,
            aspectRatio = aspectRatio,
            onCardClick = { index -> viewModel.onCardClicked(index) },
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        )
    }
}
