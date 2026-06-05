package com.example.geopeople.ui.inventory

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.geopeople.model.GeoCard
import java.text.Normalizer

private data class InventoryCollection(
    val title: String,
    val cards: List<CollectionCard>,
    val score: Int
)

private data class CollectionCard(
    val card: GeoCard,
    val coefficient: Int,
    val score: Int
)

@Composable
fun InventoryScreen(
    inventory: List<GeoCard>,
    onCardClick: (GeoCard) -> Unit = {}
) {
    if (inventory.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Aucune carte capturee", style = MaterialTheme.typography.bodyLarge)
        }
        return
    }

    val collections = remember(inventory) { buildInventoryCollections(inventory) }
    val displayCollections = remember(inventory, collections) {
        collections.ifEmpty { listOf(buildBaseCollection(inventory)) }
    }
    val totalScore = remember(collections) {
        val multipliers = mutableMapOf<String, Int>()
        collections.forEach { collection ->
            collection.cards.forEach { collectionCard ->
                val cardId = collectionCard.card.id
                multipliers[cardId] = (multipliers[cardId] ?: 0) + collectionCard.coefficient
            }
        }
        inventory.sumOf { card -> card.power * maxOf(1, multipliers[card.id] ?: 0) }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            Text(
                text = "Inventaire (${inventory.size})",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Score collections: $totalScore pts",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(6.dp))
        }

        items(displayCollections) { collection ->
            CollectionSection(
                collection = collection,
                onCardClick = onCardClick
            )
        }
    }
}

@Composable
private fun CollectionSection(
    collection: InventoryCollection,
    onCardClick: (GeoCard) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = collection.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    text = "${collection.score} pts",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
            }

            collection.cards.forEach { collectionCard ->
                CollectionCardRow(
                    collectionCard = collectionCard,
                    onCardClick = onCardClick
                )
            }
        }
    }
}

@Composable
private fun CollectionCardRow(
    collectionCard: CollectionCard,
    onCardClick: (GeoCard) -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCardClick(collectionCard.card) },
        shape = MaterialTheme.shapes.small,
        color = MaterialTheme.colorScheme.surface
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = collectionCard.card.name,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = "Puissance ${collectionCard.card.power} x${collectionCard.coefficient}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Text(
                text = "${collectionCard.score}",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}

private fun buildInventoryCollections(inventory: List<GeoCard>): List<InventoryCollection> {
    val collectionSpecs = listOf(
        "Initiales" to inventory.groupBy { initials(it.name) },
        "Premiere lettre" to inventory.groupBy { normalize(it.name).firstOrNull()?.toString() ?: "?" },
        "Description" to inventory.groupBy { normalize(it.description).ifBlank { "SANS DESCRIPTION" } }
    )

    return collectionSpecs.flatMap { (label, groups) ->
        groups.entries
            .filter { it.value.isNotEmpty() }
            .filter { it.value.size > 1 }
            .map { entry ->
                buildCollection("$label: ${entry.key}", entry.value)
            }
    }.sortedWith(
        compareByDescending<InventoryCollection> { it.cards.size }
            .thenByDescending { it.score }
            .thenBy { it.title }
    )
}

private fun buildBaseCollection(cards: List<GeoCard>): InventoryCollection {
    val collectionCards = cards.sortedBy { it.name }.map { card ->
        CollectionCard(
            card = card,
            coefficient = 1,
            score = card.power
        )
    }

    return InventoryCollection(
        title = "Cartes capturees",
        cards = collectionCards,
        score = collectionCards.sumOf { it.score }
    )
}

private fun buildCollection(title: String, cards: List<GeoCard>): InventoryCollection {
    val sortedCards = cards.sortedBy { it.name }
    var score = 0
    val collectionCards = sortedCards.mapIndexed { index, card ->
        val coefficient = fibonacci(index + 1)
        val cardScore = card.power * coefficient
        score += cardScore
        CollectionCard(card = card, coefficient = coefficient, score = cardScore)
    }

    return InventoryCollection(
        title = title,
        cards = collectionCards,
        score = score
    )
}

private fun fibonacci(n: Int): Int {
    if (n <= 2) return 1
    var a = 1
    var b = 1
    for (i in 3..n) {
        val next = a + b
        a = b
        b = next
    }
    return b
}

private fun initials(name: String): String {
    return name
        .split(Regex("\\s+"))
        .filter { it.isNotBlank() }
        .joinToString("") { it.first().uppercase() }
        .ifBlank { "?" }
}

private fun normalize(value: String): String {
    val withoutAccents = Normalizer
        .normalize(value.trim(), Normalizer.Form.NFD)
        .replace(Regex("\\p{Mn}+"), "")

    return withoutAccents.uppercase()
}
