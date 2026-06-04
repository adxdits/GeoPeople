package com.example.mini_game.view

import android.graphics.BitmapFactory
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableIntState
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.mini_game.controller.GameEngine
import com.example.mini_game.model.GameResult
import com.example.mini_game.model.PlatformType
import com.example.mini_game.controller.AccelerometerManager
import com.example.mini_game.controller.ClapDetector
import kotlinx.coroutines.delay

private fun loadAssetBitmap(context: android.content.Context, name: String): ImageBitmap {
    return context.assets.open(name).use { BitmapFactory.decodeStream(it).asImageBitmap() }
}



@Composable
fun GameScreen(
    accelerometerManager: AccelerometerManager,
    clapDetector: ClapDetector,
    onGameEnd: (isVictory: Boolean) -> Unit = {}
) {
    val context = LocalContext.current
    val skyBitmap = remember { loadAssetBitmap(context, "background_only_the_sky.png") }
    val platformBitmap = remember { loadAssetBitmap(context, "where_myball_lands.png") }

    val engine = remember { GameEngine() }

    // Single tick state - drives only the drawBehind invalidation
    val frameTick = remember { mutableIntStateOf(0) }

    // HUD state - only updates when integer values actually change
    val scoreState = remember { mutableIntStateOf(0) }
    val timeState = remember { mutableIntStateOf(20) }
    val resultState = remember { mutableStateOf(GameResult.PLAYING) }
    val showClapHintState = remember { mutableStateOf(false) }

    val winnerAlpha = remember { Animatable(0f) }
    val pokeballLiftOff = remember { Animatable(0f) }
    var canvasReady by remember { mutableStateOf(false) }

    LaunchedEffect(canvasReady) {
        if (!canvasReady) return@LaunchedEffect
        accelerometerManager.start()
        clapDetector.onClap = { engine.consumeClap() }
        clapDetector.start()
        engine.reset()
        scoreState.intValue = 0
        timeState.intValue = 20
        resultState.value = GameResult.PLAYING

        while (engine.result == GameResult.PLAYING) {
            withFrameNanos { _ ->
                engine.update(accelerometerManager.tiltX)

                if (engine.score != scoreState.intValue) {
                    scoreState.intValue = engine.score
                }
                val newTime = engine.timeRemaining.toInt()
                if (newTime != timeState.intValue) {
                    timeState.intValue = newTime
                }
                val shouldShowHint = engine.secondsSinceClap >= GameEngine.CLAP_HINT_AFTER_SECONDS
                if (shouldShowHint != showClapHintState.value) {
                    showClapHintState.value = shouldShowHint
                }
                frameTick.intValue++
            }
        }

        resultState.value = engine.result
        accelerometerManager.stop()
        clapDetector.stop()
        clapDetector.onClap = null

        if (engine.result == GameResult.VICTORY) {
            winnerAlpha.animateTo(1f, tween(400))
            pokeballLiftOff.animateTo(1f, tween(1200))
            delay(1500)
            onGameEnd(true)
        } else {
            delay(1500)
            onGameEnd(false)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF5BAEE8))
    ) {
        // Drawing surface - drawBehind isolates invalidation to the draw layer
        Box(
            modifier = Modifier
                .fillMaxSize()
                .onSizeChanged { size ->
                    if (!canvasReady) {
                        engine.init(size.width.toFloat(), size.height.toFloat())
                        canvasReady = true
                    }
                }
                .drawBehind {
                    // Subscribe to invalidation
                    frameTick.intValue
                    if (!canvasReady) return@drawBehind

                    val camY = engine.cameraY
                    val w = size.width
                    val h = size.height

                    // ── Sky background (tiled vertically with parallax) ──
                    val skyAspect = skyBitmap.width.toFloat() / skyBitmap.height.toFloat()
                    val scaledSkyH = w / skyAspect           // height when scaled to fill width
                    val parallax = camY * 0.3f               // slow parallax
                    val bgOffset = ((parallax % scaledSkyH) + scaledSkyH) % scaledSkyH
                    var tileY = -bgOffset
                    while (tileY < h) {
                        drawImage(
                            image = skyBitmap,
                            srcOffset = IntOffset.Zero,
                            srcSize = IntSize(skyBitmap.width, skyBitmap.height),
                            dstOffset = IntOffset(0, tileY.toInt()),
                            dstSize = IntSize(w.toInt(), scaledSkyH.toInt())
                        )
                        tileY += scaledSkyH
                    }

                    // ── Platforms (grass image, aspect-ratio preserved) ──
                    val platforms = engine.platforms
                    val platImgAspect = platformBitmap.width.toFloat() / platformBitmap.height.toFloat()
                    for (i in platforms.indices) {
                        val p = platforms[i]
                        if (!p.isActive) continue
                        val screenY = p.y - camY
                        if (screenY < -80f || screenY > h + 80f) continue

                        // Scale image to platform width, preserve aspect ratio
                        val drawW = p.width
                        val drawH = drawW / platImgAspect
                        val drawX = p.x
                        val drawY = screenY - drawH * 0.44f  // grass top sits at collision line

                        drawImage(
                            image = platformBitmap,
                            srcOffset = IntOffset.Zero,
                            srcSize = IntSize(platformBitmap.width, platformBitmap.height),
                            dstOffset = IntOffset(drawX.toInt(), drawY.toInt()),
                            dstSize = IntSize(drawW.toInt(), drawH.toInt())
                        )

                        // Color indicator dot for special platform types
                        when (p.type) {
                            PlatformType.BOOST -> {
                                drawCircle(
                                    color = Color(0xFFFFD700).copy(alpha = 0.7f),
                                    radius = 5f,
                                    center = Offset(p.x + p.width / 2f, screenY + p.height / 2f)
                                )
                            }
                            PlatformType.BREAKABLE -> {
                                drawCircle(
                                    color = Color(0xFFE53935).copy(alpha = 0.7f),
                                    radius = 5f,
                                    center = Offset(p.x + p.width / 2f, screenY + p.height / 2f)
                                )
                            }
                            PlatformType.MOVING -> {
                                drawCircle(
                                    color = Color(0xFF42A5F5).copy(alpha = 0.7f),
                                    radius = 5f,
                                    center = Offset(p.x + p.width / 2f, screenY + p.height / 2f)
                                )
                            }
                            else -> {}
                        }
                    }

                    // ── Pokéball drawn ──
                    val ball = engine.pokeball
                    val ballR = ball.radius
                    val ballScreenY = ball.y - camY -
                        (if (resultState.value == GameResult.VICTORY)
                            pokeballLiftOff.value * h * 0.5f else 0f)
                    val ballCenter = Offset(ball.x, ballScreenY)

                    rotate(degrees = engine.ballRotation, pivot = ballCenter) {
                        // Top half - red
                        drawArc(
                            color = Color(0xFFE53935),
                            startAngle = 180f,
                            sweepAngle = 180f,
                            useCenter = true,
                            topLeft = Offset(ball.x - ballR, ballScreenY - ballR),
                            size = Size(ballR * 2f, ballR * 2f)
                        )
                        // Bottom half - white
                        drawArc(
                            color = Color.White,
                            startAngle = 0f,
                            sweepAngle = 180f,
                            useCenter = true,
                            topLeft = Offset(ball.x - ballR, ballScreenY - ballR),
                            size = Size(ballR * 2f, ballR * 2f)
                        )
                        // Black outline
                        drawCircle(
                            color = Color.Black,
                            radius = ballR,
                            center = ballCenter,
                            style = Stroke(width = 3f)
                        )
                        // Center horizontal line
                        drawLine(
                            color = Color.Black,
                            start = Offset(ball.x - ballR, ballScreenY),
                            end = Offset(ball.x + ballR, ballScreenY),
                            strokeWidth = 3f
                        )
                        // Center button - outer ring
                        drawCircle(
                            color = Color.Black,
                            radius = ballR * 0.25f,
                            center = ballCenter,
                            style = Stroke(width = 3f)
                        )
                        // Center button - white fill
                        drawCircle(
                            color = Color.White,
                            radius = ballR * 0.18f,
                            center = ballCenter
                        )
                    }
                }
        )

        HudOverlay(scoreState, timeState)
        ClapHint(showClapHintState)
        ResultOverlays(resultState, winnerAlpha, scoreState)
    }
}

