package com.hufsteam.shuttletrack.ui.student

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hufsteam.shuttletrack.data.remote.ShuttleApiClient
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import java.net.URLEncoder

private const val OFF_CAMPUS = 1
private const val ON_CAMPUS = 0
private const val FIXED_TOTAL_SEATS = 45

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
    val stopInfos: Map<String, StopArrivalInfo>
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
    val usingMockData: Boolean = true
)

class StudentBusViewModel(
    private val repository: StudentBusRepository = StudentBusRepository()
) : ViewModel() {
    var uiState by mutableStateOf(StudentBusUiState())
        private set

    init {
        refreshSchedules()
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

    fun loadRouteStatus(schedule: BusSchedule) {
        viewModelScope.launch {
            val detail = repository.loadRouteDetail(schedule)
            uiState = uiState.copy(selectedRouteDetail = detail)
        }
    }

    fun saveFavorite(schedule: BusSchedule, days: Set<String>) {
        if (days.isEmpty()) return
        viewModelScope.launch {
            repository.saveFavorite(schedule, days)
            uiState = uiState.copy(
                favoriteSchedules = uiState.favoriteSchedules
                    .filterNot { it.schedule.id == schedule.id } + FavoriteSchedule(schedule, days)
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
    private val apiClient: ShuttleApiClient = ShuttleApiClient
) {
    suspend fun loadSchedules(): ScheduleLoadResult {
        val off = loadScheduleArray("/api/timetable?inOutCampus=OUT_CAMPUS")
            ?: loadScheduleArray("/api/timetables?campus=OFF_CAMPUS")
            ?: loadScheduleArray("/api/schedules?campus=OFF_CAMPUS")
        val on = loadScheduleArray("/api/timetable?inOutCampus=IN_CAMPUS")
            ?: loadScheduleArray("/api/timetables?campus=ON_CAMPUS")
            ?: loadScheduleArray("/api/schedules?campus=ON_CAMPUS")

        val offSchedules = off?.toSchedules() ?: mockOffCampusSchedules
        val onSchedules = on?.toSchedules() ?: mockOnCampusSchedules
        val usingMock = off == null && on == null

        return ScheduleLoadResult(
            offCampusSchedules = offSchedules,
            onCampusSchedules = onSchedules,
            usingMockData = usingMock,
            errorMessage = if (usingMock) "백엔드 연결 전이라 임시 데이터로 표시 중입니다." else null
        )
    }

    suspend fun loadRouteDetail(schedule: BusSchedule): RouteDetail {
        if (schedule.routeStops.isNotEmpty()) return routeDetailFromSchedule(schedule)

        val encodedRoute = URLEncoder.encode(schedule.routeName, "UTF-8")
        val response = apiClient.getObject("/api/routes/$encodedRoute/status?scheduleId=${schedule.id}")
            ?.payloadObject()
            ?: apiClient.getObject("/api/bus/status?scheduleId=${schedule.id}")?.payloadObject()
        return response?.toRouteDetail(schedule) ?: mockRouteDetailFor(schedule)
    }

    suspend fun saveFavorite(schedule: BusSchedule, days: Set<String>): Boolean {
        val body = JSONObject().apply {
            put("timetableId", schedule.id)
            put("days", JSONArray(days.map(::toApiDay)))
        }
        return apiClient.postObject("/api/favorite", body)
            || apiClient.postObject("/api/favorites", body)
    }

    private suspend fun loadScheduleArray(path: String): JSONArray? {
        return apiClient.getArray(path) ?: apiClient.getObject(path)?.payloadArray()
    }

    private fun JSONObject.payloadArray(): JSONArray? {
        return optJSONArray("data")
            ?: optJSONArray("items")
            ?: optJSONArray("schedules")
            ?: optJSONArray("timetables")
            ?: optJSONArray("content")
            ?: optJSONObject("data")?.payloadArray()
    }

    private fun JSONObject.payloadObject(): JSONObject {
        return optJSONObject("data")
            ?: optJSONObject("item")
            ?: optJSONObject("result")
            ?: this
    }

    private fun JSONArray.toSchedules(): List<BusSchedule> = buildList {
        for (index in 0 until length()) {
            val item = optJSONObject(index) ?: continue
            add(item.toSchedule(index))
        }
    }

    private fun JSONObject.toSchedule(index: Int): BusSchedule {
        val routeStops = optJSONArray("routeList")?.toStopPoints()?.map { it.name }.orEmpty()
        val fallbackRoute = if (routeStops.size >= 2) {
            "${routeStops.first()} → ${routeStops.last()}"
        } else {
            val startStop = optStringFlexible("startStop", default = "")
            val endStop = optStringFlexible("endStop", default = "")
            if (startStop.isNotBlank() && endStop.isNotBlank()) "$startStop → $endStop" else "노선"
        }
        val routeName = optStringFlexible("routeName", "route", "name", default = fallbackRoute)
        val remainingSeats = optIntFlexible("remainingSeats", "availableSeats", "seatLeft", default = FIXED_TOTAL_SEATS)
            .coerceIn(0, FIXED_TOTAL_SEATS)
        return BusSchedule(
            id = optIntFlexible("id", "scheduleId", "timetableId", default = index + 1),
            routeName = routeName,
            departureTime = optStringFlexible("departureTime", "departAt", "time", "plannedDeparture", default = "00:00"),
            remainingSeats = remainingSeats,
            totalSeats = FIXED_TOTAL_SEATS,
            currentLocation = optStringFlexible("currentLocation", "location", "currentStop", default = "위치 확인 중입니다"),
            routeStops = routeStops
        )
    }

    private fun JSONObject.toRouteDetail(schedule: BusSchedule): RouteDetail {
        val stopsJson = optJSONArray("stops") ?: optJSONArray("stopNames") ?: optJSONArray("routeList")
        val stopPoints = stopsJson?.toStopPoints().orEmpty()
        val stops = stopPoints.map { it.name }.ifEmpty { mockRouteDetailFor(schedule).stops }
        val currentIndex = optIntFlexible("currentStopIndex", "busStopIndex", "currentIndex", default = (stops.lastIndex - 1).coerceAtLeast(0))
        val busLatitude = optDoubleFlexibleOrNull("busLatitude", "busLat", "vehicleLatitude", "latitude", "lat")
        val busLongitude = optDoubleFlexibleOrNull("busLongitude", "busLng", "vehicleLongitude", "longitude", "lng")
        val coordinateProgress = estimateProgressFromCoordinates(stopPoints, busLatitude, busLongitude)
        val progressIndex = optFloatFlexibleOrNull(
            "busProgressIndex",
            "currentProgressIndex",
            "routeProgressIndex",
            "progressIndex"
        ) ?: coordinateProgress ?: currentIndex.toFloat()
        val planned = optStringFlexible("plannedDeparture", "departureTime", default = schedule.departureTime)
        val actual = optStringFlexible("actualDeparture", "actualTime", default = "미정")
        val eta = optStringFlexible("etaText", "estimatedMinutes", "etaMinutes", default = "03분")
        val infos = stops.associate { stop ->
            val normalized = stop.replace("\n", " ")
            normalized to StopArrivalInfo(
                stopName = normalized,
                arrivalText = optStringFlexible("arrivalText", "arrivalInfo", default = "약 3분 후 도착"),
                seatText = optStringFlexible("seatText", "remainingSeats", default = "${schedule.remainingSeats.toString().padStart(2, '0')}석")
            )
        }
        return RouteDetail(stops, currentIndex.coerceIn(0, stops.lastIndex), progressIndex.coerceIn(0f, stops.lastIndex.toFloat()), planned, actual, eta, infos)
    }

    private fun routeDetailFromSchedule(schedule: BusSchedule): RouteDetail {
        val stops = schedule.routeStops
        val currentIndex = (stops.lastIndex - 1).coerceAtLeast(0)
        val infos = stops.associate { stop ->
            val normalized = stop.replace("\n", " ")
            normalized to StopArrivalInfo(
                stopName = normalized,
                arrivalText = "약 3분 후 도착",
                seatText = "${schedule.remainingSeats.toString().padStart(2, '0')}석"
            )
        }
        return RouteDetail(
            stops = stops,
            currentStopIndex = currentIndex,
            busProgressIndex = currentIndex.toFloat(),
            plannedDeparture = schedule.departureTime,
            actualDeparture = "미정",
            etaText = "03분",
            stopInfos = infos
        )
    }

    private fun JSONArray.toStopPoints(): List<RouteStopPoint> = buildList {
        for (index in 0 until length()) {
            val value = opt(index)
            when (value) {
                is String -> add(RouteStopPoint(value, null, null))
                is JSONObject -> add(
                    RouteStopPoint(
                        name = value.optStringFlexible("name", "stopName", default = "정류장${index + 1}"),
                        latitude = value.optDoubleFlexibleOrNull("latitude", "lat", "stopLatitude", "stopLat"),
                        longitude = value.optDoubleFlexibleOrNull("longitude", "lng", "stopLongitude", "stopLng")
                    )
                )
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

    private fun JSONObject.optStringFlexible(vararg keys: String, default: String): String {
        keys.forEach { key ->
            if (has(key) && !isNull(key)) return optString(key)
        }
        return default
    }

    private fun toApiDay(day: String): String {
        return when (day) {
            "월요일", "MON" -> "MON"
            "화요일", "TUE" -> "TUE"
            "수요일", "WED" -> "WED"
            "목요일", "THU" -> "THU"
            "금요일", "FRI" -> "FRI"
            else -> day
        }
    }

    private fun JSONObject.optIntFlexible(vararg keys: String, default: Int): Int {
        keys.forEach { key ->
            if (has(key) && !isNull(key)) return optString(key).toIntOrNull() ?: optInt(key, default)
        }
        return default
    }

    private fun JSONObject.optFloatFlexibleOrNull(vararg keys: String): Float? {
        keys.forEach { key ->
            if (has(key) && !isNull(key)) return optString(key).toFloatOrNull()
        }
        return null
    }

    private fun JSONObject.optDoubleFlexibleOrNull(vararg keys: String): Double? {
        keys.forEach { key ->
            if (has(key) && !isNull(key)) return optString(key).toDoubleOrNull()
        }
        return null
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

val mockOffCampusSchedules = listOf(
    BusSchedule(1, "경기광주역 → 외대(글)", "08:20", 8, 45, "법학관을 지나고 있습니다"),
    BusSchedule(2, "경기광주역 → 외대(글)", "08:30", 45, 45, "위치 확인 중입니다"),
    BusSchedule(3, "경기광주역 → 외대(글)", "08:40", 44, 45, "내리실 정거장에 접근 중입니다"),
    BusSchedule(4, "경기광주역 → 외대(글)", "08:50", 42, 45, "인문경상관 근처입니다"),
    BusSchedule(9, "외대(글) → 경기광주역", "10:30", 36, 45, "후생관 근처입니다")
)

val mockOnCampusSchedules = listOf(
    BusSchedule(5, "지석묘 → 인문경상관", "09:20", 8, 45, "지석묘 출발"),
    BusSchedule(6, "지석묘 → 인문경상관", "09:30", 45, 45, "위치입니다"),
    BusSchedule(7, "지석묘 → 인문경상관", "09:40", 44, 45, "도서관 근처입니다"),
    BusSchedule(8, "지석묘 → 인문경상관", "09:50", 42, 45, "인문경상관 근처입니다")
)

fun mockRouteDetailFor(schedule: BusSchedule): RouteDetail {
    val stops = when {
        schedule.routeName.startsWith("외대(글)") -> listOf("인문경상관\n(기점)", "교양관", "후생관", "공학관", "백년관", "기숙사", "지석묘\n(종점)")
        schedule.routeName.contains("경기광주역") -> listOf("인경관\n(기점)", "교양관", "후생관", "공학관", "백년관", "기숙사", "지석묘\n(종점)")
        schedule.routeName.contains("지석묘") -> listOf("지석묘\n(기점)", "기숙사", "도서관", "어문관", "인문경상관\n(종점)")
        else -> listOf("정류장1", "정류장2", "정류장3", "정류장4")
    }
    val currentIndex = if (stops.size >= 7) 2 else (stops.lastIndex - 1).coerceAtLeast(1)
    val infos = stops.associate { stop ->
        val normalized = stop.replace("\n", " ")
        normalized to StopArrivalInfo(
            stopName = normalized,
            arrivalText = "약 3분 후 도착",
            seatText = "${schedule.remainingSeats.toString().padStart(2, '0')}석"
        )
    }
    return RouteDetail(
        stops = stops,
        currentStopIndex = currentIndex,
        busProgressIndex = currentIndex.toFloat(),
        plannedDeparture = schedule.departureTime,
        actualDeparture = if (schedule.id == 9) "미정" else "08:32",
        etaText = "03분",
        stopInfos = infos
    )
}
