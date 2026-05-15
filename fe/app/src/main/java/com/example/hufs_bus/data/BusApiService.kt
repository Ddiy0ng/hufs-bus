package com.example.hufs_bus.data

import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

interface BusApiService {

    @GET("api/routes")
    suspend fun getRoutes(
        @Query("type") type: String
    ): Response<ApiResponse<List<BusRoute>>>

    @GET("api/routes/{routeId}/schedules")
    suspend fun getSchedules(
        @Path("routeId") routeId: Long,
        @Query("hour") hour: Int? = null
    ): Response<ApiResponse<List<BusSchedule>>>

    @GET("api/buses/{scheduleId}/location")
    suspend fun getBusLocation(
        @Path("scheduleId") scheduleId: Long
    ): Response<ApiResponse<BusLocationInfo>>
}