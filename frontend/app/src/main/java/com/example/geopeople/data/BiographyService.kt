package com.example.geopeople.data

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.net.URLDecoder
import java.net.URLEncoder
import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale
import java.util.concurrent.TimeUnit

data class BiographyDetails(
    val wikidataId: String?,
    val displayName: String,
    val birthDate: String?,
    val birthPlace: String?,
    val deathDate: String?,
    val deathPlace: String?,
    val occupation: String?,
    val summary: String?,
    val imageUrl: String?,
    val wikidataUrl: String?,
    val wikipediaUrl: String?
)

object BiographyService {
    private const val TAG = "GeoPeopleBio"

    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()

    suspend fun fetchBiography(name: String): BiographyDetails = withContext(Dispatchers.IO) {
        val fallback = BiographyDetails(
            wikidataId = null,
            displayName = name,
            birthDate = null,
            birthPlace = null,
            deathDate = null,
            deathPlace = null,
            occupation = null,
            summary = null,
            imageUrl = null,
            wikidataUrl = "https://www.wikidata.org/w/index.php?search=${encode(name)}",
            wikipediaUrl = "https://fr.wikipedia.org/w/index.php?search=${encode(name)}"
        )

        try {
            val wikidata = fetchWikidataBiography(name) ?: return@withContext fallback
            val wikipedia = wikidata.wikipediaUrl?.let { fetchWikipediaSummary(it) }

            wikidata.copy(
                summary = wikipedia?.summary ?: wikidata.summary,
                imageUrl = wikipedia?.imageUrl ?: wikidata.imageUrl,
                wikipediaUrl = wikipedia?.pageUrl ?: wikidata.wikipediaUrl
            )
        } catch (exception: Exception) {
            Log.e(TAG, "Biography fetch failed for name=$name", exception)
            fallback
        }
    }

    private fun fetchWikidataBiography(name: String): BiographyDetails? {
        val query = """
            SELECT ?person ?personLabel ?birthDate ?deathDate ?birthPlaceLabel ?deathPlaceLabel
                   ?occupationLabel ?image ?article WHERE {
              SERVICE wikibase:mwapi {
                bd:serviceParam wikibase:endpoint "www.wikidata.org" ;
                                wikibase:api "EntitySearch" ;
                                mwapi:search "$name" ;
                                mwapi:language "fr" .
                ?person wikibase:apiOutputItem mwapi:item .
              }
              ?person wdt:P31 wd:Q5 .
              OPTIONAL { ?person wdt:P569 ?birthDate . }
              OPTIONAL { ?person wdt:P570 ?deathDate . }
              OPTIONAL { ?person wdt:P19 ?birthPlace . }
              OPTIONAL { ?person wdt:P20 ?deathPlace . }
              OPTIONAL { ?person wdt:P106 ?occupation . }
              OPTIONAL { ?person wdt:P18 ?image . }
              OPTIONAL {
                ?article schema:about ?person ;
                         schema:isPartOf <https://fr.wikipedia.org/> .
              }
              SERVICE wikibase:label { bd:serviceParam wikibase:language "fr,en" . }
            }
            LIMIT 1
        """.trimIndent()

        val url = "https://query.wikidata.org/sparql?query=${encode(query)}&format=json"
        val request = Request.Builder()
            .url(url)
            .header("User-Agent", "GeoPeople/1.0")
            .header("Accept", "application/sparql-results+json")
            .build()

        val response = client.newCall(request).execute()
        val body = response.body?.string() ?: return null
        if (!response.isSuccessful) {
            Log.w(TAG, "Wikidata status=${response.code} body=$body")
            return null
        }

        val bindings = JSONObject(body)
            .getJSONObject("results")
            .getJSONArray("bindings")
        if (bindings.length() == 0) return null

        val item = bindings.getJSONObject(0)
        val personUri = item.optBinding("person") ?: return null
        val wikidataId = personUri.substringAfterLast("/")

        return BiographyDetails(
            wikidataId = wikidataId,
            displayName = item.optBinding("personLabel") ?: name,
            birthDate = item.optBinding("birthDate")?.let(::formatWikidataDate),
            birthPlace = item.optBinding("birthPlaceLabel"),
            deathDate = item.optBinding("deathDate")?.let(::formatWikidataDate),
            deathPlace = item.optBinding("deathPlaceLabel"),
            occupation = item.optBinding("occupationLabel"),
            summary = null,
            imageUrl = item.optBinding("image"),
            wikidataUrl = "https://www.wikidata.org/wiki/$wikidataId",
            wikipediaUrl = item.optBinding("article")
        )
    }

    private fun fetchWikipediaSummary(articleUrl: String): WikipediaSummary? {
        val title = articleUrl.substringAfterLast("/wiki/", missingDelimiterValue = "")
        if (title.isBlank()) return null

        val decodedTitle = URLDecoder.decode(title, "UTF-8")
        val url = "https://fr.wikipedia.org/api/rest_v1/page/summary/${encodePath(decodedTitle)}"
        val request = Request.Builder()
            .url(url)
            .header("User-Agent", "GeoPeople/1.0")
            .build()

        val response = client.newCall(request).execute()
        val body = response.body?.string() ?: return null
        if (!response.isSuccessful) {
            Log.w(TAG, "Wikipedia status=${response.code} body=$body")
            return null
        }

        val json = JSONObject(body)
        val contentUrls = json.optJSONObject("content_urls")
            ?.optJSONObject("desktop")
            ?.optString("page")

        return WikipediaSummary(
            summary = json.optString("extract").takeIf { it.isNotBlank() },
            imageUrl = json.optJSONObject("thumbnail")?.optString("source")?.takeIf { it.isNotBlank() },
            pageUrl = contentUrls ?: articleUrl
        )
    }

    private fun JSONObject.optBinding(key: String): String? {
        if (!has(key)) return null
        return optJSONObject(key)?.optString("value")?.takeIf { it.isNotBlank() }
    }

    private fun formatWikidataDate(value: String): String {
        return runCatching {
            val dateTime = OffsetDateTime.parse(value)
            dateTime.format(DateTimeFormatter.ofPattern("d MMMM yyyy", Locale.FRANCE))
        }.getOrElse {
            value.substringBefore("T").removePrefix("+")
        }
    }

    private fun encode(value: String): String = URLEncoder.encode(value, "UTF-8")

    private fun encodePath(value: String): String = value
        .split("/")
        .joinToString("/") { encode(it).replace("+", "%20") }

    private data class WikipediaSummary(
        val summary: String?,
        val imageUrl: String?,
        val pageUrl: String?
    )
}
