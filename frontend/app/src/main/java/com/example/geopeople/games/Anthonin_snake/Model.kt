package tp4.uge.snake

import androidx.compose.ui.graphics.Color
import com.example.geopeople.R
import kotlin.random.Random

const val GRID_SIZE = 15

enum class Direction { UP, DOWN, LEFT, RIGHT }

enum class Speed(val labelRes: Int, val delayMs: Long, val color: Color) {
    SLOW  (R.string.snake_speed_slow,   300L, Color(0xFF4FC3F7)),
    MEDIUM(R.string.snake_speed_medium,  200L, Color(0xFFFFB74D)),
    FAST  (R.string.snake_speed_fast, 120L, Color(0xFFFF5252));

    companion object {
        fun random(): Speed = entries.random()
    }
}

data class SnakeGameState(
    val snake: List<Pair<Int, Int>>,
    val apples: Set<Pair<Int, Int>>,
    val totalApples: Int,
    val direction: Direction,
    val isGameOver: Boolean,
    val isWin: Boolean,
    val score: Int,
    val speed: Speed
)

fun initialState(): SnakeGameState {
    val snake = listOf(Pair(5, 5))
    val appleCount = Random.nextInt(5, 11)
    return SnakeGameState(
        snake       = snake,
        apples      = generateApples(occupied = snake.toSet(), count = appleCount),
        totalApples = appleCount,
        direction   = Direction.RIGHT,
        isGameOver  = false,
        isWin       = false,
        score       = 0,
        speed       = Speed.random()
    )
}

fun generateApples(occupied: Set<Pair<Int, Int>>, count: Int): Set<Pair<Int, Int>> {
    val result = mutableSetOf<Pair<Int, Int>>()
    while (result.size < count) {
        val pos = Pair(Random.nextInt(0, GRID_SIZE), Random.nextInt(0, GRID_SIZE))
        if (pos !in occupied && pos !in result) result.add(pos)
    }
    return result
}

/** Pure function — facile à tester unitairement */
fun moveSnake(state: SnakeGameState): SnakeGameState {
    if (state.isGameOver || state.isWin) return state

    val head = state.snake.first()
    val newHead = when (state.direction) {
        Direction.UP    -> head.copy(second = head.second - 1)
        Direction.DOWN  -> head.copy(second = head.second + 1)
        Direction.LEFT  -> head.copy(first  = head.first  - 1)
        Direction.RIGHT -> head.copy(first  = head.first  + 1)
    }

    val outOfBounds = newHead.first  !in 0 until GRID_SIZE ||
            newHead.second !in 0 until GRID_SIZE
    val ateApple    = newHead in state.apples
    val newSnake    = if (ateApple) listOf(newHead) + state.snake
    else          listOf(newHead) + state.snake.dropLast(1)
    val selfCollide = newHead in newSnake.drop(1)

    if (outOfBounds || selfCollide) {
        return state.copy(isGameOver = true)
    }

    val newApples = if (ateApple) state.apples - newHead else state.apples
    val isWin     = ateApple && newApples.isEmpty()

    return state.copy(
        snake      = newSnake,
        apples     = newApples,
        isGameOver = isWin,   // stoppe la boucle dans les deux cas
        isWin      = isWin,
        score      = if (ateApple) state.score + 1 else state.score
    )
}

/** Empêche les demi-tours */
fun Direction.isUTurnFrom(current: Direction) =
    (this == Direction.UP    && current == Direction.DOWN)  ||
            (this == Direction.DOWN  && current == Direction.UP)    ||
            (this == Direction.LEFT  && current == Direction.RIGHT) ||
            (this == Direction.RIGHT && current == Direction.LEFT)
