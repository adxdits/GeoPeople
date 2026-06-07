package com.example.tp4.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.geopeople.R
import kotlin.math.min

@Composable
fun MemorySettingsScreen(
    maxPairs: Int,
    onStartGame: (numberOfPairs: Int) -> Unit,
    modifier: Modifier = Modifier
) {
    var numberOfPairs by remember { mutableIntStateOf(min(8, maxPairs)) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = stringResource(R.string.memory_game_title),
            style = MaterialTheme.typography.headlineLarge
        )
        Spacer(modifier = Modifier.height(48.dp))
        Text(
            text = stringResource(R.string.memory_pair_count, numberOfPairs),
            style = MaterialTheme.typography.titleMedium
        )
        Spacer(modifier = Modifier.height(8.dp))
        Slider(
            value = numberOfPairs.toFloat(),
            onValueChange = { numberOfPairs = it.toInt() },
            valueRange = 2f..maxPairs.toFloat(),
            steps = (maxPairs - 3).coerceAtLeast(0)
        )
        Spacer(modifier = Modifier.height(48.dp))
        Button(onClick = { onStartGame(numberOfPairs) }) {
            Text(stringResource(R.string.memory_play))
        }
    }
}
