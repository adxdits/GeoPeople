package com.example.geopeople.ui.inventory

import android.content.Intent
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.geopeople.data.BiographyDetails
import com.example.geopeople.data.BiographyService
import com.example.geopeople.model.GeoCard
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.URL

@Composable
fun CardDetailScreen(
    card: GeoCard,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var selectedSource by remember { mutableStateOf<Pair<String, String>?>(null) }
    var biography by remember(card.name) { mutableStateOf<BiographyDetails?>(null) }
    var isLoading by remember(card.name) { mutableStateOf(true) }

    LaunchedEffect(card.name) {
        isLoading = true
        biography = BiographyService.fetchBiography(card.name)
        isLoading = false
    }

    fun openUrl(url: String) {
        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        OutlinedButton(onClick = onBack) {
            Text("Retour")
        }

        HeaderCard(card = card, biography = biography)

        if (isLoading) {
            Card(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.padding(18.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CircularProgressIndicator(modifier = Modifier.size(28.dp))
                    Text("Chargement des informations Wikidata et Wikipedia...")
                }
            }
        } else {
            BiographyInfoCard(card = card, biography = biography)
            SummaryCard(summary = biography?.summary)
            SourceCard(
                biography = biography,
                onSelectSource = { selectedSource = it }
            )
        }
    }

    selectedSource?.let { source ->
        AlertDialog(
            onDismissRequest = { selectedSource = null },
            title = { Text(source.first) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Lien externe pour ${card.name}")
                    Text(source.second, style = MaterialTheme.typography.bodySmall)
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        openUrl(source.second)
                        selectedSource = null
                    }
                ) {
                    Text("Ouvrir")
                }
            },
            dismissButton = {
                TextButton(onClick = { selectedSource = null }) {
                    Text("Fermer")
                }
            }
        )
    }
}

@Composable
private fun HeaderCard(card: GeoCard, biography: BiographyDetails?) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            RemotePortrait(
                imageUrl = biography?.imageUrl ?: card.imageUrl,
                name = biography?.displayName ?: card.name
            )
            Spacer(modifier = Modifier.height(14.dp))
            Text(
                text = biography?.displayName ?: card.name,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            Text(card.description, style = MaterialTheme.typography.bodyLarge)
            Spacer(modifier = Modifier.height(12.dp))
            Text("Puissance : ${card.power}", style = MaterialTheme.typography.titleMedium)
            Text("Identifiant carte : ${card.id}", style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
private fun RemotePortrait(imageUrl: String?, name: String) {
    var bitmap by remember(imageUrl) { mutableStateOf<ImageBitmap?>(null) }

    LaunchedEffect(imageUrl) {
        bitmap = null
        if (imageUrl != null) {
            bitmap = withContext(Dispatchers.IO) {
                runCatching {
                    URL(imageUrl).openStream().use { stream ->
                        BitmapFactory.decodeStream(stream)?.asImageBitmap()
                    }
                }.getOrNull()
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(210.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xFFE8ECF3)),
        contentAlignment = Alignment.Center
    ) {
        val loadedBitmap = bitmap
        if (loadedBitmap != null) {
            Image(
                bitmap = loadedBitmap,
                contentDescription = "Portrait de $name",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        } else {
            Text("Photo indisponible", color = Color(0xFF5B6472))
        }
    }
}

@Composable
private fun BiographyInfoCard(card: GeoCard, biography: BiographyDetails?) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "Informations biographiques",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            InfoLine("Naissance", listOfNotNull(biography?.birthDate, biography?.birthPlace).joinToString(" - "))
            InfoLine("Mort", listOfNotNull(biography?.deathDate, biography?.deathPlace).joinToString(" - "))
            InfoLine("Profession", biography?.occupation.orEmpty())
            InfoLine("Wikidata", biography?.wikidataId.orEmpty())
            InfoLine("Coordonnees", "${"%.5f".format(card.latitude)}, ${"%.5f".format(card.longitude)}")
        }
    }
}

@Composable
private fun SummaryCard(summary: String?) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "Resume",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = summary?.takeIf { it.isNotBlank() }
                    ?: "Aucun resume Wikipedia disponible pour cette carte.",
                style = MaterialTheme.typography.bodyLarge
            )
        }
    }
}

@Composable
private fun SourceCard(
    biography: BiographyDetails?,
    onSelectSource: (Pair<String, String>) -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = "Sources biographiques",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Les liens externes restent disponibles sans afficher de page HTML dans l'application.",
                style = MaterialTheme.typography.bodyMedium
            )
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Button(
                    onClick = {
                        biography?.wikidataUrl?.let { onSelectSource("Wikidata" to it) }
                    },
                    enabled = biography?.wikidataUrl != null,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Wikidata")
                }
                Button(
                    onClick = {
                        biography?.wikipediaUrl?.let { onSelectSource("Wikipedia" to it) }
                    },
                    enabled = biography?.wikipediaUrl != null,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Wikipedia")
                }
            }
        }
    }
}

@Composable
private fun InfoLine(label: String, value: String) {
    Column {
        Text(label, style = MaterialTheme.typography.labelLarge, color = Color(0xFF5B6472))
        Text(
            text = value.takeIf { it.isNotBlank() } ?: "Information indisponible",
            style = MaterialTheme.typography.bodyLarge
        )
    }
}
