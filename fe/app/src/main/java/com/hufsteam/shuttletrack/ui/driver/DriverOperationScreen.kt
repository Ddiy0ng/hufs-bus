package com.hufsteam.shuttletrack.ui.driver

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import android.os.Looper
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.hufsteam.shuttletrack.ui.common.BusIcon
import com.hufsteam.shuttletrack.ui.theme.NavyBlue
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.coroutines.resume

private val DriverAccentRed = Color(0xFFB83A25)
private val DriverSoftGray = Color(0xFFF4F6F8)
private const val TAG_GPS_SCREEN = "GPS"

// ── 메인 화면 ─────────────────────────────────────────────────

@Composable
fun DriverOperationScreen(
    driverViewModel: DriverViewModel,
    onBack: () -> Unit
) {
    val route = driverViewModel.selectedRoute ?: return
    val state = driverViewModel.operationState
    val passengers = driverViewModel.passengerCount
    val total = route.totalSeats
    val actualTime = driverViewModel.actualDepartureTime
    val isGpsTracking = driverViewModel.isGpsTracking
    val operationMessage = driverViewModel.operationMessage
    val lastGpsText = driverViewModel.lastGpsText
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var refreshDragDistance by remember { mutableStateOf(0f) }
    var isManualRefreshing by remember { mutableStateOf(false) }

    fun refreshDriverOperation() {
        if (isManualRefreshing) return
        scope.launch {
            isManualRefreshing = true
            if (driverViewModel.operationState == OperationState.OPERATING) {
                driverViewModel.updateOperationMessage("운행 상태를 새로고침 중입니다")
                driverViewModel.refreshPassengerStateFromServer(showMessage = false)
                val location = readCurrentLocation(context)
                if (location != null) {
                    driverViewModel.sendCurrentLocation(
                        latitude = location.latitude,
                        longitude = location.longitude
                    )
                } else {
                    driverViewModel.updateOperationMessage("현재 GPS 위치를 가져오지 못했습니다. 위치 권한과 GPS 설정을 확인해 주세요")
                }
            } else {
                driverViewModel.updateOperationMessage("운행 전입니다. 출발 등록 후 GPS를 새로고침할 수 있습니다")
            }
            delay(600)
            isManualRefreshing = false
        }
    }
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val granted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
                permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        if (granted) {
            driverViewModel.updateOperationMessage("현재 GPS 위치 확인 중입니다")
            scope.launch { startOperationWithCurrentLocation(context, driverViewModel) }
        } else {
            // 권한 거부 — GPS 비활성 상태로 출발 등록 (ViewModel이 메시지 설정)
            scope.launch { driverViewModel.startOperation(null, null, permissionGranted = false) }
        }
    }

    // 화면 진입 시 서버 상태 복원 (운행 중에는 호출하지 않음 — 로컬 집계 보호)
    LaunchedEffect(route.id) {
        if (driverViewModel.operationState != OperationState.OPERATING) {
            val timetableId = route.timetableId ?: route.id.filter { it.isDigit() }.toLongOrNull() ?: 1L
            driverViewModel.restoreStateFromServer(timetableId)
        }
    }

    LaunchedEffect(state, isGpsTracking, route.id) {
        if (state == OperationState.OPERATING && isGpsTracking) {
            while (true) {
                delay(5_000)
                val location = readCurrentLocation(context)
                if (location != null) {
                    driverViewModel.sendCurrentLocation(
                        latitude = location.latitude,
                        longitude = location.longitude
                    )
                }
            }
        }
    }


    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .pointerInput(state, isManualRefreshing) {
                detectVerticalDragGestures(
                    onVerticalDrag = { _, dragAmount ->
                        if (dragAmount > 0) refreshDragDistance += dragAmount
                    },
                    onDragEnd = {
                        if (refreshDragDistance > 80f) refreshDriverOperation()
                        refreshDragDistance = 0f
                    },
                    onDragCancel = { refreshDragDistance = 0f }
                )
            }
            .background(Color.White)
    ) {
        // ── 상단 헤더 ──────────────────────────────────────────
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp)
        ) {
            Icon(
                Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "뒤로가기",
                tint     = Color.Black,
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .size(24.dp)
                    .clickable { onBack() }
            )
            Text(
                text      = route.routeName,
                fontSize  = 16.sp,
                fontWeight = FontWeight.SemiBold,
                color     = Color.Black,
                modifier  = Modifier.align(Alignment.Center)
            )
        }

        Column(
            modifier            = Modifier
                .weight(1f)
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(24.dp))

            // ── 버스 아이콘 ────────────────────────────────────
            BusIcon(size = 58.dp)

            Spacer(Modifier.height(20.dp))

            // ── 출발 시간 행 ───────────────────────────────────
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(31.dp)
                    .clip(RoundedCornerShape(50.dp))
                    .border(1.4.dp, Color(0xFFD8DEE8), RoundedCornerShape(50.dp))
                    .padding(horizontal = 20.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(
                    modifier            = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        "출발 계획 | ${route.scheduledTime}",
                        fontSize = 12.sp,
                        color    = Color(0xFF444444)
                    )
                    Text(
                        "실제 출발 | ${actualTime ?: "미정"}",
                        fontSize = 12.sp,
                        color    = Color(0xFF444444)
                    )
                }
            }

            Spacer(Modifier.height(32.dp))

            when (state) {
                // ── 출발 전 ────────────────────────────────────
                OperationState.BEFORE_DEPARTURE -> {
                    BeforeDepartureContent(
                        passengers = passengers,
                        total      = total,
                        message    = operationMessage
                    )
                }

                // ── 운행 중 ────────────────────────────────────
                OperationState.OPERATING -> {
                    OperatingContent(
                        passengers    = passengers,
                        total         = total,
                        isGpsTracking = isGpsTracking,
                        message       = operationMessage,
                        lastGpsText   = lastGpsText,
                        onBoard       = { driverViewModel.increasePassengers() },
                        onAlight      = { driverViewModel.decreasePassengers() }
                    )
                }

                // ── 운행 완료 ──────────────────────────────────
                OperationState.COMPLETED -> {
                    CompletedContent(
                        passengers = passengers,
                        total      = total,
                        message    = operationMessage
                    )
                }
            }
        }

        // ── 하단 버튼 ──────────────────────────────────────────
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 20.dp),
            contentAlignment = Alignment.Center
        ) {
            when (state) {
                OperationState.BEFORE_DEPARTURE -> {
                    OperationButton(
                        text    = "출발 등록하기",
                        enabled = true,
                        onClick = {
                            driverViewModel.updateOperationMessage("현재 GPS 위치 확인 중입니다")
                            if (hasLocationPermission(context)) {
                                scope.launch { startOperationWithCurrentLocation(context, driverViewModel) }
                            } else {
                                driverViewModel.updateOperationMessage("위치 권한을 요청했습니다")
                                permissionLauncher.launch(
                                    arrayOf(
                                        Manifest.permission.ACCESS_FINE_LOCATION,
                                        Manifest.permission.ACCESS_COARSE_LOCATION
                                    )
                                )
                            }
                        }
                    )
                }
                OperationState.OPERATING -> {
                    OperationButton(
                        text    = "운행 종료하기",
                        enabled = true,
                        onClick = { driverViewModel.endOperation() }
                    )
                }
                OperationState.COMPLETED -> {
                    OperationButton(
                        text    = "다시 출발 등록하기",
                        enabled = true,
                        onClick = { driverViewModel.resetForNewDeparture() }
                    )
                }
            }
        }

        // ── 하단 바 (시간표 / 즐겨찾기 / 마이페이지) ──────────
        BottomNavBar(
            selected      = BottomTab.Timetable,
            onTabSelected = {}
        )
    }
}

