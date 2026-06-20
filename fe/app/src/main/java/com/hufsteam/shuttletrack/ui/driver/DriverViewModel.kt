package com.hufsteam.shuttletrack.ui.driver

import android.util.Log
import androidx.compose.runtime.getValue

import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hufsteam.shuttletrack.data.remote.dto.BusTagRequest
import com.hufsteam.shuttletrack.data.repository.ShuttleRepository
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private const val DRIVER_GPS_TAG = "DriverGps"
private const val BUS_TAG_LOG_TAG = "BusTagApi"

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

// ── 목 데이터 ─────────────────────────────────────────────────

val mockDriverRoutes = listOf(
    DriverRoute(
        id            = "route_1",
        routeName     = "경기광주역 → 외대(글)",
        scheduledTime = "08:30",
        totalSeats    = 45,
        stops         = listOf("경기광주역(기점)", "기숙사", "백년관", "인경관(종점)")
    ),
    DriverRoute(
        id            = "route_2",
        routeName     = "외대(글) → 경기광주역",
        scheduledTime = "13:00",
        totalSeats    = 45,
        stops         = listOf("인경관(기점)", "백년관", "기숙사", "경기광주역(종점)")
    ),
    DriverRoute(
        id            = "route_3",
        routeName     = "판교역 → 외대(글)",
        scheduledTime = "17:30",
        totalSeats    = 45,
        stops         = listOf("판교역(기점)", "성남역", "서현역", "외대-글(종점)")
    )
)

// ── ViewModel ────────────────────────────────────────────────

