package com.hufsteam.shuttletrack.ui.student

import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.net.Uri
import android.provider.OpenableColumns
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hufsteam.shuttletrack.data.remote.ShuttleApiClient
import com.hufsteam.shuttletrack.data.repository.ShuttleRepository
import com.hufsteam.shuttletrack.ui.auth.AuthViewModel
import com.hufsteam.shuttletrack.ui.common.BusIcon
import com.hufsteam.shuttletrack.data.model.UserRole
import com.hufsteam.shuttletrack.ui.driver.DriverRoute
import com.hufsteam.shuttletrack.ui.driver.DriverViewModel
import com.hufsteam.shuttletrack.ui.driver.OperationState
import com.hufsteam.shuttletrack.ui.theme.AdminBadge
import com.hufsteam.shuttletrack.ui.theme.AdminBadgeText
import com.hufsteam.shuttletrack.ui.theme.DividerColor
import com.hufsteam.shuttletrack.ui.theme.DriverBadge
import com.hufsteam.shuttletrack.ui.theme.DriverBadgeText
import com.hufsteam.shuttletrack.ui.theme.NavyBlue
import com.hufsteam.shuttletrack.ui.theme.StudentBadge
import com.hufsteam.shuttletrack.ui.theme.StudentBadgeText
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

// ── 내부 화면 상태 ──────────────────────────────────────────────
private enum class StudentScreen { TIMETABLE, ROUTE_STATUS, FAVORITES, MYPAGE }
private enum class StudentTab    { TIMETABLE, FAVORITES, MYPAGE }

// ── 데이터 모델 ─────────────────────────────────────────────────
data class BusSchedule(
    val id: Int,
    val timetableId: Long? = null,
    val routeName: String,
    val departureTime: String,
    val remainingSeats: Int,
    val totalSeats: Int,
    val currentLocation: String,
    val routeStops: List<String> = emptyList(),
    val campusType: String = "미분류"
)

data class FavoriteSchedule(
    val schedule: BusSchedule,
    val days: Set<String>
)

private data class AdminUploadFile(
    val uri: Uri,
    val name: String,
    val sizeLabel: String,
    val sizeBytes: Long,
    val mimeType: String?
)

private val notificationDays = listOf("월요일", "화요일", "수요일", "목요일", "금요일")
private const val FIXED_TOTAL_SEATS = 45
private const val MAX_ADMIN_UPLOAD_BYTES = 10L * 1024L * 1024L

private val ShuttleRegularTextStyle = TextStyle(
    fontFamily = FontFamily.SansSerif,
    fontWeight = FontWeight.Normal,
    fontSize = 16.sp,
    lineHeight = 16.sp,
    letterSpacing = 0.sp
)
private val offCampusSchedules = mockOffCampusSchedules
private val onCampusSchedules = mockOnCampusSchedules

private val offCampusRoutes = listOf(
    "판교역 → 외대(글)",
    "외대(글) → 판교역",
    "경기광주역 → 외대(글)"
)
private val onCampusRoutes = listOf("지석묘 → 인문경상관", "인문경상관 → 지석묘")
private val hours = listOf(7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18)

// ── 메인 화면 ───────────────────────────────────────────────────
@Composable
fun StudentMainScreen(
    viewModel: AuthViewModel,
    onLogout: () -> Unit,
    onGoServiceTerms: () -> Unit = {},
    onGoPrivacyTerms: () -> Unit = {},
    onGoDriverTimetable: () -> Unit = {},
    onGoDriverOperation: (DriverRoute) -> Unit = {},
    onGoAdminRouteManagement: () -> Unit = {},
    onGoAdminStopManagement: () -> Unit = {},
    onGoAdminTimetableManagement: () -> Unit = {},
    driverViewModel: DriverViewModel,
    studentBusViewModel: StudentBusViewModel = androidx.lifecycle.viewmodel.compose.viewModel()
) {
    val studentUiState = studentBusViewModel.uiState
    val baseOffCampusSchedules = studentUiState.offCampusSchedules
    val baseOnCampusSchedules = studentUiState.onCampusSchedules
    val liveOffCampusSchedules = baseOffCampusSchedules.withLivePassengerState(driverViewModel)
    val liveOnCampusSchedules = baseOnCampusSchedules.withLivePassengerState(driverViewModel)
    val favoriteSchedules = studentUiState.favoriteSchedules.map { favorite ->
        favorite.copy(schedule = favorite.schedule.withLivePassengerState(driverViewModel))
    }
    var currentScreen by remember { mutableStateOf(StudentScreen.TIMETABLE) }
    var selectedTab by remember { mutableStateOf(StudentTab.TIMETABLE) }
    var selectedSchedule by remember { mutableStateOf(liveOffCampusSchedules.firstOrNull() ?: offCampusSchedules.first()) }
    var favoriteTarget by remember { mutableStateOf<BusSchedule?>(null) }
    val liveSelectedSchedule = selectedSchedule.withLivePassengerState(driverViewModel)
    val liveRouteDetail = studentUiState.selectedRouteDetail ?: mockRouteDetailFor(liveSelectedSchedule).withSeatText(liveSelectedSchedule)

    LaunchedEffect(liveOffCampusSchedules, liveOnCampusSchedules) {
        val allSchedules = liveOffCampusSchedules + liveOnCampusSchedules
        if (allSchedules.isNotEmpty() && allSchedules.none { it.id == selectedSchedule.id }) {
            selectedSchedule = allSchedules.first()
        }
    }

    DisposableEffect(currentScreen, selectedSchedule.id) {
        if (currentScreen == StudentScreen.ROUTE_STATUS) {
            studentBusViewModel.startRouteStatusUpdates(selectedSchedule)
        } else {
            studentBusViewModel.stopRouteStatusUpdates()
        }
        onDispose {
            if (currentScreen == StudentScreen.ROUTE_STATUS) {
                studentBusViewModel.stopRouteStatusUpdates()
            }
        }
    }

    BackHandler(enabled = currentScreen != StudentScreen.TIMETABLE || selectedTab != StudentTab.TIMETABLE) {
        currentScreen = StudentScreen.TIMETABLE
        selectedTab = StudentTab.TIMETABLE
    }

    Scaffold(
        bottomBar = {
            StudentBottomBar(
                selected = selectedTab,
                onTabClick = { tab ->
                    when (tab) {
                        StudentTab.TIMETABLE -> studentBusViewModel.refreshAll()
                        StudentTab.FAVORITES -> studentBusViewModel.refreshAll()
                        StudentTab.MYPAGE -> Unit
                    }
                    selectedTab = tab
                    currentScreen = when (tab) {
                        StudentTab.TIMETABLE -> StudentScreen.TIMETABLE
                        StudentTab.FAVORITES -> StudentScreen.FAVORITES
                        StudentTab.MYPAGE -> StudentScreen.MYPAGE
                    }
                }
            )
        }
    ) { innerPadding ->
        Box(modifier = Modifier.padding(innerPadding)) {
            when (currentScreen) {
                StudentScreen.TIMETABLE -> StudentTimetableContent(
                    offCampusSchedules = liveOffCampusSchedules,
                    onCampusSchedules = liveOnCampusSchedules,
                    isLoading = studentUiState.isLoading,
                    errorMessage = studentUiState.errorMessage,
                    onRefresh = { studentBusViewModel.refreshAll() },
                    onScheduleClick = {
                        if (viewModel.userRole == UserRole.DRIVER) {
                            onGoDriverOperation(it.toDriverRoute())
                        } else {
                            selectedSchedule = it
                            studentBusViewModel.loadRouteStatus(it)
                            currentScreen = StudentScreen.ROUTE_STATUS
                        }
                    }
                )

                StudentScreen.ROUTE_STATUS -> StudentRouteStatusContent(
                    schedule = liveSelectedSchedule,
                    routeDetail = liveRouteDetail,
                    isFavorite = favoriteSchedules.any { it.schedule.id == liveSelectedSchedule.id },
                    onRefresh = {
                        studentBusViewModel.refreshAll(selectedSchedule)
                    },
                    onBackClick = {
                        currentScreen = StudentScreen.TIMETABLE
                        selectedTab = StudentTab.TIMETABLE
                    },
                    onFavoriteClick = { favoriteTarget = liveSelectedSchedule }
                )

                StudentScreen.FAVORITES -> StudentFavoritesContent(
                    favorites = favoriteSchedules,
                    onRefresh = { studentBusViewModel.refreshAll() },
                    onScheduleClick = {
                        selectedSchedule = it.schedule
                        studentBusViewModel.loadRouteStatus(it.schedule)
                        currentScreen = StudentScreen.ROUTE_STATUS
                    },
                    onFavoriteEditClick = { favorite ->
                        favoriteTarget = favorite.schedule
                    }
                )

                StudentScreen.MYPAGE -> StudentMyPageContent(
                    viewModel = viewModel,
                    onLogout = onLogout,
                    onGoServiceTerms = onGoServiceTerms,
                    onGoPrivacyTerms = onGoPrivacyTerms,
                    onGoDriverTimetable = onGoDriverTimetable,
                    onGoDriverOperation = onGoDriverOperation,
                    onGoAdminRouteManagement = onGoAdminRouteManagement,
                    onGoAdminStopManagement = onGoAdminStopManagement,
                    onGoAdminTimetableManagement = onGoAdminTimetableManagement,
                    driverViewModel = driverViewModel,
                    schedules = liveOffCampusSchedules + liveOnCampusSchedules,
                    onRefreshSchedules = { studentBusViewModel.refreshAll() }
                )
            }

            favoriteTarget?.let { target ->
                FavoriteDaySheet(
                    schedule = target,
                    initialDays = favoriteSchedules.firstOrNull { it.schedule.id == target.id }?.days ?: emptySet(),
                    onDismiss = { favoriteTarget = null },
                    onSave = { days ->
                        studentBusViewModel.saveFavorite(target, days)
                        favoriteTarget = null
                    }
                )
            }
        }
    }
}