// ── 출발 전 콘텐츠 ─────────────────────────────────────────────

@Composable
private fun BeforeDepartureContent(
    passengers: Int,
    total: Int,
    message: String
) {
    SeatSummaryContent(passengers = passengers, total = total)
    Spacer(Modifier.height(14.dp))
    OperationStatePill(message = message, isActive = false)
}

// ── 운행 중 콘텐츠 ─────────────────────────────────────────────

@Composable
private fun OperatingContent(
    passengers: Int,
    total: Int,
    isGpsTracking: Boolean,
    message: String,
    lastGpsText: String?,
    onBoard: () -> Unit,
    onAlight: () -> Unit
) {
    OperationStatePill(message = message, isActive = isGpsTracking)
    lastGpsText?.let {
        Spacer(Modifier.height(8.dp))
        Text(
            "마지막 GPS $it",
            fontSize = 11.sp,
            color = Color(0xFF777777),
            textAlign = TextAlign.Center
        )
    }

    Spacer(Modifier.height(18.dp))

    SeatSummaryContent(passengers = passengers, total = total)

    Spacer(Modifier.height(24.dp))

    // ── 상차/하차 수기 집계 버튼 ───────────────────────────────
    Row(
        horizontalArrangement = Arrangement.spacedBy(20.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Button(
            onClick  = onAlight,
            enabled  = passengers > 0,
            shape    = RoundedCornerShape(10.dp),
            colors   = ButtonDefaults.buttonColors(
                containerColor         = Color(0xFFE53935),
                disabledContainerColor = Color(0xFFE0E0E0)
            ),
            modifier = Modifier
                .width(110.dp)
                .height(52.dp)
        ) {
            Text(
                "− 하차",
                fontSize   = 15.sp,
                fontWeight = FontWeight.Bold,
                color      = Color.White
            )
        }
        Button(
            onClick  = onBoard,
            enabled  = passengers < total,
            shape    = RoundedCornerShape(10.dp),
            colors   = ButtonDefaults.buttonColors(
                containerColor         = NavyBlue,
                disabledContainerColor = Color(0xFFE0E0E0)
            ),
            modifier = Modifier
                .width(110.dp)
                .height(52.dp)
        ) {
            Text(
                "+ 탑승",
                fontSize   = 15.sp,
                fontWeight = FontWeight.Bold,
                color      = Color.White
            )
        }
    }

    Spacer(Modifier.height(12.dp))
    Text(
        "버튼을 누르면 서버에 즉시 반영됩니다",
        fontSize = 12.sp,
        color = Color(0xFF777777),
        textAlign = TextAlign.Center
    )
}

// ── 운행 완료 콘텐츠 ────────────────────────────────────────────

@Composable
private fun CompletedContent(passengers: Int, total: Int, message: String) {
    Spacer(Modifier.height(16.dp))
    SeatSummaryContent(passengers = passengers, total = total)
    Spacer(Modifier.height(18.dp))
    OperationStatePill(message = message, isActive = false)
    Spacer(Modifier.height(18.dp))
    Text(
        "운행이 완료되었습니다",
        fontSize  = 16.sp,
        color     = Color(0xFF888888),
        textAlign = TextAlign.Center
    )
}

@Composable
private fun SeatSummaryContent(passengers: Int, total: Int) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            "탑승 수",
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = DriverAccentRed
        )
        Spacer(Modifier.height(6.dp))
        Text(
            "${passengers.toString().padStart(2, '0')}/$total",
            fontSize = 48.sp,
            fontWeight = FontWeight.Bold,
            color = DriverAccentRed,
            lineHeight = 48.sp
        )
    }
}

