package com.hufsteam.shuttletrack.ui.student

import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.hufsteam.shuttletrack.data.remote.dto.BusStatusResponse
import com.hufsteam.shuttletrack.data.remote.dto.DriverLocationResponse
import com.hufsteam.shuttletrack.data.remote.dto.FavoriteResponse
import com.hufsteam.shuttletrack.data.remote.dto.LiveEtaResponse
import com.hufsteam.shuttletrack.data.remote.dto.TimetableResponse
import com.hufsteam.shuttletrack.data.repository.ShuttleRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch

private const val OFF_CAMPUS = 1
private const val ON_CAMPUS = 0
private const val FIXED_TOTAL_SEATS = 45
private const val SEAT_DISPLAY_LOG_TAG = "SeatDisplay"

data class StopArrivalInfo(
    val stopName: String,
    val arrivalText: String = "약 3분 후 도착",
    val seatText: String = "03석",
    val detailText: String = "특정 정류장 정보 + 버스 정보"
)

data class RouteDetail(
    val stops: List<String>,
    val currentStopIndex: Int,
    val busProgressIndex: Float,
    val plannedDeparture: String,
    val actualDeparture: String,
    val etaText: String,
    val stopInfos: Map<String, StopArrivalInfo>,
    val isRunning: Boolean = false,
    val statusText: String = "운행 전"
) {
    fun infoFor(stopName: String): StopArrivalInfo {
        val normalized = stopName.replace("\n", " ")
        return stopInfos[normalized] ?: StopArrivalInfo(stopName = normalized)
    }
}

data class StudentBusUiState(
    val offCampusSchedules: List<BusSchedule> = emptyList(),
    val onCampusSchedules: List<BusSchedule> = emptyList(),
    val selectedRouteDetail: RouteDetail? = null,
    val favoriteSchedules: List<FavoriteSchedule> = emptyList(),
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val usingMockData: Boolean = false
)

class StudentBusViewModel(
    private val repository: StudentBusRepository = StudentBusRepository()
) : ViewModel() {
    var uiState by mutableStateOf(StudentBusUiState())
        private set
    private var routeStatusJob: Job? = null

    init {
        refreshSchedules()
        refreshFavorites()
    }

    fun refreshSchedules() {
        viewModelScope.launch {
            uiState = uiState.copy(isLoading = true, errorMessage = null)
            val result = repository.loadSchedules()
            uiState = uiState.copy(
                offCampusSchedules = result.offCampusSchedules,
                onCampusSchedules = result.onCampusSchedules,
                isLoading = false,
                errorMessage = result.errorMessage,
                usingMockData = result.usingMockData
            )
        }
    }

    fun refreshAll(selectedSchedule: BusSchedule? = null, showLoading: Boolean = true) {
        viewModelScope.launch {
            if (showLoading) {
                uiState = uiState.copy(isLoading = true, errorMessage = null)
            }
            val scheduleResult = repository.loadSchedules()
            val favorites = repository.loadFavorites()
            val routeDetail = selectedSchedule?.let { repository.loadRouteDetail(it) }
            uiState = uiState.copy(
                offCampusSchedules = scheduleResult.offCampusSchedules,
                onCampusSchedules = scheduleResult.onCampusSchedules,
                favoriteSchedules = favorites,
                selectedRouteDetail = routeDetail ?: uiState.selectedRouteDetail,
                isLoading = false,
                errorMessage = scheduleResult.errorMessage,
                usingMockData = scheduleResult.usingMockData
            )
        }
    }

    fun loadRouteStatus(schedule: BusSchedule) {
        viewModelScope.launch {
            val detail = repository.loadRouteDetail(schedule)
            uiState = uiState.copy(selectedRouteDetail = detail)
        }
    }

    fun startRouteStatusUpdates(schedule: BusSchedule) {
        routeStatusJob?.cancel()
        routeStatusJob = viewModelScope.launch {
            repository.subscribeRouteDetail(schedule)
                .catch {
                    val detail = repository.loadRouteDetail(schedule)
                    uiState = uiState.copy(selectedRouteDetail = detail)
                }
                .collectLatest { detail ->
                    uiState = uiState.copy(selectedRouteDetail = detail)
                }
        }
    }

    fun stopRouteStatusUpdates() {
        routeStatusJob?.cancel()
        routeStatusJob = null
    }

    fun saveFavorite(schedule: BusSchedule, days: Set<String>) {
        viewModelScope.launch {
            val currentFavorites = repository.loadFavorites()
            val hadFavorite = currentFavorites.any { it.schedule.id == schedule.id } ||
                uiState.favoriteSchedules.any { it.schedule.id == schedule.id }
            val saved = repository.saveFavorite(schedule, days, hadFavorite)
            if (saved) {
                val refreshedFavorites = repository.loadFavorites()
                uiState = uiState.copy(
                    favoriteSchedules = refreshedFavorites,
                    errorMessage = null
                )
            } else {
                uiState = uiState.copy(errorMessage = "즐겨찾기 설정에 실패했습니다.")
            }
        }
    }

    fun refreshFavorites() {
        viewModelScope.launch {
            val favorites = repository.loadFavorites()
            uiState = uiState.copy(favoriteSchedules = favorites)
        }
    }

    fun refreshSeatStatuses() {
        val off = uiState.offCampusSchedules
        val on = uiState.onCampusSchedules
        if (off.isEmpty() && on.isEmpty()) return
        viewModelScope.launch {
            val updatedOff = repository.loadSeatStatuses(off)
            val updatedOn = repository.loadSeatStatuses(on)
            uiState = uiState.copy(
                offCampusSchedules = updatedOff,
                onCampusSchedules = updatedOn
            )
        }
    }

    fun clearError() {
        uiState = uiState.copy(errorMessage = null)
    }
}

