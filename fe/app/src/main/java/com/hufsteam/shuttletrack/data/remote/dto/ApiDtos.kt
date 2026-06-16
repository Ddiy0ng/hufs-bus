package com.hufsteam.shuttletrack.data.remote.dto

import com.google.gson.JsonElement

data class LoginRequest(
    val email: String,
    val password: String
)

data class SignupRequest(
    val email: String,
    val password: String,
    val privacyTermAgree: Boolean,
    val serviceTermAgree: Boolean
)

data class AuthResponse(
    val accessToken: String? = null,
    val token: String? = null,
    val role: String? = null,
    val userRole: String? = null,
    val email: String? = null,
    val user: UserResponse? = null
)

data class UserResponse(
    val id: Long? = null,
    val userId: Long? = null,
    val email: String? = null,
    val name: String? = null,
    val role: String? = null,
    val userRole: String? = null
)

data class TimetableResponse(
    val timetableId: Long? = null,
    val specificTimetableId: Long? = null,
    val id: Long? = null,
    val routeId: Long? = null,
    val routeName: String? = null,
    val route: String? = null,
    val startStop: String? = null,
    val endStop: String? = null,
    val departureTime: String? = null,
    val departAt: String? = null,
    val time: String? = null,
    val plannedDeparture: String? = null,
    val inOutCampus: String? = null,
    val remainingSeats: Int? = null,
    val availableSeats: Int? = null,
    val seatLeft: Int? = null,
    val currentLocation: String? = null,
    val currentStop: String? = null,
    val routeList: List<JsonElement>? = null,
    val stops: List<JsonElement>? = null
)

data class RouteStopResponse(
    val name: String? = null,
    val stopName: String? = null,
    val latitude: Double? = null,
    val lat: Double? = null,
    val stopLatitude: Double? = null,
    val stopLat: Double? = null,
    val longitude: Double? = null,
    val lng: Double? = null,
    val stopLongitude: Double? = null,
    val stopLng: Double? = null
)

data class LiveEtaResponse(
    val timetableId: Long? = null,
    val plannedDepartureTime: String? = null,
    val actualDepartureTime: String? = null,
    val currentSeats: Int? = null,
    val status: String? = null,
    val currentLocation: LiveLocationResponse? = null,
    val etaText: String? = null,
    val estimatedMinutes: Int? = null,
    val etaMinutes: Int? = null,
    val currentStopIndex: Int? = null,
    val busStopIndex: Int? = null,
    val currentIndex: Int? = null,
    val busProgressIndex: Float? = null,
    val currentProgressIndex: Float? = null,
    val routeProgressIndex: Float? = null,
    val progressIndex: Float? = null,
    val busLatitude: Double? = null,
    val busLat: Double? = null,
    val vehicleLatitude: Double? = null,
    val latitude: Double? = null,
    val lat: Double? = null,
    val busLongitude: Double? = null,
    val busLng: Double? = null,
    val vehicleLongitude: Double? = null,
    val longitude: Double? = null,
    val lng: Double? = null,
    val actualDeparture: String? = null,
    val actualTime: String? = null,
    val currentStop: String? = null,
    val arrivalText: String? = null,
    val arrivalInfo: String? = null,
    val stops: List<JsonElement>? = null,
    val stopNames: List<JsonElement>? = null,
    val routeList: List<JsonElement>? = null
)

data class LiveLocationResponse(
    val latitude: Double? = null,
    val longitude: Double? = null
)

data class BusStatusResponse(
    val remainingSeats: Int? = null,
    val availableSeats: Int? = null,
    val currentSeats: Int? = null,
    val currentPassengers: Int? = null,
    val passengerCount: Int? = null,
    val totalSeats: Int? = null,
    val status: String? = null,
    val busId: Long? = null
)

data class BusTagRequest(
    val type: String
)

data class FavoriteCreateRequest(
    val timetableId: Long,
    val specificTimetableId: Long = timetableId,
    val days: Set<String>
)

data class BusTagResponse(
    val busId: Long? = null,
    val totalSeats: Int? = null,
    val currentSeats: Int? = null,
    val tagType: String? = null,
    val success: Boolean? = null,
    val message: String? = null,
    val remainingSeats: Int? = null
)

data class DepartResponse(
    val timetableId: Long? = null,
    val actualDepartureTime: String? = null,
    val busStatus: String? = null
)

data class DriverLocationRequest(
    val busId: Long,
    val latitude: Double,
    val longitude: Double
)

data class DriverLocationResponse(
    val busId: Long? = null,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val recordedAt: String? = null
)

data class FavoriteResponse(
    val favoriteId: Long? = null,
    val specificTimetableId: Long? = null,
    val timetableId: Long? = null,
    val id: Long? = null,
    val inOutCampus: String? = null,
    val routeName: String? = null,
    val route: String? = null,
    val departureTime: String? = null,
    val departAt: String? = null,
    val time: String? = null,
    val day: String? = null,
    val dayOfWeek: String? = null,
    val days: List<String>? = null,
    val dayOfWeeks: List<String>? = null,
    val weekdays: List<String>? = null,
    val weekDays: List<String>? = null,
    val notificationDays: List<String>? = null,
    val reminderDays: List<String>? = null,
    val favoriteDays: List<String>? = null,
    val timetable: TimetableResponse? = null,
    val routeList: List<JsonElement>? = null
)

data class TermsResponse(
    val title: String? = null,
    val name: String? = null,
    val contentType: String? = null,
    val url: String? = null,
    val content: String? = null,
    val fileName: String? = null
)