private fun List<BusSchedule>.withLivePassengerState(driverViewModel: DriverViewModel): List<BusSchedule> {
    return map { it.withLivePassengerState(driverViewModel) }
}

private fun BusSchedule.withLivePassengerState(driverViewModel: DriverViewModel): BusSchedule {
    val normalized = copy(
        totalSeats = FIXED_TOTAL_SEATS,
        remainingSeats = remainingSeats.coerceIn(0, FIXED_TOTAL_SEATS)
    )
    val route = driverViewModel.selectedRoute ?: return normalized
    val sameTimetable = route.timetableId != null && timetableId != null && route.timetableId == timetableId
    val sameLegacyRoute = route.timetableId == null && timetableId == null &&
        route.routeName == routeName &&
        route.scheduledTime == departureTime
    if (!sameTimetable && !sameLegacyRoute) return normalized

    val currentPassengers = driverViewModel.passengerCount.coerceIn(0, FIXED_TOTAL_SEATS)
    val liveLocation = when (driverViewModel.operationState) {
        OperationState.BEFORE_DEPARTURE -> "운행 전입니다"
        OperationState.OPERATING -> "운행 중입니다"
        OperationState.COMPLETED -> "운행이 종료되었습니다"
    }
    return normalized.copy(
        remainingSeats = (FIXED_TOTAL_SEATS - currentPassengers).coerceIn(0, FIXED_TOTAL_SEATS),
        currentLocation = liveLocation
    )
}

private fun BusSchedule.toDriverRoute(): DriverRoute {
    val stops = routeStops
        .ifEmpty { mockRouteDetailFor(this).stops }
        .map { it.replace("\n", " ") }

    return DriverRoute(
        id = "timetable_$id",
        routeName = routeName,
        scheduledTime = departureTime,
        totalSeats = totalSeats,
        stops = stops,
        timetableId = timetableId ?: id.toLong()
    )
}

private fun RouteDetail.withSeatText(schedule: BusSchedule): RouteDetail {
    val seatText = "${schedule.remainingSeats.toString().padStart(2, '0')}석"
    return copy(
        plannedDeparture = schedule.departureTime,
        stopInfos = stopInfos.mapValues { (_, info) -> info.copy(seatText = seatText) }
    )
}

@Composable
private fun PullToRefreshContainer(
    modifier: Modifier = Modifier,
    isRefreshing: Boolean = false,
    onRefresh: () -> Unit,
    content: @Composable BoxScope.() -> Unit
) {
    var pullDistance by remember { mutableStateOf(0f) }
    var gestureRefreshing by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val refreshing = isRefreshing || gestureRefreshing

    Box(
        modifier = modifier.pointerInput(refreshing) {
            detectVerticalDragGestures(
                onVerticalDrag = { change, dragAmount ->
                    if (dragAmount > 0f) {
                        pullDistance = (pullDistance + dragAmount).coerceAtMost(240f)
                        change.consume()
                    }
                },
                onDragCancel = {
                    pullDistance = 0f
                },
                onDragEnd = {
                    if (pullDistance >= 120f && !refreshing) {
                        gestureRefreshing = true
                        onRefresh()
                        scope.launch {
                            delay(900)
                            gestureRefreshing = false
                        }
                    }
                    pullDistance = 0f
                }
            )
        }
    ) {
        content()
        AnimatedVisibility(
            visible = refreshing || pullDistance > 36f,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier
                .align(Alignment.TopCenter)
                .offset(y = ((pullDistance / 7f).coerceIn(0f, 30f)).dp)
        ) {
            Surface(
                shape = RoundedCornerShape(50.dp),
                color = Color.White,
                shadowElevation = 4.dp,
                border = BorderStroke(1.dp, Color(0xFFE0E4EA))
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (refreshing) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            color = NavyBlue,
                            strokeWidth = 2.dp
                        )
                    }
                    Text(
                        if (refreshing) "새로고침 중" else "놓으면 새로고침",
                        fontSize = 12.sp,
                        color = NavyBlue,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

// ── 시간표 화면 ─────────────────────────────────────────────────
@Composable
private fun StudentTimetableContent(
    offCampusSchedules: List<BusSchedule>,
    onCampusSchedules: List<BusSchedule>,
    isLoading: Boolean,
    errorMessage: String?,
    onRefresh: () -> Unit,
    onScheduleClick: (BusSchedule) -> Unit
) {
    var selectedCampus by rememberSaveable { mutableStateOf(0) }
    val schedules = if (selectedCampus == 0) onCampusSchedules else offCampusSchedules
    val routes = schedules.map { it.routeName }.distinct()
    var selectedRoute by rememberSaveable { mutableStateOf("") }
    var dropdownOpen by remember { mutableStateOf(false) }
    var selectedHour by rememberSaveable { mutableStateOf(9) }

    LaunchedEffect(selectedCampus, routes) {
        if (selectedRoute !in routes) selectedRoute = routes.firstOrNull().orEmpty()
    }

    PullToRefreshContainer(
        modifier = Modifier.fillMaxSize(),
        isRefreshing = isLoading,
        onRefresh = onRefresh
    ) {
    Column(modifier = Modifier.fillMaxSize().background(Color.White)) {
        TabRow(
            selectedTabIndex = selectedCampus,
            containerColor = Color.White,
            contentColor = NavyBlue
        ) {
            listOf("교내", "교외").forEachIndexed { idx, label ->
                Tab(
                    selected = selectedCampus == idx,
                    onClick = {
                        selectedCampus = idx
                        selectedRoute = ""
                        dropdownOpen = false
                        selectedHour = if (idx == 0) 9 else 8
                    },
                    text = {
                        Text(
                            label,
                            fontWeight = if (selectedCampus == idx) FontWeight.Bold else FontWeight.Normal,
                            color = if (selectedCampus == idx) NavyBlue else Color(0xFF999999)
                        )
                    }
                )
            }
        }

        Column(modifier = Modifier.weight(1f).padding(horizontal = 16.dp)) {
            Spacer(Modifier.height(12.dp))

            errorMessage?.let { message ->
                Text(message, fontSize = 12.sp, color = Color(0xFF999999))
                Spacer(Modifier.height(8.dp))
            }
            if (isLoading) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth(), color = NavyBlue)
                Spacer(Modifier.height(8.dp))
            }

            Box {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFFF5F6F8))
                        .clickable { dropdownOpen = !dropdownOpen }
                        .padding(horizontal = 14.dp, vertical = 12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(selectedRoute.ifBlank { "노선 정보 없음" }, fontSize = 14.sp, color = Color(0xFF333333))
                        Text(if (dropdownOpen) "⌃" else "⌄", fontSize = 16.sp, color = Color.Gray)
                    }
                }
                DropdownMenu(
                    expanded = dropdownOpen,
                    onDismissRequest = { dropdownOpen = false },
                    modifier = Modifier.background(Color.White)
                ) {
                    routes.forEach { route ->
                        DropdownMenuItem(
                            text = {
                                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text(route, fontSize = 14.sp)
                                    if (route == selectedRoute) Text("✓", color = NavyBlue, fontWeight = FontWeight.Bold)
                                }
                            },
                            onClick = { selectedRoute = route; dropdownOpen = false }
                        )
                    }
                }
            }

            Spacer(Modifier.height(12.dp))

            val routeSchedules = schedules.filter { it.routeName == selectedRoute }.ifEmpty { schedules }
            val routeHours = routeSchedules
                .mapNotNull { it.departureTime.substringBefore(":").toIntOrNull() }
                .distinct()
                .sorted()
            val visibleHours = routeHours.ifEmpty { if (schedules.isEmpty()) emptyList() else hours }

            LaunchedEffect(selectedRoute, selectedCampus, routeHours) {
                if (routeHours.isNotEmpty() && selectedHour !in routeHours) {
                    selectedHour = routeHours.first()
                }
            }

            if (visibleHours.isNotEmpty()) {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(visibleHours) { hour ->
                        val isSelected = hour == selectedHour
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(20.dp))
                                .background(if (isSelected) NavyBlue else Color(0xFFF1F3F6))
                                .clickable { selectedHour = hour }
                                .padding(horizontal = 14.dp, vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                "${hour.toString().padStart(2, '0')}시",
                                fontSize = 13.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                color = if (isSelected) Color.White else Color(0xFF555555)
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(12.dp))

            val visibleSchedules = routeSchedules.filter { it.departureTime.substringBefore(":").toIntOrNull() == selectedHour }

            if (visibleSchedules.isEmpty()) {
                EmptyStateMessage("해당 시간대 운행 정보가 없습니다.")
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    items(visibleSchedules) { schedule ->
                        StudentTimetableCard(schedule = schedule, onClick = { onScheduleClick(schedule) })
                    }
                    item { Spacer(Modifier.height(8.dp)) }
                }
            }
        }
    }
    }
}

@Composable
private fun EmptyStateMessage(message: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 44.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(message, fontSize = 14.sp, color = Color(0xFF999999), textAlign = TextAlign.Center)
    }
}