data class ScheduleLoadResult(
    val offCampusSchedules: List<BusSchedule>,
    val onCampusSchedules: List<BusSchedule>,
    val usingMockData: Boolean,
    val errorMessage: String? = null
)

class StudentBusRepository(
    private val shuttleRepository: ShuttleRepository = ShuttleRepository()
) {
    suspend fun loadSchedules(): ScheduleLoadResult {
        val offResult = shuttleRepository.getTimetable("OUT_CAMPUS")
        val onResult = shuttleRepository.getTimetable("IN_CAMPUS")

        val offBaseSchedules = offResult.getOrNull()
            ?.mapIndexed { index, dto -> dto.toSchedule(index, mockOffCampusSchedules, "교외") }
            .orEmpty()
        val onBaseSchedules = onResult.getOrNull()
            ?.mapIndexed { index, dto -> dto.toSchedule(index, mockOnCampusSchedules, "교내") }
            .orEmpty()
        val offSchedules = offBaseSchedules.withSeatStatuses()
        val onSchedules = onBaseSchedules.withSeatStatuses()
        val errorMessage = when {
            offResult.isFailure && onResult.isFailure -> "서버 시간표를 불러오지 못했습니다."
            offResult.isFailure -> "교외 시간표를 불러오지 못했습니다."
            onResult.isFailure -> "교내 시간표를 불러오지 못했습니다."
            offSchedules.isEmpty() && onSchedules.isEmpty() -> "서버 시간표 데이터가 비어 있습니다. 관리자 시간표 업로드 또는 서버 DB 반영 후 새로고침해 주세요."
            else -> null
        }

        return ScheduleLoadResult(
            offCampusSchedules = offSchedules,
            onCampusSchedules = onSchedules,
            usingMockData = false,
            errorMessage = errorMessage
        )
    }

    suspend fun loadFavorites(): List<FavoriteSchedule> {
        val favorites = shuttleRepository.getFavorites().getOrNull()
            ?.mapIndexedNotNull { index, favorite -> favorite.toFavoriteSchedule(index) }
            .orEmpty()

        return favorites
            .groupBy { it.favoriteGroupKey() }
            .map { (_, items) ->
                val first = items.first()
                first.copy(days = items.flatMap { it.days }.toSet())
            }
    }

    suspend fun loadRouteDetail(schedule: BusSchedule): RouteDetail {
        val timetableId = schedule.timetableId ?: schedule.id.toLong()
        val busStatus = shuttleRepository.getBusStatuses(timetableId).getOrNull()
        val isRunning = busStatus?.status?.trim()?.uppercase() == "RUNNING"
        val liveEta = if (isRunning) {
            shuttleRepository.getLiveEta(timetableId).getOrNull()
        } else {
            null
        }
        val driverLocation = if (isRunning) {
            busStatus?.busId?.let { shuttleRepository.getDriverLocation(it).getOrNull() }
        } else {
            null
        }
        if (liveEta == null && busStatus == null && schedule.routeStops.isEmpty()) {
            return mockRouteDetailFor(schedule)
        }
        return routeDetailFromApi(schedule, liveEta, busStatus, driverLocation)
    }

    fun subscribeRouteDetail(schedule: BusSchedule) = flow {
        val timetableId = schedule.timetableId ?: schedule.id.toLong()
        emit(loadRouteDetail(schedule))
        shuttleRepository.subscribeLiveEta(timetableId)
            .collect { liveEta ->
                val busStatus = shuttleRepository.getBusStatuses(timetableId).getOrNull()
                val driverLocation = busStatus?.busId?.let { shuttleRepository.getDriverLocation(it).getOrNull() }
                emit(routeDetailFromApi(schedule, liveEta, busStatus, driverLocation))
            }
    }

    suspend fun saveFavorite(schedule: BusSchedule, days: Set<String>, isExisting: Boolean): Boolean {
        return shuttleRepository.saveFavorite(
            timetableId = schedule.id.toLong(),
            days = days.map(::toApiDay).toSet(),
            isExisting = isExisting
        ).isSuccess
    }

    suspend fun deleteFavorite(schedule: BusSchedule): Boolean {
        return shuttleRepository.deleteFavorite(schedule.id.toLong()).isSuccess
    }

    private fun TimetableResponse.toSchedule(index: Int, fallback: List<BusSchedule>, defaultCampusType: String = ""): BusSchedule {
        val stopPoints = (routeList ?: stops).toStopPoints()
        val routeStops = stopPoints.map { it.name }
        val fallbackSchedule = fallback.getOrNull(index % fallback.size)
        val fallbackRoute = if (routeStops.size >= 2) {
            "${routeStops.first().cleanStopName()} → ${routeStops.last().cleanStopName()}"
        } else {
            val start = startStop.orEmpty()
            val end = endStop.orEmpty()
            if (start.isNotBlank() && end.isNotBlank()) "$start → $end" else fallbackSchedule?.routeName ?: "노선"
        }

        val resolvedTimetableId = firstLong(timetableId, specificTimetableId, id)

        return BusSchedule(
            id = resolvedTimetableId?.toInt() ?: fallbackSchedule?.id ?: index + 1,
            timetableId = resolvedTimetableId,
            routeName = firstText(routeName, route, fallbackRoute),
            departureTime = firstText(departureTime, departAt, time, plannedDeparture, fallbackSchedule?.departureTime, "00:00"),
            remainingSeats = FIXED_TOTAL_SEATS,
            totalSeats = FIXED_TOTAL_SEATS,
            currentLocation = firstText(currentLocation, currentStop, "운행 전"),
            routeStops = routeStops.ifEmpty { fallbackSchedule?.routeStops.orEmpty() },
            campusType = resolveCampusType(inOutCampus, routeName, route, fallbackRoute, defaultCampusType),
            hasSeatInfo = false,
            seatInfoSource = "pending"
        )
    }

    suspend fun loadSeatStatuses(schedules: List<BusSchedule>): List<BusSchedule> =
        schedules.withSeatStatuses()

    private suspend fun List<BusSchedule>.withSeatStatuses(): List<BusSchedule> {
        return map { schedule -> schedule.withLatestSeatStatus() }
    }

    private suspend fun BusSchedule.withLatestSeatStatus(): BusSchedule {
        val statusResult = shuttleRepository.getBusStatuses(timetableId ?: id.toLong())
        val busStatus = statusResult.getOrNull()
        if (busStatus == null) {
            logSeatDisplay(
                schedule = this,
                totalSeats = totalSeats,
                currentSeats = null,
                displayRemainingSeats = null,
                source = if (statusResult.isFailure) "error" else "cached",
                error = statusResult.exceptionOrNull()
            )
            return copy(hasSeatInfo = false, seatInfoSource = if (statusResult.isFailure) "error" else "cached")
        }
        val total = busStatus.totalSeats ?: totalSeats.takeIf { it > 0 } ?: FIXED_TOTAL_SEATS
        val current = firstInt(busStatus.currentSeats, busStatus.currentPassengers, busStatus.passengerCount)
        if (current == null) {
            logSeatDisplay(
                schedule = this,
                totalSeats = total,
                currentSeats = null,
                displayRemainingSeats = null,
                source = "error"
            )
            return copy(totalSeats = total, hasSeatInfo = false, seatInfoSource = "error")
        }
        val remaining = (total - current).coerceIn(0, total)
        val locationText = busStatus.currentStopName
            ?.takeIf { it.isNotBlank() }
            ?.let { "현재 위치 | $it" }
            ?: currentLocation
        logSeatDisplay(
            schedule = this,
            totalSeats = total,
            currentSeats = current,
            displayRemainingSeats = remaining,
            source = "seats API"
        )
        return copy(
            totalSeats = total,
            remainingSeats = remaining,
            currentLocation = locationText,
            hasSeatInfo = true,
            seatInfoSource = "seats API"
        )
    }

    private fun FavoriteResponse.toFavoriteSchedule(index: Int): FavoriteSchedule? {
        val base = timetable ?: TimetableResponse(
            timetableId = timetableId ?: id,
            specificTimetableId = specificTimetableId,
            routeName = routeName,
            route = route,
            departureTime = departureTime,
            departAt = departAt,
            time = time,
            routeList = routeList
        )
        val fallback = (mockOffCampusSchedules + mockOnCampusSchedules)
        val schedule = base.toSchedule(
            index = index,
            fallback = fallback,
            defaultCampusType = resolveCampusType(inOutCampus, routeName, route, base.routeName, base.route)
        )
        val parsedDays = listOfNotNull(
            days,
            dayOfWeeks,
            weekdays,
            weekDays,
            notificationDays,
            reminderDays,
            favoriteDays
        )
            .flatten()
            .map(::toUiDay)
            .toSet() + listOfNotNull(day, dayOfWeek).map(::toUiDay).toSet()
        return FavoriteSchedule(schedule, parsedDays)
    }

    private fun routeDetailFromApi(
        schedule: BusSchedule,
        liveEta: LiveEtaResponse?,
        busStatus: BusStatusResponse?,
        driverLocation: DriverLocationResponse? = null
    ): RouteDetail {
        val status = firstText(liveEta?.status, busStatus?.status, driverLocation?.status).uppercase()
        val statusIsRunning = status == "RUNNING"
        val statusIsDone = status == "DONE"
        val liveCurrentStopName = firstText(liveEta?.currentStopName, liveEta?.currentStop)
        val hasTrackedStop = busStatus?.currentStopSequence != null ||
            !busStatus?.currentStopName.isNullOrBlank() ||
            driverLocation?.currentStopSequence != null ||
            !driverLocation?.currentStopName.isNullOrBlank() ||
            liveEta?.currentStopSequence != null ||
            liveCurrentStopName.isNotBlank()
        val hasLiveBus = statusIsRunning && (liveEta != null || hasTrackedStop)
        val apiStopPoints = (liveEta?.stops ?: liveEta?.stopNames ?: liveEta?.routeList).toStopPoints()
        val fallbackStops = schedule.routeStops.ifEmpty { mockRouteDetailFor(schedule).stops }
        val stopPoints = apiStopPoints.ifEmpty { fallbackStops.map { RouteStopPoint(it, null, null) } }
        val stops = stopPoints.map { it.name }
        val currentStopName = firstText(busStatus?.currentStopName, driverLocation?.currentStopName, liveCurrentStopName)
        val currentStopNameIndex = currentStopName.takeIf { it.isNotBlank() }?.let { currentStop ->
            stops.indexOfFirst { it.cleanStopName() == currentStop.cleanStopName() }
        }?.takeIf { it >= 0 }

        val currentIndex = if (hasLiveBus) {
            firstInt(
                liveEta?.currentStopIndex,
                liveEta?.busStopIndex,
                liveEta?.currentIndex,
                liveEta?.currentStopSequence?.minus(1),
                busStatus?.currentStopSequence?.minus(1),
                driverLocation?.currentStopSequence?.minus(1),
                currentStopNameIndex
            ) ?: 0
        } else {
            0
        }
        val busLatitude = firstDouble(
            liveEta?.busLatitude,
            liveEta?.busLat,
            liveEta?.vehicleLatitude,
            liveEta?.currentLocation?.latitude,
            liveEta?.latitude,
            liveEta?.lat,
            driverLocation?.latitude
        )
        val busLongitude = firstDouble(
            liveEta?.busLongitude,
            liveEta?.busLng,
            liveEta?.vehicleLongitude,
            liveEta?.currentLocation?.longitude,
            liveEta?.longitude,
            liveEta?.lng,
            driverLocation?.longitude
        )
        val progressIndex = firstFloat(
            liveEta?.busProgressIndex,
            liveEta?.currentProgressIndex,
            liveEta?.routeProgressIndex,
            liveEta?.progressIndex
        ) ?: if (hasLiveBus) {
            estimateProgressFromCoordinates(stopPoints, busLatitude, busLongitude) ?: currentIndex.toFloat()
        } else {
            0f
        }

        val totalSeats = busStatus?.totalSeats
            ?: liveEta?.totalSeats
            ?: schedule.totalSeats.takeIf { it > 0 }
            ?: FIXED_TOTAL_SEATS
        val currentPassengers = firstInt(
            busStatus?.currentSeats,
            busStatus?.currentPassengers,
            busStatus?.passengerCount,
            liveEta?.currentSeats,
            driverLocation?.currentSeats
        )
        val remainingSeats = currentPassengers?.let { (totalSeats - it).coerceIn(0, totalSeats) }
            ?: if (schedule.hasSeatInfo) schedule.remainingSeats.coerceIn(0, totalSeats) else totalSeats
        val hasSeatInfo = currentPassengers != null || schedule.hasSeatInfo
        val statusText = when {
            statusIsRunning -> "운행 중"
            statusIsDone -> "운행 종료"
            else -> "운행 전"
        }
        val eta = if (hasLiveBus) {
            liveEta?.etaText
                ?: liveEta?.estimatedMinutes?.let { "${it.toString().padStart(2, '0')}분" }
                ?: liveEta?.etaMinutes?.let { "${it.toString().padStart(2, '0')}분" }
                ?: "조회 중"
        } else if (statusIsDone) {
            "운행 종료"
        } else {
            "운행 전"
        }
        val arrival = if (hasLiveBus) {
            firstText(liveEta?.arrivalText, liveEta?.arrivalInfo, "도착 정보 조회 중")
        } else if (statusIsDone) {
            "운행이 종료되었습니다"
        } else {
            "현재 운행 중인 버스가 없습니다"
        }

        val infos = stops.associate { stop ->
            val normalized = stop.replace("\n", " ")
            normalized to StopArrivalInfo(
                stopName = normalized,
                arrivalText = arrival,
                seatText = if (hasSeatInfo) "${remainingSeats.toString().padStart(2, '0')}석" else "확인 중"
            )
        }
        val safeLastIndex = stops.lastIndex.coerceAtLeast(0)

        return RouteDetail(
            stops = stops,
            currentStopIndex = currentIndex.coerceIn(0, safeLastIndex),
            busProgressIndex = progressIndex.coerceIn(0f, safeLastIndex.toFloat()),
            plannedDeparture = schedule.departureTime,
            actualDeparture = if (hasLiveBus) {
                firstText(liveEta?.actualDepartureTime, liveEta?.actualDeparture, liveEta?.actualTime, "미정")
            } else {
                "미정"
            },
            etaText = eta,
            stopInfos = infos,
            isRunning = hasLiveBus,
            statusText = statusText
        )
    }

    private fun List<JsonElement>?.toStopPoints(): List<RouteStopPoint> {
        return this?.mapIndexedNotNull { index, item -> item.toStopPoint(index) }.orEmpty()
    }

    private fun JsonElement.toStopPoint(index: Int): RouteStopPoint? {
        if (isJsonPrimitive) return RouteStopPoint(asString, null, null)
        if (!isJsonObject) return null
        val obj = asJsonObject
        return RouteStopPoint(
            name = firstText(obj.findString("name"), obj.findString("stopName"), "정류장${index + 1}"),
            latitude = firstDouble(
                obj.findDouble("latitude"),
                obj.findDouble("lat"),
                obj.findDouble("stopLatitude"),
                obj.findDouble("stopLat")
            ),
            longitude = firstDouble(
                obj.findDouble("longitude"),
                obj.findDouble("lng"),
                obj.findDouble("stopLongitude"),
                obj.findDouble("stopLng")
            )
        )
    }

    private fun estimateProgressFromCoordinates(
        stopPoints: List<RouteStopPoint>,
        busLatitude: Double?,
        busLongitude: Double?
    ): Float? {
        if (busLatitude == null || busLongitude == null) return null
        val points = stopPoints.mapIndexedNotNull { index, point ->
            val lat = point.latitude
            val lng = point.longitude
            if (lat == null || lng == null) null else IndexedStopPoint(index, lat, lng)
        }
        if (points.size < 2) return null

        var bestProgress = points.first().index.toFloat()
        var bestDistance = Double.MAX_VALUE
        points.zipWithNext().forEach { (start, end) ->
            val vx = end.longitude - start.longitude
            val vy = end.latitude - start.latitude
            val wx = busLongitude - start.longitude
            val wy = busLatitude - start.latitude
            val segmentLength = vx * vx + vy * vy
            val t = if (segmentLength == 0.0) 0.0 else ((wx * vx + wy * vy) / segmentLength).coerceIn(0.0, 1.0)
            val projectedLng = start.longitude + t * vx
            val projectedLat = start.latitude + t * vy
            val dx = busLongitude - projectedLng
            val dy = busLatitude - projectedLat
            val distance = dx * dx + dy * dy
            if (distance < bestDistance) {
                bestDistance = distance
                bestProgress = (start.index + (end.index - start.index) * t).toFloat()
            }
        }
        return bestProgress.coerceIn(0f, stopPoints.lastIndex.toFloat())
    }

    private fun firstText(vararg values: String?): String {
        return values.firstOrNull { !it.isNullOrBlank() }.orEmpty()
    }

    private fun firstInt(vararg values: Int?): Int? {
        return values.firstOrNull { it != null }
    }

    private fun firstLong(vararg values: Long?): Long? {
        return values.firstOrNull { it != null }
    }

    private fun firstFloat(vararg values: Float?): Float? {
        return values.firstOrNull { it != null }
    }

    private fun firstDouble(vararg values: Double?): Double? {
        return values.firstOrNull { it != null }
    }

    private fun String.distanceFromNowMinutes(): Int {
        val parts = take(5).split(":")
        val hour = parts.getOrNull(0)?.toIntOrNull() ?: return Int.MAX_VALUE
        val minute = parts.getOrNull(1)?.toIntOrNull() ?: 0
        val scheduled = hour * 60 + minute
        val calendar = java.util.Calendar.getInstance()
        val now = calendar.get(java.util.Calendar.HOUR_OF_DAY) * 60 + calendar.get(java.util.Calendar.MINUTE)
        val direct = kotlin.math.abs(scheduled - now)
        return minOf(direct, 24 * 60 - direct)
    }

    private fun String.cleanStopName(): String {
        return replace("\n", " ").replace("(기점)", "").replace("(종점)", "").trim()
    }

    private fun toUiDay(day: String): String {
        val normalized = day.trim().uppercase()
        return when (normalized) {
            "MON", "MONDAY", "월", "월요일" -> "월요일"
            "TUE", "TUESDAY", "화", "화요일" -> "화요일"
            "WED", "WEDNESDAY", "수", "수요일" -> "수요일"
            "THU", "THURSDAY", "목", "목요일" -> "목요일"
            "FRI", "FRIDAY", "금", "금요일" -> "금요일"
            else -> day.trim()
        }
    }

    private fun toApiDay(day: String): String {
        return when (day.trim().uppercase()) {
            "월", "월요일", "MON", "MONDAY" -> "MON"
            "화", "화요일", "TUE", "TUESDAY" -> "TUE"
            "수", "수요일", "WED", "WEDNESDAY" -> "WED"
            "목", "목요일", "THU", "THURSDAY" -> "THU"
            "금", "금요일", "FRI", "FRIDAY" -> "FRI"
            else -> day.trim()
        }
    }

    private fun FavoriteSchedule.favoriteGroupKey(): String {
        return listOf(
            schedule.id.toString(),
            schedule.campusType,
            schedule.routeName,
            schedule.departureTime
        ).joinToString("|")
    }

    private fun resolveCampusType(vararg values: String?): String {
        val joined = values.filterNotNull().joinToString(" ").uppercase()
        return when {
            joined.contains("IN_CAMPUS") || joined.contains("교내") ||
                joined.contains("지석묘") || joined.contains("인문경상관") -> "교내"
            joined.contains("OUT_CAMPUS") || joined.contains("교외") ||
                joined.contains("판교") || joined.contains("경기광주") || joined.contains("외대(글)") -> "교외"
            else -> "미분류"
        }
    }
}

