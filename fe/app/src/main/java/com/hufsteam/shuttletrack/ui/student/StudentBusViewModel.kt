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
import com.hufsteam.shuttletrack.data.remote.TokenStore
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
private const val LOCATION_POLL_INTERVAL_MS = 5_000L

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
        Log.i("PollingStart", "[PollingStart] startRouteStatusUpdates called timetableId=$timetableId")
        routeStatusJob = viewModelScope.launch {
            Log.i("PollingStart", "[PollingStart] coroutine started timetableId=$timetableId")

            // 초기 전체 상태 로드
            val initial = try {
                repository.loadRouteDetail(schedule).also { detail ->
                    uiState = uiState.copy(selectedRouteDetail = detail)
                    Log.i("PollingStart", "[PollingStart] loadRouteDetail done isRunning=${detail.isRunning} stops=${detail.stops.size}")
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e("PollingStart", "[PollingStart] loadRouteDetail error: ${e.message}")
                null
            }

            // busId 확보
            var busId = repository.getBusId(timetableId)
            val hasToken = !TokenStore.accessToken.isNullOrBlank()
            Log.i("PollingStart", "[PollingStart] busId=$busId timetableId=$timetableId tokenExists=$hasToken isRunning=${initial?.isRunning}")

            var tickCount = 0
            while (isActive) {
                try {
                    if (busId == null) {
                        busId = repository.getBusId(timetableId)
                        if (busId == null) {
                            Log.i("LocationPolling", "[LocationPolling] busId=null, waiting... timetableId=$timetableId")
                            delay(LOCATION_POLL_INTERVAL_MS)
                            tickCount++
                            continue
                        }
                        Log.i("PollingStart", "[PollingStart] busId=$busId timetableId=$timetableId resolved")
                    }

                    // 서버 부담을 줄이기 위해 5초마다 위치 API 호출
                    val tokenExists = !TokenStore.accessToken.isNullOrBlank()
                    Log.i("LocationApiRequest", "[LocationApiRequest] busId=$busId tokenExists=$tokenExists")
                    val location = repository.getLocation(busId!!)
                    val lat = location?.latitude
                    val lng = location?.longitude

                    if (location != null) {
                        busLocation = location
                        val curr = uiState.selectedRouteDetail
                        if (curr != null) {
                            val maxIdx = (curr.stops.size - 1).toFloat().coerceAtLeast(0f)
                            val coordinateProgress =
                                if (lat != null && lng != null) repository.nearestStopProgress(lat, lng) else null
                            val sequenceProgress = location.currentStopSequence
                                ?.let { seq -> (seq - 1).toFloat().coerceIn(0f, maxIdx) }
                            val nameProgress = location.currentStopName
                                ?.takeIf { it.isNotBlank() }
                                ?.let { currentStop ->
                                    val normalizedCurrentStop = currentStop.replace("\n", " ").trim()
                                    curr.stops.indexOfFirst { it.replace("\n", " ").trim() == normalizedCurrentStop }
                                }
                                ?.takeIf { it >= 0 }
                                ?.toFloat()

                            val newProgress = coordinateProgress
                                ?: sequenceProgress
                                ?: nameProgress
                                ?: curr.busProgressIndex
                            val hasServerMarkerPosition =
                                coordinateProgress != null || sequenceProgress != null || nameProgress != null

                            Log.i(
                                "MapMarker",
                                "[MapMarker] updated busId=$busId lat=$lat lng=$lng " +
                                    "currentStopSequence=${location.currentStopSequence} " +
                                    "currentStopName=${location.currentStopName} " +
                                    "busProgressIndex=$newProgress source=" +
                                    when {
                                        coordinateProgress != null -> "gps-coordinate"
                                        sequenceProgress != null -> "currentStopSequence"
                                        nameProgress != null -> "currentStopName"
                                        else -> "previous"
                                    }
                            )

                            if (hasServerMarkerPosition) {
                                uiState = uiState.copy(
                                    selectedRouteDetail = curr.copy(
                                        isRunning = true,
                                        busProgressIndex = newProgress
                                    )
                                )
                            }
                        }
                    } else {
                        Log.i("LocationPolling", "[LocationPolling] empty response for busId=$busId")
                    }

                    // 위치 polling과 별개로 일정 주기마다 전체 경로 상세 정보 갱신
                    tickCount++
                    if (tickCount % 10 == 0) {
                        val refreshed = repository.loadRouteDetail(schedule)
                        val loc = busLocation
                        val hasServerMarkerPosition =
                            loc?.latitude != null && loc?.longitude != null ||
                                loc?.currentStopSequence != null ||
                                !loc?.currentStopName.isNullOrBlank()
                        uiState = uiState.copy(
                            selectedRouteDetail = if (hasServerMarkerPosition) {
                                refreshed.copy(
                                    isRunning = true,
                                    busProgressIndex = uiState.selectedRouteDetail?.busProgressIndex
                                        ?: refreshed.busProgressIndex
                                )
                            } else {
                                refreshed
                            }
                        )
                    }
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    Log.w("LocationPolling", "[LocationPolling] error: ${e.message}")
                }
                delay(LOCATION_POLL_INTERVAL_MS)
            }
            Log.i("PollingStop", "[PollingStop] reason=cancelled timetableId=$timetableId")
        }
    }

    fun stopRouteStatusUpdates() {
        Log.i("PollingStop", "[PollingStop] reason=stopCalled")
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
    private var cachedStopPoints: List<RouteStopPoint> = emptyList()

    fun nearestStopProgress(lat: Double, lng: Double): Float? {
        val points = cachedStopPoints
        val hasCoords = points.any { it.latitude != null && it.longitude != null }
        if (!hasCoords || points.size < 2) return null
        val progress = estimateProgressFromCoordinates(points, lat, lng) ?: return null
        val nearestIndex = progress.toInt().coerceIn(0, points.lastIndex)
        val stopName = points.getOrNull(nearestIndex)?.name ?: "정류장${nearestIndex + 1}"
        Log.i("NearestStop", "[NearestStop] stopName=$stopName progress=$progress lat=$lat lng=$lng")
        return progress
    }
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
        val stopPoints = (routeList ?: stops).toStopPoints().withFallbackCoordinates()
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
        val liveCurrentStopName = firstText(liveEta?.currentStopName, liveEta?.currentStop)
        val hasTrackedStop = busStatus?.currentStopSequence != null ||
            !busStatus?.currentStopName.isNullOrBlank() ||
            driverLocation?.currentStopSequence != null ||
            !driverLocation?.currentStopName.isNullOrBlank() ||
            liveEta?.currentStopSequence != null ||
            liveCurrentStopName.isNotBlank()
        val status = if (rawStatus.isBlank() && (hasGpsLocation || hasTrackedStop)) "RUNNING" else rawStatus
        val statusIsRunning = status == "RUNNING"
        val statusIsDone = status == "DONE"
        val hasLiveBus = statusIsRunning && (liveEta != null || hasTrackedStop || hasGpsLocation)
        val apiStopPoints = (liveEta?.stops ?: liveEta?.stopNames ?: liveEta?.routeList).toStopPoints()
        val stopPoints = apiStopPoints
            .ifEmpty { schedule.routeStops.map { RouteStopPoint(it, null, null) } }
            .withFallbackCoordinates()
        if (stopPoints.any { it.latitude != null }) cachedStopPoints = stopPoints
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
        if (hasLiveBus) {
            Log.i(
                "MapMarker",
                "[MapMarker] updated lat=$busLatitude lng=$busLongitude " +
                    "currentStopSequence=${firstInt(liveEta?.currentStopSequence, busStatus?.currentStopSequence, driverLocation?.currentStopSequence)} " +
                    "currentStopName=$currentStopName " +
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

    private fun List<RouteStopPoint>.withFallbackCoordinates(): List<RouteStopPoint> {
        return map { point ->
            if (point.latitude != null && point.longitude != null) {
                val canonicalName = canonicalStopDisplayName(point.name, null)
                Log.i(
                    "StopCoordinateMap",
                    "[StopCoordinateMap] source=api stop=${point.name} mapped=$canonicalName " +
                        "lat=${point.latitude} lng=${point.longitude}"
                )
                point.copy(name = canonicalName)
            } else {
                fallbackCoordinateFor(point.name)?.let { coordinate ->
                    val canonicalName = canonicalStopDisplayName(point.name, coordinate)
                    Log.i(
                        "StopCoordinateMap",
                        "[StopCoordinateMap] source=fallback stop=${point.name} mapped=$canonicalName " +
                            "lat=${coordinate.latitude} lng=${coordinate.longitude}"
                    )
                    point.copy(
                        name = canonicalName,
                        latitude = coordinate.latitude,
                        longitude = coordinate.longitude
                    )
                } ?: run {
                    Log.i(
                        "StopCoordinateMap",
                        "[StopCoordinateMap] source=missing stop=${point.name}"
                    )
                    point
                }
            }
        }
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

private data class StopCoordinate(
    val name: String,
    val latitude: Double,
    val longitude: Double,
    val aliases: List<String> = emptyList()
)

private val fallbackStopCoordinates = listOf(
    StopCoordinate("모현지석묘입구", 37.335772, 127.25411, listOf("지석묘", "지석묘 앞", "지석묘입구")),
    StopCoordinate("기숙사사거리", 37.3361875, 127.2623125, listOf("기숙사")),
    StopCoordinate("도서관 앞", 37.3368125, 127.2669375, listOf("도서관")),
    StopCoordinate("인문경상관 앞", 37.339152, 127.274092, listOf("인문경상관", "인문경상관 하차지점")),
    StopCoordinate("인문경상관 회차장 앞", 37.33955, 127.2755, listOf("인문경상관 회차장")),
    StopCoordinate("교양관 앞", 37.339257, 127.273723, listOf("교양관")),
    StopCoordinate("후생복지관 앞", 37.337745, 127.269356, listOf("후생복지관", "후생관")),
    StopCoordinate("공학관 앞", 37.337074, 127.267947, listOf("공학관")),
    StopCoordinate("백년관 앞", 37.336764, 127.265806, listOf("백년관", "글로벌캠퍼스 백년관")),
    StopCoordinate("국제사회교육원 앞", 37.336108, 127.261757, listOf("국제사회교육원")),
    StopCoordinate("지석묘 앞", 37.336065, 127.254137, listOf("지석묘")),
    StopCoordinate("판교역 1번출구 판교역북편", 37.396089, 127.111397, listOf("판교역", "판교")),
    StopCoordinate("성남역 1번출구 방면 백현마을3단지", 37.391788, 127.118136, listOf("성남역", "백현마을3단지")),
    StopCoordinate("서현역 5번출구 방면 이매촌한신", 37.388030, 127.124703, listOf("서현역", "이매촌한신")),
    StopCoordinate("글로벌캠퍼스", 37.336804, 127.265963, listOf("외대글로벌캠퍼스", "외대(글)", "외대")),
    StopCoordinate("경기광주역 1번 출구 택시승차장", 37.398995, 127.251903, listOf("경기광주역")),
    StopCoordinate("외대사거리 앞", 37.337153, 127.249848, listOf("외대사거리", "외대 사거리 정류장")),
    StopCoordinate("서울캠퍼스", 37.597298, 127.058171, listOf("서울캠퍼스 본관")),
    StopCoordinate("망우역 건너편 명동칼국수", 37.598427, 127.094630, listOf("망우역")),
    StopCoordinate("도농역 동화고 정문 앞", 37.608029, 127.161697, listOf("도농역", "동화고")),
    StopCoordinate("양정역 버스정류장", 37.605680, 127.192834, listOf("양정역")),
    StopCoordinate("한국애니메이션고교건너편 버스정류장", 37.533998, 127.226088, listOf("한국애니메이션고교")),
    StopCoordinate("6호선 돌곶이역 5번출구 앞", 37.611007, 127.057721, listOf("돌곶이역")),
    StopCoordinate("6호선 석계역 4번출구", 37.614779, 127.066709, listOf("석계역")),
    StopCoordinate("6,7호선 태릉입구역 7번출구", 37.617864, 127.076456, listOf("태릉입구역")),
    StopCoordinate("노원역 4번 출구 맞은편 KT노원지사 앞", 37.655274, 127.063762, listOf("노원역", "KT노원지사")),
    StopCoordinate("구리 롯데백화점 맞은편 LG베스트샵 앞", 37.601764, 127.143389, listOf("구리 롯데백화점", "구리롯데백화점")),
    StopCoordinate("구리롯데백화점", 37.602733, 127.143840),
    StopCoordinate("광화문 1번출구", 37.571967, 126.974879, listOf("광화문")),
    StopCoordinate("경복궁역 6번 출구", 37.569765, 126.976548, listOf("경복궁역")),
    StopCoordinate("1호선 신길역 1,2번 출구", 37.516728, 126.916910, listOf("신길역")),
    StopCoordinate("삼성역 3번 출구 200m 전방 현대오토웨이 앞", 37.506360, 127.064227, listOf("삼성역", "현대오토웨이")),
    StopCoordinate("대치동 은미아파트 강남나무병원 앞", 37.499445, 127.068009, listOf("은미아파트", "강남나무병원")),
    StopCoordinate("수서역 1-1번 출구 서울의료원 셔틀버스정류장", 37.487440, 127.101078, listOf("수서역", "서울의료원")),
    StopCoordinate("잠실역 10번 출구 앞", 37.514795, 127.105335, listOf("잠실역")),
    StopCoordinate("천호역 6번 출구 7번 출구 사이", 37.538243, 127.124111, listOf("천호역")),
    StopCoordinate("길동사거리, 강동세무서 버스정류장", 37.534757, 127.135976, listOf("길동사거리", "강동세무서")),
    StopCoordinate("길동주민센터 정류장", 37.533995, 127.142251, listOf("길동주민센터")),
    StopCoordinate("강동자이, 프라자아 아파트 정류장", 37.536309, 127.147884, listOf("강동자이", "프라자아")),
    StopCoordinate("상일 초등학교", 37.545800, 127.170731, listOf("상일초등학교")),
    StopCoordinate("황산사거리", 37.549379, 127.183925),
    StopCoordinate("하남시청역, 장지마을", 37.540494, 127.209302, listOf("하남시청역", "장지마을")),
    StopCoordinate("마두역 4번 출구 앞", 37.652406, 126.777487, listOf("마두역")),
    StopCoordinate("대곡역 중앙차로 서울방면 버스정류장", 37.631463, 126.809570, listOf("대곡역")),
    StopCoordinate("고양경찰서 앞 서울방면 버스정류장", 37.628986, 126.829293, listOf("고양경찰서")),
    StopCoordinate("디지털미디어 시티역 버스정류장", 37.579104, 126.900550, listOf("디지털미디어시티역", "DMC역")),
    StopCoordinate("부평역 대아지하상가 입구 큰맘할매순대국 앞", 37.490471, 126.724432, listOf("부평역")),
    StopCoordinate("범계역 1번 출구 하나증권 앞", 37.391424, 126.952616, listOf("범계역")),
    StopCoordinate("안산중앙역", 37.316289, 126.838909, listOf("중앙역", "안산 중앙역")),
    StopCoordinate("중앙역 1번 출구 좌측 지하도입구 앞", 37.316342, 126.838904),
    StopCoordinate("수원역 9번출구 100m 앞 신한은행 건너편", 37.267110, 127.002678, listOf("수원역")),
    StopCoordinate("장안공원 정류장", 37.288257, 127.012562, listOf("장안공원")),
    StopCoordinate("우만주공 4단지 앞 정류장", 37.292207, 127.030823, listOf("우만주공4단지")),
    StopCoordinate("수지지역난방공사 앞 버스정류장", 37.315226, 127.088272, listOf("수지지역난방공사")),
    StopCoordinate("수지구청", 37.321307, 127.097831),
    StopCoordinate("풍덕천 삼거리버스 정류장 용인포은아트홀", 37.324626, 127.104781, listOf("풍덕천", "용인포은아트홀")),
    StopCoordinate("한신아파트 버스정류장", 37.327413, 127.112706, listOf("한신아파트")),
    StopCoordinate("동부아파트 버스정류장", 37.329704, 127.124042, listOf("동부아파트")),
    StopCoordinate("신갈굴다리 버스 정류장", 37.270645, 127.103139, listOf("신갈굴다리")),
    StopCoordinate("상갈파출소 앞", 37.271638, 127.108938, listOf("상갈파출소")),
    StopCoordinate("기흥역", 37.275720, 127.115947),
    StopCoordinate("강남대역", 37.270254, 127.125991),
    StopCoordinate("쌍용아파트 앞", 37.258760, 127.142342, listOf("쌍용아파트")),
    StopCoordinate("코업호텔 앞", 37.238013, 127.179560, listOf("코업호텔")),
    StopCoordinate("명지대입구", 37.235815, 127.189974),
    StopCoordinate("용인터미널", 37.233804, 127.209275),
    StopCoordinate("용인중앙시장역", 37.237222, 127.208812),
    StopCoordinate("유림동 버스정류장", 37.258633, 127.212802, listOf("유림동")),
    StopCoordinate("학생회관 앞", 37.337455, 127.269280, listOf("학생회관"))
)

private fun fallbackCoordinateFor(stopName: String): StopCoordinate? {
    val key = stopName.normalizedStopKey()
    if (key.isBlank()) return null

    val exact = fallbackStopCoordinates.firstOrNull { coordinate ->
        coordinate.name.normalizedStopKey() == key ||
            coordinate.aliases.any { it.normalizedStopKey() == key }
    }
    if (exact != null) return exact

    return fallbackStopCoordinates.firstOrNull { coordinate ->
        val names = listOf(coordinate.name) + coordinate.aliases
        names.any { candidate ->
            val candidateKey = candidate.normalizedStopKey()
            candidateKey.length >= 2 && (key.contains(candidateKey) || candidateKey.contains(key))
        }
    }
}

private fun canonicalStopDisplayName(stopName: String, coordinate: StopCoordinate?): String {
    val key = stopName.normalizedStopKey()
    val coordinateKey = coordinate?.name?.normalizedStopKey()
    return when {
        key == "도서관" || coordinateKey == "도서관" -> "도서관 앞"
        else -> stopName
    }
}

private fun String.normalizedStopKey(): String {
    return replace("\n", " ")
        .replace(Regex("\\([^)]*\\)"), "")
        .replace("하차지점", "")
        .replace("회차장", "")
        .replace("버스정류장", "")
        .replace("정류장", "")
        .replace("앞", "")
        .replace("입구", "")
        .replace("방면", "")
        .replace("출구", "")
        .replace(Regex("[^0-9A-Za-z가-힣]"), "")
        .uppercase()
        .trim()
}

private fun JsonObject.findString(key: String): String? {
    val element = get(key) ?: return null
    return if (element.isJsonPrimitive) element.asString.takeIf { it.isNotBlank() } else null
}

private fun JsonObject.findDouble(key: String): Double? {
    val element = get(key) ?: return null
    return if (element.isJsonPrimitive) element.asString.toDoubleOrNull() else null
}
