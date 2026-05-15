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
    MONDAY("월요일", "MONDAY"),
    TUESDAY("화요일", "TUESDAY"),
    WEDNESDAY("수요일", "WEDNESDAY"),
    THURSDAY("목요일", "THURSDAY"),
    FRIDAY("금요일", "FRIDAY")
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

data class FavoriteRequest(
    val busId: Long,
    val routeId: Long,
    val notificationDays: List<NotificationDay>
)

data class FavoriteNotificationRequest(
    val favoriteId: String,
    val notificationDays: List<NotificationDay>
)

data class ApiResponse<T>(
    val success: Boolean,
    val data: T?,
    val message: String?
)