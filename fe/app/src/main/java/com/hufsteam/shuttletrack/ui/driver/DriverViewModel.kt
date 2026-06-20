package com.hufsteam.shuttletrack.ui.driver

import android.app.Application
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
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

    private val locallyFinishedTimetableIds = mutableSetOf<Long>()

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
                    Log.e(TAG_DRIVER_SCREEN, "loadDriverRoutes failed: ${throwable.message}", throwable)
                }
        }
    }

// ── SharedPreferences 복원 ────────────────────────────────

    private fun restorePersistedState() {
        val all = prefs.all

        val restoredBusIds = all.entries
            .filter { it.key.startsWith("busId_") }
            .mapNotNull { (key, value) ->
                val timetableId = key.removePrefix("busId_").toLongOrNull() ?: return@mapNotNull null
                val busId = when (value) {
                    is Long -> value
                    is Int -> value.toLong()
                    else -> return@mapNotNull null
                }
                timetableId to busId
            }
            .toMap()

        val restoredRunning = all.entries
            .filter { it.key.startsWith("running_") }
            .mapNotNull { (key, value) ->
                val timetableId = key.removePrefix("running_").toLongOrNull() ?: return@mapNotNull null
                timetableId to (value as? Boolean ?: false)
            }
            .toMap()

        if (restoredBusIds.isNotEmpty()) {
            activeBusIdsByTimetableId = restoredBusIds
        }

        if (restoredRunning.isNotEmpty()) {
            runningStateByTimetableId = restoredRunning
        }

        all.entries
            .filter { it.key.startsWith("finished_") }
            .forEach { (key, value) ->
                val id = key.removePrefix("finished_").toLongOrNull() ?: return@forEach
                if (value == true) locallyFinishedTimetableIds.add(id)
            }
    }

    private fun persistRunningState(timetableId: Long, isRunning: Boolean, busId: Long?) {
        prefs.edit().apply {
            putBoolean("running_$timetableId", isRunning)

            if (busId != null) {
                putLong("busId_$timetableId", busId)
            } else {
                remove("busId_$timetableId")
            }
        }.apply()
    }

// ── 노선 선택 ─────────────────────────────────────────────

    fun selectRoute(route: DriverRoute) {
        val timetableId = route.timetableId()
        val isAlreadyRunning = runningStateByTimetableId[timetableId] == true

        Log.i(
            TAG_DRIVER_SCREEN,
            "selectRoute timetableId=$timetableId routeName=${route.routeName} isAlreadyRunning=$isAlreadyRunning"
        )

        selectedRoute = if (isAlreadyRunning) {
            val savedBusId = activeBusIdsByTimetableId[timetableId] ?: route.busId
            if (savedBusId != null) route.copy(busId = savedBusId) else route
        } else {
            route
        }

        if (isAlreadyRunning) {
            operationState = OperationState.OPERATING
            isGpsTracking = true

            val seat = seatStateByTimetableId[timetableId]
            if (seat != null) {
                passengerCount = seat.currentSeats.coerceIn(0, seat.totalSeats)
                operationMessage = "운행 중 상태입니다"
            } else {
                operationMessage = "운행 중 상태입니다 (여석 조회 중)"
            }
        } else {
            reset()
        }

        restoreStateFromServer(timetableId)
    }

    fun clearSelectedRoute() {
        selectedRoute = null
        reset()
    }