@Composable
private fun OperationStatePill(message: String, isActive: Boolean) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(50.dp))
            .background(if (isActive) Color(0xFFEAF3FF) else DriverSoftGray)
            .padding(horizontal = 14.dp, vertical = 7.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            message,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            color = if (isActive) NavyBlue else Color(0xFF777777),
            textAlign = TextAlign.Center
        )
    }
}

// ── 정류장 진행 바 ─────────────────────────────────────────────

@Composable
private fun StopProgressBar(stops: List<String>, currentIndex: Int) {
    Column {
        Row(
            modifier            = Modifier.fillMaxWidth(),
            verticalAlignment   = Alignment.CenterVertically
        ) {
            stops.forEachIndexed { index, _ ->
                val isPassed  = index < currentIndex
                val isCurrent = index == currentIndex
                val circleColor = when {
                    isCurrent -> Color(0xFFCC2200)
                    isPassed  -> NavyBlue
                    else      -> Color.White
                }
                val borderColor = if (isPassed || isCurrent) NavyBlue else Color(0xFFCCCCCC)

                // 왼쪽 연결선
                if (index > 0) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(3.dp)
                            .background(if (index <= currentIndex) NavyBlue else Color(0xFFCCCCCC))
                    )
                }

                // 원
                Box(
                    modifier = Modifier
                        .size(if (isCurrent) 22.dp else 18.dp)
                        .clip(CircleShape)
                        .background(circleColor)
                        .border(2.dp, borderColor, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    if (isCurrent) {
                        // 현재 위치: 핀 모양 대신 강조 원
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(Color.White)
                        )
                    }
                }
            }
        }

        Spacer(Modifier.height(6.dp))

        // 정류장 이름
        Row(modifier = Modifier.fillMaxWidth()) {
            stops.forEachIndexed { index, stop ->
                if (index > 0) Spacer(Modifier.weight(1f))
                val isCurrent = index == currentIndex
                Text(
                    text      = stop,
                    fontSize  = 11.sp,
                    color     = if (isCurrent) Color(0xFFCC2200) else Color(0xFF555555),
                    fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Normal,
                    textAlign = if (index == 0) TextAlign.Start
                                else if (index == stops.size - 1) TextAlign.End
                                else TextAlign.Center,
                    modifier  = Modifier.width(60.dp)
                )
            }
        }
    }
}

