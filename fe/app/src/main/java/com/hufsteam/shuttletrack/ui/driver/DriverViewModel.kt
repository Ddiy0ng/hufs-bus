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
private const val TOTAL_SEATS = 45

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
    val totalSeats: Int = TOTAL_SEATS,
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

        // 운행 중인 노선의 로컬 탑승 수 복원
        all.entries
            .filter { it.key.startsWith("passengers_") }
            .forEach { (key, value) ->
                val tid = key.removePrefix("passengers_").toLongOrNull() ?: return@forEach
                val count = (value as? Int) ?: return@forEach
                if (restoredRunning[tid] == true) {
                    seatStateByTimetableId = seatStateByTimetableId + (
                        tid to SeatState(currentSeats = count, totalSeats = TOTAL_SEATS, status = "RUNNING")
                    )
                }
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

    private fun persistPassengerCount(timetableId: Long, count: Int) {
        prefs.edit().putInt("passengers_$timetableId", count).apply()
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
            if (seat != null && seat.status == "RUNNING") {
                // 운행 중 로컬 카운트 복원 — status가 RUNNING인 경우만 신뢰
                passengerCount = seat.currentSeats.coerceIn(0, TOTAL_SEATS)
            } else {
                // stale 데이터는 무시하고 0으로 안전 초기화
                passengerCount = 0
            }
            operationMessage = "운행 중 상태입니다"
        } else {
            reset()
        }

        restoreStateFromServer(timetableId)
    }

    fun clearSelectedRoute() {
        selectedRoute = null
        reset()
    }