private fun logSeatDisplay(
    schedule: BusSchedule,
    totalSeats: Int,
    currentSeats: Int?,
    displayRemainingSeats: Int?,
    source: String,
    error: Throwable? = null
) {
    val msg = "SeatDisplay id=${schedule.id} timetableId=${schedule.timetableId} " +
        "route=${schedule.routeName} total=$totalSeats current=$currentSeats " +
        "remaining=$displayRemainingSeats source=$source"
    if (error != null) {
        Log.e(SEAT_DISPLAY_LOG_TAG, msg, error)
    } else {
        Log.i(SEAT_DISPLAY_LOG_TAG, msg)
    }
}

private data class RouteStopPoint(
    val name: String,
    val latitude: Double?,
    val longitude: Double?
)

private data class IndexedStopPoint(
    val index: Int,
    val latitude: Double,
    val longitude: Double
)

private fun JsonObject.findString(key: String): String? {
    val element = get(key) ?: return null
    return if (element.isJsonPrimitive) element.asString.takeIf { it.isNotBlank() } else null
}

private fun JsonObject.findDouble(key: String): Double? {
    val element = get(key) ?: return null
    return if (element.isJsonPrimitive) element.asString.toDoubleOrNull() else null
}

val mockOffCampusSchedules = mockSchedulesFromGroups(
    100,
    "교외",
    MockScheduleGroup(
        routeName = "판교역 → 외대(글)",
        stops = listOf("판교역\n(기점)", "성남역", "서현역", "외대(글)\n(종점)"),
        times = listOf("07:40", "07:45", "07:50", "09:40", "09:50"),
        currentLocation = "판교역 출발 전입니다"
    ),
    MockScheduleGroup(
        routeName = "외대(글) → 판교역",
        stops = listOf("외대(글)\n(기점)", "서현역", "성남역", "판교역\n(종점)"),
        times = listOf("14:10", "15:10", "15:25", "17:30", "18:20"),
        currentLocation = "출발 전입니다"
    ),
    MockScheduleGroup(
        routeName = "경기광주역 → 외대(글)",
        stops = listOf("경기광주역\n(기점)", "기숙사", "백년관", "인문경상관\n(종점)"),
        times = listOf("18:05"),
        currentLocation = "경기광주역 출발 전입니다"
    )
)