@Composable
private fun StudentTimetableCard(schedule: BusSchedule, onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable { onClick() },
        shape    = RoundedCornerShape(14.dp),
        colors   = CardDefaults.cardColors(containerColor = Color(0xFFF8F9FB)),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.Bottom) {
                Text(schedule.departureTime, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color(0xFF111111))
                Spacer(Modifier.width(6.dp))
                Text(
                    "(${schedule.remainingSeats}석)",
                    fontSize   = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color      = if (schedule.remainingSeats <= 10) Color(0xFFE53935) else NavyBlue
                )
            }
            Spacer(Modifier.height(8.dp))
            Text("현재 위치  |  ${schedule.currentLocation}", fontSize = 12.sp, color = Color.Gray)
        }
    }
}

// ── 노선 상태 화면 ──────────────────────────────────────────────
@Composable
private fun StudentRouteStatusContent(
    schedule: BusSchedule,
    routeDetail: RouteDetail,
    isFavorite: Boolean,
    onRefresh: () -> Unit,
    onBackClick: () -> Unit,
    onFavoriteClick: () -> Unit
) {
    val stops = routeDetail.stops
    val busProgressIndex = routeDetail.busProgressIndex.coerceIn(0f, stops.lastIndex.toFloat())
    val isCurvedRoute = stops.size >= 7
    var selectedStopIndex by remember(schedule.id, stops) {
        mutableStateOf<Int?>(null)
    }

    PullToRefreshContainer(
        modifier = Modifier.fillMaxSize(),
        onRefresh = onRefresh
    ) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White),
        contentAlignment = Alignment.TopCenter
    ) {
        Box(
            modifier = Modifier
                .width(373.dp)
                .fillMaxHeight()
                .background(Color.White)
        ) {
            Box(
                modifier = Modifier
                    .offset(x = 0.dp, y = 42.dp)
                    .width(373.dp)
                    .height(56.dp)
                    .border(width = 0.dp, color = Color.Transparent)
                    .padding(horizontal = 12.dp, vertical = 16.dp)
            ) {
                Box(
                    modifier = Modifier
                        .align(Alignment.CenterStart)
                        .size(32.dp)
                        .offset(x = -4.dp, y = -4.dp)
                        .clickable(onClick = onBackClick),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "뒤로가기",
                        tint = Color(0xFF111111),
                        modifier = Modifier.size(17.dp)
                    )
                }
                Text(
                    schedule.routeName,
                    style = ShuttleRegularTextStyle,
                    color = Color(0xFF111111),
                    modifier = Modifier.align(Alignment.Center).offset(y = -1.dp)
                )
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .offset(y = 16.dp)
                        .width(373.dp)
                        .height(1.dp)
                        .background(Color(0xFFE7E9EE))
                )
            }

            Box(
                modifier = Modifier
                    .offset(x = 122.dp, y = 174.dp)
                    .width(129.dp)
                    .height(31.dp)
                    .clip(RoundedCornerShape(50.dp))
                    .border(3.dp, if (routeDetail.isRunning) Color(0xFFE2573B) else Color(0xFFD8DEE8), RoundedCornerShape(50.dp))
                    .background(Color.White)
                    .padding(horizontal = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    if (routeDetail.isRunning) "예상: ${routeDetail.etaText}".replace("조회 중", "조회중") else routeDetail.statusText,
                    style = ShuttleRegularTextStyle.copy(
                        fontSize = 13.sp,
                        lineHeight = 13.sp,
                        fontWeight = FontWeight.Bold
                    ),
                    color = if (routeDetail.isRunning) Color(0xFFE2573B) else Color(0xFF999999),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            BusIcon(size = 55.dp, modifier = Modifier.offset(x = 159.dp, y = 221.dp))

            Row(
                modifier = Modifier
                    .offset(x = 14.dp, y = 290.dp)
                    .width(345.dp)
                    .height(31.dp)
                    .clip(RoundedCornerShape(50.dp))
                    .border(1.5.dp, Color(0xFFD8DEE8), RoundedCornerShape(50.dp))
                    .background(Color.White)
                    .padding(horizontal = 22.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("출발 계획 | ${routeDetail.plannedDeparture}", style = ShuttleRegularTextStyle, color = Color(0xFF333333))
                Text("실제 출발 | ${routeDetail.actualDeparture.toDisplayClockText()}", style = ShuttleRegularTextStyle, color = Color(0xFF333333))
            }

            Box(
                modifier = Modifier
                    .offset(x = 26.dp, y = 332.dp)
                    .width(321.dp)
                    .height(213.dp)
            ) {
                RouteProgressBar(
                    stops = stops,
                    busProgressIndex = busProgressIndex,
                    isCurvedRoute = isCurvedRoute,
                    showBusMarker = routeDetail.isRunning,
                    onStopClick = { selectedStopIndex = it }
                )
            }

            val selectedStopInfo = selectedStopIndex?.let { index ->
                val stopName = stops.getOrElse(index) { "정류장" }.replace("\n", " ")
                routeDetail.infoFor(stopName)
            }
            AnimatedVisibility(
                visible = selectedStopInfo != null,
                enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
                exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
                modifier = Modifier.align(Alignment.BottomCenter)
            ) {
                selectedStopInfo?.let { info ->
                    StopInfoPanel(
                        stopInfo = info,
                        isFavorite = isFavorite,
                        onFavoriteClick = onFavoriteClick
                    )
                }
            }
        }
    }
    }
}
@Composable
private fun StopInfoPanel(
    stopInfo: StopArrivalInfo,
    isFavorite: Boolean,
    onFavoriteClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .width(341.dp)
            .height(164.dp)
            .clip(RoundedCornerShape(topStart = 14.dp, topEnd = 14.dp))
            .border(1.dp, Color(0xFFD5D7DC), RoundedCornerShape(topStart = 14.dp, topEnd = 14.dp))
            .background(Color.White)
            .padding(horizontal = 18.dp, vertical = 14.dp)
    ) {
        Box(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .clip(RoundedCornerShape(50.dp))
                .border(1.dp, Color(0xFFD5D7DC), RoundedCornerShape(50.dp))
                .background(Color.White)
                .padding(horizontal = 18.dp, vertical = 5.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(stopInfo.stopName, style = ShuttleRegularTextStyle, color = Color(0xFF333333))
        }

        Icon(
            Icons.Filled.Star,
            contentDescription = "즐겨찾기",
            tint = if (isFavorite) Color(0xFFFFC400) else Color(0xFFE4E7ED),
            modifier = Modifier
                .align(Alignment.TopEnd)
                .size(30.dp)
                .clickable(onClick = onFavoriteClick)
        )

        Column(modifier = Modifier.offset(y = 58.dp)) {
            Text("도착 정보: ${stopInfo.arrivalText}", style = ShuttleRegularTextStyle, color = Color(0xFF333333))
            Spacer(Modifier.height(12.dp))
            Text("예상: ${stopInfo.seatText}", style = ShuttleRegularTextStyle, color = Color(0xFF333333))
        }
    }
}
@Composable
private fun BusLocationMarker(
    modifier: Modifier = Modifier,
    width: Dp = 68.dp,
    height: Dp = 94.dp
) {
    val context = LocalContext.current
    val markerBitmap = remember {
        context.assets.open("frame/BUS_MARKER.png").use { BitmapFactory.decodeStream(it) }
    }

    Image(
        bitmap = markerBitmap.asImageBitmap(),
        contentDescription = "버스 위치",
        contentScale = ContentScale.Fit,
        modifier = modifier.size(width = width, height = height)
    )
}

@Composable
private fun RouteProgressBar(
    stops: List<String>,
    busProgressIndex: Float,
    isCurvedRoute: Boolean,
    showBusMarker: Boolean = true,
    onStopClick: (Int) -> Unit = {}
) {
    val navy = NavyBlue
    val inactive = Color(0xFFE3E7ED)

    if (isCurvedRoute && stops.size >= 7) {
        CurvedBusRoute(stops = stops, busProgressIndex = busProgressIndex, showBusMarker = showBusMarker, navy = navy, inactive = inactive, onStopClick = onStopClick)
    } else {
        StraightBusRoute(stops = stops, busProgressIndex = busProgressIndex, showBusMarker = showBusMarker, navy = navy, inactive = inactive, onStopClick = onStopClick)
    }
}

@Composable
private fun StraightBusRoute(
    stops: List<String>,
    busProgressIndex: Float,
    showBusMarker: Boolean,
    navy: Color,
    inactive: Color,
    onStopClick: (Int) -> Unit
) {
    val clampedBus = busProgressIndex.coerceIn(0f, stops.lastIndex.toFloat())
    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val lineY = maxHeight * 0.38f
        val startX = maxWidth * 0.07f
        val endX = maxWidth * 0.93f
        val gap = (endX - startX) / (stops.size - 1).coerceAtLeast(1)
        val lineStroke = 12.dp
        val stopRadius = 14.5.dp
        val stopStroke = 5.dp

        Canvas(modifier = Modifier.matchParentSize()) {
            val lineYpx = lineY.toPx()
            val startXpx = startX.toPx()
            val endXpx = endX.toPx()
            val gapPx = gap.toPx()
            val lineStrokePx = lineStroke.toPx()
            val stopRadiusPx = stopRadius.toPx()
            val stopStrokePx = stopStroke.toPx()

            drawLine(navy, Offset(startXpx, lineYpx), Offset(endXpx, lineYpx), strokeWidth = lineStrokePx, cap = StrokeCap.Round)

            stops.forEachIndexed { index, _ ->
                val x = startXpx + gapPx * index
                drawCircle(Color.White, radius = stopRadiusPx, center = Offset(x, lineYpx))
                drawCircle(navy, radius = stopRadiusPx, center = Offset(x, lineYpx), style = Stroke(width = stopStrokePx))
            }
        }

        stops.forEachIndexed { index, _ ->
            val x = startX + gap * index
            Box(
                modifier = Modifier
                    .offset(x = x - 29.dp, y = lineY - 29.dp)
                    .size(58.dp)
                    .clip(CircleShape)
                    .clickable { onStopClick(index) }
            )
        }

        if (showBusMarker) {
            val markerX = startX + gap * clampedBus - 34.dp
            BusLocationMarker(
                modifier = Modifier.offset(x = markerX, y = lineY - 67.dp),
                width = 70.dp,
                height = 96.dp
            )
        }

        val labelWidth = if (stops.size >= 6) 58.dp else 68.dp
        val labelFontSize = if (stops.size >= 6) 10.sp else 11.sp
        val labelLineHeight = if (stops.size >= 6) 12.sp else 13.sp
        stops.forEachIndexed { index, stop ->
            val x = startX + gap * index
            val rawLabelX = x - labelWidth / 2
            val maxLabelX = maxWidth - labelWidth
            val labelX = when {
                rawLabelX < 0.dp -> 0.dp
                rawLabelX > maxLabelX -> maxLabelX
                else -> rawLabelX
            }
            Text(
                text = stop,
                fontSize = labelFontSize,
                color = Color(0xFF222222),
                textAlign = TextAlign.Center,
                lineHeight = labelLineHeight,
                fontWeight = FontWeight.Medium,
                modifier = Modifier
                    .offset(x = labelX, y = lineY + 29.dp)
                    .width(labelWidth)
            )
        }
    }
}

