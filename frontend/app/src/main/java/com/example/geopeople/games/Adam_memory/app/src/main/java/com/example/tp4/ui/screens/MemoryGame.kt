package com.example.tp4.ui.screens

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.tp4.model.Fish
import com.example.tp4.model.MemoryCard
import com.example.tp4.viewmodel.BoardViewModel
import kotlin.math.min

enum class GameState {
    LOADING, SETTINGS, PLAYING, FINISHED
}

@Composable
fun MemoryGame(
    modifier: Modifier = Modifier,
    onGameComplete: () -> Unit = {}
) {
    val context = LocalContext.current
    val allFishes = remember { Fish.loadFromAssets(context) }
    val viewModel: BoardViewModel = viewModel()

    var gameState by rememberSaveable { mutableStateOf(GameState.LOADING) }
    var endPairs by rememberSaveable { mutableStateOf(0) }
    var endAttempts by rememberSaveable { mutableStateOf(0) }
    var endElapsed by rememberSaveable { mutableStateOf(0L) }

    when (gameState) {
        GameState.LOADING -> {
            GoldroidLoadScreen(
                duration = 3000L,
                onLoaded = { gameState = GameState.SETTINGS },
                modifier = modifier
            )
        }

        GameState.SETTINGS -> {
            MemorySettingsScreen(
                maxPairs = min(allFishes.size, 32).coerceAtLeast(2),
                onStartGame = { pairs ->
                    viewModel.initGame(allFishes, pairs)
                    gameState = GameState.PLAYING
                },
                modifier = modifier
            )
        }

        GameState.PLAYING -> {
            if (viewModel.cards.isEmpty()) {
                // ViewModel was lost (process death), return to settings
                gameState = GameState.SETTINGS
            } else {
                MemoryBoardManager(
                    cards = viewModel.cards,
                    aspectRatio = 1f,
                    onGameComplete = { pairs, attempts, elapsed ->
                        endPairs = pairs
                        endAttempts = attempts
                        endElapsed = elapsed
                        gameState = GameState.FINISHED
                    },
                    modifier = modifier,
                    viewModel = viewModel
                )
            }
        }

        GameState.FINISHED -> {
            MemoryEndScreen(
                pairsFound = endPairs,
                attempts = endAttempts,
                elapsedMs = endElapsed,
                onRestart = { gameState = GameState.SETTINGS },
                onValidateCapture = onGameComplete,
                modifier = modifier
            )
        }
    }
}