val mockOnCampusSchedules = mockSchedulesFromGroups(
    1,
    "교내",
    MockScheduleGroup(
        routeName = "지석묘 → 인문경상관",
        stops = listOf("지석묘\n(기점)", "기숙사", "도서관", "어문관", "인문경상관\n(종점)"),
        times = listOf(
            "08:20", "08:30", "08:40", "08:40", "08:50", "08:50", "09:00", "09:15", "09:30",
            "09:40", "09:50", "10:00", "10:15", "10:30", "10:40", "10:40", "10:50", "10:50",
            "11:00", "11:15", "11:30", "11:35", "11:40", "11:50", "12:10", "12:50", "12:50",
            "13:00", "13:15", "13:30", "13:40", "13:50", "14:00", "14:15", "14:30", "14:40",
            "14:50", "15:00", "15:30", "15:40", "15:50", "16:00", "16:20", "16:40", "17:00",
            "17:20", "17:40", "18:00", "18:30", "19:00", "19:30", "20:00", "20:30"
        ),
        currentLocation = "지석묘 출발 전입니다"
    ),
    MockScheduleGroup(
        routeName = "인문경상관 → 지석묘",
        stops = listOf("인문경상관\n(기점)", "교양관", "공학관", "백년관", "기숙사", "지석묘\n(종점)"),
        times = listOf(
            "08:30", "08:40", "08:50", "09:00", "09:10", "09:25", "09:40", "09:50", "10:00",
            "10:10", "10:25", "10:40", "10:50", "11:00", "11:10", "11:25", "11:40", "11:45",
            "11:50", "12:00", "12:20", "13:00", "13:00", "13:10", "13:25", "13:40", "13:50",
            "14:00", "14:10", "14:25", "14:40", "14:50", "14:50", "15:00", "15:10", "15:40",
            "15:50", "16:00", "16:10", "16:30", "16:50", "16:50", "17:10", "17:30", "17:50",
            "18:10", "18:40", "19:10", "19:40", "20:10", "20:40"
        ),
        currentLocation = "인문경상관 출발 전입니다"
    )
)