@Composable
private fun CurvedBusRoute(
    stops: List<String>,
    busProgressIndex: Float,
    showBusMarker: Boolean,
    navy: Color,
    inactive: Color,
    onStopClick: (Int) -> Unit
) {
    val clampedBus = busProgressIndex.coerceIn(0f, stops.lastIndex.toFloat())
    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val topY = maxHeight * 0.28f
        val bottomY = maxHeight * 0.66f
        val rightY = maxHeight * 0.45f
        val leftX = maxWidth * 0.10f
        val innerRightX = maxWidth * 0.58f
        val turnX = maxWidth * 0.86f
        val topCount = (stops.size / 2).coerceAtLeast(3)
        val sideIndex = topCount
        val bottomCount = (stops.size - topCount - 1).coerceAtLeast(1)
        val topPoints = evenlySpacedRoutePoints(topCount, leftX, innerRightX, topY)
        val sidePoint = turnX to rightY
        val bottomPoints = evenlySpacedRoutePoints(bottomCount, innerRightX, leftX, bottomY)
        val routePoints = topPoints + sidePoint + bottomPoints
        val topCorner = turnX to topY
        val bottomCorner = turnX to bottomY

        Canvas(modifier = Modifier.matchParentSize()) {
            fun p(index: Int) = Offset(routePoints[index].first.toPx(), routePoints[index].second.toPx())
            fun corner(pair: Pair<Dp, Dp>) = Offset(pair.first.toPx(), pair.second.toPx())

            val lineStroke = 12.dp.toPx()
            val stopRadius = 14.5.dp.toPx()
            val stopStroke = 5.dp.toPx()
            val routePath = Path().apply {
                moveTo(p(0).x, p(0).y)
                for (index in 1 until topCount) {
                    lineTo(p(index).x, p(index).y)
                }
                lineTo(corner(topCorner).x, corner(topCorner).y)
                lineTo(p(sideIndex).x, p(sideIndex).y)
                lineTo(corner(bottomCorner).x, corner(bottomCorner).y)
                for (index in (sideIndex + 1) until routePoints.size) {
                    lineTo(p(index).x, p(index).y)
                }
            }

            drawPath(
                path = routePath,
                color = navy,
                style = Stroke(width = lineStroke, cap = StrokeCap.Round, join = StrokeJoin.Round)
            )

            routePoints.forEachIndexed { index, _ ->
                drawCircle(Color.White, radius = stopRadius, center = p(index))
                drawCircle(navy, radius = stopRadius, center = p(index), style = Stroke(width = stopStroke))
            }
        }

        routePoints.forEachIndexed { index, point ->
            Box(
                modifier = Modifier
                    .offset(x = point.first - 31.dp, y = point.second - 31.dp)
                    .size(62.dp)
                    .clip(CircleShape)
                    .clickable { onStopClick(index) }
            )
        }

        if (showBusMarker) {
            val markerPosition = interpolateCurvedRoutePoint(routePoints, clampedBus, topCorner, bottomCorner, sideIndex)
            BusLocationMarker(
                modifier = Modifier.offset(
                    x = markerPosition.first - 35.dp,
                    y = markerPosition.second - 69.dp
                ),
                width = 70.dp,
                height = 96.dp
            )
        }

        routePoints.forEachIndexed { index, point ->
            val labelXOffset = if (index == sideIndex) (-52).dp else (-32).dp
            val labelWidth = if (index == sideIndex) 58.dp else 64.dp
            Text(
                text = stops.getOrElse(index) { "" },
                fontSize = 10.sp,
                color = Color(0xFF222222),
                textAlign = TextAlign.Center,
                lineHeight = 11.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier
                    .offset(
                        x = point.first + labelXOffset,
                        y = point.second + 18.dp
                    )
                    .width(labelWidth)
            )
        }
    }
}

private fun evenlySpacedRoutePoints(
    count: Int,
    startX: Dp,
    endX: Dp,
    y: Dp
): List<Pair<Dp, Dp>> {
    if (count <= 1) return listOf(startX to y)
    return List(count) { index ->
        val fraction = index.toFloat() / (count - 1).toFloat()
        (startX + (endX - startX) * fraction) to y
    }
}

private fun interpolateRoutePoint(points: List<Pair<androidx.compose.ui.unit.Dp, androidx.compose.ui.unit.Dp>>, progress: Float): Pair<androidx.compose.ui.unit.Dp, androidx.compose.ui.unit.Dp> {
    val startIndex = progress.toInt().coerceIn(0, points.lastIndex)
    val endIndex = (startIndex + 1).coerceAtMost(points.lastIndex)
    val fraction = (progress - startIndex).coerceIn(0f, 1f)
    val start = points[startIndex]
    val end = points[endIndex]
    return (start.first + (end.first - start.first) * fraction) to (start.second + (end.second - start.second) * fraction)
}

private fun interpolateCurvedRoutePoint(
    points: List<Pair<Dp, Dp>>,
    progress: Float,
    topCorner: Pair<Dp, Dp>,
    bottomCorner: Pair<Dp, Dp>,
    sideIndex: Int
): Pair<Dp, Dp> {
    val startIndex = progress.toInt().coerceIn(0, points.lastIndex)
    val endIndex = (startIndex + 1).coerceAtMost(points.lastIndex)
    val fraction = (progress - startIndex).coerceIn(0f, 1f)
    val start = points[startIndex]
    val end = points[endIndex]

    fun lerpDp(start: Pair<Dp, Dp>, end: Pair<Dp, Dp>, t: Float): Pair<Dp, Dp> {
        return (start.first + (end.first - start.first) * t) to (start.second + (end.second - start.second) * t)
    }

    return when (startIndex) {
        sideIndex - 1 -> if (fraction < 0.5f) {
            lerpDp(start, topCorner, fraction * 2f)
        } else {
            lerpDp(topCorner, end, (fraction - 0.5f) * 2f)
        }
        sideIndex -> if (fraction < 0.5f) {
            lerpDp(start, bottomCorner, fraction * 2f)
        } else {
            lerpDp(bottomCorner, end, (fraction - 0.5f) * 2f)
        }
        else -> lerpDp(start, end, fraction)
    }
}

@Composable
private fun CurvedRouteProgressBar(stops: List<String>, currentStopIndex: Int) {
    RouteProgressBar(stops = stops, busProgressIndex = currentStopIndex.toFloat(), isCurvedRoute = true)
}

