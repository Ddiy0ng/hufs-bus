package com.hufsteam.shuttletrack.ui.driver

import android.app.Application
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.google.gson.JsonElement
import com.hufsteam.shuttletrack.data.remote.TokenStore
import com.hufsteam.shuttletrack.data.remote.dto.BusTagRequest
import com.hufsteam.shuttletrack.data.remote.dto.TimetableResponse
import com.hufsteam.shuttletrack.data.repository.ShuttleRepository
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private const val TAG_GPS = "GPS"
private const val TAG_TAG_API = "TagApi"
private const val TAG_DEPART = "Depart"
private const val TAG_RESTORE = "RestoreRunningState"
private const val TAG_DRIVER_SCREEN = "DriverScreen"
private const val PREFS_DRIVER_OPS = "driver_ops_state"

// ── 상태 열거형 ───────────────────────────────────────────────

enum class OperationState { BEFORE_DEPARTURE, OPERATING, COMPLETED }

enum class BusStatus(val label: String) {
    NORMAL("정상"),
    DELAYED("지연"),
    SUSPENDED("운행중단")
}

// ── 데이터 클래스 ─────────────────────────────────────────────

data class DriverRoute(
    val id: String,
    val routeName: String,
    val scheduledTime: String,
    val totalSeats: Int,
    val stops: List<String>,
    val timetableId: Long? = null,
    val busId: Long? = null
)

data class SeatState(
    val currentSeats: Int = 0,
    val totalSeats: Int = 45,
    val status: String = "WAITING"
) {
    val remainingSeats: Int = (totalSeats - currentSeats).coerceIn(0, totalSeats)
}

// ── ViewModel ────────────────────────────────────────────────

class DriverViewModel(application: Application) : AndroidViewModel(application) {

    private val shuttleRepository = ShuttleRepository()
    private val prefs = application.getSharedPreferences(PREFS_DRIVER_OPS, android.content.Context.MODE_PRIVATE)

    var selectedRoute by mutableStateOf<DriverRoute?>(null)
        private set

    var operationState by mutableStateOf(OperationState.BEFORE_DEPARTURE)
        private set

    var passengerCount by mutableStateOf(0)
        private set

    var actualDepartureTime by mutableStateOf<String?>(null)
        private set

    var isGpsTracking by mutableStateOf(false)
        private set

    var operationMessage by mutableStateOf("운행 전입니다")
        private set

    var lastGpsText by mutableStateOf<String?>(null)
        private set

    var activeBusIdsByTimetableId by mutableStateOf<Map<Long, Long>>(emptyMap())
        private set

    var seatStateByTimetableId by mutableStateOf<Map<Long, SeatState>>(emptyMap())
        private set

    var runningStateByTimetableId by mutableStateOf<Map<Long, Boolean>>(emptyMap())
        private set

    var driverRoutes by mutableStateOf<List<DriverRoute>>(emptyList())
        private set

    var driverRoutesLoading by mutableStateOf(false)
        private set

    var driverRoutesError by mutableStateOf<String?>(null)
        private set

    init {
        restorePersistedState()
        loadDriverRoutes()
    }

    // ── 서버 노선 로드 ────────────────────────────────────────

    fun loadDriverRoutes() {
        viewModelScope.launch {
            driverRoutesLoading = true
            driverRoutesError = null
            shuttleRepository.getTimetable(null)
                .onSuccess { timetables ->
                    val routes = timetables.mapIndexedNotNull { index, dto ->
                        dto.toDriverRoute(index)
                    }
                    driverRoutes = routes
                    driverRoutesLoading = false
                    if (routes.isEmpty()) {
                        driverRoutesError = "배정된 운행 일정이 없습니다"
                    }
                    Log.i(TAG_DRIVER_SCREEN, "loadDriverRoutes count=${routes.size}")
                }
                .onFailure { throwable ->
                    driverRoutesLoading = false
                    driverRoutesError = "운행 일정을 불러오지 못했습니다: ${throwable.message ?: "서버 응답 없음"}"
                    Log.e(TAG_DRIVER_SCREEN, "loadDriverRoutes failed: ${throwable.message}")
                }
        }
    }

