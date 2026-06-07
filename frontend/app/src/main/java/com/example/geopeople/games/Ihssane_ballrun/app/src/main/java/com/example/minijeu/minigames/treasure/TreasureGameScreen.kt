package com.example.minijeu.minigames.treasure

import android.Manifest
import android.content.pm.PackageManager
import android.media.MediaRecorder
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.geopeople.R
import com.example.minijeu.sensors.MotionSensorHandler
import kotlinx.coroutines.delay
import java.io.File
import kotlin.math.abs
import kotlin.math.sqrt

@Composable
fun TreasureGameScreen(
    onGameEnd: (isVictory: Boolean) -> Unit = {}
) {
    val context = LocalContext.current
    val config = remember { TreasureGameConfig() }
    val stars = remember { treasureStars }

    var gameWidth by remember { mutableStateOf(0f) }
    var gameHeight by remember { mutableStateOf(0f) }
    var tiltX by remember { mutableStateOf(0f) }
    var shakePower by remember { mutableStateOf(0f) }
    var playerX by remember { mutableStateOf(0f) }
    var velocityX by remember { mutableStateOf(0f) }
    var distance by remember { mutableStateOf(0f) }
    var phase by remember { mutableStateOf(GamePhase.Playing) }
    var safeFrames by remember { mutableStateOf(config.startSafeFrames) }
    var treasureDistance by remember { mutableStateOf(config.finishDistance) }
    var shakeProgress by remember { mutableStateOf(0f) }
    var breathCount by remember { mutableStateOf(0) }
    var micLevel by remember { mutableStateOf(0) }
    var collectedStars by remember { mutableStateOf(setOf<Int>()) }
    var message by remember { mutableStateOf(context.getString(R.string.treasure_initial_message)) }
    var hasAudioPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.RECORD_AUDIO
            ) == PackageManager.PERMISSION_GRANTED
        )
    }

    val playerScreenY = gameHeight * 0.78f
    val startPlayerWorldY = if (gameHeight == 0f) 0f else gameHeight - playerScreenY

    fun restartGame() {
        playerX = if (gameWidth == 0f) 0f else roadCenter(startPlayerWorldY, gameWidth)
        velocityX = 0f
        distance = 0f
        treasureDistance = config.finishDistance
        safeFrames = config.startSafeFrames
        shakeProgress = 0f
        breathCount = 0
        micLevel = 0
        collectedStars = emptySet()
        message = context.getString(R.string.treasure_initial_message)
        phase = GamePhase.Playing
    }

    val audioPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasAudioPermission = isGranted
        message = if (isGranted) {
            context.getString(R.string.treasure_micro_ready_dust)
        } else {
            context.getString(R.string.treasure_micro_denied)
        }
    }

    val latestMotionHandler by rememberUpdatedState(
        newValue = { x: Float, y: Float, z: Float ->
            tiltX = x
            shakePower = abs(sqrt(x * x + y * y + z * z) - 9.8f)
        }
    )

    DisposableEffect(context) {
        val handler = MotionSensorHandler(context) { x, y, z ->
            latestMotionHandler(x, y, z)
        }
        handler.register()

        onDispose {
            handler.unregister()
        }
    }

    LaunchedEffect(gameWidth) {
        if (gameWidth > 0f && playerX == 0f) {
            playerX = roadCenter(startPlayerWorldY, gameWidth)
        }
    }

    LaunchedEffect(gameWidth, gameHeight, phase) {
        if (gameWidth == 0f || gameHeight == 0f || phase != GamePhase.Playing) return@LaunchedEffect

        while (phase == GamePhase.Playing) {
            distance += config.roadSpeed

            velocityX += -tiltX * config.tiltAcceleration
            velocityX *= config.friction
            playerX = (playerX + velocityX).coerceIn(
                config.playerRadius,
                gameWidth - config.playerRadius
            )

            val playerWorldY = distance + (gameHeight - playerScreenY)
            val center = roadCenter(playerWorldY, gameWidth)
            val currentRoadHalfWidth = roadHalfWidthAt(
                playerWorldY,
                config.roadHalfWidth,
                config.finishDistance
            )
            val distanceFromCenter = abs(playerX - center)

            if (safeFrames > 0) {
                safeFrames--
            }

            if (safeFrames == 0 && distanceFromCenter > currentRoadHalfWidth - config.playerRadius) {
                phase = GamePhase.Lost
                velocityX = 0f
            }

            stars.forEachIndexed { index, star ->
                if (index !in collectedStars) {
                    val lap = kotlin.math.floor(distance / config.finishDistance).toInt()
                    var starWorldY = star.worldY + lap * config.finishDistance
                    if (starWorldY < distance - 80f) {
                        starWorldY += config.finishDistance
                    }
                    val starScreenY = gameHeight - (starWorldY - distance)
                    val starCenterX = roadCenter(starWorldY, gameWidth) + star.offset
                    val dx = playerX - starCenterX
                    val dy = playerScreenY - starScreenY
                    val hitDistance = sqrt(dx * dx + dy * dy)

                    if (hitDistance < config.playerRadius + 24f) {
                        collectedStars = collectedStars + index
                    }
                }
            }

            if (distance >= treasureDistance) {
                velocityX = 0f

                if (collectedStars.size == stars.size) {
                    phase = GamePhase.Treasure
                    message = context.getString(R.string.treasure_card_found)
                } else {
                    treasureDistance += config.finishDistance
                    message = context.getString(R.string.treasure_card_locked, stars.size - collectedStars.size)
                }
            }

            delay(16)
        }
    }

    LaunchedEffect(phase) {
        if (phase != GamePhase.Treasure) return@LaunchedEffect

        while (phase == GamePhase.Treasure) {
            if (shakePower > config.shakeThreshold) {
                shakeProgress = (shakeProgress + 0.035f).coerceAtMost(1f)
                message = context.getString(R.string.treasure_card_clear_percent, (shakeProgress * 100).toInt())
            } else {
                shakeProgress = (shakeProgress - 0.006f).coerceAtLeast(0f)
            }

            if (shakeProgress >= 1f) {
                phase = GamePhase.Cleaning
                message = context.getString(R.string.treasure_blow_twice)
            }

            delay(50)
        }
    }

    LaunchedEffect(phase, hasAudioPermission) {
        if (phase != GamePhase.Cleaning || !hasAudioPermission) return@LaunchedEffect

        val outputFile = File(context.cacheDir, "treasure_breath.3gp")
        val recorder = MediaRecorder()
        var soundCooldown = false

        try {
            recorder.setAudioSource(MediaRecorder.AudioSource.MIC)
            recorder.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP)
            recorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB)
            recorder.setOutputFile(outputFile.absolutePath)
            recorder.prepare()
            recorder.start()

            while (phase == GamePhase.Cleaning) {
                micLevel = recorder.maxAmplitude

                if (micLevel > config.breathThreshold && !soundCooldown) {
                    breathCount++
                    soundCooldown = true
                    message = context.getString(R.string.treasure_dust_removed, breathCount)

                    if (breathCount >= 2) {
                        phase = GamePhase.Won
                        message = context.getString(R.string.treasure_card_captured)
                    }

                    delay(800)
                    soundCooldown = false
                }

                delay(100)
            }
        } catch (exception: Exception) {
            message = context.getString(R.string.treasure_micro_unavailable, exception.message ?: exception.javaClass.simpleName)
        } finally {
            try {
                recorder.stop()
            } catch (exception: Exception) {
                // The recorder may not have started if the emulator blocks the microphone.
            }
            recorder.release()
        }
    }

    LaunchedEffect(phase) {
        when (phase) {
            GamePhase.Won -> {
                delay(1200)
                onGameEnd(true)
            }
            GamePhase.Lost -> {
                delay(1200)
                onGameEnd(false)
            }
            else -> Unit
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF07111F))
            .padding(16.dp)
    ) {
        Text(
            text = stringResource(R.string.treasure_title),
            color = Color(0xFFEAF2FF),
            fontSize = 32.sp,
            fontWeight = FontWeight.Bold
        )

        Text(
            text = stringResource(R.string.treasure_subtitle),
            color = Color(0xFFA9B8CC),
            fontSize = 15.sp
        )

        Spacer(modifier = Modifier.height(14.dp))

        TreasureGameCanvas(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            config = config,
            phase = phase,
            distance = distance,
            treasureDistance = treasureDistance,
            playerX = playerX,
            playerScreenY = playerScreenY,
            safeFrames = safeFrames,
            shakeProgress = shakeProgress,
            breathCount = breathCount,
            collectedStars = collectedStars,
            stars = stars,
            onSizeChanged = { width, height ->
                gameWidth = width
                gameHeight = height
            }
        )

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = statusText(phase, safeFrames, distance, treasureDistance, collectedStars.size, stars.size, shakeProgress, breathCount),
            color = statusColor(phase),
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(8.dp))

        Row {
            Button(
                onClick = { velocityX -= config.buttonImpulse },
                enabled = phase == GamePhase.Playing
            ) {
                Text(text = stringResource(R.string.treasure_left))
            }

            Spacer(modifier = Modifier.padding(8.dp))

            Button(
                onClick = { velocityX += config.buttonImpulse },
                enabled = phase == GamePhase.Playing
            ) {
                Text(text = stringResource(R.string.treasure_right))
            }

            Spacer(modifier = Modifier.padding(8.dp))

            Button(onClick = { restartGame() }) {
                Text(text = stringResource(R.string.treasure_restart))
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Row {
            Button(
                onClick = {
                    if (!hasAudioPermission) {
                        audioPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                    } else {
                        message = context.getString(R.string.treasure_micro_ready_short)
                    }
                },
                enabled = phase == GamePhase.Cleaning
            ) {
                Text(text = stringResource(R.string.treasure_enable_micro))
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = if (phase == GamePhase.Cleaning && hasAudioPermission) {
                stringResource(R.string.treasure_micro_level, message, micLevel)
            } else {
                message
            },
            color = Color(0xFFA9B8CC),
            fontSize = 14.sp
        )
    }
}

@Composable
private fun statusText(
    phase: GamePhase,
    safeFrames: Int,
    distance: Float,
    finishDistance: Float,
    collectedStars: Int,
    totalStars: Int,
    shakeProgress: Float,
    breathCount: Int
): String {
    return when (phase) {
        GamePhase.Won -> stringResource(R.string.treasure_card_captured)
        GamePhase.Lost -> stringResource(R.string.treasure_lost)
        GamePhase.Treasure -> stringResource(R.string.treasure_clear_card, (shakeProgress * 100).toInt())
        GamePhase.Cleaning -> stringResource(R.string.treasure_clean_card, breathCount)
        GamePhase.Playing -> {
            if (safeFrames > 0) {
                stringResource(R.string.treasure_protected_start)
            } else {
                stringResource(R.string.treasure_progress, (distance / finishDistance * 100f).toInt(), collectedStars, totalStars)
            }
        }
    }
}

private fun statusColor(phase: GamePhase): Color {
    return when (phase) {
        GamePhase.Won -> Color(0xFF69F0AE)
        GamePhase.Lost -> Color(0xFFFF8A80)
        GamePhase.Treasure -> Color(0xFFFFD54F)
        GamePhase.Cleaning -> Color(0xFFFFE0B2)
        GamePhase.Playing -> Color(0xFFEAF2FF)
    }
}
