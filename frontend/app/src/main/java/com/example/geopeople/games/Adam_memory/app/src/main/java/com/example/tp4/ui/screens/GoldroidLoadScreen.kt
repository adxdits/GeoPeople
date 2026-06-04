package com.example.tp4.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.example.tp4.ui.components.GoldroidDisplayer
import com.example.tp4.ui.components.LoadBar
import kotlinx.coroutines.delay

@Composable
fun GoldroidLoadScreen(
    duration: Long = 5000L,
    onLoaded: () -> Unit,
    modifier: Modifier = Modifier
) {
    var fillRatio by remember { mutableFloatStateOf(0f) }

    LaunchedEffect(duration) {
        val stepMs = 20L
        val totalSteps = duration / stepMs
        for (i in 1..totalSteps) {
            delay(stepMs)
            fillRatio = i.toFloat() / totalSteps.toFloat()
        }
        fillRatio = 1f
        onLoaded()
    }

    Column(modifier = modifier.fillMaxSize()) {
        GoldroidDisplayer(
            modifier = Modifier
                .weight(0.9f)
                .fillMaxWidth()
        )
        LoadBar(
            backgroundColor = Color.White,
            foregroundColor = Color.Blue,
            fillRatio = fillRatio,
            modifier = Modifier
                .weight(0.1f)
                .fillMaxWidth()
        )
    }
}
