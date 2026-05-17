package com.example.geopeople.ui.inventory

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.geopeople.model.GeoCard
import com.example.geopeople.ui.card.GeoCardDisplayer
import com.example.geopeople.ui.notification.sendNotification


@Composable
fun InventoryScreen(inventory: List<GeoCard>) {

    // a l'arrivé sur l'écran on envoie une notification :
    val context = LocalContext.current

    Button(onClick = { sendNotification(context) }) {
        Text("Tester la notification")
    }

    val FakeInventory = listOf(
        GeoCard("GEO-001", "Tour Eiffel", "Symbole emblématique de Paris, offrant une vue panoramique sur la ville.", 48.8584, 2.2945, power = 8),
        GeoCard("GEO-002", "Pyramides de Gizeh", "Anciennes merveilles d'Égypte, témoins de l'ingéniosité humaine.", 29.9792, 31.1342, power = 9),
        GeoCard("GEO-003", "Statue de la Liberté", "Icône de la liberté à New York, accueillant les visiteurs du monde entier.", 40.6892, -74.0444, power = 7)
    )

    if (FakeInventory.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Aucune carte capturée", style = MaterialTheme.typography.bodyLarge)
        }
        return
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text(
                "Inventaire (${inventory.size})",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
            )
            Spacer(modifier = Modifier.height(8.dp))
        }
        items(inventory) { card ->
            GeoCardDisplayer(card = card)
        }
    }
}