@Composable
private fun HudOverlay(scoreState: MutableIntState, timeState: MutableIntState) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 48.dp, start = 20.dp, end = 20.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = "Score: ${scoreState.intValue}",
            color = Color.White,
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold
        )
        val time = timeState.intValue
        Text(
            text = "${time}s",
            color = if (time < 5) Color(0xFFFF5252) else Color.White,
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold
        )
    }
    Box(modifier = Modifier.fillMaxSize()) {
        Text(
            text = "Goal: ${GameEngine.WIN_SCORE}",
            color = Color.White.copy(alpha = 0.5f),
            fontSize = 14.sp,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 80.dp)
        )
    }
}

@Composable
private fun ClapHint(visibleState: MutableState<Boolean>) {
    if (!visibleState.value) return
    Box(modifier = Modifier.fillMaxSize()) {
        Text(
            text = "Clap to jump",
            color = Color.White.copy(alpha = 0.7f),
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 130.dp)
        )
    }
}

@Composable
private fun ResultOverlays(
    resultState: MutableState<GameResult>,
    winnerAlpha: Animatable<Float, *>,
    scoreState: MutableIntState
) {
    when (resultState.value) {
        GameResult.VICTORY -> {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = winnerAlpha.value * 0.6f)),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "WINNER!",
                        color = Color(0xFFFFD700).copy(alpha = winnerAlpha.value),
                        fontSize = 64.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                    Text(
                        text = "Card Captured!",
                        color = Color.White.copy(alpha = winnerAlpha.value),
                        fontSize = 24.sp,
                        modifier = Modifier.padding(top = 12.dp)
                    )
                }
            }
        }
        GameResult.DEFEAT -> {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.7f)),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "GAME OVER",
                        color = Color(0xFFFF5252),
                        fontSize = 48.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                    Text(
                        text = "Score: ${scoreState.intValue} / 1000",
                        color = Color.White,
                        fontSize = 22.sp,
                        modifier = Modifier.padding(top = 12.dp)
                    )
                }
            }
        }
        GameResult.PLAYING -> {}
    }
}
