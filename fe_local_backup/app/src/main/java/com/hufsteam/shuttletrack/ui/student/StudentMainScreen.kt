package com.hufsteam.shuttletrack.ui.student

import android.graphics.BitmapFactory
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.*
import androidx.compose.runtime.*
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hufsteam.shuttletrack.ui.auth.AuthViewModel
import com.hufsteam.shuttletrack.ui.common.BusIcon
import com.hufsteam.shuttletrack.data.model.UserRole
import com.hufsteam.shuttletrack.ui.theme.AdminBadge
import com.hufsteam.shuttletrack.ui.theme.AdminBadgeText
import com.hufsteam.shuttletrack.ui.theme.DividerColor
import com.hufsteam.shuttletrack.ui.theme.DriverBadge
import com.hufsteam.shuttletrack.ui.theme.DriverBadgeText
import com.hufsteam.shuttletrack.ui.theme.NavyBlue
import com.hufsteam.shuttletrack.ui.theme.StudentBadge
import com.hufsteam.shuttletrack.ui.theme.StudentBadgeText

// ── 내부 화면 상태 ──────────────────────────────────────────────
private enum class StudentScreen { TIMETABLE, ROUTE_STATUS, FAVORITES, MYPAGE }
private enum class StudentTab    { TIMETABLE, FAVORITES, MYPAGE }

// ── 데이터 모델 ─────────────────────────────────────────────────
data class BusSchedule(
    val id: Int,
    val routeName: String,
    val departureTime: String,
    val remainingSeats: Int,
    val totalSeats: Int,
    val currentLocation: String
)

data class FavoriteSchedule(
    val schedule: BusSchedule,
    val days: Set<String>
)

private val notificationDays = listOf("월요일", "화요일", "수요일", "목요일", "금요일")

private val ShuttleRegularTextStyle = TextStyle(
    fontFamily = FontFamily.SansSerif,
    fontWeight = FontWeight.Normal,
    fontSize = 16.sp,
    lineHeight = 16.sp,
    letterSpacing = 0.sp
)
private val offCampusSchedules = listOf(
    BusSchedule(1, "경기광주역 → 외대(글)", "08:20", 8,  45, "법학관을 지나고 있습니다"),
    BusSchedule(2, "경기광주역 → 외대(글)", "08:30", 48, 48, "위치 확인 중입니다"),
    BusSchedule(3, "경기광주역 → 외대(글)", "08:40", 44, 45, "내리실 정거장에 접근 중입니다"),
    BusSchedule(4, "경기광주역 → 외대(글)", "08:50", 42, 45, "인문경상관 근처입니다"),
    BusSchedule(9, "외대(글) → 경기광주역", "10:30", 36, 45, "후생관 근처입니다")
)

private val onCampusSchedules = listOf(
    BusSchedule(5, "지석묘 → 인문경상관", "09:20", 8,  40, "지석묘 출발"),
    BusSchedule(6, "지석묘 → 인문경상관", "09:30", 48, 48, "위치입니다"),
    BusSchedule(7, "지석묘 → 인문경상관", "09:40", 44, 45, "나름 적당히 긴 위치이름입니다"),
    BusSchedule(8, "지석묘 → 인문경상관", "09:50", 42, 45, "엄청엄청 긴 위치이름입니다초")
)

private val offCampusRoutes = listOf(
    "경기광주역 → 외대(글)",
    "외대(글) → 경기광주역",
    "판교역 → 외대(글)",
    "외대(글) → 판교역"
)
private val onCampusRoutes = listOf("지석묘 → 인문경상관")
private val hours = listOf(8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18)

