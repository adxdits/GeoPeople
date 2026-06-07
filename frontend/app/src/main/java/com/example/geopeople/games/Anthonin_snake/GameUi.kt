package tp4.uge.snake

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.geopeople.R

@Composable
fun SnakeGame(
    viewModel: SnakeViewModel,
    onFinished: (Boolean) -> Unit = {}
) {
    val state by viewModel.state.collectAsState()

    LaunchedEffect(state.isGameOver, state.isWin) {
        if (state.isGameOver) {
            kotlinx.coroutines.delay(900)
            onFinished(state.isWin)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(SnakeTheme.Background),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            HUD(state)
            Spacer(Modifier.height(12.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f)
                    .clip(RoundedCornerShape(12.dp))
                    .background(SnakeTheme.GridDark)
            ) {
                GameCanvas(state)
                if (state.isGameOver) {
                    GameOverOverlay(state, onRestart = viewModel::restart)
                }
            }
        }
    }
}

@Composable
private fun GameCanvas(state: SnakeGameState) {
    Canvas(
        modifier = Modifier
            .fillMaxSize()
            .clip(RoundedCornerShape(8.dp))
    ) {
        val cellSize = minOf(size.width, size.height) / GRID_SIZE.toFloat()
        drawGrid(cellSize)
        drawApples(state.apples, cellSize)
        drawSnake(state.snake, cellSize)
        drawRect(
            color    = SnakeTheme.GridLine,
            topLeft  = Offset(0f, 0f),
            size     = Size(GRID_SIZE * cellSize, GRID_SIZE * cellSize),
            style    = Stroke(width = 2f)
        )
    }
}

@Composable
private fun HUD(state: SnakeGameState) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        LabeledValue(stringResource(R.string.snake_hud_apples), "${state.apples.size} / ${state.totalApples}", SnakeTheme.Apple)
        LabeledValue(stringResource(R.string.snake_hud_speed), stringResource(state.speed.labelRes), state.speed.color)
    }
}

@Composable
private fun LabeledValue(label: String, value: String, valueColor: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, color = SnakeTheme.ScoreText.copy(alpha = 0.5f),
            fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.sp)
        Text(value, color = valueColor, fontSize = 18.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun GameOverOverlay(state: SnakeGameState, onRestart: () -> Unit) {
    val isWin = state.isWin
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.80f)),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                if (isWin) stringResource(R.string.snake_win) else stringResource(R.string.snake_lost),
                color = if (isWin) SnakeTheme.Gold else SnakeTheme.Apple,
                fontSize = 34.sp, fontWeight = FontWeight.Bold, letterSpacing = 3.sp
            )
            Spacer(Modifier.height(4.dp))
            Text(stringResource(R.string.snake_apples_score, state.score, state.totalApples),
                color = SnakeTheme.Apple, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(24.dp))
            Button(
                onClick = onRestart,
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isWin) SnakeTheme.Gold else SnakeTheme.SnakeHead
                ),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text(stringResource(R.string.snake_replay), color = Color.Black,
                    fontWeight = FontWeight.Bold, letterSpacing = 2.sp)
            }
        }
    }
}