// ── 즐겨찾기 ───────────────────────────────────────────────────
@Composable
private fun StudentFavoritesContent(
    favorites: List<FavoriteSchedule>,
    onRefresh: () -> Unit,
    onScheduleClick: (FavoriteSchedule) -> Unit,
    onFavoriteEditClick: (FavoriteSchedule) -> Unit
) {
    var selectedCampus by remember { mutableStateOf("교내") }
    var selectedDay by remember { mutableStateOf(notificationDays.first()) }
    val campusOptions = listOf("교내", "교외")
    val dayOptions = notificationDays
    val visibleFavorites = favorites.filter { favorite ->
        favorite.schedule.campusType == selectedCampus && selectedDay in favorite.days
    }

    PullToRefreshContainer(
        modifier = Modifier.fillMaxSize(),
        onRefresh = onRefresh
    ) {
    Column(modifier = Modifier.fillMaxSize().background(Color.White)) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(54.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                "즐겨찾기",
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold,
                color = Color.Black
            )
        }

        TabRow(
            selectedTabIndex = campusOptions.indexOf(selectedCampus).coerceAtLeast(0),
            containerColor = Color.White,
            contentColor = NavyBlue
        ) {
            campusOptions.forEach { option ->
                Tab(
                    selected = selectedCampus == option,
                    onClick = { selectedCampus = option },
                    text = {
                        Text(
                            option,
                            fontSize = 14.sp,
                            fontWeight = if (selectedCampus == option) FontWeight.Bold else FontWeight.Normal,
                            color = if (selectedCampus == option) NavyBlue else Color(0xFF9DA3AD)
                        )
                    }
                )
            }
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFFF5F6F8))
                .padding(horizontal = 16.dp)
        ) {
            Spacer(Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(9.dp)
            ) {
                dayOptions.forEach { day ->
                    FavoriteDayChip(
                        label = shortDayLabel(day),
                        selected = selectedDay == day,
                        modifier = Modifier.weight(1f),
                        onClick = { selectedDay = day }
                    )
                }
            }
            Spacer(Modifier.height(14.dp))

            if (favorites.isEmpty() || visibleFavorites.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(top = 40.dp),
                    contentAlignment = Alignment.TopCenter
                ) {
                    Text(
                        if (favorites.isEmpty()) "아직 추가된 즐겨찾기가 없습니다" else "선택한 분류의 즐겨찾기가 없습니다",
                        color = Color(0xFFB5BAC2),
                        fontSize = 13.sp
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    contentPadding = PaddingValues(bottom = 20.dp)
                ) {
                    items(
                        items = visibleFavorites,
                        key = { favorite -> "${favorite.schedule.id}-${favorite.schedule.routeName}-${favorite.schedule.departureTime}" }
                    ) { favorite ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(8.dp),
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(8.dp))
                                        .clickable { onScheduleClick(favorite) }
                                ) {
                                    Text(
                                        favorite.schedule.routeName,
                                        fontSize = 13.sp,
                                        color = NavyBlue,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Spacer(Modifier.height(10.dp))
                                    Row(verticalAlignment = Alignment.Bottom) {
                                        Text(
                                            favorite.schedule.departureTime,
                                            fontSize = 21.sp,
                                            color = Color(0xFF1F2430),
                                            fontWeight = FontWeight.Bold
                                        )
                                        Spacer(Modifier.width(6.dp))
                                        Text(
                                            "(${favorite.schedule.remainingSeats}석)",
                                            color = Color(0xFFE53935),
                                            fontSize = 18.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                    Spacer(Modifier.height(10.dp))
                                    Text(
                                        "탑승 정류장 | ${favorite.schedule.currentLocation}",
                                        fontSize = 12.sp,
                                        color = Color(0xFF8A8F98)
                                    )
                                }
                                Box(
                                    modifier = Modifier
                                        .size(44.dp)
                                        .clip(CircleShape)
                                        .clickable { onFavoriteEditClick(favorite) },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        Icons.Filled.Star,
                                        contentDescription = "즐겨찾기 요일 수정",
                                        tint = Color(0xFFFFC400),
                                        modifier = Modifier.size(34.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
    }
}

@Composable
private fun FavoriteDayChip(
    label: String,
    selected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Box(
        modifier = modifier
            .height(34.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(if (selected) NavyBlue else Color(0xFFECEFF4))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(
            label,
            fontSize = 12.sp,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
            color = if (selected) Color.White else Color(0xFF9AA1AB)
        )
    }
}

private fun shortDayLabel(day: String): String = when (day) {
    "월요일" -> "월"
    "화요일" -> "화"
    "수요일" -> "수"
    "목요일" -> "목"
    "금요일" -> "금"
    else -> day
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FavoriteDaySheet(
    schedule: BusSchedule,
    initialDays: Set<String>,
    onDismiss: () -> Unit,
    onSave: (Set<String>) -> Unit
) {
    var selectedDays by remember(schedule.id) { mutableStateOf(initialDays) }
    var isSubmitting by remember(schedule.id) { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Color.White
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 18.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("알림 받을 요일을 선택해 주세요", style = ShuttleRegularTextStyle, color = Color.Black)
                    Spacer(Modifier.height(6.dp))
                    Text("중복 선택 가능", style = ShuttleRegularTextStyle, color = Color(0xFFB8BEC8))
                }
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        Icons.Filled.Close,
                        contentDescription = "닫기",
                        tint = Color(0xFF9AA1AB)
                    )
                }
            }
            Spacer(Modifier.height(6.dp))
            Spacer(Modifier.height(8.dp))

            notificationDays.forEach { day ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(42.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (day in selectedDays) Color(0xFFEFF3FB) else Color(0xFFF5F6F8))
                        .clickable {
                            selectedDays = if (day in selectedDays) selectedDays - day else selectedDays + day
                        }
                        .padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        day,
                        style = ShuttleRegularTextStyle,
                        color = if (day in selectedDays) NavyBlue else Color(0xFF555555),
                        modifier = Modifier.weight(1f)
                    )
                }
                Spacer(Modifier.height(8.dp))
            }

            Spacer(Modifier.height(4.dp))
            Button(
                onClick = {
                    if (!isSubmitting) {
                        isSubmitting = true
                        onSave(selectedDays)
                        onDismiss()
                    }
                },
                enabled = !isSubmitting,
                modifier = Modifier.fillMaxWidth().height(48.dp),
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = NavyBlue,
                    disabledContainerColor = Color(0xFF9FB0C4)
                )
            ) {
                if (isSubmitting) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        strokeWidth = 2.dp,
                        color = Color.White
                    )
                    Spacer(Modifier.width(8.dp))
                    Text("저장 중", style = ShuttleRegularTextStyle, color = Color.White)
                } else {
                    Text(if (selectedDays.isEmpty()) "즐겨찾기 해제하기" else "설정하기", style = ShuttleRegularTextStyle, color = Color.White)
                }
            }
        }
    }
}
// ── 역할별 마이페이지 ───────────────────────────────────────────
@Composable
private fun StudentMyPageContent(
    viewModel: AuthViewModel,
    onLogout: () -> Unit,
    onGoServiceTerms: () -> Unit,
    onGoPrivacyTerms: () -> Unit,
    onGoDriverTimetable: () -> Unit,
    onGoDriverOperation: (DriverRoute) -> Unit,
    onGoAdminRouteManagement: () -> Unit,
    onGoAdminStopManagement: () -> Unit,
    onGoAdminTimetableManagement: () -> Unit,
    driverViewModel: DriverViewModel,
    schedules: List<BusSchedule>,
    onRefreshSchedules: () -> Unit
) {
    val role = viewModel.userRole ?: UserRole.STUDENT
    val roleLabel = when (role) {
        UserRole.ADMIN -> "관리자"
        UserRole.DRIVER -> "버스기사"
        UserRole.STUDENT -> "사용자"
    }
    val badgeColor = when (role) {
        UserRole.ADMIN -> AdminBadge
        UserRole.DRIVER -> DriverBadge
        UserRole.STUDENT -> StudentBadge
    }
    val badgeTextColor = when (role) {
        UserRole.ADMIN -> AdminBadgeText
        UserRole.DRIVER -> DriverBadgeText
        UserRole.STUDENT -> StudentBadgeText
    }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val apiClient = remember { ShuttleApiClient }
    var adminFile by remember { mutableStateOf<AdminUploadFile?>(null) }
    var adminUploaded by remember { mutableStateOf(false) }
    var privacyPdfFile by remember { mutableStateOf<AdminUploadFile?>(null) }
    var privacyPdfUploaded by remember { mutableStateOf(false) }
    var serviceTermsPdfFile by remember { mutableStateOf<AdminUploadFile?>(null) }
    var serviceTermsPdfUploaded by remember { mutableStateOf(false) }
    var noticeMessage by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(viewModel.errorMessage) {
        viewModel.errorMessage?.let { message ->
            noticeMessage = message
            viewModel.clearErrors()
        }
    }
    val timetableFilePicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri ?: return@rememberLauncherForActivityResult
        persistReadPermission(context, uri)
        val file = resolveAdminUploadFile(context, uri, fallbackName = "시간표 파일")
        if (!file.isWithinUploadLimit()) {
            noticeMessage = "파일은 최대 10MB 이하만 등록할 수 있습니다"
        } else {
            adminFile = file
            adminUploaded = false
        }
    }
    val privacyPdfPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri ?: return@rememberLauncherForActivityResult
        persistReadPermission(context, uri)
        val file = resolveAdminUploadFile(context, uri, fallbackName = "개인정보 처리 방침.pdf")
        if (!file.isValidPdfUpload()) {
            noticeMessage = "PDF 파일만 10MB 이하로 등록할 수 있습니다"
        } else {
            privacyPdfFile = file
            privacyPdfUploaded = false
        }
    }
    val serviceTermsPdfPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri ?: return@rememberLauncherForActivityResult
        persistReadPermission(context, uri)
        val file = resolveAdminUploadFile(context, uri, fallbackName = "서비스 이용 약관.pdf")
        if (!file.isValidPdfUpload()) {
            noticeMessage = "PDF 파일만 10MB 이하로 등록할 수 있습니다"
        } else {
            serviceTermsPdfFile = file
            serviceTermsPdfUploaded = false
        }
    }

    var isPageRefreshing by remember { mutableStateOf(false) }
    fun refreshMyPage() {
        if (isPageRefreshing) return
        scope.launch {
            isPageRefreshing = true
            onRefreshSchedules()
            delay(700)
            isPageRefreshing = false
        }
    }

    PullToRefreshContainer(
        modifier = Modifier.fillMaxSize(),
        isRefreshing = isPageRefreshing,
        onRefresh = { refreshMyPage() }
    ) {
    Box(modifier = Modifier.fillMaxSize().background(Color.White)) {
        Column(modifier = Modifier.fillMaxSize()) {
            MyPageProfileHeader(
                roleLabel = roleLabel,
                email = viewModel.currentEmail,
                badgeColor = badgeColor,
                badgeTextColor = badgeTextColor
            )
            HorizontalDivider(color = DividerColor)

            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp)
            ) {
                Spacer(Modifier.height(24.dp))

                when (role) {
                    UserRole.ADMIN -> {
                        Text("버스 시간표 등록", fontSize = 15.sp, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(10.dp))
                        AdminTimetableUploadCard(
                            file = adminFile,
                            isUploaded = adminUploaded,
                            onPickFile = {
                                timetableFilePicker.launch(
                                    arrayOf(
                                        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                                        "application/vnd.ms-excel",
                                        "text/csv",
                                        "application/octet-stream"
                                    )
                                )
                            },
                            onRemoveFile = {
                                adminFile = null
                                adminUploaded = false
                            },
                            onSubmit = {
                                val file = adminFile
                                if (file == null) {
                                    noticeMessage = "등록할 시간표 파일을 먼저 선택해 주세요"
                                } else if (!file.isWithinUploadLimit()) {
                                    noticeMessage = "파일은 최대 10MB 이하만 등록할 수 있습니다"
                                } else if (adminUploaded) {
                                    adminUploaded = false
                                    noticeMessage = "시간표 파일을 다시 수정할 수 있습니다"
                                } else {
                                    scope.launch {
                                        noticeMessage = "버스 시간표를 업로드 중입니다"
                                        val success = apiClient.uploadFile(
                                            context = context,
                                            path = "/api/timetable",
                                            uri = file.uri,
                                            fileName = file.name,
                                            mimeType = file.mimeType
                                        )
                                        adminUploaded = success
                                        noticeMessage = if (success) {
                                            onRefreshSchedules()
                                            "버스 시간표가 업로드되었습니다"
                                        } else {
                                            "시간표 업로드에 실패했습니다. 서버 상태를 확인해 주세요"
                                        }
                                    }
                                }
                            }
                        )

                        Spacer(Modifier.height(24.dp))
                        HorizontalDivider(color = DividerColor)
                        Spacer(Modifier.height(20.dp))

                        AdminPassengerControlSection(
                            driverViewModel = driverViewModel,
                            schedules = schedules,
                            onNotice = { noticeMessage = it }
                        )
                        Spacer(Modifier.height(24.dp))
                        HorizontalDivider(color = DividerColor)
                        Spacer(Modifier.height(20.dp))

                        MyPageSectionHeader("서비스 관리")
                        MyPageRow("노선 관리", onClick = onGoAdminRouteManagement)
                        MyPageRow("정류장 관리", onClick = onGoAdminStopManagement)
                        MyPageRow("시간표 관리", onClick = onGoAdminTimetableManagement)
                        Spacer(Modifier.height(24.dp))
                        HorizontalDivider(color = DividerColor)
                        Spacer(Modifier.height(20.dp))
                    }

                    UserRole.DRIVER -> Unit

                    UserRole.STUDENT -> Unit
                }

                MyPageSectionHeader("서비스 안내")
                if (role == UserRole.ADMIN) {
                    AdminPdfUploadSection(
                        title = "개인정보 처리 방침",
                        file = privacyPdfFile,
                        isUploaded = privacyPdfUploaded,
                        onPickFile = { privacyPdfPicker.launch(arrayOf("application/pdf")) },
                        onRemoveFile = {
                            privacyPdfFile = null
                            privacyPdfUploaded = false
                        },
                        onSubmit = {
                            val file = privacyPdfFile
                            if (file?.isValidPdfUpload() == true) {
                                scope.launch {
                                    noticeMessage = "개인정보 처리 방침 PDF를 업로드 중입니다"
                                    val success = apiClient.uploadFile(
                                        context = context,
                                        path = "/api/terms/privacy",
                                        uri = file.uri,
                                        fileName = file.name,
                                        mimeType = file.mimeType
                                    )
                                    privacyPdfUploaded = success
                                    noticeMessage = if (success) {
                                        "개인정보 처리 방침 PDF가 업로드되었습니다"
                                    } else {
                                        "개인정보 처리 방침 PDF 업로드에 실패했습니다"
                                    }
                                }
                            } else {
                                noticeMessage = "PDF 파일만 10MB 이하로 등록할 수 있습니다"
                            }
                        }
                    )
                    Spacer(Modifier.height(14.dp))
                    AdminPdfUploadSection(
                        title = "서비스 이용 약관",
                        file = serviceTermsPdfFile,
                        isUploaded = serviceTermsPdfUploaded,
                        onPickFile = { serviceTermsPdfPicker.launch(arrayOf("application/pdf")) },
                        onRemoveFile = {
                            serviceTermsPdfFile = null
                            serviceTermsPdfUploaded = false
                        },
                        onSubmit = {
                            val file = serviceTermsPdfFile
                            if (file?.isValidPdfUpload() == true) {
                                scope.launch {
                                    noticeMessage = "서비스 이용 약관 PDF를 업로드 중입니다"
                                    val success = apiClient.uploadFile(
                                        context = context,
                                        path = "/api/terms/service",
                                        uri = file.uri,
                                        fileName = file.name,
                                        mimeType = file.mimeType
                                    )
                                    serviceTermsPdfUploaded = success
                                    noticeMessage = if (success) {
                                        "서비스 이용 약관 PDF가 업로드되었습니다"
                                    } else {
                                        "서비스 이용 약관 PDF 업로드에 실패했습니다"
                                    }
                                }
                            } else {
                                noticeMessage = "PDF 파일만 10MB 이하로 등록할 수 있습니다"
                            }
                        }
                    )
                } else {
                    MyPageRow("개인정보 처리 방침", onClick = onGoPrivacyTerms)
                    MyPageRow("서비스 이용 약관", onClick = onGoServiceTerms)
                }
                Spacer(Modifier.height(24.dp))

                MyPageSectionHeader("계정 설정")
                MyPageRow("로그아웃", onClick = onLogout)
                MyPageRow("탈퇴하기") {
                    noticeMessage = "회원 탈퇴를 처리 중입니다"
                    viewModel.withdraw(onSuccess = onLogout)
                }
                Spacer(Modifier.height(24.dp))
            }
        }

        noticeMessage?.let { message ->
            MyPageNoticeBar(
                message = message,
                onDismiss = { noticeMessage = null },
                modifier = Modifier.align(Alignment.BottomCenter)
            )
        }
    }
    }
}

