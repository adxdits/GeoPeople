package com.example.geopeople.data

import com.example.geopeople.location.DistanceUtils
import com.example.geopeople.model.GeoCard
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class CardRepository {
    private val _cards = MutableStateFlow<List<GeoCard>>(emptyList())
    val cards: StateFlow<List<GeoCard>> = _cards.asStateFlow()

    suspend fun loadCardsAround(lat: Double, lon: Double) {
        val places = WikidataService.fetchNearbyPlaces(lat, lon, 20)
        val playablePlaces = places.filter {
            DistanceUtils.haversine(lat, lon, it.latitude, it.longitude) <= 500.0
        }
        _cards.value = if (playablePlaces.isNotEmpty()) places else buildLocalDemoCards(lat, lon)
    }

    fun setCards(cards: List<GeoCard>) {
        _cards.value = cards
    }

    fun getCardsInRange(playerLat: Double, playerLon: Double, rangeMeters: Double): List<GeoCard> {
        return _cards.value.filter {
            DistanceUtils.haversine(playerLat, playerLon, it.latitude, it.longitude) <= rangeMeters
        }
    }

    private fun buildLocalDemoCards(lat: Double, lon: Double): List<GeoCard> {
        val people = listOf(
            "Victor Hugo" to "Collection locale test",
            "Valentin Hauy" to "Collection locale test",
            "Vera Rubin" to "Collection locale test",
            "Voltaire" to "Collection locale test",
            "Alan Turing" to "Enigme locale",
            "Ada Lovelace" to "Enigme locale",
        )
        val offsets = listOf(
            8.0 to 0.0,
            -12.0 to 8.0,
            18.0 to -10.0,
            -65.0 to -10.0,
            85.0 to 20.0,
            -110.0 to 35.0,
        )
        val metersPerDegreeLat = 111_320.0
        val metersPerDegreeLon = 111_320.0 * kotlin.math.cos(Math.toRadians(lat))
        val cellId = "${(lat * 1000).toInt()}-${(lon * 1000).toInt()}"

        return people.mapIndexed { index, person ->
            val offset = offsets[index]
            GeoCard(
                id = "local-demo-$cellId-${index + 1}",
                name = person.first,
                description = person.second,
                latitude = lat + offset.first / metersPerDegreeLat,
                longitude = lon + offset.second / metersPerDegreeLon,
                power = 10 + index
            )
        }
    }
}
