package com.hufsteam.shuttletrack.data.repository

import android.util.Log
import com.google.gson.Gson
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.hufsteam.shuttletrack.data.remote.ApiService
import com.hufsteam.shuttletrack.data.remote.RetrofitClient
import com.hufsteam.shuttletrack.data.remote.TokenStore
import com.hufsteam.shuttletrack.data.remote.dto.BusStatusResponse
import com.hufsteam.shuttletrack.data.remote.dto.BusTagRequest
import com.hufsteam.shuttletrack.data.remote.dto.BusTagResponse
import com.hufsteam.shuttletrack.data.remote.dto.DepartResponse
import com.hufsteam.shuttletrack.data.remote.dto.DriverLocationRequest
import com.hufsteam.shuttletrack.data.remote.dto.DriverLocationResponse
import com.hufsteam.shuttletrack.data.remote.dto.FavoriteCreateRequest
import com.hufsteam.shuttletrack.data.remote.dto.FavoriteResponse
import com.hufsteam.shuttletrack.data.remote.dto.LiveEtaResponse
import com.hufsteam.shuttletrack.data.remote.dto.TermsResponse
import com.hufsteam.shuttletrack.data.remote.dto.TimetableResponse
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import okhttp3.Request
import okhttp3.HttpUrl.Companion.toHttpUrl
import java.io.IOException
import java.util.concurrent.TimeUnit

private const val BUS_TAG_LOG_TAG = "BusTagApi"
private const val LIVE_LOG_TAG = "LiveSseApi"
private const val DRIVER_LOCATION_LOG_TAG = "DriverLocationApi"

