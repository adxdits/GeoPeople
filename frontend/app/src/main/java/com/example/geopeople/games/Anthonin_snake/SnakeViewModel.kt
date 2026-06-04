package tp4.uge.snake

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class SnakeViewModel : ViewModel() {

    private val _state = MutableStateFlow(initialState())
    val state: StateFlow<SnakeGameState> = _state.asStateFlow()

    private var pendingDirection: Direction? = null

    private var gameLoopJob: Job? = null

    init { startGameLoop() }

    fun pushDirection(dir: Direction) {
        pendingDirection = dir
    }

    fun restart() {
        gameLoopJob?.cancel()
        _state.value = initialState()
        startGameLoop()
    }

    private fun startGameLoop() {
        gameLoopJob = viewModelScope.launch {
            while (!_state.value.isGameOver) {
                delay(_state.value.speed.delayMs)
                applyPendingDirection()
                _state.value = moveSnake(_state.value)
            }
        }
    }

    private fun applyPendingDirection() {
        val dir = pendingDirection ?: return
        pendingDirection = null
        if (!dir.isUTurnFrom(_state.value.direction)) {
            _state.value = _state.value.copy(direction = dir)
        }
    }
}