@Composable
private fun MyPageProfileHeader(
    roleLabel: String,
    email: String,
    badgeColor: Color,
    badgeTextColor: Color
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White)
            .padding(horizontal = 20.dp, vertical = 16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(92.dp)
                .background(Color(0xFFF5F5F5))
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            BusIcon(size = 64.dp)
            Column(modifier = Modifier.padding(start = 16.dp)) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(badgeColor)
                        .padding(horizontal = 10.dp, vertical = 3.dp)
                ) {
                    Text(roleLabel, color = badgeTextColor, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                }
                Spacer(Modifier.height(4.dp))
                Text(
                    email.ifBlank { "로그인 정보 확인 중" },
                    color = Color(0xFF555555),
                    fontSize = 14.sp
                )
            }
        }
    }
}

@Composable
private fun AdminTimetableUploadCard(
    file: AdminUploadFile?,
    isUploaded: Boolean,
    onPickFile: () -> Unit,
    onRemoveFile: () -> Unit,
    onSubmit: () -> Unit
) {
    if (file == null) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(96.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(Color(0xFFF5F5F5))
                .border(1.dp, Color(0xFFEEEEEE), RoundedCornerShape(6.dp))
                .clickable(onClick = onPickFile),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    Icons.Filled.Add,
                    contentDescription = "시간표 파일 추가",
                    tint = Color(0xFFB8B8B8),
                    modifier = Modifier.size(24.dp)
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    "파일은 최대 10MB 이하까지만 첨부할 수 있어요",
                    color = Color(0xFFB8B8B8),
                    fontSize = 11.sp,
                    textAlign = TextAlign.Center
                )
            }
        }
    } else {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(46.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(Color(0xFFF5F6F8))
                .border(1.dp, Color(0xFFE9ECEF), RoundedCornerShape(6.dp))
                .padding(horizontal = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(file.name, fontSize = 14.sp, color = Color(0xFF333333), modifier = Modifier.weight(1f))
            Text("(${file.sizeLabel})", fontSize = 13.sp, color = Color(0xFF999999))
            if (!isUploaded) {
                Spacer(Modifier.width(8.dp))
                Icon(
                    Icons.Filled.Close,
                    contentDescription = "시간표 파일 제거",
                    tint = Color(0xFF999999),
                    modifier = Modifier.size(20.dp).clickable(onClick = onRemoveFile)
                )
            }
        }
    }

    Spacer(Modifier.height(8.dp))
    Row(modifier = Modifier.fillMaxWidth()) {
        Spacer(Modifier.weight(1f))
        Button(
            onClick = onSubmit,
            enabled = file != null,
            modifier = Modifier
                .width(72.dp)
                .height(36.dp),
            shape = RoundedCornerShape(18.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = NavyBlue,
                disabledContainerColor = Color(0xFFD9D9D9)
            )
        ) {
            Text(if (isUploaded) "수정" else "등록", color = Color.White, fontSize = 13.sp)
        }
    }
}

