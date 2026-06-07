package com.example.minijeu.minigames.treasure

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.onSizeChanged

@Composable
fun TreasureGameCanvas(
    modifier: Modifier,
    config: TreasureGameConfig,
    phase: GamePhase,
    distance: Float,
    treasureDistance: Float,
    playerX: Float,
    playerScreenY: Float,
    safeFrames: Int,
    shakeProgress: Float,
    breathCount: Int,
    collectedStars: Set<Int>,
    stars: List<Star>,
    onSizeChanged: (width: Float, height: Float) -> Unit
) {
    Canvas(
        modifier = modifier
            .background(Color.Black)
            .onSizeChanged { size ->
                onSizeChanged(size.width.toFloat(), size.height.toFloat())
            }
    ) {
        drawRect(
            brush = Brush.verticalGradient(
                colors = listOf(Color(0xFF0B1728), Color(0xFF020409))
            ),
            size = Size(size.width, size.height)
        )

        repeat(70) { index ->
            val x = ((index * 61) % size.width.toInt()).toFloat()
            val y = ((index * 127) % size.height.toInt()).toFloat()
            drawCircle(
                color = Color.White.copy(alpha = 0.05f),
                radius = 1.4f,
                center = Offset(x, y)
            )
        }

        val leftPath = mutableListOf<Offset>()
        val rightPath = mutableListOf<Offset>()
        var screenY = -40f
        while (screenY <= size.height + 40f) {
            val worldY = distance + (size.height - screenY)
            val center = roadCenter(worldY, size.width)
            val halfWidth = roadHalfWidthAt(worldY, config.roadHalfWidth, config.finishDistance)
            leftPath += Offset(center - halfWidth, screenY)
            rightPath += Offset(center + halfWidth, screenY)
            screenY += 24f
        }

        val road = Path().apply {
            moveTo(leftPath.first().x, leftPath.first().y)
            leftPath.drop(1).forEach { lineTo(it.x, it.y) }
            rightPath.asReversed().forEach { lineTo(it.x, it.y) }
            close()
        }

        drawPath(
            path = road,
            brush = Brush.verticalGradient(
                colors = listOf(Color(0xFF253B55), Color(0xFF142235))
            )
        )

        leftPath.zipWithNext().forEach { (start, end) ->
            drawLine(Color(0xFFFFD166), start, end, strokeWidth = 5f)
        }
        rightPath.zipWithNext().forEach { (start, end) ->
            drawLine(Color(0xFFFFD166), start, end, strokeWidth = 5f)
        }

        drawRoadMarkers(distance)
        drawClues(distance, config.finishDistance, collectedStars, stars)
        drawFinishLine(distance, treasureDistance, config)
        drawPlayer(playerX, playerScreenY, config.playerRadius)

        if (safeFrames > 0 && phase == GamePhase.Playing) {
            drawCircle(
                color = Color(0xFF69F0AE).copy(alpha = 0.18f),
                radius = config.playerRadius + 20f,
                center = Offset(playerX, playerScreenY)
            )
        }

        drawProgressBar(distance, treasureDistance)

        if (phase == GamePhase.Treasure || phase == GamePhase.Cleaning || phase == GamePhase.Won) {
            drawTreasureOverlay(phase, shakeProgress, breathCount)
        }
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawRoadMarkers(distance: Float) {
    var markerY = ((distance % 180f) * -1f) + size.height
    while (markerY > -80f) {
        val worldY = distance + (size.height - markerY)
        val center = roadCenter(worldY, size.width)
        drawLine(
            color = Color.White.copy(alpha = 0.18f),
            start = Offset(center - 22f, markerY),
            end = Offset(center + 22f, markerY),
            strokeWidth = 4f
        )
        markerY -= 180f
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawClues(
    distance: Float,
    finishDistance: Float,
    collectedStars: Set<Int>,
    stars: List<Star>
) {
    stars.forEachIndexed { index, star ->
        if (index !in collectedStars) {
            val lap = kotlin.math.floor(distance / finishDistance).toInt()
            var starWorldY = star.worldY + lap * finishDistance
            if (starWorldY < distance - 80f) {
                starWorldY += finishDistance
            }
            val starY = size.height - (starWorldY - distance)
            if (starY in -40f..size.height + 40f) {
                val starX = roadCenter(starWorldY, size.width) + star.offset
                drawCircle(
                    color = Color(0xFF80DEEA).copy(alpha = 0.24f),
                    radius = 32f,
                    center = Offset(starX, starY)
                )
                drawRoundRect(
                    color = Color(0xFFFFF3C4),
                    topLeft = Offset(starX - 16f, starY - 20f),
                    size = Size(32f, 40f),
                    cornerRadius = CornerRadius(5f, 5f)
                )
                drawLine(
                    color = Color(0xFF8D6E63).copy(alpha = 0.75f),
                    start = Offset(starX - 9f, starY - 7f),
                    end = Offset(starX + 9f, starY - 7f),
                    strokeWidth = 3f
                )
                drawLine(
                    color = Color(0xFF8D6E63).copy(alpha = 0.55f),
                    start = Offset(starX - 9f, starY + 3f),
                    end = Offset(starX + 5f, starY + 3f),
                    strokeWidth = 3f
                )
                drawCircle(
                    color = Color(0xFFE53935),
                    radius = 5f,
                    center = Offset(starX + 9f, starY - 14f)
                )
            }
        }
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawFinishLine(
    distance: Float,
    treasureDistance: Float,
    config: TreasureGameConfig
) {
    val finishY = size.height - (treasureDistance - distance)
    if (finishY in -40f..size.height + 80f) {
        val center = roadCenter(treasureDistance, size.width)
        val halfWidth = roadHalfWidthAt(treasureDistance, config.roadHalfWidth, config.finishDistance)
        drawRect(
            color = Color(0xFF69F0AE),
            topLeft = Offset(center - halfWidth, finishY - 8f),
            size = Size(halfWidth * 2f, 16f)
        )
        drawRoundRect(
            brush = Brush.verticalGradient(
                colors = listOf(Color(0xFFFFF3C4), Color(0xFFDCA85C))
            ),
            topLeft = Offset(center - 44f, finishY - 68f),
            size = Size(88f, 56f),
            cornerRadius = CornerRadius(10f, 10f)
        )
        drawCircle(
            color = Color(0xFFE53935),
            radius = 8f,
            center = Offset(center, finishY - 42f)
        )
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawPlayer(
    playerX: Float,
    playerScreenY: Float,
    playerRadius: Float
) {
    drawCircle(
        color = Color.Black.copy(alpha = 0.45f),
        radius = playerRadius + 7f,
        center = Offset(playerX + 7f, playerScreenY + 10f)
    )
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(Color(0xFFFFF59D), Color(0xFFFFA726), Color(0xFFE65100)),
            center = Offset(playerX - 8f, playerScreenY - 8f),
            radius = playerRadius * 1.5f
        ),
        radius = playerRadius,
        center = Offset(playerX, playerScreenY)
    )
    drawCircle(
        color = Color.White.copy(alpha = 0.85f),
        radius = 5f,
        center = Offset(playerX - 7f, playerScreenY - 8f)
    )
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawProgressBar(
    distance: Float,
    finishDistance: Float
) {
    val progress = (distance / finishDistance).coerceIn(0f, 1f)
    drawRoundRect(
        color = Color.White.copy(alpha = 0.18f),
        topLeft = Offset(20f, 20f),
        size = Size((size.width - 40f) * progress, 12f),
        cornerRadius = CornerRadius(8f, 8f)
    )
    drawRoundRect(
        color = Color.White.copy(alpha = 0.08f),
        topLeft = Offset(20f, 20f),
        size = Size(size.width - 40f, 12f),
        cornerRadius = CornerRadius(8f, 8f),
        style = Stroke(width = 2f)
    )
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawTreasureOverlay(
    phase: GamePhase,
    shakeProgress: Float,
    breathCount: Int
) {
    val center = Offset(size.width / 2f, size.height / 2f)
    drawCircle(
        color = Color(0xFFFFD54F).copy(alpha = 0.18f + shakeProgress * 0.25f),
        radius = 150f + shakeProgress * 55f,
        center = center
    )

    if (phase == GamePhase.Treasure) {
        drawBiographyCard(center)
    } else {
        drawBiographyCard(center)

        if (phase == GamePhase.Cleaning) {
            val dustAlpha = if (breathCount == 0) 0.78f else 0.36f
            repeat(22) { index ->
                drawCircle(
                    color = Color(0xFF7A5546).copy(alpha = dustAlpha),
                    radius = 10f + (index % 4) * 5f,
                    center = Offset(
                        center.x - 80f + ((index * 37) % 160),
                        center.y - 105f + ((index * 61) % 210)
                    )
                )
            }
        }
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawBiographyCard(center: Offset) {
    drawRoundRect(
        brush = Brush.verticalGradient(
            colors = listOf(Color(0xFFFFF6D6), Color(0xFFDCA85C))
        ),
        topLeft = Offset(center.x - 95f, center.y - 130f),
        size = Size(190f, 260f),
        cornerRadius = CornerRadius(22f, 22f)
    )
    drawRoundRect(
        color = Color(0xFF6D4C41).copy(alpha = 0.24f),
        topLeft = Offset(center.x - 78f, center.y - 112f),
        size = Size(156f, 226f),
        cornerRadius = CornerRadius(16f, 16f),
        style = Stroke(width = 4f)
    )
    drawCircle(
        color = Color(0xFF5D4037).copy(alpha = 0.82f),
        radius = 30f,
        center = Offset(center.x, center.y - 52f)
    )
    drawRoundRect(
        color = Color(0xFF5D4037).copy(alpha = 0.82f),
        topLeft = Offset(center.x - 48f, center.y - 12f),
        size = Size(96f, 54f),
        cornerRadius = CornerRadius(26f, 26f)
    )
    drawCircle(
        color = Color(0xFFE53935),
        radius = 10f,
        center = Offset(center.x + 58f, center.y + 82f)
    )
    drawCircle(
        color = Color.White.copy(alpha = 0.92f),
        radius = 4f,
        center = Offset(center.x + 58f, center.y + 82f)
    )
    drawLine(
        color = Color(0xFF6D4C41).copy(alpha = 0.55f),
        start = Offset(center.x - 48f, center.y + 68f),
        end = Offset(center.x + 28f, center.y + 68f),
        strokeWidth = 5f
    )
    drawLine(
        color = Color(0xFF6D4C41).copy(alpha = 0.38f),
        start = Offset(center.x - 42f, center.y + 92f),
        end = Offset(center.x + 38f, center.y + 92f),
        strokeWidth = 4f
    )
}
