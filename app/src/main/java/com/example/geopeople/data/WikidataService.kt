package com.example.geopeople.data

import com.example.geopeople.model.GeoCard
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.net.URLEncoder
import java.util.concurrent.TimeUnit

object WikidataService {
    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()

    suspend fun fetchNearbyPlaces(lat: Double, lon: Double, radiusKm: Int = 20): List<GeoCard> =
        withContext(Dispatchers.IO) {
            try {
                val query = """
                    SELECT ?place ?placeLabel ?location ?image WHERE {
                      SERVICE wikibase:around {
                        ?place wdt:P625 ?location .
                        bd:serviceParam wikibase:center "Point($lon $lat)"^^geo:wktLiteral .
                        bd:serviceParam wikibase:radius "$radiusKm" .
                      }
                      OPTIONAL { ?place wdt:P18 ?image }
                      SERVICE wikibase:label { bd:serviceParam wikibase:language "fr,en" }
                    }
                    LIMIT 50
                """.trimIndent()

                val url = "https://query.wikidata.org/sparql?query=" +
                        URLEncoder.encode(query, "UTF-8") + "&format=json"
                val request = Request.Builder()
                    .url(url)
                    .header("User-Agent", "GeoPeople/1.0")
                    .header("Accept", "application/sparql-results+json")
                    .build()

                val response = client.newCall(request).execute()
                val body = response.body?.string() ?: return@withContext emptyList()
                parseResponse(body)
            } catch (e: Exception) {
                e.printStackTrace()
                emptyList()
            }
        }

    private fun parseResponse(json: String): List<GeoCard> {
        val root = JSONObject(json)
        val bindings = root.getJSONObject("results").getJSONArray("bindings")
        val cards = mutableListOf<GeoCard>()

        for (i in 0 until bindings.length()) {
            val item = bindings.getJSONObject(i)
            val entityUri = item.getJSONObject("place").getString("value")
            val id = entityUri.substringAfterLast("/")
            val name = item.getJSONObject("placeLabel").getString("value")
            val wkt = item.getJSONObject("location").getString("value")
            val imageUrl = if (item.has("image")) item.getJSONObject("image").getString("value") else null
            val coords = parseWktPoint(wkt) ?: continue

            cards.add(
                GeoCard(
                    id = id,
                    name = name,
                    description = "Lieu Wikidata",
                    latitude = coords.first,
                    longitude = coords.second,
                    imageUrl = imageUrl
                )
            )
        }
        return cards
    }

    private fun parseWktPoint(wkt: String): Pair<Double, Double>? {
        val regex = Regex("""Point\((-?[\d.]+)\s+(-?[\d.]+)\)""")
        val match = regex.find(wkt) ?: return null
        val lon = match.groupValues[1].toDoubleOrNull() ?: return null
        val lat = match.groupValues[2].toDoubleOrNull() ?: return null
        return Pair(lat, lon)
    }
}
