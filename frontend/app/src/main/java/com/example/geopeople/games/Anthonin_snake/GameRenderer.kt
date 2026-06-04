package tp4.uge.snake

import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope

fun DrawScope.drawGrid(cellSize: Float) {
    for (x in 0 until GRID_SIZE) {
        for (y in 0 until GRID_SIZE) {
            val color = if ((x + y) % 2 == 0) SnakeTheme.GridDark else SnakeTheme.GridLight
            drawRect(color, Offset(x * cellSize, y * cellSize), Size(cellSize, cellSize))
        }
    }
    for (i in 0..GRID_SIZE) {
        drawLine(SnakeTheme.GridLine, Offset(i * cellSize, 0f),
            Offset(i * cellSize, GRID_SIZE * cellSize), strokeWidth = 1f)
        drawLine(SnakeTheme.GridLine, Offset(0f, i * cellSize),
            Offset(GRID_SIZE * cellSize, i * cellSize), strokeWidth = 1f)
    }
}

fun DrawScope.drawSnake(snake: List<Pair<Int, Int>>, cellSize: Float) {
    val padding = cellSize * 0.08f
    snake.forEachIndexed { index, (x, y) ->
        val ratio = 1f - (index.toFloat() / snake.size) * 0.5f
        val color = when {
            index == 0               -> SnakeTheme.SnakeHead
            index < snake.size * 0.4 -> SnakeTheme.SnakeBody
            else                     -> SnakeTheme.SnakeTail
        }.copy(alpha = ratio)

        drawRoundRect(
            color        = color,
            topLeft      = Offset(x * cellSize + padding, y * cellSize + padding),
            size         = Size(cellSize - padding * 2, cellSize - padding * 2),
            cornerRadius = CornerRadius(cellSize * 0.25f)
        )

        if (index == 0) drawSnakeEyes(x, y, cellSize)
    }
}

private fun DrawScope.drawSnakeEyes(x: Int, y: Int, cellSize: Float) {
    val eyeR = cellSize * 0.1f
    val cx   = x * cellSize + cellSize / 2
    val cy   = y * cellSize + cellSize / 2
    drawCircle(Color.Black, eyeR, Offset(cx - cellSize * 0.18f, cy - cellSize * 0.18f))
    drawCircle(Color.Black, eyeR, Offset(cx + cellSize * 0.18f, cy - cellSize * 0.18f))
    drawCircle(Color.White, eyeR * 0.4f, Offset(cx - cellSize * 0.16f, cy - cellSize * 0.2f))
    drawCircle(Color.White, eyeR * 0.4f, Offset(cx + cellSize * 0.20f, cy - cellSize * 0.2f))
}

fun DrawScope.drawApples(apples: Set<Pair<Int, Int>>, cellSize: Float) {
    apples.forEach { (x, y) ->
        val cx = x * cellSize + cellSize / 2
        val cy = y * cellSize + cellSize / 2
        val r  = cellSize * 0.38f

        drawCircle(Color.Black.copy(alpha = 0.4f), r * 1.1f, Offset(cx + 2f, cy + 3f))
        drawCircle(
            brush  = Brush.radialGradient(
                colors = listOf(SnakeTheme.AppleShine, SnakeTheme.Apple),
                center = Offset(cx - r * 0.3f, cy - r * 0.3f),
                radius = r * 1.2f
            ),
            radius = r,
            center = Offset(cx, cy)
        )
        drawCircle(Color.White.copy(alpha = 0.35f), r * 0.3f, Offset(cx - r * 0.3f, cy - r * 0.3f))
        drawLine(Color(0xFF5D4037), Offset(cx + r * 0.1f, cy - r),
            Offset(cx + r * 0.3f, cy - r * 1.4f), strokeWidth = 2.5f)
    }
}