// ── 메인 화면 ───────────────────────────────────────────────────
@Composable
fun StudentMainScreen(
    viewModel: AuthViewModel,
    onLogout: () -> Unit,
    studentBusViewModel: StudentBusViewModel = androidx.lifecycle.viewmodel.compose.viewModel()
) {
    val studentUiState = studentBusViewModel.uiState
    val favoriteSchedules = studentUiState.favoriteSchedules
    var currentScreen by remember { mutableStateOf(StudentScreen.TIMETABLE) }
    var selectedTab by remember { mutableStateOf(StudentTab.TIMETABLE) }
    var selectedSchedule by remember { mutableStateOf((studentUiState.offCampusSchedules.ifEmpty { offCampusSchedules }).first()) }
    var favoriteTarget by remember { mutableStateOf<BusSchedule?>(null) }

    LaunchedEffect(studentUiState.offCampusSchedules, studentUiState.onCampusSchedules) {
        val allSchedules = studentUiState.offCampusSchedules + studentUiState.onCampusSchedules
        if (allSchedules.isNotEmpty() && allSchedules.none { it.id == selectedSchedule.id }) {
            selectedSchedule = allSchedules.first()
        }
    }

    Scaffold(
        bottomBar = {
            StudentBottomBar(
                selected = selectedTab,
                onTabClick = { tab ->
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
                    offCampusSchedules = studentUiState.offCampusSchedules.ifEmpty { offCampusSchedules },
                    onCampusSchedules = studentUiState.onCampusSchedules.ifEmpty { onCampusSchedules },
                    isLoading = studentUiState.isLoading,
                    errorMessage = studentUiState.errorMessage,
                    onScheduleClick = {
                        selectedSchedule = it
                        studentBusViewModel.loadRouteStatus(it)
                        currentScreen = StudentScreen.ROUTE_STATUS
                    }
                )

                StudentScreen.ROUTE_STATUS -> StudentRouteStatusContent(
                    schedule = selectedSchedule,
                    routeDetail = studentUiState.selectedRouteDetail ?: mockRouteDetailFor(selectedSchedule),
                    isFavorite = favoriteSchedules.any { it.schedule.id == selectedSchedule.id },
                    onBackClick = {
                        currentScreen = StudentScreen.TIMETABLE
                        selectedTab = StudentTab.TIMETABLE
                    },
                    onFavoriteClick = { favoriteTarget = selectedSchedule }
                )

                StudentScreen.FAVORITES -> StudentFavoritesContent(
                    favorites = favoriteSchedules,
                    onScheduleClick = {
                        selectedSchedule = it.schedule
                        studentBusViewModel.loadRouteStatus(it.schedule)
                        currentScreen = StudentScreen.ROUTE_STATUS
                    }
                )

                StudentScreen.MYPAGE -> StudentMyPageContent(viewModel = viewModel, onLogout = onLogout)
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

// ── 시간표 화면 ─────────────────────────────────────────────────
@Composable
private fun StudentTimetableContent(
    offCampusSchedules: List<BusSchedule>,
    onCampusSchedules: List<BusSchedule>,
    isLoading: Boolean,
    errorMessage: String?,
    onScheduleClick: (BusSchedule) -> Unit
) {
    var selectedCampus by remember { mutableStateOf(0) }
    val schedules = if (selectedCampus == 0) onCampusSchedules else offCampusSchedules
    val routes = schedules.map { it.routeName }.distinct().ifEmpty { if (selectedCampus == 0) onCampusRoutes else offCampusRoutes }
    var selectedRoute by remember(selectedCampus, routes) { mutableStateOf(routes.firstOrNull().orEmpty()) }
    var dropdownOpen by remember { mutableStateOf(false) }
    var selectedHour by remember { mutableStateOf(if (selectedCampus == 0) 9 else 8) }

    LaunchedEffect(selectedCampus, routes) {
        if (selectedRoute !in routes) selectedRoute = routes.firstOrNull().orEmpty()
    }

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
                        Text(selectedRoute, fontSize = 14.sp, color = Color(0xFF333333))
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

            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(hours) { hour ->
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

            Spacer(Modifier.height(12.dp))

            val routeSchedules = schedules.filter { it.routeName == selectedRoute }.ifEmpty { schedules }
            val hourSchedules = routeSchedules.filter { it.departureTime.substringBefore(":").toIntOrNull() == selectedHour }
            val visibleSchedules = hourSchedules.ifEmpty { routeSchedules }

            LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                items(visibleSchedules) { schedule ->
                    StudentTimetableCard(schedule = schedule, onClick = { onScheduleClick(schedule) })
                }
                item { Spacer(Modifier.height(8.dp)) }
            }
        }
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
    onBackClick: () -> Unit,
    onFavoriteClick: () -> Unit
) {
    val stops = routeDetail.stops
    val busProgressIndex = routeDetail.busProgressIndex.coerceIn(0f, stops.lastIndex.toFloat())
    val isCurvedRoute = stops.size >= 7
    var selectedStopIndex by remember(schedule.id, routeDetail.currentStopIndex, stops) {
        mutableStateOf<Int?>(stops.getOrNull(routeDetail.currentStopIndex)?.let { routeDetail.currentStopIndex })
    }

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
                    .offset(x = 139.dp, y = 174.dp)
                    .width(95.dp)
                    .height(31.dp)
                    .clip(RoundedCornerShape(50.dp))
                    .border(3.dp, Color(0xFFE2573B), RoundedCornerShape(50.dp))
                    .background(Color.White)
                    .padding(horizontal = 12.dp),
                contentAlignment = Alignment.Center
            ) {
                Text("예상: ${routeDetail.etaText}", style = ShuttleRegularTextStyle, color = Color(0xFFE2573B))
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
                Text("실제 출발 | ${routeDetail.actualDeparture}", style = ShuttleRegularTextStyle, color = Color(0xFF333333))
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
    onStopClick: (Int) -> Unit = {}
) {
    val navy = NavyBlue
    val inactive = Color(0xFFE3E7ED)

    if (isCurvedRoute && stops.size >= 7) {
        CurvedBusRoute(stops = stops, busProgressIndex = busProgressIndex, navy = navy, inactive = inactive, onStopClick = onStopClick)
    } else {
        StraightBusRoute(stops = stops, busProgressIndex = busProgressIndex, navy = navy, inactive = inactive, onStopClick = onStopClick)
    }
}

@Composable
private fun StraightBusRoute(
    stops: List<String>,
    busProgressIndex: Float,
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
            val activeEndPx = startXpx + gapPx * clampedBus
            val lineStrokePx = lineStroke.toPx()
            val stopRadiusPx = stopRadius.toPx()
            val stopStrokePx = stopStroke.toPx()

            drawLine(navy, Offset(startXpx, lineYpx), Offset(activeEndPx, lineYpx), strokeWidth = lineStrokePx, cap = StrokeCap.Round)
            drawLine(inactive, Offset(activeEndPx, lineYpx), Offset(endXpx, lineYpx), strokeWidth = lineStrokePx, cap = StrokeCap.Round)

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

        val markerX = startX + gap * clampedBus - 34.dp
        BusLocationMarker(
            modifier = Modifier.offset(x = markerX, y = lineY - 67.dp),
            width = 70.dp,
            height = 96.dp
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .offset(y = maxHeight * 0.63f),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            stops.forEach { stop ->
                Text(
                    text = stop,
                    fontSize = 11.sp,
                    color = Color(0xFF222222),
                    textAlign = TextAlign.Center,
                    lineHeight = 13.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.width(64.dp)
                )
            }
        }
    }
}

@Composable
private fun CurvedBusRoute(
    stops: List<String>,
    busProgressIndex: Float,
    navy: Color,
    inactive: Color,
    onStopClick: (Int) -> Unit
) {
    val clampedBus = busProgressIndex.coerceIn(0f, stops.lastIndex.toFloat())
    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val topY = maxHeight * 0.28f
        val bottomY = maxHeight * 0.66f
        val rightY = maxHeight * 0.45f
        val routePoints = listOf(
            (maxWidth * 0.10f) to topY,
            (maxWidth * 0.33f) to topY,
            (maxWidth * 0.56f) to topY,
            (maxWidth * 0.86f) to rightY,
            (maxWidth * 0.56f) to bottomY,
            (maxWidth * 0.33f) to bottomY,
            (maxWidth * 0.10f) to bottomY
        )
        val topCorner = routePoints[3].first to topY
        val bottomCorner = routePoints[3].first to bottomY
        val labelOffsets = listOf(
            (-26).dp to 18.dp,
            (-26).dp to 18.dp,
            (-26).dp to 18.dp,
            (-52).dp to 22.dp,
            (-26).dp to 18.dp,
            (-26).dp to 18.dp,
            (-26).dp to 18.dp
        )

        Canvas(modifier = Modifier.matchParentSize()) {
            fun p(index: Int) = Offset(routePoints[index].first.toPx(), routePoints[index].second.toPx())
            fun corner(pair: Pair<Dp, Dp>) = Offset(pair.first.toPx(), pair.second.toPx())

            val lineStroke = 12.dp.toPx()
            val stopRadius = 14.5.dp.toPx()
            val stopStroke = 5.dp.toPx()
            val routePath = Path().apply {
                moveTo(p(0).x, p(0).y)
                lineTo(p(1).x, p(1).y)
                lineTo(p(2).x, p(2).y)
                lineTo(corner(topCorner).x, corner(topCorner).y)
                lineTo(p(3).x, p(3).y)
                lineTo(corner(bottomCorner).x, corner(bottomCorner).y)
                lineTo(p(4).x, p(4).y)
                lineTo(p(5).x, p(5).y)
                lineTo(p(6).x, p(6).y)
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

        val markerPosition = interpolateCurvedRoutePoint(routePoints, clampedBus, topCorner, bottomCorner)
        BusLocationMarker(
            modifier = Modifier.offset(
                x = markerPosition.first - 35.dp,
                y = markerPosition.second - 69.dp
            ),
            width = 70.dp,
            height = 96.dp
        )

        routePoints.forEachIndexed { index, point ->
            val labelOffset = labelOffsets.getOrElse(index) { (-26).dp to 18.dp }
            Text(
                text = stops.getOrElse(index) { "" },
                fontSize = 10.sp,
                color = Color(0xFF222222),
                textAlign = TextAlign.Center,
                lineHeight = 11.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier
                    .offset(
                        x = point.first + labelOffset.first,
                        y = point.second + labelOffset.second
                    )
                    .width(64.dp)
            )
        }
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
    bottomCorner: Pair<Dp, Dp>
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
        2 -> if (fraction < 0.5f) {
            lerpDp(start, topCorner, fraction * 2f)
        } else {
            lerpDp(topCorner, end, (fraction - 0.5f) * 2f)
        }
        3 -> if (fraction < 0.5f) {
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
    onScheduleClick: (FavoriteSchedule) -> Unit
) {
    Column(modifier = Modifier.fillMaxSize().background(Color.White).padding(horizontal = 16.dp)) {
        Spacer(Modifier.height(18.dp))
        Text("즐겨찾기", fontSize = 17.sp, fontWeight = FontWeight.Bold, color = Color.Black)
        Spacer(Modifier.height(12.dp))

        if (favorites.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("★", fontSize = 40.sp, color = Color(0xFFDDDDDD))
                    Spacer(Modifier.height(12.dp))
                    Text("아직 추가된 즐겨찾기가 없습니다.", color = Color(0xFF999999), fontSize = 14.sp)
                }
            }
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                items(favorites) { favorite ->
                    Card(
                        modifier = Modifier.fillMaxWidth().clickable { onScheduleClick(favorite) },
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(favorite.schedule.routeName, fontSize = 13.sp, color = NavyBlue, fontWeight = FontWeight.Bold)
                                Spacer(Modifier.height(6.dp))
                                Row(verticalAlignment = Alignment.Bottom) {
                                    Text(favorite.schedule.departureTime, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                                    Spacer(Modifier.width(6.dp))
                                    Text("(${favorite.schedule.remainingSeats}석)", color = Color(0xFFE53935), fontWeight = FontWeight.Bold)
                                }
                                Spacer(Modifier.height(6.dp))
                                Text(favorite.days.joinToString(" · "), fontSize = 12.sp, color = Color(0xFF777777))
                            }
                            Icon(Icons.Filled.Star, contentDescription = null, tint = Color(0xFFFFC400))
                        }
                    }
                }
            }
        }
    }
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
            Text("알림 받을 요일을 선택해 주세요", style = ShuttleRegularTextStyle, color = Color.Black)
            Spacer(Modifier.height(6.dp))
            Text("중복 선택 가능", style = ShuttleRegularTextStyle, color = Color(0xFFB8BEC8))
            Spacer(Modifier.height(14.dp))

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
                onClick = { onSave(selectedDays) },
                enabled = selectedDays.isNotEmpty(),
                modifier = Modifier.fillMaxWidth().height(48.dp),
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = NavyBlue,
                    disabledContainerColor = Color(0xFFD9D9D9)
                )
            ) {
                Text("설정하기", style = ShuttleRegularTextStyle, color = Color.White)
            }
        }
    }
}
// ── 학생 마이페이지 ─────────────────────────────────────────────
@Composable
private fun StudentMyPageContent(viewModel: AuthViewModel, onLogout: () -> Unit) {
    val role = viewModel.userRole ?: UserRole.STUDENT
    val roleLabel = when (role) {
        UserRole.ADMIN -> "관리자"
        UserRole.DRIVER -> "기사"
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

    Column(modifier = Modifier.fillMaxSize().background(Color.White)) {
        // 프로필 헤더 (연한 회색 배경 밴드)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFFF5F5F5))
                .padding(horizontal = 20.dp, vertical = 20.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
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
                    Text(viewModel.currentEmail, color = Color(0xFF555555), fontSize = 14.sp)
                }
            }
        }
        HorizontalDivider(color = DividerColor)

        // 메뉴 영역
        Column(modifier = Modifier.padding(horizontal = 20.dp)) {
            Spacer(Modifier.height(24.dp))
            Text("서비스 안내", fontSize = 15.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(8.dp))
            MyPageRow("개인정보 처리 방침") {}
            MyPageRow("서비스 이용 약관") {}
            Spacer(Modifier.height(24.dp))
            if (role != UserRole.STUDENT) {
                Text("운영 기능", fontSize = 15.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(8.dp))
                when (role) {
                    UserRole.DRIVER -> {
                        MyPageRow("오늘 운행 일정") {}
                        MyPageRow("운행 상태 관리") {}
                    }
                    UserRole.ADMIN -> {
                        MyPageRow("노선 관리") {}
                        MyPageRow("정류장 관리") {}
                        MyPageRow("시간표 관리") {}
                    }
                    UserRole.STUDENT -> Unit
                }
                Spacer(Modifier.height(24.dp))
            }
            Text("계정 설정", fontSize = 15.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(8.dp))
            MyPageRow("로그아웃", onClick = onLogout)
            MyPageRow("탈퇴하기") {}
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

// ── 하단 탭바 ───────────────────────────────────────────────────
@Composable
private fun StudentBottomBar(selected: StudentTab, onTabClick: (StudentTab) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(NavyBlue)
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
