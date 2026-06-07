package com.example.geopeople.ui.inventory

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.geopeople.R
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

private data class InventoryCollectionLabels(
    val initials: String,
    val place: String,
    val relation: String,
    val firstLetter: String,
    val titleFormat: String,
    val baseCollection: String,
    val withoutPlace: String
)

private val InventoryBackground = Color(0xFF0E1620)
private val InventoryPanel = Color(0xFF17212B)
private val InventoryPanelLight = Color(0xFF202B38)
private val InventoryBorder = Color(0xFF334253)
private val InventoryText = Color(0xFFF2F5F8)
private val InventoryMuted = Color(0xFFB7C2CD)
private val InventoryAccent = Color(0xFFFFCB05)

@Composable
fun InventoryScreen(
    inventory: List<GeoCard>,
    onLeaderboardClick: () -> Unit = {},
    onCardClick: (GeoCard) -> Unit = {}
) {
    if (inventory.isEmpty()) {
        EmptyInventory(onLeaderboardClick = onLeaderboardClick)
        return
    }

    val labels = InventoryCollectionLabels(
        initials = stringResource(R.string.inventory_collection_initials),
        place = stringResource(R.string.inventory_collection_place),
        relation = stringResource(R.string.inventory_collection_relation),
        firstLetter = stringResource(R.string.inventory_collection_first_letter),
        titleFormat = stringResource(R.string.inventory_collection_title),
        baseCollection = stringResource(R.string.inventory_base_collection),
        withoutPlace = stringResource(R.string.inventory_without_place)
    )
    val collections = remember(inventory, labels) { buildInventoryCollections(inventory, labels) }
    val displayCollections = remember(inventory, collections) {
        collections.ifEmpty { listOf(buildBaseCollection(inventory, labels.baseCollection)) }
    }
    val baseScore = remember(inventory) { inventory.sumOf { it.power } }
    val cardMultipliers = remember(collections) { buildCardMultipliers(collections) }
    val totalScore = remember(inventory, cardMultipliers) {
        inventory.sumOf { card -> card.power * effectiveMultiplier(card.id, cardMultipliers) }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(InventoryBackground)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            InventoryHeader(
                cardCount = inventory.size,
                collectionCount = collections.size,
                baseScore = baseScore,
                totalScore = totalScore,
                onLeaderboardClick = onLeaderboardClick
            )
        }

        items(displayCollections) { collection ->
            CollectionSection(
                collection = collection,
                cardMultipliers = cardMultipliers,
                onCardClick = onCardClick
            )
        }

        item {
            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}

@Composable
private fun EmptyInventory(onLeaderboardClick: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(InventoryBackground)
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            shape = RoundedCornerShape(8.dp),
            color = InventoryPanel,
            border = BorderStroke(1.dp, InventoryBorder)
        ) {
            Column(
                modifier = Modifier.padding(22.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = stringResource(R.string.inventory_empty_title),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = InventoryText
                )
                Text(
                    text = stringResource(R.string.inventory_empty_subtitle),
                    style = MaterialTheme.typography.bodyMedium,
                    color = InventoryMuted
                )
                Box(modifier = Modifier.padding(top = 8.dp)) {
                    LeaderboardIcon(onLeaderboardClick = onLeaderboardClick)
                }
                Text(
                    text = stringResource(R.string.leaderboard_title),
                    style = MaterialTheme.typography.labelSmall,
                    color = InventoryMuted
                )
            }
        }
    }
}

@Composable
private fun LeaderboardIcon(onLeaderboardClick: () -> Unit) {
    Surface(
        modifier = Modifier
            .size(46.dp)
            .clip(RoundedCornerShape(8.dp))
            .clickable { onLeaderboardClick() },
        shape = RoundedCornerShape(8.dp),
        color = InventoryPanelLight,
        border = BorderStroke(1.dp, InventoryBorder)
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Image(
                painter = painterResource(id = R.drawable.ic_trophy),
                contentDescription = stringResource(R.string.leaderboard_title),
                modifier = Modifier.size(28.dp)
            )
        }
    }
}

@Composable
private fun InventoryHeader(
    cardCount: Int,
    collectionCount: Int,
    baseScore: Int,
    totalScore: Int,
    onLeaderboardClick: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        color = InventoryPanel
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.inventory_title),
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.ExtraBold,
                        color = InventoryText
                    )
                    Text(
                        text = stringResource(R.string.inventory_captured_cards, cardCount),
                        style = MaterialTheme.typography.bodyMedium,
                        color = InventoryMuted
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    LeaderboardIcon(onLeaderboardClick = onLeaderboardClick)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "$totalScore",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = InventoryAccent
                    )
                    Text(
                        text = stringResource(R.string.inventory_points),
                        style = MaterialTheme.typography.labelMedium,
                        color = InventoryMuted
                    )
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) { // j'ai enlevé certaines stats pour alléger la comprehension de l'interface (Anthonin)
                StatPill(label = stringResource(R.string.inventory_collections_label), value = "$collectionCount")
            }
        }
    }
}

