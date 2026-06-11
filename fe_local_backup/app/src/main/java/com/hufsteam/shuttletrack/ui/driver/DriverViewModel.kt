package com.hufsteam.shuttletrack.ui.driver

import androidx.compose.runtime.getValue

import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
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
    val stops: List<String>
)

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

class DriverViewModel : ViewModel() {

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

    fun selectRoute(route: DriverRoute) {
        selectedRoute = route
        reset()
    }

    fun startOperation() {
        val now = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())
        actualDepartureTime = now
        operationState = OperationState.OPERATING
        isGpsTracking = true
        operationMessage = "GPS 위치와 좌석 수를 전송 중입니다"
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
    }
}
