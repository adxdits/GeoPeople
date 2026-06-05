package com.example.geopeople.ui.capture

import android.content.Context
import android.media.AudioManager
import android.media.ToneGenerator
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.geopeople.model.GeoCard
import kotlinx.coroutines.delay

@Composable
fun CaptureOverlay(
    card: GeoCard,
    distance: Double,
    bearingDegrees: Double?,
    directionLabel: String,
    approachMessage: String,
    approachSignal: Int,
    canCapture: Boolean,
    alreadyCaptured: Boolean,
    onCapture: () -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val distanceText = if (distance.isFinite()) "${distance.toInt()} m" else "GPS en attente"
    val remainingMeters = (distance - CAPTURE_RANGE_METERS).coerceAtLeast(0.0)
    val progress = if (distance.isFinite()) {
        ((HELP_RANGE_METERS - distance) / (HELP_RANGE_METERS - CAPTURE_RANGE_METERS))
            .toFloat()
            .coerceIn(0f, 1f)
    } else {
        0f
    }
    val statusColor = when {
        alreadyCaptured -> Color(0xFF607D8B)
        canCapture -> Color(0xFF2E7D32)
        distance <= 120.0 -> Color(0xFFF57C00)
        else -> Color(0xFF5267A0)
    }
    val statusText = when {
        alreadyCaptured -> "Carte deja capturee"
        canCapture -> "Capture possible"
        distance.isFinite() -> "Encore ${remainingMeters.toInt()} m avant capture"
        else -> "Position GPS en attente"
    }
    val bearingText = bearingDegrees?.let { "${it.toInt()} deg" } ?: "--"

    LaunchedEffect(card.id, canCapture, alreadyCaptured, approachSignal) {
        if (alreadyCaptured) {
            return@LaunchedEffect
        }
        if (canCapture) {
            signalCaptureReady(context)
        } else {
            signalApproachChange(context, approachSignal)
        }
    }

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.BottomCenter
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            elevation = CardDefaults.cardElevation(8.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        card.name,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, "Fermer")
                    }
                }
                Text(card.description, style = MaterialTheme.typography.bodyMedium)
                Spacer(modifier = Modifier.height(8.dp))

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFF3F6FF))
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = statusText,
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = statusColor
                                )
                                Text(
                                    text = approachMessage,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color(0xFF40515F)
                                )
                            }
                            Text(
                                text = directionLabel,
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.Bold,
                                color = statusColor
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))
                        LinearProgressIndicator(
                            progress = { progress },
                            modifier = Modifier.fillMaxWidth(),
                            color = statusColor,
                            trackColor = Color(0xFFD8DEEF)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Distance: $distanceText", style = MaterialTheme.typography.bodySmall)
                            Text("Direction: $bearingText", style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))
                Text("Puissance: ${card.power}", style = MaterialTheme.typography.bodySmall)
                Spacer(modifier = Modifier.height(16.dp))

                when {
                    alreadyCaptured -> {
                        Button(onClick = {}, enabled = false, modifier = Modifier.fillMaxWidth()) {
                            Text("Déjà capturée ✓")
                        }
                    }
                    canCapture -> {
                        Button(
                            onClick = onCapture,
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50))
                        ) {
                            Text("Capturer !")
                        }
                    }
                    else -> {
                        Button(onClick = {}, enabled = false, modifier = Modifier.fillMaxWidth()) {
                            Text("Trop loin (approchez à < 50m)")
                        }
                    }
                }
            }
        }
    }
}

private const val CAPTURE_RANGE_METERS = 50.0
private const val HELP_RANGE_METERS = 500.0

private suspend fun signalCaptureReady(context: Context) {
    vibrate(context, longArrayOf(0, 180))
    playTone(ToneGenerator.TONE_PROP_ACK, 160)
}

private suspend fun signalApproachChange(context: Context, approachSignal: Int) {
    when (approachSignal) {
        1 -> {
            vibrate(context, longArrayOf(0, 70))
            playTone(ToneGenerator.TONE_PROP_BEEP, 80)
        }
        -1 -> {
            vibrate(context, longArrayOf(0, 60, 70, 60))
            playTone(ToneGenerator.TONE_PROP_NACK, 120)
        }
    }
}

private fun vibrate(context: Context, pattern: LongArray) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        val vibratorManager = context.getSystemService(VibratorManager::class.java)
        vibratorManager?.defaultVibrator?.vibrate(
            VibrationEffect.createWaveform(pattern, -1)
        )
        return
    }

    @Suppress("DEPRECATION")
    val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator ?: return
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        vibrator.vibrate(VibrationEffect.createWaveform(pattern, -1))
    } else {
        @Suppress("DEPRECATION")
        vibrator.vibrate(pattern, -1)
    }
}

private suspend fun playTone(tone: Int, durationMs: Int) {
    val toneGenerator = ToneGenerator(AudioManager.STREAM_NOTIFICATION, 70)
    toneGenerator.startTone(tone, durationMs)
    delay(durationMs.toLong() + 40)
    toneGenerator.release()
}

@Composable
fun CaptureSuccessAnimation(onDismiss: () -> Unit) {
    var visible by remember { mutableStateOf(true) }

    val scale by animateFloatAsState(
        targetValue = if (visible) 1.2f else 0f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "scale"
    )
    val alpha by animateFloatAsState(
        targetValue = if (visible) 1f else 0f,
        animationSpec = tween(500),
        label = "alpha"
    )

    LaunchedEffect(Unit) {
        delay(1500)
        visible = false
    }

    LaunchedEffect(visible, alpha) {
        if (!visible && alpha == 0f) onDismiss()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.5f * alpha)),
        contentAlignment = Alignment.Center
    ) {
        Text(
            "Carte capturée !",
            modifier = Modifier
                .scale(scale)
                .alpha(alpha),
            style = MaterialTheme.typography.displaySmall,
            color = Color.White,
            fontWeight = FontWeight.Bold
        )
    }
}
