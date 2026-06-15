package com.hufsteam.shuttletrack.data.repository

import com.google.gson.Gson
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.hufsteam.shuttletrack.data.remote.ApiService
import com.hufsteam.shuttletrack.data.remote.RetrofitClient
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

class ShuttleRepository(
    private val apiService: ApiService = RetrofitClient.apiService,
    private val gson: Gson = RetrofitClient.gson
) {
    suspend fun getTimetable(inOutCampus: String? = null): Result<List<TimetableResponse>> = runCatching {
        apiService.getTimetable(inOutCampus)
            .payloadList()
            .mapNotNull { gson.decode<TimetableResponse>(it) }
    }

    suspend fun getLiveEta(timetableId: Long): Result<LiveEtaResponse?> = runCatching {
        apiService.getLiveEta(timetableId).payloadSingleOrListFirst()?.let { gson.decode<LiveEtaResponse>(it) }
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
        val response = runCatching { apiService.getFavorite() }
            .recoverCatching { apiService.getFavorites() }
            .getOrThrow()
        response
            .payloadList()
            .mapNotNull { gson.decode<FavoriteResponse>(it) }
    }

    suspend fun addFavorite(specificTimetableId: Long, days: Set<String> = emptySet()): Result<Unit> = runCatching {
        runCatching {
            apiService.addFavoriteLegacy(
                FavoriteCreateRequest(
                    timetableId = specificTimetableId,
                    days = days.ifEmpty { setOf("MON") }
                )
            )
        }
            .recoverCatching { apiService.addFavorite(specificTimetableId) }
            .getOrThrow()
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
