package com.example.hufs_bus.data

data class BusRoute(
    val id: Long,
    val name: String,
    val departure: String,
    val destination: String,
    val type: RouteType
)

enum class RouteType {
    CAMPUS,
    OFF_CAMPUS
}

data class BusSchedule(
    val id: Long,
    val routeId: Long,
    val departureTime: String,
    val totalSeats: Int,
    val remainingSeats: Int,
    val currentLocation: String,
    val status: BusStatus
)

enum class BusStatus {
    SCHEDULED,
    IN_OPERATION,
    COMPLETED,
    CANCELLED
}

data class BusStop(
    val id: Long,
    val name: String,
    val order: Int
)

data class BusLocationInfo(
    val busId: Long,
    val routeId: Long,
    val routeName: String,
    val currentStopIndex: Int,
    val currentStopName: String,
    val stops: List<BusStop>,
    val departureTime: String,
    val arrivalTime: String,
    val remainingSeats: Int,
    val totalSeats: Int,
    val status: BusStatus
)

enum class NotificationDay(
    val label: String,
    val serverValue: String
) {
    MONDAY("월요일", "MON"),
    TUESDAY("화요일", "TUE"),
    WEDNESDAY("수요일", "WED"),
    THURSDAY("목요일", "THU"),
    FRIDAY("금요일", "FRI")
}

data class FavoriteBus(
    val id: String,
    val busId: Long,
    val routeId: Long,
    val routeName: String,
    val currentStopName: String,
    val departureTime: String,
    val arrivalTime: String,
    val remainingSeats: Int,
    val totalSeats: Int,
    val notificationDays: List<NotificationDay>
)

data class SignupRequest(
    val email: String,
    val password: String,
    val privacyTermAgree: Boolean,
    val serviceTermAgree: Boolean
)

data class LoginRequest(
    val email: String,
    val password: String
)

data class LoginResponse(
    val accessToken: String,
    val userId: Long,
    val email: String,
    val role: String
)

data class AuthResult(
    val accessToken: String,
    val userId: Long,
    val email: String,
    val role: String
)

data class BusRouteResponse(
    val routeId: Long,
    val inOutCampus: String,
    val startStop: String,
    val endStop: String,
    val route: String
)

data class TimetableResponse(
    val timetableId: Long,
    val routeId: Long,
    val inOutCampus: String,
    val startStop: String,
    val endStop: String,
    val departAt: String,
    val routeList: List<String> = emptyList()
)

data class LiveTimetableResponse(
    val timetableId: Long,
    val plannedDepartureTime: String?,
    val actualDepartureTime: String?,
    val currentSeats: Int?,
    val status: String?,
    val currentLocation: LiveLocation?,
    val stops: List<StopEta> = emptyList()
)

data class LiveLocation(
    val latitude: Double?,
    val longitude: Double?
)

data class StopEta(
    val stopName: String,
    val eta: String?,
    val sequence: Int
)

data class DepartResponse(
    val timetableId: Long?,
    val status: String?
)

data class FavoriteCreateRequest(
    val timetableId: Long,
    val days: Set<String>
)

data class FavoriteResponse(
    val favoriteId: Long,
    val timetableId: Long,
    val day: String,
    val departAt: String,
    val route: String
)

data class DriverLocationRequest(
    val busId: Long,
    val latitude: Double,
    val longitude: Double
)

data class DriverLocationResponse(
    val busId: Long,
    val latitude: Double,
    val longitude: Double,
    val recordedAt: String
)

data class ApiResponse<T>(
    val message: String?,
    val data: T?,
    val localDateTime: String?
)
