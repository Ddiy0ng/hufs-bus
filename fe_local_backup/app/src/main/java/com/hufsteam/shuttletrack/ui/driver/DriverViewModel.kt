package com.hufsteam.shuttletrack.ui.driver

import androidx.compose.runtime.getValue

import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hufsteam.shuttletrack.data.repository.ShuttleRepository
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

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
    val busId: Long? = null
)

// ── 목 데이터 ─────────────────────────────────────────────────

val mockDriverRoutes = listOf(
    DriverRoute(
        id            = "route_1",
        routeName     = "경기광주역 → 외대(글)",
        scheduledTime = "08:30",
        totalSeats    = 45,
        stops         = listOf("경기광주역(기점)", "기숙사", "백년관", "인경관(종점)"),
        busId         = 1
    ),
    DriverRoute(
        id            = "route_2",
        routeName     = "외대(글) → 경기광주역",
        scheduledTime = "13:00",
        totalSeats    = 45,
        stops         = listOf("인경관(기점)", "백년관", "기숙사", "경기광주역(종점)"),
        busId         = 2
    ),
    DriverRoute(
        id            = "route_3",
        routeName     = "판교역 → 외대(글)",
        scheduledTime = "17:30",
        totalSeats    = 45,
        stops         = listOf("판교역(기점)", "성남역", "서현역", "외대-글(종점)"),
        busId         = 3
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

    var operationMessage by mutableStateOf("출발 전입니다")
        private set

    var lastGpsText by mutableStateOf<String?>(null)
        private set

    fun selectRoute(route: DriverRoute) {
        selectedRoute = route
        reset()
    }

    fun clearSelectedRoute() {
        selectedRoute = null
        reset()
    }

    fun startOperation(latitude: Double, longitude: Double) {
        val now = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())
        val route = selectedRoute
        actualDepartureTime = now
        operationState = OperationState.OPERATING
        isGpsTracking = true
        lastGpsText = "%.6f, %.6f".format(Locale.US, latitude, longitude)
        operationMessage = "GPS 위치를 서버에 전송 중입니다"

        val busId = route?.busId ?: route?.id?.digitsAsLong() ?: 1L
        viewModelScope.launch {
            shuttleRepository.postDriverLocation(
                busId = busId,
                latitude = latitude,
                longitude = longitude
            ).onSuccess {
                operationMessage = "GPS 위치가 서버에 전송되었습니다"
            }.onFailure { throwable ->
                operationMessage = "GPS 전송 실패: ${throwable.message ?: "서버 응답을 확인해 주세요"}"
            }
        }
    }

    fun setGpsStartError(message: String) {
        operationMessage = message
    }

    fun endOperation() {
        operationState = OperationState.COMPLETED
        isGpsTracking = false
        operationMessage = "운행이 종료되어 GPS 전송이 중지되었습니다"
    }

    fun increasePassengers() {
        val total = selectedRoute?.totalSeats ?: 45
        if (operationState == OperationState.OPERATING && passengerCount < total) {
            passengerCount++
            operationMessage = "탑승 수가 수기로 반영되었습니다"
        }
    }

    fun decreasePassengers() {
        if (operationState == OperationState.OPERATING && passengerCount > 0) {
            passengerCount--
            operationMessage = "하차 수가 수기로 반영되었습니다"
        }
    }

    fun reset() {
        operationState      = OperationState.BEFORE_DEPARTURE
        passengerCount      = 0
        actualDepartureTime = null
        isGpsTracking       = false
        operationMessage    = "출발 전입니다"
        lastGpsText         = null
    }
}

private fun String.digitsAsLong(): Long? {
    return filter { it.isDigit() }.takeIf { it.isNotBlank() }?.toLongOrNull()
}