private data class MockScheduleGroup(
    val routeName: String,
    val stops: List<String>,
    val times: List<String>,
    val currentLocation: String
)

private fun mockSchedulesFromGroups(startId: Int, campusType: String, vararg groups: MockScheduleGroup): List<BusSchedule> {
    var nextId = startId
    return groups.flatMap { group ->
        group.times.mapIndexed { index, time ->
            BusSchedule(
                id = nextId++,
                timetableId = null,
                routeName = group.routeName,
                departureTime = time,
                remainingSeats = mockRemainingSeats(index),
                totalSeats = FIXED_TOTAL_SEATS,
                currentLocation = group.currentLocation,
                routeStops = group.stops,
                campusType = campusType
            )
        }
    }
}

private fun mockRemainingSeats(index: Int): Int {
    return when (index % 4) {
        0 -> 45
        1 -> 44
        2 -> 42
        else -> 36
    }
}

fun mockRouteDetailFor(schedule: BusSchedule): RouteDetail {
    val stops = schedule.routeStops.ifEmpty {
        when {
        schedule.routeName.startsWith("판교역") -> listOf("판교역\n(기점)", "성남역", "서현역", "외대(글)\n(종점)")
        schedule.routeName.endsWith("판교역") -> listOf("외대(글)\n(기점)", "서현역", "성남역", "판교역\n(종점)")
        schedule.routeName.startsWith("외대(글)") -> listOf("인문경상관\n(기점)", "교양관", "후생관", "공학관", "백년관", "기숙사", "지석묘\n(종점)")
        schedule.routeName.contains("경기광주역") -> listOf("인경관\n(기점)", "교양관", "후생관", "공학관", "백년관", "기숙사", "지석묘\n(종점)")
        schedule.routeName.contains("지석묘") -> listOf("지석묘\n(기점)", "기숙사", "도서관", "어문관", "인문경상관\n(종점)")
        else -> listOf("정류장1", "정류장2", "정류장3", "정류장4")
        }
    }
    val currentIndex = 0
    val infos = stops.associate { stop ->
        val normalized = stop.replace("\n", " ")
        normalized to StopArrivalInfo(
            stopName = normalized,
            arrivalText = "현재 운행 중인 버스가 없습니다",
            seatText = "${schedule.remainingSeats.toString().padStart(2, '0')}석"
        )
    }
    return RouteDetail(
        stops = stops,
        currentStopIndex = currentIndex,
        busProgressIndex = currentIndex.toFloat(),
        plannedDeparture = schedule.departureTime,
        actualDeparture = "미정",
        etaText = "운행 전",
        stopInfos = infos,
        isRunning = false
    )
}
