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
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

private const val OFF_CAMPUS = 1
private const val ON_CAMPUS = 0
private const val FIXED_TOTAL_SEATS = 45
private const val SEAT_DISPLAY_LOG_TAG = "SeatDisplay"

data class StopArrivalInfo(
    val stopName: String,
    val arrivalText: String = "도착 정보 없음",
    val seatText: String = "확인 중",
    val detailText: String = ""
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
    val statusText: String = "운행 전",
    val currentPassengerCount: Int = -1,
    val totalSeats: Int = FIXED_TOTAL_SEATS
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
    val errorMessage: String? = null
)

class StudentBusViewModel : ViewModel() {

    private val repository = StudentBusRepository()
    var uiState by mutableStateOf(StudentBusUiState())
        private set
    private var routeStatusJob: Job? = null
    var busLocation by mutableStateOf<DriverLocationResponse?>(null)
        private set

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
                errorMessage = result.errorMessage
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
                errorMessage = scheduleResult.errorMessage
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
        val timetableId = schedule.timetableId ?: schedule.id.toLong()
        routeStatusJob = viewModelScope.launch {
            // 초기 전체 상태 로드
            val initial = repository.loadRouteDetail(schedule)
            uiState = uiState.copy(selectedRouteDetail = initial)

            // busId 확보
            var busId = repository.getBusId(timetableId)
            Log.d("PollingStart", "[PollingStart] busId=$busId timetableId=$timetableId")

            var tickCount = 0
            while (isActive) {
                try {
                    if (busId == null) {
                        busId = repository.getBusId(timetableId)
                        if (busId == null) {
                            delay(3_000L)
                            tickCount++
                            continue
                        }
                        Log.d("PollingStart", "[PollingStart] busId=$busId timetableId=$timetableId resolved")
                    }

                    // 1초마다 위치 API 호출
                    Log.d("LocationApiRequest", "[LocationApiRequest] busId=$busId")
                    val location = repository.getLocation(busId!!)
                    val lat = location?.latitude
                    val lng = location?.longitude

                    if (lat != null && lng != null) {
                        busLocation = location
                        Log.d("MapMarker", "[MapMarker] updated lat=$lat lng=$lng")
                        val curr = uiState.selectedRouteDetail
                        if (curr != null && !curr.isRunning) {
                            uiState = uiState.copy(selectedRouteDetail = curr.copy(isRunning = true))
                        }
                    }

                    // 10초마다 전체 경로 상세 정보 갱신 (busProgressIndex 업데이트)
                    tickCount++
                    if (tickCount % 10 == 0) {
                        val refreshed = repository.loadRouteDetail(schedule)
                        val loc = busLocation
                        uiState = uiState.copy(
                            selectedRouteDetail = if (loc?.latitude != null) refreshed.copy(isRunning = true)
                                                  else refreshed
                        )
                    }
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    Log.w("LocationPolling", "[LocationPolling] error: ${e.message}")
                }
                delay(1_000L)
            }
            Log.d("PollingStop", "[PollingStop] reason=cancelled timetableId=$timetableId")
        }
    }

    fun stopRouteStatusUpdates() {
        Log.d("PollingStop", "[PollingStop] reason=stopCalled")
        routeStatusJob?.cancel()
        routeStatusJob = null
        busLocation = null
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
    val errorMessage: String? = null
)