@Composable
private fun StatPill(label: String, value: String) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = InventoryPanelLight,
        border = BorderStroke(1.dp, InventoryBorder)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = InventoryMuted
            )
            Text(
                text = value,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = InventoryText
            )
        }
    }
}

@Composable
private fun CollectionSection(
    collection: InventoryCollection,
    cardMultipliers: Map<String, Int>,
    onCardClick: (GeoCard) -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        color = InventoryPanel,
        border = BorderStroke(1.dp, InventoryBorder)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = collection.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = InventoryText,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = stringResource(R.string.inventory_card_count, collection.cards.size),
                        style = MaterialTheme.typography.labelMedium,
                        color = InventoryMuted
                    )
                }
                ScoreBadge(stringResource(R.string.inventory_score_points, collection.score))
            }

            Spacer(modifier = Modifier.height(10.dp))

            collection.cards.forEachIndexed { index, collectionCard ->
                CollectionCardRow(
                    collectionCard = collectionCard,
                    totalMultiplier = effectiveMultiplier(collectionCard.card.id, cardMultipliers),
                    onCardClick = onCardClick
                )
                if (index < collection.cards.lastIndex) {
                    Spacer(modifier = Modifier.height(6.dp))
                }
            }
        }
    }
}

@Composable
private fun ScoreBadge(text: String) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = Color(0xFF263B2D),
        border = BorderStroke(1.dp, Color(0xFF4C7657))
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold,
            color = Color(0xFFBCE5C5)
        )
    }
}

@Composable
private fun CollectionCardRow(
    collectionCard: CollectionCard,
    totalMultiplier: Int,
    onCardClick: (GeoCard) -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .clickable { onCardClick(collectionCard.card) },
        shape = RoundedCornerShape(8.dp),
        color = InventoryPanelLight,
        border = BorderStroke(1.dp, InventoryBorder)
    ) {
        Row(
            modifier = Modifier.padding(10.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            InitialBadge(collectionCard.card.name)

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = collectionCard.card.name,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = InventoryText,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = collectionCard.card.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = InventoryMuted,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = stringResource(R.string.inventory_power_coefficient, collectionCard.card.power, totalMultiplier),
                    style = MaterialTheme.typography.labelSmall,
                    color = InventoryMuted,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun InitialBadge(name: String) {
    Box(
        modifier = Modifier
            .size(38.dp)
            .clip(CircleShape)
            .background(Color(0xFFFFCB05)),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = initials(name).take(2),
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.ExtraBold,
            color = InventoryPanel
        )
    }
}

private fun buildInventoryCollections(inventory: List<GeoCard>, labels: InventoryCollectionLabels): List<InventoryCollection> {
    val collectionSpecs = listOf(
        labels.initials to inventory.groupBy { initials(it.name) },
        labels.place to inventory.groupBy { normalize(placeName(it, labels.withoutPlace)) },
        labels.relation to inventory.groupBy { normalize(relationName(it)) },
        labels.firstLetter to inventory.groupBy { normalize(it.name).firstOrNull()?.toString() ?: "?" },
    )

    return collectionSpecs.flatMap { (label, groups) ->
        groups.entries
            .filter { it.value.size > 1 }
            .map { entry ->
                buildCollection(labels.titleFormat.format(label, entry.key), entry.value)
            }
    }.sortedWith(
        compareByDescending<InventoryCollection> { it.cards.size }
            .thenByDescending { it.score }
            .thenBy { it.title }
    )
}

private fun buildCardMultipliers(collections: List<InventoryCollection>): Map<String, Int> {
    val multipliers = mutableMapOf<String, Int>()
    collections.forEach { collection ->
        collection.cards.forEach { collectionCard ->
            val cardId = collectionCard.card.id
            multipliers[cardId] = (multipliers[cardId] ?: 0) + collectionCard.coefficient
        }
    }
    return multipliers
}

private fun effectiveMultiplier(cardId: String, cardMultipliers: Map<String, Int>): Int {
    return maxOf(1, cardMultipliers[cardId] ?: 0)
}

private fun buildBaseCollection(cards: List<GeoCard>, title: String): InventoryCollection {
    val collectionCards = cards.sortedBy { it.name }.map { card ->
        CollectionCard(
            card = card,
            coefficient = 1,
            score = card.power
        )
    }

    return InventoryCollection(
        title = title,
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

private fun relationName(card: GeoCard): String {
    return card.relationName
        ?.takeIf { it.isNotBlank() }
        ?: card.description.substringBefore(" - ", card.description)
}

private fun placeName(card: GeoCard, withoutPlace: String): String {
    return card.placeName
        ?.takeIf { it.isNotBlank() }
        ?: card.description.substringAfter(" - ", "")
            .ifBlank { withoutPlace }
}

private fun normalize(value: String): String {
    val withoutAccents = Normalizer
        .normalize(value.trim(), Normalizer.Form.NFD)
        .replace(Regex("\\p{Mn}+"), "")

    return withoutAccents.uppercase()
}