class ShuttleRepository(
    private val apiService: ApiService = RetrofitClient.apiService,
    private val gson: Gson = RetrofitClient.gson
) {
    private val favoriteCampuses = listOf("IN_CAMPUS", "OUT_CAMPUS")
    private val favoriteDays = listOf("MON", "TUE", "WED", "THU", "FRI")

    suspend fun getTimetable(inOutCampus: String? = null): Result<List<TimetableResponse>> = runCatching {
        val primary = apiService.getTimetable(inOutCampus).toTimetableResponses()
        if (primary.isNotEmpty() || inOutCampus.isNullOrBlank()) {
            primary
        } else {
            apiService.getTimetable(null)
                .toTimetableResponses()
                .filter { it.matchesCampus(inOutCampus) }
        }
    }

    suspend fun getLiveEta(timetableId: Long): Result<LiveEtaResponse?> = runCatching {
        withContext(Dispatchers.IO) {
            val token = TokenStore.accessToken?.takeIf { it.isNotBlank() }
            val url = "${RetrofitClient.BASE_URL}/api/timetables/$timetableId/live"
                .toHttpUrl()
                .newBuilder()
                .build()
            val request = Request.Builder()
                .url(url)
                .header("Accept", "text/event-stream")
                .apply { token?.let { header("Authorization", "Bearer $it") } }
                .build()
            val liveClient = RetrofitClient.okHttpClient.newBuilder()
                .readTimeout(2, TimeUnit.SECONDS)
                .build()

            liveClient.newCall(request).execute().use { response ->
                Log.i(
                    LIVE_LOG_TAG,
                    "getLiveEta timetableId=$timetableId url=$url hasAuth=${token != null} code=${response.code}"
                )
                if (!response.isSuccessful) {
                    val errorBody = response.body?.string().orEmpty()
                    Log.e(LIVE_LOG_TAG, "getLiveEta failed timetableId=$timetableId code=${response.code} body=$errorBody")
                    throw IOException("HTTP ${response.code}: ${errorBody.ifBlank { "empty error body" }}")
                }

                val source = response.body?.source() ?: return@withContext null
                val dataLines = mutableListOf<String>()
                var eventName: String? = null
                repeat(40) {
                    val line = source.readUtf8Line() ?: return@repeat
                    when {
                        line.startsWith("event:") -> eventName = line.removePrefix("event:").trim()
                        line.startsWith("data:") -> dataLines += line.removePrefix("data:").trim()
                        line.isBlank() && dataLines.isNotEmpty() -> {
                            Log.i(LIVE_LOG_TAG, "getLiveEta event=$eventName timetableId=$timetableId")
                            return@withContext dataLines.toLiveEtaOrNull()
                        }
                    }
                }
                null
            }
        }
    }

    fun subscribeLiveEta(timetableId: Long): Flow<LiveEtaResponse> = flow {
        val token = TokenStore.accessToken?.takeIf { it.isNotBlank() }
        val url = "${RetrofitClient.BASE_URL}/api/timetables/$timetableId/live"
            .toHttpUrl()
            .newBuilder()
            .build()
        val request = Request.Builder()
            .url(url)
            .header("Accept", "text/event-stream")
            .apply { token?.let { header("Authorization", "Bearer $it") } }
            .build()
        val liveClient = RetrofitClient.okHttpClient.newBuilder()
            .readTimeout(0, TimeUnit.SECONDS)
            .build()

        liveClient.newCall(request).execute().use { response ->
            Log.i(
                LIVE_LOG_TAG,
                "subscribeLiveEta timetableId=$timetableId url=$url hasAuth=${token != null} code=${response.code}"
            )
            if (!response.isSuccessful) {
                val errorBody = response.body?.string().orEmpty()
                Log.e(LIVE_LOG_TAG, "subscribeLiveEta failed timetableId=$timetableId code=${response.code} body=$errorBody")
                throw IOException("HTTP ${response.code}: ${errorBody.ifBlank { "empty error body" }}")
            }

            val source = response.body?.source() ?: return@use
            val dataLines = mutableListOf<String>()
            var eventName: String? = null
            while (currentCoroutineContext().isActive) {
                val line = source.readUtf8Line() ?: break
                when {
                    line.startsWith("event:") -> eventName = line.removePrefix("event:").trim()
                    line.startsWith("data:") -> dataLines += line.removePrefix("data:").trim()
                    line.isBlank() && dataLines.isNotEmpty() -> {
                        Log.i(LIVE_LOG_TAG, "subscribeLiveEta event=$eventName timetableId=$timetableId")
                        dataLines.toLiveEtaOrNull()?.let { emit(it) }
                        dataLines.clear()
                        eventName = null
                    }
                }
            }
        }
    }.flowOn(Dispatchers.IO)

    suspend fun getBusStatuses(timetableId: Long): Result<BusStatusResponse?> = runCatching {
        val response = apiService.getBusSeats(timetableId)
        response.payloadSingleOrListFirst()?.let { gson.decode<BusStatusResponse>(it) }
    }

    suspend fun departTimetable(timetableId: Long): Result<DepartResponse?> = runCatching {
        apiService.departTimetable(timetableId)
            .payloadSingleOrListFirst()
            ?.let { gson.decode<DepartResponse>(it) }
    }

    suspend fun postBusTag(timetableId: Long, request: BusTagRequest): Result<BusTagResponse?> = runCatching {
        val endpoint = "/api/buses/$timetableId/tags"
        val requestJson = gson.toJson(request)
        Log.i(BUS_TAG_LOG_TAG, "request endpoint=$endpoint timetableId=$timetableId body=$requestJson")

        val response = apiService.postBusTagRaw(timetableId, request)
        val responseCode = response.code()
        val responseBody = response.body()
        val errorBody = response.errorBody()?.string().orEmpty()
        val responseText = responseBody?.toString() ?: errorBody

        if (!response.isSuccessful) {
            Log.e(
                BUS_TAG_LOG_TAG,
                "failed endpoint=$endpoint timetableId=$timetableId body=$requestJson code=$responseCode response=$responseText"
            )
            throw IOException("HTTP $responseCode: ${responseText.ifBlank { "empty error body" }}")
        }

        val decoded = responseBody
            ?.payloadSingleOrListFirst()
            ?.let { gson.decode<BusTagResponse>(it) }
        Log.i(
            BUS_TAG_LOG_TAG,
            "success endpoint=$endpoint timetableId=$timetableId body=$requestJson code=$responseCode " +
                "busId=${decoded?.busId} currentSeats=${decoded?.currentSeats} totalSeats=${decoded?.totalSeats} " +
                "remainingSeats=${decoded?.remainingSeats} tagType=${decoded?.tagType}"
        )
        decoded
    }

    suspend fun postDriverLocation(
        busId: Long,
        latitude: Double,
        longitude: Double
    ): Result<DriverLocationResponse?> = runCatching {
        Log.i(
            DRIVER_LOCATION_LOG_TAG,
            "postDriverLocation busId=$busId latitude=$latitude longitude=$longitude"
        )
        apiService.postDriverLocation(
            DriverLocationRequest(
                busId = busId,
                latitude = latitude,
                longitude = longitude
            )
        ).payloadSingleOrListFirst()?.let { gson.decode<DriverLocationResponse>(it) }
    }

    suspend fun getDriverLocation(busId: Long): Result<DriverLocationResponse?> = runCatching {
        Log.i(DRIVER_LOCATION_LOG_TAG, "getDriverLocation busId=$busId endpoint=/api/driver/location/$busId")
        apiService.getDriverLocation(busId)
            .payloadSingleOrListFirst()
            ?.let { gson.decode<DriverLocationResponse>(it) }
    }

    private fun List<String>.toLiveEtaOrNull(): LiveEtaResponse? {
        return runCatching {
            val payload = gson.fromJson(joinToString(""), JsonElement::class.java)
            payload.payloadSingleOrListFirst()?.let { gson.decode<LiveEtaResponse>(it) }
        }.getOrNull()
    }

    suspend fun getFavorites(): Result<List<FavoriteResponse>> = runCatching {
        val favorites = mutableListOf<FavoriteResponse>()

        favoriteCampuses.forEach { campus ->
            favoriteDays.forEach { day ->
                runCatching { apiService.getFavorite(inOutCampus = campus, day = day) }
                    .onSuccess { response ->
                        response.payloadList()
                            .map { it.withFavoriteMetadata(campus, day) }
                            .mapNotNullTo(favorites) { gson.decode<FavoriteResponse>(it) }
                    }
            }
        }

        favorites.distinctBy {
            listOfNotNull(
                it.timetableId ?: it.specificTimetableId ?: it.id,
                it.day ?: it.dayOfWeek,
                it.inOutCampus,
                it.departAt ?: it.departureTime ?: it.time,
                it.route ?: it.routeName
            ).joinToString(":")
        }
    }

    suspend fun saveFavorite(timetableId: Long, days: Set<String>, isExisting: Boolean): Result<Unit> = runCatching {
        val request = FavoriteCreateRequest(
            timetableId = timetableId,
            days = days
        )
        when {
            isExisting -> apiService.updateFavorite(request)
            days.isNotEmpty() -> apiService.addFavoriteLegacy(request)
            else -> return@runCatching
        }
        Unit
    }

    suspend fun addFavorite(timetableId: Long, days: Set<String> = emptySet()): Result<Unit> = runCatching {
        apiService.addFavoriteLegacy(
            FavoriteCreateRequest(
                timetableId = timetableId,
                days = days
            )
        )
        Unit
    }

    suspend fun deleteFavorite(favoriteId: Long, day: String = "MON"): Result<Unit> = runCatching {
        apiService.deleteFavoriteLegacy(favoriteId, day)
        Unit
    }

    suspend fun getPrivacy(): Result<TermsResponse?> = runCatching {
        val response = runCatching { apiService.getPrivacyLegacy() }
            .recoverCatching { apiService.getPrivacy() }
            .getOrThrow()
        response.payloadSingleOrListFirst()?.let { gson.decode<TermsResponse>(it) }
    }

    suspend fun getService(): Result<TermsResponse?> = runCatching {
        val response = runCatching { apiService.getServiceLegacy() }
            .recoverCatching { apiService.getService() }
            .getOrThrow()
        response.payloadSingleOrListFirst()?.let { gson.decode<TermsResponse>(it) }
    }
}

