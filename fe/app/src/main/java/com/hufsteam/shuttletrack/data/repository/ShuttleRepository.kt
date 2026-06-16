package com.hufsteam.shuttletrack.data.repository

import com.google.gson.Gson
import com.google.gson.JsonElement
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
import kotlinx.coroutines.withContext
import okhttp3.Request
import okhttp3.HttpUrl.Companion.toHttpUrl
import java.io.IOException
import java.util.concurrent.TimeUnit

class ShuttleRepository(
    private val apiService: ApiService = RetrofitClient.apiService,
    private val gson: Gson = RetrofitClient.gson
) {
    private val favoriteCampuses = listOf("IN_CAMPUS", "OUT_CAMPUS")
    private val favoriteDays = listOf("MON", "TUE", "WED", "THU", "FRI")

    suspend fun getTimetable(inOutCampus: String? = null): Result<List<TimetableResponse>> = runCatching {
        apiService.getTimetable(inOutCampus)
            .payloadList()
            .mapNotNull { gson.decode<TimetableResponse>(it) }
    }

    suspend fun getLiveEta(timetableId: Long): Result<LiveEtaResponse?> = runCatching {
        withContext(Dispatchers.IO) {
            val token = TokenStore.accessToken?.takeIf { it.isNotBlank() }
            val url = "${RetrofitClient.BASE_URL}/api/timetables/$timetableId/live"
                .toHttpUrl()
                .newBuilder()
                .apply { token?.let { addQueryParameter("token", it) } }
                .build()
            val request = Request.Builder()
                .url(url)
                .header("Accept", "text/event-stream")
                .build()
            val liveClient = RetrofitClient.okHttpClient.newBuilder()
                .readTimeout(2, TimeUnit.SECONDS)
                .build()

            liveClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    throw IOException("HTTP ${response.code}")
                }

                val source = response.body?.source() ?: return@withContext null
                val dataLines = mutableListOf<String>()
                repeat(40) {
                    val line = source.readUtf8Line() ?: return@repeat
                    if (line.startsWith("data:")) {
                        dataLines += line.removePrefix("data:").trim()
                    }
                    if (line.isBlank() && dataLines.isNotEmpty()) {
                        val payload = gson.fromJson(dataLines.joinToString(""), JsonElement::class.java)
                        return@withContext payload.payloadSingleOrListFirst()?.let { gson.decode<LiveEtaResponse>(it) }
                    }
                }
                null
            }
        }
    }

    suspend fun getBusStatuses(timetableId: Long): Result<BusStatusResponse?> = runCatching {
        val response = runCatching { apiService.getBusStatuses(timetableId) }
            .recoverCatching { apiService.getBusSeats(timetableId) }
            .getOrThrow()
        response.payloadSingleOrListFirst()?.let { gson.decode<BusStatusResponse>(it) }
    }

    suspend fun departTimetable(timetableId: Long): Result<DepartResponse?> = runCatching {
        apiService.departTimetable(timetableId)
            .payloadSingleOrListFirst()
            ?.let { gson.decode<DepartResponse>(it) }
    }

    suspend fun postBusTag(timetableId: Long, request: BusTagRequest): Result<BusTagResponse?> = runCatching {
        apiService.postBusTag(timetableId, request).payloadSingleOrListFirst()?.let { gson.decode<BusTagResponse>(it) }
    }

    suspend fun postDriverLocation(
        busId: Long,
        latitude: Double,
        longitude: Double
    ): Result<DriverLocationResponse?> = runCatching {
        apiService.postDriverLocation(
            DriverLocationRequest(
                busId = busId,
                latitude = latitude,
                longitude = longitude
            )
        ).payloadSingleOrListFirst()?.let { gson.decode<DriverLocationResponse>(it) }
    }

    suspend fun getFavorites(): Result<List<FavoriteResponse>> = runCatching {
        val favorites = mutableListOf<FavoriteResponse>()
        var requestCount = 0
        var failureCount = 0

        favoriteCampuses.forEach { campus ->
            favoriteDays.forEach { day ->
                requestCount += 1
                runCatching { apiService.getFavorite(inOutCampus = campus, day = day) }
                    .onSuccess { response ->
                        response.payloadList()
                            .map { it.withFavoriteMetadata(campus, day) }
                            .mapNotNullTo(favorites) { gson.decode<FavoriteResponse>(it) }
                    }
                    .onFailure { failureCount += 1 }
            }
        }

        val resolvedFavorites = if (favorites.isEmpty() && requestCount == failureCount) {
            apiService.getFavorites()
                .payloadList()
                .mapNotNull { gson.decode<FavoriteResponse>(it) }
        } else {
            favorites
        }

        resolvedFavorites.distinctBy {
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
        if (isExisting) {
            runCatching { apiService.updateFavorite(request) }
                .recoverCatching { apiService.addFavoriteLegacy(request) }
                .getOrThrow()
        } else {
            runCatching { apiService.addFavoriteLegacy(request) }
                .recoverCatching { apiService.updateFavorite(request) }
                .getOrThrow()
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

    suspend fun deleteFavorite(specificTimetableId: Long): Result<Unit> = runCatching {
        runCatching { apiService.deleteFavoriteLegacy(specificTimetableId, "MON") }
            .recoverCatching { apiService.deleteFavorite(specificTimetableId) }
            .getOrThrow()
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
    listOf("items", "schedules", "timetables", "content", "favorites", "data").forEach { key ->
        val value = obj.get(key)
        if (value != null && value.isJsonArray) return value.asJsonArray.toList()
    }
    return listOf(payload)
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
