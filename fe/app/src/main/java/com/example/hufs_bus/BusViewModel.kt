package com.example.hufs_bus

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.hufs_bus.data.*
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class BusViewModel : ViewModel() {

    private val repository = BusRepository()

    var isAuthenticated by mutableStateOf(false)
        private set

    var userEmail by mutableStateOf("")
        private set

    var userName by mutableStateOf("승객")
        private set

    var selectedCampusType by mutableStateOf(RouteType.OFF_CAMPUS)
        private set

    var routes by mutableStateOf<List<BusRoute>>(emptyList())
        private set

    var selectedRoute by mutableStateOf<BusRoute?>(null)
        private set

    var selectedHour by mutableIntStateOf(8)
        private set

    var schedules by mutableStateOf<List<BusSchedule>>(emptyList())
        private set

    var isLoading by mutableStateOf(false)
        private set

    var busLocation by mutableStateOf<BusLocationInfo?>(null)
        private set

    var isNavLoading by mutableStateOf(false)
        private set

    var favoriteBuses by mutableStateOf<List<FavoriteBus>>(emptyList())
        private set

    var isFavoriteSheetVisible by mutableStateOf(false)
        private set

    var selectedNotificationDays by mutableStateOf<Set<NotificationDay>>(emptySet())
        private set

    private var pendingFavoriteBus by mutableStateOf<BusLocationInfo?>(null)

    var lastNotificationMessage by mutableStateOf<String?>(null)
        private set

    private var autoRefreshJob: Job? = null

    init {
        loadRoutes()
    }

    fun selectCampusType(type: RouteType) {
        if (selectedCampusType == type) return
        selectedCampusType = type
        selectedRoute = null
        loadRoutes()
    }

    fun selectRoute(route: BusRoute) {
        selectedRoute = route
        loadSchedules()
    }

    fun selectHour(hour: Int) {
        selectedHour = hour
        loadSchedules()
    }

    private fun loadRoutes() {
        viewModelScope.launch {
            isLoading = true
            routes = repository.getRoutes(selectedCampusType)
            if (routes.isNotEmpty() && selectedRoute == null) {
                selectedRoute = routes[0]
            }
            loadSchedules()
            isLoading = false
        }
    }

    private fun loadSchedules() {
        val route = selectedRoute ?: return
        viewModelScope.launch {
            isLoading = true
            schedules = repository.getSchedules(route.id, selectedHour)
            isLoading = false
        }
    }

    fun refreshSchedules() {
        loadSchedules()
    }

    fun signIn(email: String, password: String): Boolean {
        val normalizedEmail = email.trim()
        if (normalizedEmail.isBlank() || password.length < 4) return false

        userEmail = normalizedEmail
        userName = normalizedEmail.substringBefore("@").ifBlank { "승객" }
        isAuthenticated = true
        return true
    }

    fun signUp(email: String, password: String, confirmedPassword: String): Boolean {
        if (email.isBlank() || password.length < 6 || password != confirmedPassword) return false
        return signIn(email, password)
    }

    fun signOut() {
        isAuthenticated = false
        userEmail = ""
        userName = "승객"
        stopAutoRefresh()
    }

    fun loadBusLocation(scheduleId: Long) {
        viewModelScope.launch {
            isNavLoading = true
            busLocation = repository.getBusLocation(scheduleId)
            isNavLoading = false
        }
    }

    fun refreshBusLocation() {
        val loc = busLocation ?: return
        viewModelScope.launch {
            busLocation = repository.getBusLocation(loc.busId)
        }
    }

    fun startAutoRefresh() {
        stopAutoRefresh()
        autoRefreshJob = viewModelScope.launch {
            while (true) {
                delay(30_000)
                refreshBusLocation()
            }
        }
    }

    fun stopAutoRefresh() {
        autoRefreshJob?.cancel()
        autoRefreshJob = null
    }

    fun isBusFavorite(location: BusLocationInfo?): Boolean {
        if (location == null) return false

        return favoriteBuses.any {
            it.busId == location.busId && it.routeId == location.routeId
        }
    }

    fun toggleFavoriteFromNavigator() {
        val location = busLocation ?: return

        if (isBusFavorite(location)) {
            removeFavorite(location.busId, location.routeId)
        } else {
            openFavoriteSheet(location)
        }
    }

    private fun openFavoriteSheet(location: BusLocationInfo) {
        pendingFavoriteBus = location

        val existingFavorite = favoriteBuses.find {
            it.busId == location.busId && it.routeId == location.routeId
        }

        selectedNotificationDays = existingFavorite
            ?.notificationDays
            ?.toSet()
            ?: emptySet()

        isFavoriteSheetVisible = true
    }

    fun closeFavoriteSheet() {
        isFavoriteSheetVisible = false
        selectedNotificationDays = emptySet()
        pendingFavoriteBus = null
    }

    fun toggleNotificationDay(day: NotificationDay) {
        selectedNotificationDays = if (selectedNotificationDays.contains(day)) {
            selectedNotificationDays - day
        } else {
            selectedNotificationDays + day
        }
    }

    fun saveFavoriteWithNotificationDays() {
        val location = pendingFavoriteBus ?: return
        if (selectedNotificationDays.isEmpty()) return

        val favorite = FavoriteBus(
            id = "${location.routeId}_${location.busId}",
            busId = location.busId,
            routeId = location.routeId,
            routeName = location.routeName,
            currentStopName = location.currentStopName,
            departureTime = location.departureTime,
            arrivalTime = location.arrivalTime,
            remainingSeats = location.remainingSeats,
            totalSeats = location.totalSeats,
            notificationDays = selectedNotificationDays.toList()
        )

        favoriteBuses = favoriteBuses
            .filterNot { it.busId == location.busId && it.routeId == location.routeId } + favorite

        lastNotificationMessage = "${location.routeName} 알림이 설정되었습니다."

        closeFavoriteSheet()
    }

    fun clearNotificationMessage() {
        lastNotificationMessage = null
    }

    fun removeFavorite(busId: Long, routeId: Long) {
        favoriteBuses = favoriteBuses.filterNot {
            it.busId == busId && it.routeId == routeId
        }
    }

    override fun onCleared() {
        super.onCleared()
        stopAutoRefresh()
    }
}