    // ── SharedPreferences 복원 ────────────────────────────────

    private fun restorePersistedState() {
        val all = prefs.all
        val restoredBusIds = all.entries
            .filter { it.key.startsWith("busId_") }
            .mapNotNull { (k, v) ->
                val tid = k.removePrefix("busId_").toLongOrNull() ?: return@mapNotNull null
                val bid = when (v) {
                    is Long -> v
                    is Int -> v.toLong()
                    else -> return@mapNotNull null
                }
                tid to bid
            }.toMap()

        val restoredRunning = all.entries
            .filter { it.key.startsWith("running_") }
            .mapNotNull { (k, v) ->
                val tid = k.removePrefix("running_").toLongOrNull() ?: return@mapNotNull null
                tid to (v as? Boolean ?: false)
            }.toMap()

        if (restoredBusIds.isNotEmpty()) activeBusIdsByTimetableId = restoredBusIds
        if (restoredRunning.isNotEmpty()) runningStateByTimetableId = restoredRunning
    }

    private fun persistRunningState(timetableId: Long, isRunning: Boolean, busId: Long?) {
        prefs.edit()
            .putBoolean("running_$timetableId", isRunning)
            .apply()
        if (busId != null) {
            prefs.edit().putLong("busId_$timetableId", busId).apply()
        }
    }

    // ── 노선 선택 ─────────────────────────────────────────────

    fun selectRoute(route: DriverRoute) {
        val tid = route.timetableId()
        val isAlreadyRunning = runningStateByTimetableId[tid] == true
        Log.i(TAG_DRIVER_SCREEN,
            "selectRoute timetableId=$tid routeName=${route.routeName} " +
            "isAlreadyRunning=$isAlreadyRunning")

        if (isAlreadyRunning) {
            // 이미 운행 중이면 상태 초기화하지 않고 복원
            val savedBusId = activeBusIdsByTimetableId[tid] ?: route.busId
            selectedRoute = if (savedBusId != null) route.copy(busId = savedBusId) else route
            val seat = seatStateByTimetableId[tid]
            if (seat != null) {
                operationState = OperationState.OPERATING
                passengerCount = seat.currentSeats.coerceIn(0, seat.totalSeats)
                isGpsTracking = true
                operationMessage = "운행 중 상태입니다"
            } else {
                operationState = OperationState.OPERATING
                isGpsTracking = true
                operationMessage = "운행 중 상태입니다 (여석 조회 중)"
            }
        } else {
            selectedRoute = route
            reset()
        }
    }

    fun clearSelectedRoute() {
        selectedRoute = null
        reset()
    }

    // ── 서버 상태 복원 ────────────────────────────────────────