private inline fun <reified T> Gson.decode(element: JsonElement): T? {
    return runCatching { fromJson(element, T::class.java) }.getOrNull()
}

private fun JsonElement.toTimetableResponses(): List<TimetableResponse> {
    val gson = RetrofitClient.gson
    return payloadList()
        .mapNotNull { gson.decode<TimetableResponse>(it) }
}

private fun TimetableResponse.matchesCampus(inOutCampus: String): Boolean {
    val joined = listOfNotNull(
        this.inOutCampus,
        routeName,
        route,
        startStop,
        endStop,
        routeList?.joinToString(" ") { it.asStringOrObjectName() }
    ).joinToString(" ").uppercase()

    return when (inOutCampus.uppercase()) {
        "IN_CAMPUS" -> joined.contains("IN_CAMPUS") ||
            joined.contains("교내") ||
            joined.contains("지석묘") ||
            joined.contains("인문경상관")
        "OUT_CAMPUS" -> joined.contains("OUT_CAMPUS") ||
            joined.contains("교외") ||
            joined.contains("판교") ||
            joined.contains("경기광주") ||
            joined.contains("외대")
        else -> true
    }
}

private fun JsonElement.asStringOrObjectName(): String {
    return when {
        isJsonPrimitive -> asString
        isJsonObject -> asJsonObject.get("name")?.asString
            ?: asJsonObject.get("stopName")?.asString
            ?: asJsonObject.get("busStopName")?.asString
            ?: toString()
        else -> toString()
    }
}

