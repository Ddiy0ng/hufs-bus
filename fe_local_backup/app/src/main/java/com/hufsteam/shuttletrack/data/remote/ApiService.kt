package com.hufsteam.shuttletrack.data.remote

import com.google.gson.JsonElement
import com.hufsteam.shuttletrack.data.remote.dto.BusTagRequest
import com.hufsteam.shuttletrack.data.remote.dto.DriverLocationRequest
import com.hufsteam.shuttletrack.data.remote.dto.FavoriteCreateRequest
import com.hufsteam.shuttletrack.data.remote.dto.LoginRequest
import com.hufsteam.shuttletrack.data.remote.dto.SignupRequest
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

interface ApiService {
    @POST("/api/auth/login")
    suspend fun login(@Body request: LoginRequest): JsonElement

    @POST("/api/auth/signup")
    suspend fun signup(@Body request: SignupRequest): JsonElement

    @GET("/api/timetable")
    suspend fun getTimetable(
        @Query("inOutCampus") inOutCampus: String? = null
    ): JsonElement

    @GET("/api/timetables/{timetableId}/live")
    suspend fun getLiveEta(@Path("timetableId") timetableId: Long): JsonElement

    @GET("/api/buses/{timetableId}/statuses")
    suspend fun getBusStatuses(@Path("timetableId") timetableId: Long): JsonElement

    @GET("/api/buses/{timetableId}/seats")
    suspend fun getBusSeats(@Path("timetableId") timetableId: Long): JsonElement

    @POST("/api/buses/{timetableId}/tags")
    suspend fun postBusTag(
        @Path("timetableId") timetableId: Long,
        @Body request: BusTagRequest
    ): JsonElement

    @POST("/api/driver/location")
    suspend fun postDriverLocation(@Body request: DriverLocationRequest): JsonElement

    @GET("/api/favorites")
    suspend fun getFavorites(): JsonElement

    @GET("/api/favorite")
    suspend fun getFavorite(
        @Query("inOutCampus") inOutCampus: String? = null,
        @Query("day") day: String? = null
    ): JsonElement

    @POST("/api/favorites/{specificTimetableId}")
    suspend fun addFavorite(@Path("specificTimetableId") specificTimetableId: Long): JsonElement

    @POST("/api/favorite")
    suspend fun addFavoriteLegacy(@Body request: FavoriteCreateRequest): JsonElement

    @DELETE("/api/favorites/{specificTimetableId}")
    suspend fun deleteFavorite(@Path("specificTimetableId") specificTimetableId: Long): JsonElement

    @GET("/api/user")
    suspend fun getUser(): JsonElement

    @GET("/api/privacy")
    suspend fun getPrivacy(): JsonElement

    @GET("/api/terms/privacy")
    suspend fun getPrivacyLegacy(): JsonElement

    @GET("/api/service")
    suspend fun getService(): JsonElement

    @GET("/api/terms/service")
    suspend fun getServiceLegacy(): JsonElement
}
