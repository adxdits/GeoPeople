package com.example.tp4.ui.components

import android.os.SystemClock
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import kotlinx.coroutines.delay

@Composable
fun Chronometer(startTime: Long, modifier: Modifier = Modifier) {
    var elapsed by remember { mutableLongStateOf(0L) }

    LaunchedEffect(startTime) {
        if (startTime > 0L) {
            while (true) {
                elapsed = SystemClock.elapsedRealtime() - startTime
                delay(100L)
            }
        }
    }

    val totalSeconds = elapsed / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    Text(
        text = String.format("%02d:%02d", minutes, seconds),
        modifier = modifier
    )
}