// ── 공통 버튼 ──────────────────────────────────────────────────

@Composable
private fun OperationButton(text: String, enabled: Boolean, onClick: () -> Unit) {
    Button(
        onClick  = onClick,
        enabled  = enabled,
        shape    = RoundedCornerShape(7.dp),
        colors   = ButtonDefaults.buttonColors(
            containerColor         = NavyBlue,
            disabledContainerColor = Color(0xFFF0F1F3)
        ),
        modifier = Modifier
            .width(150.dp)
            .height(36.dp)
    ) {
        Text(
            text,
            fontSize   = 12.sp,
            fontWeight = FontWeight.SemiBold,
            color      = if (enabled) Color.White else Color(0xFF9AA1AB)
        )
    }
}

private fun hasLocationPermission(context: Context): Boolean {
    val fine = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION)
    val coarse = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION)
    return fine == PackageManager.PERMISSION_GRANTED || coarse == PackageManager.PERMISSION_GRANTED
}

private suspend fun startOperationWithCurrentLocation(
    context: Context,
    driverViewModel: DriverViewModel
) {
    if (!hasLocationPermission(context)) {
        // 권한 없음 → GPS 비활성 상태로 출발 등록
        Log.i(TAG_GPS_SCREEN, "[GPS] permissionGranted=false providerEnabled=N/A")
        driverViewModel.startOperation(null, null, permissionGranted = false)
        return
    }

    // 권한 있음 — 프로바이더 상태 로그 후 좌표 획득 시도
    val providerEnabled = hasEnabledLocationProvider(context)
    Log.i(TAG_GPS_SCREEN, "[GPS] permissionGranted=true providerEnabled=$providerEnabled")

    val location = readCurrentLocation(context)
    // 좌표 null이어도 권한은 있으므로 permissionGranted=true → isGpsTracking 유지, 5초 주기 재시도
    driverViewModel.startOperation(location?.latitude, location?.longitude, permissionGranted = true)
}

private fun hasEnabledLocationProvider(context: Context): Boolean {
    val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
    return listOf(LocationManager.GPS_PROVIDER, LocationManager.NETWORK_PROVIDER).any { provider ->
        runCatching { locationManager.isProviderEnabled(provider) }.getOrDefault(false)
    }
}

@SuppressLint("MissingPermission")
private suspend fun readCurrentLocation(context: Context): Location? = withTimeoutOrNull(15_000L) {
    suspendCancellableCoroutine { cont ->
        if (!hasLocationPermission(context)) {
            cont.resume(null)
            return@suspendCancellableCoroutine
        }

        val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        val providers = listOf(LocationManager.GPS_PROVIDER, LocationManager.NETWORK_PROVIDER)
            .filter { provider -> runCatching { locationManager.isProviderEnabled(provider) }.getOrDefault(false) }

        val lastKnownLocation = providers
            .mapNotNull { provider -> runCatching { locationManager.getLastKnownLocation(provider) }.getOrNull() }
            .maxByOrNull { it.time }

        // 30분 이내 캐시된 위치는 그대로 사용 (기존 10분에서 확대)
        if (lastKnownLocation != null && System.currentTimeMillis() - lastKnownLocation.time < 30 * 60_000L) {
            cont.resume(lastKnownLocation)
            return@suspendCancellableCoroutine
        }

        val provider = providers.firstOrNull()
        if (provider == null) {
            cont.resume(lastKnownLocation)
            return@suspendCancellableCoroutine
        }

        val listener = object : LocationListener {
            override fun onLocationChanged(location: Location) {
                if (cont.isActive) {
                    locationManager.removeUpdates(this)
                    cont.resume(location)
                }
            }

            @Deprecated("Deprecated in Android API")
            override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) = Unit

            override fun onProviderEnabled(provider: String) = Unit

            override fun onProviderDisabled(provider: String) {
                // 프로바이더 꺼지면 lastKnown으로 폴백
                if (cont.isActive) {
                    locationManager.removeUpdates(this)
                    cont.resume(lastKnownLocation)
                }
            }
        }

        runCatching {
            // requestSingleUpdate는 API 29+ deprecated — requestLocationUpdates 사용
            locationManager.requestLocationUpdates(provider, 0L, 0f, listener, Looper.getMainLooper())
        }.onFailure {
            if (cont.isActive) cont.resume(lastKnownLocation)
        }

        cont.invokeOnCancellation {
            locationManager.removeUpdates(listener)
        }
    }
}