// ── 서버 상태 복원 ────────────────────────────────────────

    fun restoreStateFromServer(timetableId: Long) {
        val savedBusId = activeBusIdsByTimetableId[timetableId]

        Log.i(
            TAG_DRIVER_SCREEN,
            "enter timetableId=$timetableId saved busId=$savedBusId saved runningState=${runningStateByTimetableId[timetableId]}"
        )

        viewModelScope.launch {
            shuttleRepository.getBusStatuses(timetableId)
                .onSuccess { response ->
                    val status = response?.status?.trim()?.uppercase() ?: "UNKNOWN"
                    val busId = response?.busId
                    val totalSeats = response?.totalSeats ?: selectedRoute?.totalSeats ?: 45
                    val currentSeats: Int = if (status == "RUNNING") {
                        response?.currentSeats
                            ?: response?.currentPassengers
                            ?: response?.passengerCount
                            ?: passengerCount
                    } else {
                        response?.currentSeats ?: response?.currentPassengers ?: response?.passengerCount ?: 0
                    }

                    val safeCurrentSeats = currentSeats.coerceIn(0, totalSeats)
                    val remainingSeats = (totalSeats - safeCurrentSeats).coerceIn(0, totalSeats)

                    Log.i(
                        TAG_RESTORE,
                        "timetableId=$timetableId busId=$busId status=$status currentSeats=$safeCurrentSeats totalSeats=$totalSeats remainingSeats=$remainingSeats"
                    )

                    seatStateByTimetableId = seatStateByTimetableId + (
                            timetableId to SeatState(
                                currentSeats = safeCurrentSeats,
                                totalSeats = totalSeats,
                                status = status
                            )
                            )

                    passengerCount = safeCurrentSeats

                    if (busId != null) {
                        activeBusIdsByTimetableId = activeBusIdsByTimetableId + (timetableId to busId)
                        selectedRoute = selectedRoute?.copy(busId = busId, totalSeats = totalSeats)
                    } else {
                        selectedRoute = selectedRoute?.copy(totalSeats = totalSeats)
                    }

                    when (status) {
                        "RUNNING" -> {
                            val wasLocallyFinished = locallyFinishedTimetableIds.contains(timetableId) ||
                                prefs.getBoolean("finished_$timetableId", false)
                            if (wasLocallyFinished) {
                                Log.i(TAG_RESTORE,
                                    "timetableId=$timetableId server=RUNNING but locally finished - forcing DONE")
                                operationState = OperationState.COMPLETED
                                isGpsTracking = false
                                runningStateByTimetableId = runningStateByTimetableId + (timetableId to false)
                                activeBusIdsByTimetableId = activeBusIdsByTimetableId - timetableId
                                operationMessage = "운행이 종료된 상태입니다"
                                persistRunningState(timetableId, false, null)
                            } else {
                                operationState = OperationState.OPERATING
                                isGpsTracking = true
                                runningStateByTimetableId = runningStateByTimetableId + (timetableId to true)
                                operationMessage = "서버에서 운행 중 상태를 복원했습니다"
                                if (busId != null) {
                                    persistRunningState(timetableId, true, busId)
                                }
                            }
                        }

                        "DONE" -> {
                            operationState = OperationState.COMPLETED
                            isGpsTracking = false
                            runningStateByTimetableId = runningStateByTimetableId + (timetableId to false)
                            activeBusIdsByTimetableId = activeBusIdsByTimetableId - timetableId
                            operationMessage = "운행이 종료된 상태입니다"
                            persistRunningState(timetableId, false, null)
                        }

                        "WAITING" -> {
                            if (runningStateByTimetableId[timetableId] != true) {
                                operationState = OperationState.BEFORE_DEPARTURE
                                passengerCount = 0
                                isGpsTracking = false
                                operationMessage = "출발 전 상태입니다"
                            }
                        }

                        else -> {
                            if (runningStateByTimetableId[timetableId] == true) {
                                operationState = OperationState.OPERATING
                                isGpsTracking = true
                                operationMessage = "저장된 운행 중 상태를 유지합니다"
                            } else {
                                operationState = OperationState.BEFORE_DEPARTURE
                                isGpsTracking = false
                                operationMessage = "출발 전 상태입니다"
                            }
                        }
                    }
                }
                .onFailure { throwable ->
                    Log.e(TAG_RESTORE, "failed timetableId=$timetableId ${throwable.message}", throwable)

                    if (runningStateByTimetableId[timetableId] == true) {
                        operationState = OperationState.OPERATING
                        isGpsTracking = true
                        operationMessage = "서버 상태 확인 실패, 저장된 운행 상태를 유지합니다"
                    } else if (operationMessage == "운행 전입니다") {
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
            persistRunningState(timetableId, true, busId)
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

// ── 출발 등록 / 운행 종료 ──────────────────────────────────

    fun startOperation(latitude: Double, longitude: Double) {
        val now = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())
        val route = selectedRoute ?: run {
            operationMessage = "선택된 노선이 없습니다"
            return
        }
        val timetableId = route.timetableId()

        viewModelScope.launch {
            val isCurrentlyRunning = runningStateByTimetableId[timetableId] == true

            Log.d(
                TAG_DRIVER_SCREEN,
                "운행 버튼 클릭, timetableId=$timetableId isCurrentlyRunning=$isCurrentlyRunning passengerCount=$passengerCount"
            )

            if (isCurrentlyRunning) {
                operationMessage = "운행 종료 처리 중입니다"

                val finish = shuttleRepository.finishTimetable(timetableId)
                    .getOrElse { throwable ->
                        Log.e(
                            TAG_DEPART,
                            "finish failed: timetableId=$timetableId message=${throwable.message}",
                            throwable
                        )
                        operationMessage = "운행 종료 실패: ${throwable.message ?: "서버 응답 확인 필요"}"
                        return@launch
                    }

                Log.i(
                    TAG_DEPART,
                    "finish success: timetableId=$timetableId busId=${finish?.busId} status=${finish?.busStatus}"
                )

                operationState = OperationState.COMPLETED
                isGpsTracking = false
                operationMessage = "운행 종료 완료"

                runningStateByTimetableId =
                    runningStateByTimetableId + (timetableId to false)

                activeBusIdsByTimetableId =
                    activeBusIdsByTimetableId - timetableId

                persistRunningState(timetableId, false, null)
                locallyFinishedTimetableIds.add(timetableId)
                prefs.edit().putBoolean("finished_$timetableId", true).apply()

                finish?.busId?.let { finishedBusId ->
                    selectedRoute = route.copy(busId = finishedBusId)
                }

                restoreStateFromServer(timetableId)

                return@launch
            }

            operationMessage = "출발 등록 및 GPS 전송 중입니다"

            val depart = shuttleRepository.departTimetable(timetableId)
                .getOrElse { throwable ->
                    Log.e(
                        TAG_DEPART,
                        "depart failed: timetableId=$timetableId message=${throwable.message}",
                        throwable
                    )
                    isGpsTracking = false
                    operationMessage = "출발 등록 실패: ${throwable.message ?: "서버 응답 확인 필요"}"
                    return@launch
                }

            Log.i(
                TAG_DEPART,
                "depart success: timetableId=$timetableId busId=${depart?.busId} status=${depart?.busStatus}"
            )

            val busId = depart?.busId
                ?: activeBusIdsByTimetableId[timetableId]
                ?: shuttleRepository.getBusStatuses(timetableId).getOrNull()?.busId
                ?: route.busId

            if (busId == null) {
                isGpsTracking = false
                operationMessage = "출발 등록은 완료됐지만 busId를 받지 못했습니다. GPS 전송을 보류합니다."
                Log.w(TAG_GPS, "startOperation skipped: busId is null, timetableId=$timetableId")
                return@launch
            }

            selectedRoute = route.copy(busId = busId)

            activeBusIdsByTimetableId =
                activeBusIdsByTimetableId + (timetableId to busId)

            runningStateByTimetableId =
                runningStateByTimetableId + (timetableId to true)

            persistRunningState(timetableId, true, busId)
            locallyFinishedTimetableIds.remove(timetableId)
            prefs.edit().remove("finished_$timetableId").apply()

            operationState = OperationState.OPERATING
            isGpsTracking = true
            lastGpsText = "%.6f, %.6f".format(Locale.US, latitude, longitude)
            actualDepartureTime = depart?.actualDepartureTime?.take(5) ?: now
            operationMessage = "출발 등록 완료, GPS 위치를 전송 중입니다"

            shuttleRepository.postDriverLocation(
                busId = busId,
                latitude = latitude,
                longitude = longitude
            ).onSuccess {
                operationMessage = "GPS 위치가 서버에 전송되었습니다"
                Log.i(
                    TAG_GPS,
                    "timetableId=$timetableId busId=$busId latitude=$latitude longitude=$longitude responseCode=200"
                )
            }.onFailure { throwable ->
                operationMessage = "GPS 전송 실패: ${throwable.message ?: "서버 응답을 확인해 주세요"}"
                Log.e(
                    TAG_GPS,
                    "timetableId=$timetableId busId=$busId latitude=$latitude longitude=$longitude errorBody=${throwable.message}",
                    throwable
                )
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
                Log.i(
                    TAG_GPS,
                    "timetableId=$timetableId busId=$busId latitude=$latitude longitude=$longitude responseCode=200"
                )
            }.onFailure { throwable ->
                operationMessage = "GPS 갱신 실패: ${throwable.message ?: "서버 응답을 확인해 주세요"}"
                Log.e(
                    TAG_GPS,
                    "timetableId=$timetableId busId=$busId latitude=$latitude longitude=$longitude errorBody=${throwable.message}",
                    throwable
                )
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

                    val busId = response?.busId ?: route.busId

                    seatStateByTimetableId = seatStateByTimetableId + (
                            timetableId to SeatState(
                                currentSeats = passengerCount,
                                totalSeats = total,
                                status = response?.status ?: operationState.toApiStatus()
                            )
                            )

                    selectedRoute = route.copy(
                        totalSeats = total,
                        busId = busId
                    )

                    if (busId != null) {
                        activeBusIdsByTimetableId = activeBusIdsByTimetableId + (timetableId to busId)
                    }

                    Log.i(
                        TAG_RESTORE,
                        "refresh seats timetableId=$timetableId busId=$busId total=$total current=$passengerCount remaining=${total - passengerCount}"
                    )

                    if (showMessage) {
                        operationMessage = "서버 좌석 상태를 새로고침했습니다"
                    }
                }
                .onFailure { throwable ->
                    Log.e(TAG_RESTORE, "refresh seats failed timetableId=$timetableId ${throwable.message}", throwable)

                    if (showMessage) {
                        operationMessage = "좌석 상태 새로고침 실패: ${throwable.message ?: "서버 응답 확인 필요"}"
                    }
                }
        }
    }

// ── 운행 종료 ─────────────────────────────────────────────

    fun endOperation() {
        val route = selectedRoute ?: return
        val timetableId = route.timetableId()

        viewModelScope.launch {
            operationMessage = "운행 종료 처리 중입니다"

            shuttleRepository.finishTimetable(timetableId)
                .onSuccess { response ->
                    Log.i(
                        TAG_DEPART,
                        "endOperation finish success timetableId=$timetableId busId=${response?.busId} status=${response?.busStatus}"
                    )

                    operationState = OperationState.COMPLETED
                    isGpsTracking = false
                    operationMessage = "운행이 종료되어 GPS 전송이 중지되었습니다"

                    runningStateByTimetableId =
                        runningStateByTimetableId + (timetableId to false)

                    activeBusIdsByTimetableId =
                        activeBusIdsByTimetableId - timetableId

                    persistRunningState(timetableId, false, null)
                    locallyFinishedTimetableIds.add(timetableId)
                    prefs.edit().putBoolean("finished_$timetableId", true).apply()

                    refreshPassengerStateFromServer(showMessage = false)
                }
                .onFailure { throwable ->
                    operationMessage = "운행 종료 실패: ${throwable.message ?: "서버 응답 확인 필요"}"
                    Log.e(
                        TAG_DEPART,
                        "endOperation finish failed timetableId=$timetableId ${throwable.message}",
                        throwable
                    )
                }
        }
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
        selectedRoute ?: return

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
        operationState = OperationState.BEFORE_DEPARTURE
        passengerCount = 0
        actualDepartureTime = null
        isGpsTracking = false
        operationMessage = "운행 전입니다"
        lastGpsText = null
    }

    fun resetForNewDeparture() {
        val timetableId = selectedRoute?.timetableId() ?: return
        locallyFinishedTimetableIds.remove(timetableId)
        prefs.edit().remove("finished_$timetableId").apply()
        runningStateByTimetableId = runningStateByTimetableId + (timetableId to false)
        reset()
    }

// ── 탑승/하차 서버 반영 ───────────────────────────────────

    private fun syncPassengerTag(type: String) {
        val route = selectedRoute ?: return
        val timetableId = route.timetableId()
        val token = TokenStore.accessToken
        val role = TokenStore.role
        val email = TokenStore.email

        Log.i(
            TAG_TAG_API,
            "timetableId=$timetableId type=$type currentRole=$role currentEmail=$email " +
                    "hasAccessToken=${!token.isNullOrBlank()} requestUrl=/api/buses/$timetableId/tags"
        )
        Log.d(TAG_DRIVER_SCREEN, "click $type: passengerCount 변경 전=$passengerCount")

        if (token.isNullOrBlank()) {
            operationMessage = "로그인이 만료되었습니다. 다시 로그인해주세요."
            Log.w(TAG_TAG_API, "timetableId=$timetableId type=$type aborted: no accessToken")
            return
        }

        operationMessage = if (type == "BOARD") {
            "탑승 수를 서버에 반영 중입니다"
        } else {
            "하차 수를 서버에 반영 중입니다"
        }

        viewModelScope.launch {
            shuttleRepository.postBusTag(timetableId, BusTagRequest(type = type))
                .onSuccess { response ->
                    Log.i(
                        TAG_TAG_API,
                        "timetableId=$timetableId tagType=${response?.tagType} responseCode=200"
                    )

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

                            val busId = seatResponse?.busId ?: response?.busId ?: route.busId

                            selectedRoute = route.copy(totalSeats = total, busId = busId)

                            if (busId != null) {
                                activeBusIdsByTimetableId =
                                    activeBusIdsByTimetableId + (timetableId to busId)
                            }

                            seatStateByTimetableId = seatStateByTimetableId + (
                                    timetableId to SeatState(
                                        currentSeats = passengerCount,
                                        totalSeats = total,
                                        status = seatResponse?.status ?: operationState.toApiStatus()
                                    )
                                    )

                            Log.d(
                                TAG_DRIVER_SCREEN,
                                "click $type: passengerCount 변경 후=$passengerCount remaining=${total - passengerCount}"
                            )
                        }
                        .onFailure { throwable ->
                            Log.e(
                                TAG_TAG_API,
                                "seat refresh failed timetableId=$timetableId ${throwable.message}",
                                throwable
                            )
                        }

                    operationMessage = if (type == "BOARD") {
                        "탑승 수가 서버에 반영되었습니다"
                    } else {
                        "하차 수가 서버에 반영되었습니다"
                    }
                }
                .onFailure { throwable ->
                    operationMessage = "서버 반영 실패: ${throwable.message ?: "서버 응답 확인 필요"}"
                    Log.e(
                        TAG_TAG_API,
                        "timetableId=$timetableId type=$type errorBody=${throwable.message}",
                        throwable
                    )
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
    val timetableId = listOfNotNull(timetableId, specificTimetableId, id).firstOrNull() ?: return null


    val stopNames = (routeList ?: stops)?.mapIndexedNotNull { index, element ->
        when {
            element.isJsonPrimitive -> element.asString

            element.isJsonObject -> {
                val obj = element.asJsonObject
                listOf("name", "stopName", "busStopName")
                    .firstNotNullOfOrNull { key ->
                        obj.get(key)?.takeIf { it.isJsonPrimitive }?.asString
                    } ?: "정류장${index + 1}"
            }

            else -> null
        }
    }.orEmpty()

    val routeName = listOfNotNull(routeName, route)
        .firstOrNull { it.isNotBlank() }
        ?: listOfNotNull(startStop, endStop)
            .filter { it.isNotBlank() }
            .joinToString(" → ")
            .takeIf { it.isNotBlank() }
        ?: "노선 ${index + 1}"

    val scheduledTime = listOfNotNull(departureTime, departAt, this.time, plannedDeparture)
        .firstOrNull { it.isNotBlank() }
        ?: "00:00"

    return DriverRoute(
        id = "timetable_$timetableId",
        routeName = routeName,
        scheduledTime = scheduledTime,
        totalSeats = 45,
        stops = stopNames,
        timetableId = timetableId
    )


}