    fun restoreStateFromServer(timetableId: Long) {
        val savedBusId = activeBusIdsByTimetableId[timetableId]
        Log.i(TAG_DRIVER_SCREEN,
            "enter timetableId=$timetableId saved busId=$savedBusId " +
            "saved runningState=${runningStateByTimetableId[timetableId]}")

        viewModelScope.launch {
            shuttleRepository.getBusStatuses(timetableId)
                .onSuccess { response ->
                    val status = response?.status?.trim()?.uppercase() ?: "UNKNOWN"
                    val busId = response?.busId
                    val currentSeats = response?.currentSeats
                        ?: response?.currentPassengers
                        ?: response?.passengerCount
                    val totalSeats = response?.totalSeats
                        ?: selectedRoute?.totalSeats
                        ?: 45
                    val remaining = currentSeats?.let { (totalSeats - it).coerceIn(0, totalSeats) }

                    Log.i(TAG_RESTORE,
                        "timetableId=$timetableId busId=$busId status=$status " +
                        "currentSeats=$currentSeats totalSeats=$totalSeats remainingSeats=$remaining")

                    when (status) {
                        "RUNNING" -> {
                            runningStateByTimetableId = runningStateByTimetableId + (timetableId to true)
                            if (operationState != OperationState.OPERATING) {
                                operationState = OperationState.OPERATING
                                isGpsTracking = true
                                operationMessage = "서버에서 운행 중 상태를 복원했습니다"
                            }
                            if (currentSeats != null) {
                                passengerCount = currentSeats.coerceIn(0, totalSeats)
                            }
                            if (busId != null) {
                                activeBusIdsByTimetableId = activeBusIdsByTimetableId + (timetableId to busId)
                                selectedRoute = selectedRoute?.copy(busId = busId)
                                persistRunningState(timetableId, true, busId)
                            }
                            seatStateByTimetableId = seatStateByTimetableId + (
                                timetableId to SeatState(
                                    currentSeats = passengerCount,
                                    totalSeats = totalSeats,
                                    status = "RUNNING"
                                )
                            )
                        }
                        "DONE" -> {
                            if (operationState == OperationState.OPERATING) {
                                operationState = OperationState.COMPLETED
                                isGpsTracking = false
                                operationMessage = "운행이 종료된 상태입니다"
                            }
                            runningStateByTimetableId = runningStateByTimetableId + (timetableId to false)
                            persistRunningState(timetableId, false, null)
                        }
                        else -> {
                            // WAITING: 이미 출발 등록된 상태가 아닌 경우만 BEFORE_DEPARTURE로 유지
                            if (operationState == OperationState.BEFORE_DEPARTURE) {
                                operationMessage = "출발 전 상태입니다"
                            }
                        }
                    }
                }
                .onFailure { throwable ->
                    Log.e(TAG_RESTORE, "failed timetableId=$timetableId ${throwable.message}")
                    // 서버 실패 시 기존 상태 유지, 메시지만 변경
                    if (operationMessage == "운행 전입니다") {
                        operationMessage = "서버 상태 확인 실패, 기존 상태 유지"
                    }
                }
        }
    }

    fun resolveTimetableId(): Long = selectedRoute?.timetableId() ?: 1L

    // ── 이미 운행 중인 노선 서버 동기화 ─────────────────────────

    fun syncRunningRouteFromServer(route: DriverRoute, passengers: Int) {
        selectedRoute = route
        operationState = OperationState.OPERATING
        passengerCount = passengers.coerceIn(0, route.totalSeats)
        actualDepartureTime = route.scheduledTime
        isGpsTracking = true
        operationMessage = "서버에서 운행 중인 운행을 불러왔습니다"
        val timetableId = route.timetableId()
        route.busId?.let { busId ->
            activeBusIdsByTimetableId = activeBusIdsByTimetableId + (timetableId to busId)
        }
        runningStateByTimetableId = runningStateByTimetableId + (timetableId to true)
        seatStateByTimetableId = seatStateByTimetableId + (
            timetableId to SeatState(
                currentSeats = passengerCount,
                totalSeats = route.totalSeats,
                status = "RUNNING"
            )
        )
    }

    // ── 출발 등록 ─────────────────────────────────────────────

    fun startOperation(latitude: Double, longitude: Double) {
        val now = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())
        val route = selectedRoute
        val timetableId = route?.timetableId() ?: 1L
        lastGpsText = "%.6f, %.6f".format(Locale.US, latitude, longitude)
        operationMessage = "출발 등록 중입니다"
        Log.d(TAG_DRIVER_SCREEN, "운행 시작, passengerCount=$passengerCount timetableId=$timetableId")