@Composable
private fun AdminPdfUploadSection(
    title: String,
    file: AdminUploadFile?,
    isUploaded: Boolean,
    onPickFile: () -> Unit,
    onRemoveFile: () -> Unit,
    onSubmit: () -> Unit
) {
    Text(title, fontSize = 14.sp, fontWeight = FontWeight.Medium, color = Color(0xFF333333))
    Spacer(Modifier.height(8.dp))
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            modifier = Modifier
                .weight(1f)
                .height(46.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(Color(0xFFF5F6F8))
                .border(1.dp, Color(0xFFE9ECEF), RoundedCornerShape(6.dp))
                .clickable(onClick = onPickFile)
                .padding(horizontal = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                file?.name ?: "PDF 파일 선택",
                fontSize = 14.sp,
                color = if (file == null) Color(0xFF999999) else Color(0xFF333333),
                modifier = Modifier.weight(1f)
            )
            Text(
                "(${file?.sizeLabel ?: "0mb"})",
                fontSize = 13.sp,
                color = Color(0xFF999999)
            )
            if (file != null && !isUploaded) {
                Spacer(Modifier.width(8.dp))
                Icon(
                    Icons.Filled.Close,
                    contentDescription = "$title PDF 제거",
                    tint = Color(0xFF999999),
                    modifier = Modifier.size(20.dp).clickable(onClick = onRemoveFile)
                )
            }
        }
        Spacer(Modifier.width(10.dp))
        Button(
            onClick = onSubmit,
            enabled = file != null,
            modifier = Modifier
                .width(72.dp)
                .height(36.dp),
            shape = RoundedCornerShape(18.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = NavyBlue,
                disabledContainerColor = Color(0xFFD9D9D9)
            )
        ) {
            Text(if (isUploaded) "수정" else "등록", color = Color.White, fontSize = 13.sp)
        }
    }
}

private fun persistReadPermission(context: Context, uri: Uri) {
    runCatching {
        context.contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
}

private fun resolveAdminUploadFile(context: Context, uri: Uri, fallbackName: String): AdminUploadFile {
    var fileName = uri.lastPathSegment?.substringAfterLast('/') ?: fallbackName
    var fileSize = -1L
    val mimeType = context.contentResolver.getType(uri)

    context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
        val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
        val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)
        if (cursor.moveToFirst()) {
            if (nameIndex >= 0) fileName = cursor.getString(nameIndex) ?: fileName
            if (sizeIndex >= 0) fileSize = cursor.getLong(sizeIndex)
        }
    }

    return AdminUploadFile(
        uri = uri,
        name = fileName,
        sizeLabel = formatAdminFileSize(fileSize),
        sizeBytes = fileSize,
        mimeType = mimeType
    )
}

private fun AdminUploadFile.isWithinUploadLimit(): Boolean {
    return sizeBytes in 1..MAX_ADMIN_UPLOAD_BYTES
}

private fun AdminUploadFile.isValidPdfUpload(): Boolean {
    val looksLikePdf = mimeType == "application/pdf" || name.endsWith(".pdf", ignoreCase = true)
    return looksLikePdf && isWithinUploadLimit()
}

private fun formatAdminFileSize(bytes: Long): String {
    if (bytes <= 0L) return "0mb"
    val mb = bytes / (1024f * 1024f)
    return if (mb < 1f) {
        "${kotlin.math.max(1, (bytes / 1024L).toInt())}kb"
    } else {
        "${kotlin.math.ceil(mb).toInt()}mb"
    }
}

@Composable
private fun MyPageSectionHeader(title: String) {
    Text(title, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Color.Black)
    Spacer(Modifier.height(8.dp))
}

@Composable
private fun AdminPassengerControlSection(
    driverViewModel: DriverViewModel,
    schedules: List<BusSchedule>,
    onNotice: (String) -> Unit
) {
    val scope = rememberCoroutineScope()
    val shuttleRepository = remember { ShuttleRepository() }
    var isRefreshingOperation by remember { mutableStateOf(false) }
    var manualSelectionMode by remember { mutableStateOf(false) }
    val route = driverViewModel.selectedRoute
    val operationState = driverViewModel.operationState
    val passengers = driverViewModel.passengerCount
    val totalSeats = route?.totalSeats ?: 45
    val remainingSeats = (totalSeats - passengers).coerceAtLeast(0)
    val isOperating = operationState == OperationState.OPERATING
    val stateLabel = when (operationState) {
        OperationState.BEFORE_DEPARTURE -> "운행 전"
        OperationState.OPERATING -> "운행 중"
        OperationState.COMPLETED -> "운행 종료"
    }
    val manualSchedules = remember(schedules) {
        schedules
            .distinctBy { it.id }
            .sortedWith(compareBy<BusSchedule>({ it.campusType }, { it.routeName }, { it.departureTime }))
    }
    val manualHours = remember(manualSchedules) {
        manualSchedules
            .mapNotNull { it.departureHourOrNull() }
            .distinct()
            .sorted()
    }
    var manualHour by rememberSaveable { mutableStateOf<Int?>(null) }
    LaunchedEffect(manualHours) {
        if (manualHour == null || manualHours.none { it == manualHour }) {
            manualHour = manualHours.closestToCurrentHourOrNull()
        }
    }
    val manualCandidates = remember(manualSchedules, manualHour) {
        manualSchedules
            .filter { it.departureHourOrNull() == manualHour }
            .sortedWith(compareBy<BusSchedule>({ it.departureTime }, { it.routeName }))
    }

    fun refreshRunningOperation(showNotice: Boolean) {
        if (isRefreshingOperation) return
        scope.launch {
            isRefreshingOperation = true
            findRunningAdminOperation(schedules, shuttleRepository)
                .onSuccess { snapshot ->
                    if (snapshot != null) {
                        driverViewModel.syncRunningRouteFromServer(snapshot.route, snapshot.passengers)
                        manualSelectionMode = false
                        if (showNotice) {
                            onNotice("운행 중인 시간표를 불러왔습니다")
                        }
                    } else if (showNotice) {
                        onNotice("현재 운행 중인 시간표가 없습니다")
                    }
                }
                .onFailure { throwable ->
                    if (showNotice) {
                        onNotice("운행 상태 조회 실패: ${throwable.message ?: "서버 응답 확인 필요"}")
                    }
                }
            isRefreshingOperation = false
        }
    }

    fun selectManualSchedule(schedule: BusSchedule) {
        scope.launch {
            buildAdminOperationSnapshot(schedule, shuttleRepository, requireRunning = false)
                .onSuccess { snapshot ->
                    if (snapshot != null) {
                        driverViewModel.syncRunningRouteFromServer(snapshot.route, snapshot.passengers)
                        manualSelectionMode = false
                        onNotice("선택한 시간표로 수기 집계를 시작합니다")
                    } else {
                        onNotice("시간표 상태를 불러오지 못했습니다")
                    }
                }
                .onFailure { throwable ->
                    onNotice("시간표 선택 실패: ${throwable.message ?: "서버 응답 확인 필요"}")
                }
        }
    }

    LaunchedEffect(schedules, route?.id, operationState, manualSelectionMode) {
        if (!manualSelectionMode && (route == null || operationState != OperationState.OPERATING)) {
            refreshRunningOperation(showNotice = false)
        }
    }

    MyPageSectionHeader("탑승 수 수기 집계")
    Text(
        "NFC 장비 입력을 대체하는 관리자용 수기 집계입니다.",
        fontSize = 12.sp,
        color = Color(0xFF777777)
    )
    Spacer(Modifier.height(12.dp))

    if (route == null) {
        Text(
            if (isRefreshingOperation) "운행 중인 시간표를 확인하고 있습니다." else "현재 운행 중인 시간표가 없습니다.",
            fontSize = 13.sp,
            color = Color(0xFF777777)
        )
        Spacer(Modifier.height(10.dp))
        AdminPassengerButton(
            text = if (isRefreshingOperation) "조회 중" else "운행 상태 새로고침",
            enabled = !isRefreshingOperation,
            filled = false,
            modifier = Modifier.fillMaxWidth(),
            onClick = { refreshRunningOperation(showNotice = true) }
        )
        Spacer(Modifier.height(8.dp))
        Text(
            "기사가 시간표에서 운행 시작하기를 누르면 이곳에 해당 운행이 자동으로 표시됩니다.",
            fontSize = 12.sp,
            color = Color(0xFF999999)
        )
        Spacer(Modifier.height(14.dp))
        Text(
            "자동 조회가 안 되면 아래에서 운행 시간표를 직접 선택해 수기 집계를 시작할 수 있습니다.",
            fontSize = 12.sp,
            color = Color(0xFF777777)
        )
        Spacer(Modifier.height(8.dp))
        if (manualSchedules.isEmpty()) {
            Text(
                "등록된 시간표가 없습니다. 관리자 시간표 등록 후 다시 새로고침해 주세요.",
                fontSize = 12.sp,
                color = Color(0xFF999999)
            )
        } else {
            LazyRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(manualHours) { hour ->
                    FilterChip(
                        selected = manualHour == hour,
                        onClick = { manualHour = hour },
                        label = {
                            Text(
                                "${hour.toString().padStart(2, '0')}시",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        },
                        shape = RoundedCornerShape(18.dp),
                        border = null,
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = NavyBlue,
                            selectedLabelColor = Color.White,
                            containerColor = Color(0xFFF2F4F7),
                            labelColor = Color(0xFF777777)
                        )
                    )
                }
            }
            Spacer(Modifier.height(10.dp))
            if (manualCandidates.isEmpty()) {
                Text(
                    "선택한 시간대에 등록된 운행이 없습니다.",
                    fontSize = 12.sp,
                    color = Color(0xFF999999)
                )
            }
            manualCandidates.take(12).forEach { schedule ->
                AdminRouteSelectRow(
                    route = schedule.toDriverRoute(),
                    onClick = { selectManualSchedule(schedule) }
                )
                Spacer(Modifier.height(8.dp))
            }
        }
        return
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .border(1.dp, Color(0xFFE0E0E0), RoundedCornerShape(10.dp))
            .padding(horizontal = 16.dp, vertical = 14.dp)
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(route.routeName, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF222222))
                    Spacer(Modifier.height(4.dp))
                    Text("출발 ${route.scheduledTime}", fontSize = 12.sp, color = Color(0xFF777777))
                }
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(50.dp))
                        .background(if (isOperating) Color(0xFFEAF3FF) else Color(0xFFF1F2F4))
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text(
                        stateLabel,
                        fontSize = 12.sp,
                        color = if (isOperating) NavyBlue else Color(0xFF777777),
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            Spacer(Modifier.height(18.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                AdminSeatMetric("현재 탑승", "${passengers}/${totalSeats}")
                AdminSeatMetric("남은 여석", "${remainingSeats}석")
            }

            Spacer(Modifier.height(16.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                AdminPassengerButton(
                    text = "하차 -",
                    enabled = isOperating && passengers > 0,
                    filled = false,
                    modifier = Modifier.weight(1f),
                    onClick = {
                        driverViewModel.decreasePassengers()
                        onNotice("하차 요청을 서버에 전송했습니다")
                    }
                )
                AdminPassengerButton(
                    text = "탑승 +",
                    enabled = isOperating && passengers < totalSeats,
                    filled = true,
                    modifier = Modifier.weight(1f),
                    onClick = {
                        driverViewModel.increasePassengers()
                        onNotice("탑승 요청을 서버에 전송했습니다")
                    }
                )
            }

            if (!isOperating) {
                Spacer(Modifier.height(10.dp))
                Text(
                    "기사가 출발 등록을 누른 뒤 수기 집계가 활성화됩니다.",
                    fontSize = 12.sp,
                    color = Color(0xFF999999)
                )
            }

            Spacer(Modifier.height(12.dp))
            AdminPassengerButton(
                text = if (isRefreshingOperation) "조회 중" else "운행 상태 새로고침",
                enabled = !isRefreshingOperation,
                filled = false,
                modifier = Modifier.fillMaxWidth(),
                onClick = {
                    driverViewModel.clearSelectedRoute()
                    refreshRunningOperation(showNotice = true)
                }
            )
            Spacer(Modifier.height(8.dp))
            AdminPassengerButton(
                text = "시간표 직접 다시 선택",
                enabled = true,
                filled = false,
                modifier = Modifier.fillMaxWidth(),
                onClick = {
                    manualSelectionMode = true
                    driverViewModel.clearSelectedRoute()
                    onNotice("아래 시간표에서 집계 대상을 직접 선택해 주세요")
                }
            )
            Spacer(Modifier.height(8.dp))
            Text(
                driverViewModel.operationMessage,
                fontSize = 12.sp,
                color = Color(0xFF999999)
            )
            if (isOperating) {
                Spacer(Modifier.height(8.dp))
                Text(
                    "운행 중에는 집계 대상 변경을 할 수 없습니다.",
                    fontSize = 12.sp,
                    color = Color(0xFF999999)
                )
            }
        }
    }
}

