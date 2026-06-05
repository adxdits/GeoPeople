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
            "Alan Turing" to "Carte locale de secours",
            "Simone Veil" to "Carte locale de secours",
            "Leonard de Vinci" to "Carte locale de secours",
            "Frida Kahlo" to "Carte locale de secours",
        )
        val offsets = listOf(
            80.0 to 20.0,
            -90.0 to 70.0,
            140.0 to -110.0,
            -160.0 to -60.0,
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