        viewModelScope.launch {
            var departBusId: Long? = null
            var departSucceeded = false
            shuttleRepository.departTimetable(timetableId)
                .onSuccess { depart ->
                    departSucceeded = true
                    departBusId = depart?.busId
                    Log.i(TAG_DEPART,
                        "timetableId=$timetableId busId=${depart?.busId} status=${depart?.busStatus}")
                    actualDepartureTime = depart?.actualDepartureTime?.take(5) ?: now
                    operationMessage = "출발 등록 완료, busId 확인 중입니다"
                    val resolvedBusId = departBusId
                    if (resolvedBusId != null) {
                        selectedRoute = route?.copy(busId = resolvedBusId)
                        activeBusIdsByTimetableId = activeBusIdsByTimetableId + (timetableId to resolvedBusId)
                        runningStateByTimetableId = runningStateByTimetableId + (timetableId to true)
                        persistRunningState(timetableId, true, resolvedBusId)
                    }
                }
                .onFailure { throwable ->
                    Log.e(TAG_DEPART, "timetableId=$timetableId failed: ${throwable.message}")
                    operationMessage = "출발 등록 실패: ${throwable.message ?: "서버 응답 확인 필요"}"
                }
            if (!departSucceeded) return@launch

            val busId = departBusId
                ?: activeBusIdsByTimetableId[timetableId]
                ?: shuttleRepository.getBusStatuses(timetableId).getOrNull()?.busId
                ?: route?.busId
            if (busId == null) {
                operationMessage = "출발 등록 완료, busId 조회 실패로 운행 시작을 보류했습니다"
                Log.w(TAG_GPS, "startOperation skipped: busId is null, timetableId=$timetableId")
                return@launch
            }
            selectedRoute = route?.copy(busId = busId)
            activeBusIdsByTimetableId = activeBusIdsByTimetableId + (timetableId to busId)
            runningStateByTimetableId = runningStateByTimetableId + (timetableId to true)
            persistRunningState(timetableId, true, busId)
            operationState = OperationState.OPERATING
            isGpsTracking = true
            actualDepartureTime = actualDepartureTime ?: now
            operationMessage = "운행 시작 완료, GPS 위치를 전송 중입니다"

            shuttleRepository.postDriverLocation(
                busId = busId,
                latitude = latitude,
                longitude = longitude
            ).onSuccess {
                operationMessage = "GPS 위치가 서버에 전송되었습니다"
                Log.i(TAG_GPS,
                    "timetableId=$timetableId busId=$busId latitude=$latitude longitude=$longitude responseCode=200")
            }.onFailure { throwable ->
                operationMessage = "GPS 전송 실패: ${throwable.message ?: "서버 응답을 확인해 주세요"}"
                Log.e(TAG_GPS,
                    "timetableId=$timetableId busId=$busId latitude=$latitude longitude=$longitude errorBody=${throwable.message}")
            }
        }
    }

    // ── GPS 주기 전송 ─────────────────────────────────────────

    fun sendCurrentLocation(latitude: Double, longitude: Double) {
        val route = selectedRoute ?: return
        if (operationState != OperationState.OPERATING) return

        val timetableId = route.timetableId()
        lastGpsText = "%.6f, %.6f".format(Locale.US, latitude, longitude)
        viewModelScope.launch {
            val busId = route.busId
                ?: activeBusIdsByTimetableId[timetableId]
                ?: shuttleRepository.getBusStatuses(timetableId)
                    .getOrNull()
                    ?.busId
            if (busId == null) {
                Log.w(TAG_GPS, "timetableId=$timetableId busId=null, skipping location update")
                return@launch
            }
            if (route.busId == null) {
                selectedRoute = route.copy(busId = busId)
            }
            activeBusIdsByTimetableId = activeBusIdsByTimetableId + (timetableId to busId)

            shuttleRepository.postDriverLocation(
                busId = busId,
                latitude = latitude,
                longitude = longitude
            ).onSuccess {
                operationMessage = "GPS 위치가 서버에 갱신되었습니다"
                Log.i(TAG_GPS,
                    "timetableId=$timetableId busId=$busId latitude=$latitude longitude=$longitude responseCode=200")
            }.onFailure { throwable ->
                operationMessage = "GPS 갱신 실패: ${throwable.message ?: "서버 응답을 확인해 주세요"}"
                Log.e(TAG_GPS,
                    "timetableId=$timetableId busId=$busId latitude=$latitude longitude=$longitude errorBody=${throwable.message}")
            }
        }
    }

    fun setGpsStartError(message: String) {
        operationMessage = message
    }

    fun updateOperationMessage(message: String) {
        operationMessage = message
    }

    // ── 서버 좌석 상태 갱신 ───────────────────────────────────

    fun refreshPassengerStateFromServer(showMessage: Boolean = false) {
        val route = selectedRoute ?: return
        val timetableId = route.timetableId()
        viewModelScope.launch {
            shuttleRepository.getBusStatuses(timetableId)
                .onSuccess { response ->
                    val total = response?.totalSeats ?: route.totalSeats
                    val passengers = response?.currentSeats
                        ?: response?.currentPassengers
                        ?: response?.passengerCount
                    if (passengers != null) {
                        passengerCount = passengers.coerceIn(0, total)
                    }
                    seatStateByTimetableId = seatStateByTimetableId + (
                        timetableId to SeatState(
                            currentSeats = passengerCount,
                            totalSeats = total,
                            status = response?.status ?: operationState.toApiStatus()
                        )
                    )
                    selectedRoute = route.copy(
                        totalSeats = total,
                        busId = response?.busId ?: route.busId
                    )
                    if (showMessage) {
                        operationMessage = "서버 좌석 상태를 새로고침했습니다"
                    }
                }
                .onFailure { throwable ->
                    if (showMessage) {
                        operationMessage = "좌석 상태 새로고침 실패: ${throwable.message ?: "서버 응답 확인 필요"}"
                    }
                }
        }
    }

    // ── 운행 종료 ─────────────────────────────────────────────

    fun endOperation() {
        operationState = OperationState.COMPLETED
        isGpsTracking = false
        operationMessage = "운행이 종료되어 GPS 전송이 중지되었습니다"
        val timetableId = selectedRoute?.timetableId() ?: return
        runningStateByTimetableId = runningStateByTimetableId + (timetableId to false)
        persistRunningState(timetableId, false, null)
    }

    // ── 상차/하차 수기 집계 ───────────────────────────────────

    fun increasePassengers() {
        val route = selectedRoute ?: return
        val total = route.totalSeats
        when {
            operationState != OperationState.OPERATING -> {
                operationMessage = "운행 중에만 탑승 수를 집계할 수 있습니다"
            }
            passengerCount >= total -> {
                operationMessage = "탑승 수가 총 좌석 수를 초과할 수 없습니다"
            }
            else -> syncPassengerTag(type = "BOARD")
        }
    }

    fun decreasePassengers() {
        val route = selectedRoute ?: return
        when {
            operationState != OperationState.OPERATING -> {
                operationMessage = "운행 중에만 하차 수를 집계할 수 있습니다"
            }
            passengerCount <= 0 -> {
                operationMessage = "현재 탑승 수가 0명이라 하차 처리할 수 없습니다"
            }
            else -> syncPassengerTag(type = "ALIGHT")
        }
    }

    fun reset() {
        operationState      = OperationState.BEFORE_DEPARTURE
        passengerCount      = 0
        actualDepartureTime = null
        isGpsTracking       = false
        operationMessage    = "운행 전입니다"
        lastGpsText         = null
    }

    // ── 탑승/하차 서버 반영 ───────────────────────────────────

    private fun syncPassengerTag(type: String) {
        val route = selectedRoute ?: return
        val timetableId = route.timetableId()
        val token = TokenStore.accessToken
        val role = TokenStore.role
        val email = TokenStore.email

        Log.i(TAG_TAG_API,
            "timetableId=$timetableId type=$type currentRole=$role currentEmail=$email " +
            "hasAccessToken=${!token.isNullOrBlank()} requestUrl=/api/buses/$timetableId/tags")
        Log.d(TAG_DRIVER_SCREEN, "click $type: passengerCount 변경 전=$passengerCount")

        if (token.isNullOrBlank()) {
            operationMessage = "로그인이 만료되었습니다. 다시 로그인해주세요."
            Log.w(TAG_TAG_API, "timetableId=$timetableId type=$type aborted: no accessToken")
            return
        }

        operationMessage = if (type == "BOARD") "탑승 수를 서버에 반영 중입니다"
                          else "하차 수를 서버에 반영 중입니다"

        viewModelScope.launch {
            shuttleRepository.postBusTag(timetableId, BusTagRequest(type = type))
                .onSuccess { response ->
                    Log.i(TAG_TAG_API,
                        "timetableId=$timetableId tagType=${response?.tagType} responseCode=200")
                    // 태그 성공 후 seats API로 정확한 상태 재조회
                    shuttleRepository.getBusStatuses(timetableId)
                        .onSuccess { seatResponse ->
                            val total = seatResponse?.totalSeats
                                ?: response?.totalSeats
                                ?: route.totalSeats
                            val current = seatResponse?.currentSeats
                                ?: seatResponse?.currentPassengers
                                ?: seatResponse?.passengerCount
                                ?: response?.currentSeats
                            if (current != null) {
                                passengerCount = current.coerceIn(0, total)
                            }
                            Log.d(TAG_DRIVER_SCREEN, "click $type: passengerCount 변경 후=$passengerCount (GPS 전송은 별도, 좌석 수에 영향 없음)")
                            val busId = seatResponse?.busId ?: response?.busId ?: route.busId
                            selectedRoute = route.copy(totalSeats = total, busId = busId)
                            busId?.let {
                                activeBusIdsByTimetableId = activeBusIdsByTimetableId + (timetableId to it)
                            }
                            seatStateByTimetableId = seatStateByTimetableId + (
                                timetableId to SeatState(
                                    currentSeats = passengerCount,
                                    totalSeats = total,
                                    status = operationState.toApiStatus()
                                )
                            )
                        }
                    operationMessage = if (type == "BOARD") "탑승 수가 서버에 반영되었습니다"
                                      else "하차 수가 서버에 반영되었습니다"
                }
                .onFailure { throwable ->
                    operationMessage = "서버 반영 실패: ${throwable.message ?: "서버 응답 확인 필요"}"
                    Log.e(TAG_TAG_API,
                        "timetableId=$timetableId type=$type errorBody=${throwable.message}")
                }
        }
    }
}