// ── 서버 상태 복원 (운행/종료 상태 + busId만 동기화, 좌석 수는 로컬 유지) ────

    fun restoreStateFromServer(timetableId: Long) {
        val savedBusId = activeBusIdsByTimetableId[timetableId]
        val stateBeforeRestore = operationState

        Log.i(
            TAG_DRIVER_SCREEN,
            "restoreStateFromServer timetableId=$timetableId stateBeforeRestore=$stateBeforeRestore"
        )

        viewModelScope.launch {
            shuttleRepository.getBusStatuses(timetableId)
                .onSuccess { response ->
                    val status = response?.status?.trim()?.uppercase() ?: "UNKNOWN"
                    val busId = response?.busId

                    Log.i(
                        TAG_RESTORE,
                        "timetableId=$timetableId busId=$busId status=$status stateBeforeRestore=$stateBeforeRestore"
                    )

                    // 좌석 수는 항상 로컬 passengerCount 사용 — 서버 currentSeats 무시
                    val localStatus = if (stateBeforeRestore == OperationState.OPERATING) "RUNNING" else status
                    seatStateByTimetableId = seatStateByTimetableId + (
                        timetableId to SeatState(
                            currentSeats = passengerCount,
                            totalSeats = TOTAL_SEATS,
                            status = localStatus
                        )
                    )

                    if (busId != null) {
                        activeBusIdsByTimetableId = activeBusIdsByTimetableId + (timetableId to busId)
                        selectedRoute = selectedRoute?.copy(busId = busId)
                    }

                    when (status) {
                        "RUNNING" -> {
                            val wasLocallyFinished = locallyFinishedTimetableIds.contains(timetableId) ||
                                prefs.getBoolean("finished_$timetableId", false)
                            if (wasLocallyFinished) {
                                Log.i(TAG_RESTORE, "timetableId=$timetableId server=RUNNING but locally finished - forcing DONE")
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
                                // 탑승 수는 로컬 유지 — 서버 값 적용 없음
                                if (stateBeforeRestore != OperationState.OPERATING) {
                                    operationMessage = "서버에서 운행 중 상태를 복원했습니다"
                                }
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
        passengerCount = passengers.coerceIn(0, TOTAL_SEATS)
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
                totalSeats = TOTAL_SEATS,
                status = "RUNNING"
            )
        )
    }

// ── 출발 등록 / 운행 종료 ──────────────────────────────────

    fun startOperation(latitude: Double? = null, longitude: Double? = null, permissionGranted: Boolean = true) {
        val now = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())
        val route = selectedRoute ?: run {
            operationMessage = "선택된 노선이 없습니다"
            return
        }
        val timetableId = route.timetableId()

        viewModelScope.launch {
            val isCurrentlyRunning = runningStateByTimetableId[timetableId] == true

            Log.d(TAG_DRIVER_SCREEN, "운행 버튼 클릭, timetableId=$timetableId isCurrentlyRunning=$isCurrentlyRunning")

            if (isCurrentlyRunning) {
                operationMessage = "운행 종료 처리 중입니다"

                val finish = shuttleRepository.finishTimetable(timetableId)
                    .getOrElse { throwable ->
                        Log.e(TAG_DEPART, "finish failed: timetableId=$timetableId message=${throwable.message}", throwable)
                        operationMessage = "운행 종료 실패: ${throwable.message ?: "서버 응답 확인 필요"}"
                        return@launch
                    }

                Log.i(TAG_DEPART, "finish success: timetableId=$timetableId busId=${finish?.busId} status=${finish?.busStatus}")

                // 종료 시 로컬 카운트 초기화
                passengerCount = 0
                operationState = OperationState.COMPLETED
                isGpsTracking = false
                operationMessage = "운행 종료 완료"

                seatStateByTimetableId = seatStateByTimetableId + (
                    timetableId to SeatState(currentSeats = 0, totalSeats = TOTAL_SEATS, status = "DONE")
                )

                runningStateByTimetableId = runningStateByTimetableId + (timetableId to false)
                activeBusIdsByTimetableId = activeBusIdsByTimetableId - timetableId

                persistRunningState(timetableId, false, null)
                prefs.edit().remove("passengers_$timetableId").apply()
                locallyFinishedTimetableIds.add(timetableId)
                prefs.edit().putBoolean("finished_$timetableId", true).apply()

                finish?.busId?.let { finishedBusId ->
                    selectedRoute = route.copy(busId = finishedBusId)
                }

                return@launch
            }

            operationMessage = "출발 등록 중입니다"

            val depart = shuttleRepository.departTimetable(timetableId)
                .getOrElse { throwable ->
                    Log.e(TAG_DEPART, "depart failed: timetableId=$timetableId message=${throwable.message}", throwable)
                    isGpsTracking = false
                    operationMessage = "출발 등록 실패: ${throwable.message ?: "서버 응답 확인 필요"}"
                    return@launch
                }

            Log.i(TAG_DEPART, "depart success: timetableId=$timetableId busId=${depart?.busId} status=${depart?.busStatus}")

            val busId = depart?.busId
                ?: activeBusIdsByTimetableId[timetableId]
                ?: shuttleRepository.getBusStatuses(timetableId).getOrNull()?.busId
                ?: route.busId

            val hasBusId = busId != null
            if (!hasBusId) {
                Log.w(TAG_GPS, "startOperation: busId is null, GPS tracking disabled. timetableId=$timetableId")
            }

            if (busId != null) {
                selectedRoute = route.copy(busId = busId)
                activeBusIdsByTimetableId = activeBusIdsByTimetableId + (timetableId to busId)
            }

            runningStateByTimetableId = runningStateByTimetableId + (timetableId to true)
            persistRunningState(timetableId, true, busId)
            locallyFinishedTimetableIds.remove(timetableId)
            prefs.edit().remove("finished_$timetableId").apply()

            // 좌석 상태 0으로 확정 초기화
            passengerCount = 0
            seatStateByTimetableId = seatStateByTimetableId + (
                timetableId to SeatState(currentSeats = 0, totalSeats = TOTAL_SEATS, status = "RUNNING")
            )
            persistPassengerCount(timetableId, 0)

            operationState = OperationState.OPERATING
            actualDepartureTime = depart?.actualDepartureTime?.take(5) ?: now
            isGpsTracking = permissionGranted && hasBusId

            Log.i(
                TAG_GPS,
                "[GPS] busId=$busId lat=$latitude lng=$longitude " +
                "permissionGranted=$permissionGranted hasBusId=$hasBusId isGpsTracking=$isGpsTracking"
            )

            when {
                !permissionGranted -> {
                    operationMessage = "출발 등록 완료 | 00/$TOTAL_SEATS | 위치 권한 없음"
                    Log.w(TAG_GPS, "startOperation: permission denied. timetableId=$timetableId")
                }
                !hasBusId -> {
                    operationMessage = "출발 등록 완료 | 00/$TOTAL_SEATS | GPS 보류"
                    Log.w(TAG_GPS, "startOperation: busId null. timetableId=$timetableId")
                }
                latitude != null && longitude != null -> {
                    lastGpsText = "%.6f, %.6f".format(Locale.US, latitude, longitude)
                    operationMessage = "출발 등록 완료 | 00/$TOTAL_SEATS"

                    shuttleRepository.postDriverLocation(
                        busId = busId!!,
                        latitude = latitude,
                        longitude = longitude
                    ).onSuccess {
                        operationMessage = "GPS 위치 전송 완료 | 00/$TOTAL_SEATS"
                        Log.i(TAG_GPS, "[DriverLocationPost] busId=$busId lat=$latitude lng=$longitude result=success timetableId=$timetableId")
                    }.onFailure { throwable ->
                        operationMessage = "GPS 전송 실패 | 00/$TOTAL_SEATS"
                        Log.e(TAG_GPS, "[DriverLocationPost] busId=$busId lat=$latitude lng=$longitude result=failed timetableId=$timetableId message=${throwable.message}", throwable)
                    }
                }
                else -> {
                    operationMessage = "출발 등록 완료 | 00/$TOTAL_SEATS | GPS 확인 중"
                    Log.w(TAG_GPS, "startOperation: no coords yet. timetableId=$timetableId")
                }
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
                ?: shuttleRepository.getBusStatuses(timetableId).getOrNull()?.busId

            if (busId == null) {
                Log.w(TAG_GPS, "[GPS] busId=null lat=$latitude lng=$longitude timetableId=$timetableId skipping location update")
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
                Log.i(TAG_GPS, "[GPS] busId=$busId lat=$latitude lng=$longitude timetableId=$timetableId")
                Log.i(TAG_GPS, "[DriverLocationPost] busId=$busId lat=$latitude lng=$longitude result=success timetableId=$timetableId")
            }.onFailure { throwable ->
                Log.e(TAG_GPS, "[DriverLocationPost] busId=$busId lat=$latitude lng=$longitude result=failed timetableId=$timetableId message=${throwable.message}", throwable)
            }
        }
    }

    fun setGpsStartError(message: String) {
        operationMessage = message
    }

    fun updateOperationMessage(message: String) {
        operationMessage = message
    }

// ── 서버 좌석 상태 갱신 (busId만 동기화, 탑승 수는 로컬 유지) ──

    fun refreshPassengerStateFromServer(showMessage: Boolean = false) {
        val route = selectedRoute ?: return
        val timetableId = route.timetableId()

        viewModelScope.launch {
            shuttleRepository.getBusStatuses(timetableId)
                .onSuccess { response ->
                    val busId = response?.busId ?: route.busId

                    // 탑승 수는 절대 서버 값으로 덮어쓰지 않음 — 로컬 passengerCount 유지
                    val localStatus = if (operationState == OperationState.OPERATING) "RUNNING"
                                      else response?.status ?: operationState.toApiStatus()

                    seatStateByTimetableId = seatStateByTimetableId + (
                        timetableId to SeatState(
                            currentSeats = passengerCount,
                            totalSeats = TOTAL_SEATS,
                            status = localStatus
                        )
                    )

                    if (busId != null) {
                        selectedRoute = route.copy(busId = busId)
                        activeBusIdsByTimetableId = activeBusIdsByTimetableId + (timetableId to busId)
                    }

                    Log.i(TAG_RESTORE, "refresh busId=$busId localPassengerCount=$passengerCount timetableId=$timetableId")

                    if (showMessage) {
                        operationMessage = "상태를 새로고침했습니다"
                    }
                }
                .onFailure { throwable ->
                    // 좌석 관련 에러는 화면에 표시하지 않음
                    Log.e(TAG_RESTORE, "refresh failed timetableId=$timetableId ${throwable.message}", throwable)
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
                    Log.i(TAG_DEPART, "endOperation success timetableId=$timetableId busId=${response?.busId}")

                    // 로컬 카운트 초기화
                    passengerCount = 0
                    operationState = OperationState.COMPLETED
                    isGpsTracking = false
                    operationMessage = "운행이 종료되어 GPS 전송이 중지되었습니다"

                    seatStateByTimetableId = seatStateByTimetableId + (
                        timetableId to SeatState(currentSeats = 0, totalSeats = TOTAL_SEATS, status = "DONE")
                    )

                    runningStateByTimetableId = runningStateByTimetableId + (timetableId to false)
                    activeBusIdsByTimetableId = activeBusIdsByTimetableId - timetableId

                    persistRunningState(timetableId, false, null)
                    prefs.edit().remove("passengers_$timetableId").apply()
                    locallyFinishedTimetableIds.add(timetableId)
                    prefs.edit().putBoolean("finished_$timetableId", true).apply()
                }
                .onFailure { throwable ->
                    operationMessage = "운행 종료 실패: ${throwable.message ?: "서버 응답 확인 필요"}"
                    Log.e(TAG_DEPART, "endOperation failed timetableId=$timetableId ${throwable.message}", throwable)
                }
        }
    }

// ── 상차/하차 수기 집계 ───────────────────────────────────

    fun increasePassengers() {
        when {
            operationState != OperationState.OPERATING -> {
                operationMessage = "운행 중에만 탑승 수를 집계할 수 있습니다"
            }
            passengerCount >= TOTAL_SEATS -> {
                operationMessage = "만석입니다 ($TOTAL_SEATS/$TOTAL_SEATS)"
            }
            else -> applyLocalPassengerChange(type = "BOARD")
        }
    }

    fun decreasePassengers() {
        when {
            operationState != OperationState.OPERATING -> {
                operationMessage = "운행 중에만 하차 수를 집계할 수 있습니다"
            }
            passengerCount <= 0 -> {
                operationMessage = "탑승 중인 승객이 없습니다"
            }
            else -> applyLocalPassengerChange(type = "ALIGHT")
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
        prefs.edit().remove("passengers_$timetableId").apply()
        runningStateByTimetableId = runningStateByTimetableId + (timetableId to false)
        reset()
    }

// ── 탑승/하차 로컬 즉시 처리 + 서버 fire-and-forget ─────────

    private fun applyLocalPassengerChange(type: String) {
        val route = selectedRoute ?: return
        val timetableId = route.timetableId()

        val newCount = if (type == "BOARD") {
            (passengerCount + 1).coerceIn(0, TOTAL_SEATS)
        } else {
            (passengerCount - 1).coerceIn(0, TOTAL_SEATS)
        }
        val remaining = TOTAL_SEATS - newCount

        passengerCount = newCount
        seatStateByTimetableId = seatStateByTimetableId + (
            timetableId to SeatState(
                currentSeats = newCount,
                totalSeats = TOTAL_SEATS,
                status = "RUNNING"
            )
        )
        persistPassengerCount(timetableId, newCount)

        operationMessage = if (type == "BOARD") {
            "탑승 ${newCount}명 | 여석 ${remaining}석"
        } else {
            "하차 처리 | 탑승 ${newCount}명 | 여석 ${remaining}석"
        }

        Log.i(TAG_TAG_API, "[Local] timetableId=$timetableId type=$type newCount=$newCount remaining=$remaining")

        // 서버 tag API fire-and-forget — 실패해도 화면 값 변경 없음
        val token = TokenStore.accessToken
        if (token.isNullOrBlank()) {
            Log.w(TAG_TAG_API, "timetableId=$timetableId type=$type skipped: no accessToken")
            return
        }

        viewModelScope.launch {
            shuttleRepository.postBusTag(timetableId, BusTagRequest(type = type))
                .onSuccess { response ->
                    Log.i(TAG_TAG_API, "[TagApi] success timetableId=$timetableId type=$type serverSeats=${response?.currentSeats}")
                }
                .onFailure { throwable ->
                    // 400/405 에러 등 — 화면 표시 없이 로그만
                    Log.w(TAG_TAG_API, "[TagApi] failed (ignored) timetableId=$timetableId type=$type error=${throwable.message}")
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
        totalSeats = TOTAL_SEATS,
        stops = stopNames,
        timetableId = timetableId
    )
}
