package com.example.hufs_bus.data

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.PATCH
import retrofit2.http.Path
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Query

interface BusApiService {

    @POST("api/auth/signup")
    suspend fun signup(
        @Body request: SignupRequest
    ): Response<ApiResponse<Unit>>

    @POST("api/auth/login")
    suspend fun login(
        @Body request: LoginRequest
    ): Response<ApiResponse<LoginResponse>>

    @GET("api/inoutcampus")
    suspend fun getRoutes(
        @Query("inOutCampus") inOutCampus: String
    ): Response<ApiResponse<List<BusRouteResponse>>>

    @GET("api/timetable")
    suspend fun getSchedules(
        @Query("inOutCampus") inOutCampus: String? = null,
        @Query("routeId") routeId: Long? = null,
        @Query("startTime") startTime: Int? = null
    ): Response<ApiResponse<List<TimetableResponse>>>

    @GET("api/timetables/{timetableId}/live")
    suspend fun getLiveTimetable(
        @Path("timetableId") timetableId: Long
    ): Response<LiveTimetableResponse>

    @PATCH("api/timetable/{timetableId}/depart")
    suspend fun departTimetable(
        @Path("timetableId") timetableId: Long
    ): Response<ApiResponse<DepartResponse>>

    @POST("api/favorite")
    suspend fun createFavorite(
        @Body request: FavoriteCreateRequest
    ): Response<ApiResponse<Unit>>

    @PUT("api/favorite")
    suspend fun updateFavorite(
        @Body request: FavoriteCreateRequest
    ): Response<ApiResponse<Unit>>

    @GET("api/favorite")
    suspend fun getFavorites(
        @Query("inOutCampus") inOutCampus: String,
        @Query("day") day: String
    ): Response<ApiResponse<List<FavoriteResponse>>>

    @DELETE("api/favorite")
    suspend fun deleteFavorite(
        @Query("favoriteId") favoriteId: Long,
        @Query("day") day: String
    ): Response<ApiResponse<Unit>>

    @POST("api/driver/location")
    suspend fun saveDriverLocation(
        @Body request: DriverLocationRequest
    ): Response<ApiResponse<DriverLocationResponse>>

    @GET("api/driver/location/{busId}")
    suspend fun getDriverLocation(
        @Path("busId") busId: Long
    ): Response<ApiResponse<DriverLocationResponse>>
}