class StudentBusRepository(
    private val shuttleRepository: ShuttleRepository = ShuttleRepository()
) {
    suspend fun loadSchedules(): ScheduleLoadResult {
        val offResult = shuttleRepository.getTimetable("OUT_CAMPUS")
        val onResult = shuttleRepository.getTimetable("IN_CAMPUS")

        val offBaseSchedules = offResult.getOrNull()
            ?.mapIndexed { index, dto -> dto.toSchedule(index, "교외") }
            .orEmpty()
        val onBaseSchedules = onResult.getOrNull()
            ?.mapIndexed { index, dto -> dto.toSchedule(index, "교내") }
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
        return routeDetailFromApi(schedule, liveEta, busStatus, driverLocation)
    }

    suspend fun getBusId(timetableId: Long): Long? {
        return shuttleRepository.getBusStatuses(timetableId).getOrNull()?.busId
    }

    suspend fun getLocation(busId: Long): DriverLocationResponse? {
        return shuttleRepository.getDriverLocation(busId).getOrNull()
    }

    suspend fun saveFavorite(schedule: BusSchedule, days: Set<String>, isExisting: Boolean): Boolean {
        return shuttleRepository.saveFavorite(
            timetableId = schedule.timetableId ?: schedule.id.toLong(),
            days = days.map(::toApiDay).toSet(),
            isExisting = isExisting
        ).isSuccess
    }

    suspend fun deleteFavorite(schedule: BusSchedule): Boolean {
        return shuttleRepository.deleteFavorite(schedule.id.toLong()).isSuccess
    }

    private fun TimetableResponse.toSchedule(index: Int, defaultCampusType: String = ""): BusSchedule {
        val stopPoints = (routeList ?: stops).toStopPoints()
        val routeStops = stopPoints.map { it.name }
        val routeFromStops = if (routeStops.size >= 2) {
            "${routeStops.first().cleanStopName()} → ${routeStops.last().cleanStopName()}"
        } else {
            val start = startStop.orEmpty()
            val end = endStop.orEmpty()
            if (start.isNotBlank() && end.isNotBlank()) "$start → $end" else ""
        }

        val resolvedTimetableId = firstLong(timetableId, specificTimetableId, id)

        return BusSchedule(
            id = resolvedTimetableId?.toInt() ?: index + 1,
            timetableId = resolvedTimetableId,
            routeName = firstText(routeName, route, routeFromStops, "노선 정보 없음"),
            departureTime = firstText(departureTime, departAt, time, plannedDeparture, "시간 정보 없음"),
            remainingSeats = FIXED_TOTAL_SEATS,
            totalSeats = FIXED_TOTAL_SEATS,
            currentLocation = firstText(currentLocation, currentStop, "운행 전"),
            routeStops = routeStops,
            campusType = resolveCampusType(inOutCampus, routeName, route, routeFromStops, defaultCampusType),
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
                runningStatus = runningStatus,
                source = if (statusResult.isFailure) "error" else "cached",
                error = statusResult.exceptionOrNull()
            )
            return copy(hasSeatInfo = false, seatInfoSource = if (statusResult.isFailure) "error" else "cached")
        }
        val apiStatus = busStatus.status?.trim()?.uppercase() ?: "UNKNOWN"
        val total = busStatus.totalSeats ?: totalSeats.takeIf { it > 0 } ?: FIXED_TOTAL_SEATS

        if (apiStatus != "RUNNING") {
            // 운행 전/종료: 여석 표시 안 함. 정원만 저장. 절대 0석 표시 안 함.
            logSeatDisplay(
                schedule = this,
                totalSeats = total,
                currentSeats = null,
                displayRemainingSeats = null,
                runningStatus = apiStatus,
                source = "seats API (status=$apiStatus, not running)"
            )
            return copy(
                totalSeats = total,
                runningStatus = apiStatus,
                hasSeatInfo = false,
                seatInfoSource = "seats API (status=$apiStatus)"
            )
        }

        // 운행 중: 탑승 인원 계산 (1순위: currentSeats, 2순위: totalSeats - remainingSeats)
        val serverRemaining = firstInt(busStatus.remainingSeats, busStatus.availableSeats)
        val current = firstInt(busStatus.currentSeats, busStatus.currentPassengers, busStatus.passengerCount)
        val currentPassengerCount: Int
        val remaining: Int
        if (current != null) {
            currentPassengerCount = current.coerceIn(0, total)
            remaining = (total - currentPassengerCount).coerceIn(0, total)
        } else if (serverRemaining != null) {
            remaining = serverRemaining.coerceIn(0, total)
            // remainingSeats=0 → currentSeats=45 역산 금지 (서버 초기화 버그 방지)
            currentPassengerCount = 0
        } else {
            logSeatDisplay(
                schedule = this,
                totalSeats = total,
                currentSeats = null,
                displayRemainingSeats = null,
                runningStatus = apiStatus,
                source = "error: RUNNING but no seat data"
            )
            return copy(totalSeats = total, runningStatus = "RUNNING", hasSeatInfo = false, seatInfoSource = "error")
        }

        val locationText = busStatus.currentStopName
            ?.takeIf { it.isNotBlank() }
            ?.let { "현재 위치 | $it" }
            ?: currentLocation
        logSeatDisplay(
            schedule = this,
            totalSeats = total,
            currentSeats = current,
            displayRemainingSeats = remaining,
            runningStatus = apiStatus,
            source = "seats API"
        )
        return copy(
            totalSeats = total,
            remainingSeats = remaining,
            currentPassengerCount = currentPassengerCount,
            runningStatus = "RUNNING",
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
        val schedule = base.toSchedule(
            index = index,
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
        val busLatitude = liveEta?.resolvedLatitude() ?: driverLocation?.latitude
        val busLongitude = liveEta?.resolvedLongitude() ?: driverLocation?.longitude
        val hasGpsLocation = busLatitude != null && busLongitude != null
        val rawStatus = firstText(liveEta?.status, busStatus?.status, driverLocation?.status).uppercase()
        val status = if (rawStatus.isBlank() && hasGpsLocation) "RUNNING" else rawStatus
        val statusIsRunning = status == "RUNNING"
        val statusIsDone = status == "DONE"
        val liveCurrentStopName = firstText(liveEta?.currentStopName, liveEta?.currentStop)
        val hasTrackedStop = busStatus?.currentStopSequence != null ||
            !busStatus?.currentStopName.isNullOrBlank() ||
            driverLocation?.currentStopSequence != null ||
            !driverLocation?.currentStopName.isNullOrBlank() ||
            liveEta?.currentStopSequence != null ||
            liveCurrentStopName.isNotBlank()
        val hasLiveBus = statusIsRunning && (liveEta != null || hasTrackedStop || hasGpsLocation)
        val apiStopPoints = (liveEta?.stops ?: liveEta?.stopNames ?: liveEta?.routeList).toStopPoints()
        val stopPoints = apiStopPoints.ifEmpty { schedule.routeStops.map { RouteStopPoint(it, null, null) } }
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
        if (hasLiveBus && hasGpsLocation) {
            Log.i(
                "MapMarker",
                "[MapMarker] updated lat=$busLatitude lng=$busLongitude " +
                    "progress=$progressIndex timetableId=${schedule.timetableId ?: schedule.id.toLong()} status=$status"
            )
        } else {
            Log.i(
                "MapMarker",
                "[MapMarker] hidden lat=$busLatitude lng=$busLongitude " +
                    "timetableId=${schedule.timetableId ?: schedule.id.toLong()} status=$status hasLiveBus=$hasLiveBus"
            )
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
                seatText = when {
                    hasSeatInfo -> "${remainingSeats.toString().padStart(2, '0')}석"
                    statusIsDone -> "운행 종료"
                    statusIsRunning -> "확인 중"
                    else -> "운행 전"
                }
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
            statusText = statusText,
            currentPassengerCount = currentPassengers ?: -1,
            totalSeats = totalSeats
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

    private fun LiveEtaResponse.resolvedLatitude(): Double? {
        return firstDouble(
            busLatitude,
            busLat,
            vehicleLatitude,
            currentLocation?.latitude,
            latitude,
            lat
        )
    }

    private fun LiveEtaResponse.resolvedLongitude(): Double? {
        return firstDouble(
            busLongitude,
            busLng,
            vehicleLongitude,
            currentLocation?.longitude,
            longitude,
            lng
        )
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
    runningStatus: String? = null,
    source: String,
    error: Throwable? = null
) {
    val showingZero = displayRemainingSeats == 0
    val msg = "SeatDisplay id=${schedule.id} timetableId=${schedule.timetableId} " +
        "route=${schedule.routeName} status=${runningStatus ?: schedule.runningStatus} " +
        "total=$totalSeats current=$currentSeats remaining=$displayRemainingSeats " +
        "showingZero=$showingZero isRealZero=${showingZero && runningStatus == "RUNNING"} " +
        "source=$source"
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