private data class AdminOperationSnapshot(
    val route: DriverRoute,
    val passengers: Int
)

private suspend fun findRunningAdminOperation(
    schedules: List<BusSchedule>,
    shuttleRepository: ShuttleRepository
): Result<AdminOperationSnapshot?> = runCatching {
    val snapshots = schedules.distinctBy { it.id }.mapNotNull { schedule ->
        buildAdminOperationSnapshot(schedule, shuttleRepository, requireRunning = true).getOrNull()
    }
    snapshots.minByOrNull { it.route.scheduledTime.distanceFromNowMinutes() }
}

private suspend fun buildAdminOperationSnapshot(
    schedule: BusSchedule,
    shuttleRepository: ShuttleRepository,
    requireRunning: Boolean
): Result<AdminOperationSnapshot?> = runCatching {
    val busStatus = shuttleRepository.getBusStatuses(schedule.id.toLong()).getOrNull()
    if (requireRunning && !busStatus?.status.isRunningBusStatus()) return@runCatching null

    val totalSeats = busStatus?.totalSeats ?: schedule.totalSeats.takeIf { it > 0 } ?: FIXED_TOTAL_SEATS
    val serverPassengers = busStatus?.currentSeats
        ?: busStatus?.currentPassengers
        ?: busStatus?.passengerCount
        ?: (busStatus?.remainingSeats ?: busStatus?.availableSeats)?.let { totalSeats - it }
        ?: (totalSeats - schedule.remainingSeats)

    val passengers = serverPassengers.coerceIn(0, totalSeats)
    val route = schedule
        .copy(totalSeats = totalSeats, remainingSeats = (totalSeats - passengers).coerceIn(0, totalSeats))
        .toDriverRoute()
        .copy(busId = busStatus?.busId ?: schedule.id.toLong())

    AdminOperationSnapshot(route = route, passengers = passengers)
}

private fun String?.isRunningBusStatus(): Boolean {
    return this?.trim()?.uppercase() == "RUNNING"
}

private fun String.distanceFromNowMinutes(): Int {
    val parts = take(5).split(":")
    val hour = parts.getOrNull(0)?.toIntOrNull() ?: return Int.MAX_VALUE
    val minute = parts.getOrNull(1)?.toIntOrNull() ?: 0
    val scheduled = hour * 60 + minute
    val now = java.util.Calendar.getInstance().let {
        it.get(java.util.Calendar.HOUR_OF_DAY) * 60 + it.get(java.util.Calendar.MINUTE)
    }
    val direct = kotlin.math.abs(scheduled - now)
    return minOf(direct, 24 * 60 - direct)
}

private fun BusSchedule.departureHourOrNull(): Int? {
    return departureTime.take(2).toIntOrNull()
}

private fun List<Int>.closestToCurrentHourOrNull(): Int? {
    if (isEmpty()) return null
    val now = java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY)
    return minByOrNull { kotlin.math.abs(it - now) } ?: firstOrNull()
}

private fun String.toDisplayClockText(): String {
    if (this == "미정") return this
    val clockPattern = Regex("""\b([01]?\d|2[0-3]):([0-5]\d)""")
    return clockPattern.find(this)?.value ?: this
}

@Composable
private fun AdminRouteSelectRow(route: DriverRoute, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(Color(0xFFF5F6F8))
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 11.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(route.routeName, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF333333))
            Text("출발 ${route.scheduledTime}", fontSize = 12.sp, color = Color(0xFF888888))
        }
        Text("선택", fontSize = 12.sp, color = NavyBlue, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun AdminSeatMetric(label: String, value: String) {
    Column(horizontalAlignment = Alignment.Start) {
        Text(label, fontSize = 12.sp, color = Color(0xFF777777))
        Spacer(Modifier.height(4.dp))
        Text(value, fontSize = 24.sp, fontWeight = FontWeight.Bold, color = Color(0xFFB83A25))
    }
}

@Composable
private fun AdminPassengerButton(
    text: String,
    enabled: Boolean,
    filled: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val background = when {
        !enabled -> Color(0xFFE8EAEE)
        filled -> NavyBlue
        else -> Color.White
    }
    val foreground = when {
        !enabled -> Color(0xFF9AA1AB)
        filled -> Color.White
        else -> NavyBlue
    }
    Box(
        modifier = modifier
            .height(40.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(background)
            .border(1.dp, if (filled || !enabled) background else NavyBlue, RoundedCornerShape(8.dp))
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(text, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = foreground)
    }
}

@Composable
private fun DriverScheduleCard(route: DriverRoute, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .border(1.dp, Color(0xFFE0E0E0), RoundedCornerShape(10.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                route.routeName,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFF1A1A1A)
            )
            Spacer(Modifier.height(4.dp))
            Text("출발 ${route.scheduledTime}", fontSize = 13.sp, color = Color(0xFF777777))
        }
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(50.dp))
                .background(Color(0xFFEFF3FB))
                .padding(horizontal = 10.dp, vertical = 4.dp)
        ) {
            Text("운행 대기", fontSize = 12.sp, color = NavyBlue, fontWeight = FontWeight.Medium)
        }
    }
}

@Composable
private fun MyPageRow(label: String, onClick: () -> Unit = {}) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp)
    ) {
        Text(label, fontSize = 15.sp, color = Color(0xFF333333))
    }
}

@Composable
private fun MyPageNoticeBar(
    message: String,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .padding(horizontal = 16.dp, vertical = 14.dp)
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(Color(0xFF1A1A2E))
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(Icons.Filled.Info, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(8.dp))
        Text(message, color = Color.White, fontSize = 14.sp, modifier = Modifier.weight(1f))
        Icon(
            Icons.Filled.Close,
            contentDescription = "닫기",
            tint = Color.White,
            modifier = Modifier.size(18.dp).clickable(onClick = onDismiss)
        )
    }
}


// ── 하단 탭바 ───────────────────────────────────────────────────
@Composable
private fun StudentBottomBar(selected: StudentTab, onTabClick: (StudentTab) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(NavyBlue)
            .navigationBarsPadding()
            .padding(vertical = 8.dp)
    ) {
        StudentTabItem(Icons.Filled.Schedule, "시간표",   selected == StudentTab.TIMETABLE, { onTabClick(StudentTab.TIMETABLE)  }, Modifier.weight(1f))
        StudentTabItem(Icons.Filled.Star,     "즐겨찾기", selected == StudentTab.FAVORITES, { onTabClick(StudentTab.FAVORITES) }, Modifier.weight(1f))
        StudentTabItem(Icons.Filled.Person,   "마이페이지", selected == StudentTab.MYPAGE,  { onTabClick(StudentTab.MYPAGE)    }, Modifier.weight(1f))
    }
}

@Composable
private fun StudentTabItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val tint = if (selected) Color.White else Color(0x99FFFFFF)
    Column(
        modifier            = modifier.clickable(onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(icon, contentDescription = label, tint = tint, modifier = Modifier.size(24.dp))
        Text(label, color = tint, fontSize = 11.sp)
    }
}