private fun OperationState.toApiStatus(): String {
    return when (this) {
        OperationState.BEFORE_DEPARTURE -> "WAITING"
        OperationState.OPERATING -> "RUNNING"
        OperationState.COMPLETED -> "DONE"
    }
}

private fun DriverRoute.timetableId(): Long {
    return timetableId ?: id.digitsAsLong() ?: busId ?: 1L
}

private fun String.digitsAsLong(): Long? {
    return filter { it.isDigit() }.takeIf { it.isNotBlank() }?.toLongOrNull()
}

private fun TimetableResponse.toDriverRoute(index: Int): DriverRoute? {
    val tid = listOfNotNull(timetableId, specificTimetableId, id).firstOrNull() ?: return null
    val stopNames = (routeList ?: stops)?.mapIndexedNotNull { i, el ->
        when {
            el.isJsonPrimitive -> el.asString
            el.isJsonObject -> {
                val obj = el.asJsonObject
                listOf("name", "stopName", "busStopName")
                    .firstNotNullOfOrNull { key ->
                        obj.get(key)?.takeIf { it.isJsonPrimitive }?.asString
                    } ?: "정류장${i + 1}"
            }
            else -> null
        }
    }.orEmpty()
    val name = listOfNotNull(routeName, route)
        .firstOrNull { it.isNotBlank() }
        ?: listOfNotNull(startStop, endStop).filter { it.isNotBlank() }.joinToString(" → ")
            .takeIf { it.isNotBlank() }
        ?: "노선 ${index + 1}"
    val scheduledTime = listOfNotNull(departureTime, departAt, this.time, plannedDeparture)
        .firstOrNull { it.isNotBlank() } ?: "00:00"
    return DriverRoute(
        id = "timetable_$tid",
        routeName = name,
        scheduledTime = scheduledTime,
        totalSeats = 45,
        stops = stopNames,
        timetableId = tid
    )
}
