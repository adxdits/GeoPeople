package com.example.geopeople.data

import com.example.geopeople.model.GeoCard
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

object ApiService {
    // For emulator use 10.0.2.2, for real device use your machine's IP
    private const val BASE_URL = "http://10.0.2.2:3000/api"
    private val JSON_MEDIA = "application/json; charset=utf-8".toMediaType()

    private val client = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .build()

    // --- Player endpoints ---

    suspend fun registerPlayer(name: String): PlayerResponse? = withContext(Dispatchers.IO) {
        try {
            val body = JSONObject().put("name", name).toString()
                .toRequestBody(JSON_MEDIA)
            val request = Request.Builder()
                .url("$BASE_URL/players/register")
                .post(body)
                .build()
            val response = client.newCall(request).execute()
            val json = response.body?.string() ?: return@withContext null
            parsePlayerResponse(JSONObject(json))
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    suspend fun getPlayer(playerId: String): PlayerResponse? = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder()
                .url("$BASE_URL/players/$playerId")
                .get()
                .build()
            val response = client.newCall(request).execute()
            val json = response.body?.string() ?: return@withContext null
            parsePlayerResponse(JSONObject(json))
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    suspend fun updatePlayerLocation(playerId: String, lat: Double, lon: Double): Boolean =
        withContext(Dispatchers.IO) {
            try {
                val body = JSONObject()
                    .put("latitude", lat)
                    .put("longitude", lon)
                    .toString().toRequestBody(JSON_MEDIA)
                val request = Request.Builder()
                    .url("$BASE_URL/players/$playerId/location")
                    .put(body)
                    .build()
                val response = client.newCall(request).execute()
                response.isSuccessful
            } catch (e: Exception) {
                e.printStackTrace()
                false
            }
        }

    // --- Cards endpoints ---

    suspend fun getNearbyCards(lat: Double, lon: Double, radiusKm: Int = 20): List<GeoCard> =
        withContext(Dispatchers.IO) {
            try {
                val request = Request.Builder()
                    .url("$BASE_URL/cards/nearby?lat=$lat&lon=$lon&radius=$radiusKm")
                    .get()
                    .build()
                val response = client.newCall(request).execute()
                val json = response.body?.string() ?: return@withContext emptyList()
                parseCardsArray(JSONArray(json))
            } catch (e: Exception) {
                e.printStackTrace()
                emptyList()
            }
        }

    // --- Capture endpoints ---

    suspend fun captureCard(
        playerId: String,
        cardId: String,
        lat: Double,
        lon: Double,
        miniGameSuccess: Boolean
    ): CaptureResponse = withContext(Dispatchers.IO) {
        try {
            val body = JSONObject()
                .put("playerId", playerId)
                .put("cardId", cardId)
                .put("latitude", lat)
                .put("longitude", lon)
                .put("miniGameSuccess", miniGameSuccess)
                .toString().toRequestBody(JSON_MEDIA)
            val request = Request.Builder()
                .url("$BASE_URL/captures")
                .post(body)
                .build()
            val response = client.newCall(request).execute()
            val json = response.body?.string() ?: return@withContext CaptureResponse(false, "Erreur réseau")
            val obj = JSONObject(json)
            CaptureResponse(
                success = obj.optBoolean("success", false),
                message = obj.optString("message", "")
            )
        } catch (e: Exception) {
            e.printStackTrace()
            CaptureResponse(false, "Erreur réseau: ${e.message}")
        }
    }

    suspend fun getPlayerCaptures(playerId: String): List<String> = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder()
                .url("$BASE_URL/captures/$playerId")
                .get()
                .build()
            val response = client.newCall(request).execute()
            val json = response.body?.string() ?: return@withContext emptyList()
            val arr = JSONArray(json)
            val ids = mutableListOf<String>()
            for (i in 0 until arr.length()) {
                ids.add(arr.getJSONObject(i).getString("cardId"))
            }
            ids
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }

    // --- Parsers ---

    private fun parsePlayerResponse(json: JSONObject): PlayerResponse {
        return PlayerResponse(
            id = json.getString("id"),
            name = json.getString("name"),
            latitude = json.optDouble("latitude", 0.0),
            longitude = json.optDouble("longitude", 0.0),
            score = json.optInt("score", 0),
            inventory = parseStringArray(json.optJSONArray("inventory"))
        )
    }

    private fun parseCardsArray(arr: JSONArray): List<GeoCard> {
        val cards = mutableListOf<GeoCard>()
        for (i in 0 until arr.length()) {
            val obj = arr.getJSONObject(i)
            cards.add(
                GeoCard(
                    id = obj.getString("id"),
                    name = obj.optString("personName", "Inconnu"),
                    description = "${obj.optString("relationName", "")} - ${obj.optString("placeName", "")}",
                    latitude = obj.getDouble("latitude"),
                    longitude = obj.getDouble("longitude"),
                    power = obj.optInt("power", 1)
                )
            )
        }
        return cards
    }

    private fun parseStringArray(arr: JSONArray?): List<String> {
        if (arr == null) return emptyList()
        val list = mutableListOf<String>()
        for (i in 0 until arr.length()) {
            list.add(arr.getString(i))
        }
        return list
    }
}

data class PlayerResponse(
    val id: String,
    val name: String,
    val latitude: Double,
    val longitude: Double,
    val score: Int,
    val inventory: List<String>
)

data class CaptureResponse(
    val success: Boolean,
    val message: String
)
