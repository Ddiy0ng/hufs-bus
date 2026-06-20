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
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.http.Query
import retrofit2.Response

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
    suspend fun getLiveEta(
        @Path("timetableId") timetableId: Long
    ): JsonElement

    @GET("/api/buses/{timetableId}/seats")
    suspend fun getBusSeats(@Path("timetableId") timetableId: Long): JsonElement

    @PATCH("/api/timetable/{timetableId}/depart")
    suspend fun departTimetable(@Path("timetableId") timetableId: Long): JsonElement

    @PATCH("/api/timetable/{timetableId}/finish")
    suspend fun finishTimetable(
        @Path("timetableId") timetableId: Long
    ): JsonElement

    @POST("/api/buses/{timetableId}/tags")
    suspend fun postBusTag(
        @Path("timetableId") timetableId: Long,
        @Body request: BusTagRequest
    ): JsonElement

    @POST("/api/buses/{timetableId}/tags")
    suspend fun postBusTagRaw(
        @Path("timetableId") timetableId: Long,
        @Body request: BusTagRequest
    ): Response<JsonElement>

    @POST("/api/driver/location")
    suspend fun postDriverLocation(@Body request: DriverLocationRequest): JsonElement

    @GET("/api/driver/location/{busId}")
    suspend fun getDriverLocation(@Path("busId") busId: Long): JsonElement

    @GET("/api/favorite")
    suspend fun getFavorite(
        @Query("inOutCampus") inOutCampus: String? = null,
        @Query("day") day: String? = null
    ): JsonElement

    @POST("/api/favorite")
    suspend fun addFavoriteLegacy(@Body request: FavoriteCreateRequest): JsonElement

    @PUT("/api/favorite")
    suspend fun updateFavorite(@Body request: FavoriteCreateRequest): JsonElement

    @DELETE("/api/favorite")
    suspend fun deleteFavoriteLegacy(
        @Query("favoriteId") favoriteId: Long,
        @Query("day") day: String
    ): JsonElement

    @DELETE("/api/users/me")
    suspend fun withdrawUser(): JsonElement

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
