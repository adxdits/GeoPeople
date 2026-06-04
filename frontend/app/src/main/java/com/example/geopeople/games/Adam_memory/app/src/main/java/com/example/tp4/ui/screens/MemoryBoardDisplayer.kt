package com.example.tp4.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.tp4.model.MemoryCard
import com.example.tp4.ui.components.FishDisplayer
import kotlin.math.ceil
import kotlin.math.sqrt

@Composable
fun MemoryBoardDisplayer(
    cards: List<MemoryCard>,
    visibility: BooleanArray,
    aspectRatio: Float,
    onCardClick: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    BoxWithConstraints(modifier = modifier) {
        val n = cards.size
        if (n == 0) return@BoxWithConstraints

        val containerRatio = constraints.maxWidth.toFloat() / constraints.maxHeight.toFloat()
        val cols = ceil(sqrt(n.toFloat() * containerRatio / aspectRatio)).toInt().coerceIn(1, n)
        val rows = ceil(n.toFloat() / cols).toInt()

        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Center
        ) {
            for (row in 0 until rows) {
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center
                ) {
                    for (col in 0 until cols) {
                        val index = row * cols + col
                        if (index < n) {
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight()
                                    .padding(2.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable { onCardClick(index) },
                                contentAlignment = Alignment.Center
                            ) {
                                if (visibility[index]) {
                                    FishDisplayer(
                                        fish = cards[index].fish,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                } else {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .background(
                                                Color.Gray,
                                                RoundedCornerShape(8.dp)
                                            )
                                    )
                                }
                            }
                        } else {
                            Spacer(modifier = Modifier.weight(1f))
                        }
                    }
                }
            }
        }
    }
}
