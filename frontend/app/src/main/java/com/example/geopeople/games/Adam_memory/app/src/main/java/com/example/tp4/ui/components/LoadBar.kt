package com.example.tp4.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color

@Composable
fun LoadBar(
    backgroundColor: Color,
    foregroundColor: Color,
    fillRatio: Float,
    modifier: Modifier = Modifier
) {
    Row(modifier = modifier) {
        if (fillRatio > 0f) {
            Box(
                modifier = Modifier
                    .weight(fillRatio.coerceIn(0.001f, 1f))
                    .fillMaxHeight()
                    .background(foregroundColor)
            )
        }
        if (fillRatio < 1f) {
            Box(
                modifier = Modifier
                    .weight((1f - fillRatio).coerceIn(0.001f, 1f))
                    .fillMaxHeight()
                    .background(backgroundColor)
            )
        }
    }
}