class DriverViewModel(
    private val shuttleRepository: ShuttleRepository = ShuttleRepository()
) : ViewModel() {

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

    fun selectRoute(route: DriverRoute) {
        selectedRoute = route
        reset()
    }

    fun clearSelectedRoute() {
        selectedRoute = null
        reset()
    }

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
        seatStateByTimetableId = seatStateByTimetableId + (
            timetableId to SeatState(
                currentSeats = passengerCount,
                totalSeats = route.totalSeats,
                status = "RUNNING"
            )
        )
    }

    fun startOperation(latitude: Double, longitude: Double) {
        val now = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())
        val route = selectedRoute
        val timetableId = route?.timetableId() ?: 1L
        actualDepartureTime = now
        operationState = OperationState.OPERATING
        isGpsTracking = true
        lastGpsText = "%.6f, %.6f".format(Locale.US, latitude, longitude)
        operationMessage = "출발 등록 및 GPS 전송 중입니다"

        viewModelScope.launch {
            var departBusId: Long? = null
            shuttleRepository.departTimetable(timetableId)
                .onSuccess { depart ->
                    departBusId = depart?.busId
                    actualDepartureTime = depart?.actualDepartureTime?.take(5) ?: now
                    operationMessage = "출발 등록 완료, GPS 위치를 전송 중입니다"
                    val resolvedBusId = departBusId
                    if (resolvedBusId != null) {
                        selectedRoute = route?.copy(busId = resolvedBusId)
                        activeBusIdsByTimetableId = activeBusIdsByTimetableId + (timetableId to resolvedBusId)
                    }
                }
                .onFailure { throwable ->
                    operationMessage = "출발 등록 실패, GPS 전송을 계속 시도합니다: ${throwable.message ?: "서버 응답 확인 필요"}"
                }

            val busId = departBusId
                ?: shuttleRepository.getBusStatuses(timetableId).getOrNull()?.busId
                ?: route?.busId
            if (busId == null) {
                operationMessage = "출발 등록 완료, busId 조회 실패로 GPS 전송을 보류했습니다"
                Log.w(DRIVER_GPS_TAG, "startOperation skipped: busId is null, timetableId=$timetableId")
                return@launch
            }
            selectedRoute = route?.copy(busId = busId)
            activeBusIdsByTimetableId = activeBusIdsByTimetableId + (timetableId to busId)

            shuttleRepository.postDriverLocation(
                busId = busId,
                latitude = latitude,
                longitude = longitude
            ).onSuccess {
                operationMessage = "GPS 위치가 서버에 전송되었습니다"
                Log.i(DRIVER_GPS_TAG, "initial location sent: busId=$busId lat=$latitude lng=$longitude")
            }.onFailure { throwable ->
                operationMessage = "GPS 전송 실패: ${throwable.message ?: "서버 응답을 확인해 주세요"}"
                Log.e(DRIVER_GPS_TAG, "initial location failed: busId=$busId lat=$latitude lng=$longitude", throwable)
            }
        }
    }

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
                operationMessage = "busId 조회 실패로 GPS 갱신을 보류했습니다"
                Log.w(DRIVER_GPS_TAG, "location update skipped: busId is null, timetableId=$timetableId")
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
                Log.i(DRIVER_GPS_TAG, "location update sent: busId=$busId lat=$latitude lng=$longitude")
            }.onFailure { throwable ->
                operationMessage = "GPS 갱신 실패: ${throwable.message ?: "서버 응답을 확인해 주세요"}"
                Log.e(DRIVER_GPS_TAG, "location update failed: busId=$busId lat=$latitude lng=$longitude", throwable)
            }
        }
    }

    fun setGpsStartError(message: String) {
        operationMessage = message
    }

    fun updateOperationMessage(message: String) {
        operationMessage = message
    }

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
                        ?: (response?.remainingSeats ?: response?.availableSeats)?.let { total - it }
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

    fun endOperation() {
        operationState = OperationState.COMPLETED
        isGpsTracking = false
        operationMessage = "운행이 종료되어 GPS 전송이 중지되었습니다"
    }

    fun increasePassengers() {
        val route = selectedRoute ?: return
        val total = route.totalSeats
        when {
            operationState != OperationState.OPERATING -> {
                operationMessage = "운행 중에만 탑승 수를 집계할 수 있습니다"
                Log.w(BUS_TAG_LOG_TAG, "blocked BOARD: status=$operationState route=${route.routeName} timetableId=${route.timetableId()}")
            }
            passengerCount >= total -> {
                operationMessage = "탑승 수가 총 좌석 수를 초과할 수 없습니다"
                Log.w(BUS_TAG_LOG_TAG, "blocked BOARD: currentSeats=$passengerCount totalSeats=$total timetableId=${route.timetableId()}")
            }
            else -> syncPassengerTag(type = "BOARD")
        }
    }

    fun decreasePassengers() {
        val route = selectedRoute ?: return
        when {
            operationState != OperationState.OPERATING -> {
                operationMessage = "운행 중에만 하차 수를 집계할 수 있습니다"
                Log.w(BUS_TAG_LOG_TAG, "blocked ALIGHT: status=$operationState route=${route.routeName} timetableId=${route.timetableId()}")
            }
            passengerCount <= 0 -> {
                operationMessage = "현재 탑승 수가 0명이라 하차 처리할 수 없습니다"
                Log.w(BUS_TAG_LOG_TAG, "blocked ALIGHT: currentSeats=$passengerCount totalSeats=${route.totalSeats} timetableId=${route.timetableId()}")
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

    private fun syncPassengerTag(type: String) {
        val route = selectedRoute ?: return
        val timetableId = route.timetableId()
        operationMessage = if (type == "BOARD") {
            "탑승 수를 서버에 반영 중입니다"
        } else {
            "하차 수를 서버에 반영 중입니다"
        }
        Log.i(
            BUS_TAG_LOG_TAG,
            "manual tag start endpoint=/api/buses/$timetableId/tags timetableId=$timetableId busId=${route.busId} " +
                "requestBody={\"type\":\"$type\"} status=$operationState currentSeats=$passengerCount totalSeats=${route.totalSeats}"
        )
        viewModelScope.launch {
            shuttleRepository.postBusTag(timetableId, BusTagRequest(type = type))
                .onSuccess { response ->
                    val total = response?.totalSeats ?: route.totalSeats
                    val currentPassengers = response?.currentSeats
                    val remainingSeats = response?.remainingSeats
                    passengerCount = when {
                        currentPassengers != null -> currentPassengers.coerceIn(0, total)
                        remainingSeats != null -> (total - remainingSeats).coerceIn(0, total)
                        else -> passengerCount.coerceIn(0, total)
                    }
                    selectedRoute = route.copy(
                        totalSeats = total,
                        busId = response?.busId ?: route.busId
                    )
                    response?.busId?.let { busId ->
                        activeBusIdsByTimetableId = activeBusIdsByTimetableId + (timetableId to busId)
                    }
                    seatStateByTimetableId = seatStateByTimetableId + (
                        timetableId to SeatState(
                            currentSeats = passengerCount,
                            totalSeats = total,
                            status = operationState.toApiStatus()
                        )
                    )
                    operationMessage = if (type == "BOARD") {
                        "탑승 수가 서버에 반영되었습니다"
                    } else {
                        "하차 수가 서버에 반영되었습니다"
                    }
                    Log.i(
                        BUS_TAG_LOG_TAG,
                        "manual tag success timetableId=$timetableId busId=${response?.busId ?: route.busId} " +
                            "type=$type currentSeats=$passengerCount totalSeats=$total remainingSeats=${(total - passengerCount).coerceIn(0, total)}"
                    )
                }
                .onFailure { throwable ->
                    operationMessage = "서버 반영 실패: ${throwable.message ?: "서버 응답 확인 필요"}"
                    Log.e(
                        BUS_TAG_LOG_TAG,
                        "manual tag failed timetableId=$timetableId busId=${route.busId} type=$type " +
                            "status=$operationState currentSeats=$passengerCount totalSeats=${route.totalSeats}",
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
