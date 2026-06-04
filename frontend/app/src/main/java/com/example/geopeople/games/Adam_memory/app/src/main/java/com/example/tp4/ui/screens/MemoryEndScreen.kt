package com.example.tp4.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun MemoryEndScreen(
    pairsFound: Int,
    attempts: Int,
    elapsedMs: Long,
    onRestart: () -> Unit,
    onValidateCapture: () -> Unit,
    modifier: Modifier = Modifier
) {
    val totalSeconds = elapsedMs / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Félicitations !",
            style = MaterialTheme.typography.headlineLarge
        )
        Spacer(modifier = Modifier.height(24.dp))
        Text(
            text = "Paires trouvées : $pairsFound",
            style = MaterialTheme.typography.titleMedium
        )
        Text(
            text = "Tentatives : $attempts",
            style = MaterialTheme.typography.titleMedium
        )
        Text(
            text = "Temps : ${minutes}min ${seconds}s",
            style = MaterialTheme.typography.titleMedium
        )
        Spacer(modifier = Modifier.height(48.dp))
        Button(onClick = onRestart) {
            Text("Rejouer")
        }
        Spacer(modifier = Modifier.height(12.dp))
        Button(onClick = onValidateCapture) {
            Text("Valider la capture")
        }
    }
}