private fun JsonElement.withFavoriteMetadata(campus: String, day: String): JsonElement {
    if (!isJsonObject) return this
    val obj = deepCopy().asJsonObject
    if (!obj.has("inOutCampus") || obj.get("inOutCampus").isJsonNull) {
        obj.addProperty("inOutCampus", campus)
    }
    if (!obj.has("day") || obj.get("day").isJsonNull) {
        obj.addProperty("day", day)
    }
    return obj
}

private fun JsonElement.payloadSingleOrListFirst(): JsonElement? {
    val payload = payloadElement()
    return when {
        payload.isJsonArray -> payload.asJsonArray.firstOrNull()
        payload.isJsonObject -> payload
        else -> null
    }
}

private fun JsonElement.payloadList(): List<JsonElement> {
    val payload = payloadElement()
    if (payload.isJsonArray) return payload.asJsonArray.toList()
    if (!payload.isJsonObject) return emptyList()

    val obj = payload.asJsonObject
    if (obj.looksLikePayloadItem()) return listOf(payload)

    listOf("items", "schedules", "timetables", "content", "favorites", "data", "IN_CAMPUS", "OUT_CAMPUS", "inCampus", "outCampus").forEach { key ->
        val value = obj.get(key)
        if (value != null && value.isJsonArray) return value.asJsonArray.toList()
        if (value != null && value.isJsonObject) {
            val nested = value.payloadList()
            if (nested.isNotEmpty()) return nested
        }
    }

    val nestedLists = obj.entrySet()
        .flatMap { (_, value) ->
            when {
                value.isJsonArray -> value.asJsonArray.toList()
                value.isJsonObject -> value.payloadList()
                else -> emptyList()
            }
        }
    return nestedLists.ifEmpty { listOf(payload) }
}

private fun JsonElement.payloadElement(): JsonElement {
    if (!isJsonObject) return this
    val root = asJsonObject
    listOf("data", "result", "item", "body", "content").forEach { key ->
        val value = root.get(key)
        if (value != null && !value.isJsonNull) return value
    }
    return root
}

private fun JsonObject.looksLikePayloadItem(): Boolean {
    val itemKeys = setOf(
        "timetableId",
        "specificTimetableId",
        "favoriteId",
        "busId",
        "routeId",
        "departAt",
        "departureTime",
        "routeList",
        "startStop",
        "endStop",
        "currentSeats",
        "title",
        "content"
    )
    return itemKeys.any { has(it) && !get(it).isJsonNull }
}
