package com.example.hufs_bus

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.hufs_bus.data.*
import com.example.hufs_bus.ui.theme.Hufs_busTheme

private val NavyDark = Color(0xFF102A56)
private val RedAccent = Color(0xFFE0462E)
private val OrangeAccent = Color(0xFFFB8C00)
private val GrayText = Color(0xFF9E9E9E)
private val GrayLine = Color(0xFFD8DEE8)
private val BgLight = Color(0xFFF6F7F9)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            Hufs_busTheme {
                PassengerApp()
            }
        }
    }
}

enum class BottomTab { TIMETABLE, FAVORITE, MYPAGE }
enum class AppScreen { TIMETABLE, NAVIGATOR, FAVORITE, MYPAGE }

@Composable
fun PassengerApp(vm: BusViewModel = viewModel()) {
    var currentScreen by remember { mutableStateOf(AppScreen.TIMETABLE) }
    var selectedTab by remember { mutableStateOf(BottomTab.TIMETABLE) }

    Scaffold(
        bottomBar = {
            if (currentScreen != AppScreen.NAVIGATOR) {
                PassengerBottomBar(
                    selectedTab = selectedTab,
                    onTabClick = { tab ->
                        selectedTab = tab
                        currentScreen = when (tab) {
                            BottomTab.TIMETABLE -> AppScreen.TIMETABLE
                            BottomTab.FAVORITE -> AppScreen.FAVORITE
                            BottomTab.MYPAGE -> AppScreen.MYPAGE
                        }
                    }
                )
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(BgLight)
        ) {
            when (currentScreen) {
                AppScreen.TIMETABLE -> TimetableScreen(
                    vm = vm,
                    onScheduleClick = { schedule ->
                        vm.loadBusLocation(schedule.id)
                        currentScreen = AppScreen.NAVIGATOR
                    }
                )

                AppScreen.NAVIGATOR -> {
                    DisposableEffect(Unit) {
                        vm.startAutoRefresh()
                        onDispose { vm.stopAutoRefresh() }
                    }

                    NavigatorScreen(
                        vm = vm,
                        onBackClick = {
                            vm.stopAutoRefresh()
                            currentScreen = AppScreen.TIMETABLE
                            selectedTab = BottomTab.TIMETABLE
                        }
                    )
                }

                AppScreen.FAVORITE -> FavoriteScreen(
                    favorites = vm.favoriteBuses,
                    onRemoveClick = { favorite: FavoriteBus ->
                        vm.removeFavorite(favorite.busId, favorite.routeId)
                    }
                )

                AppScreen.MYPAGE -> PlaceholderScreen(
                    title = "마이페이지",
                    message = "마이페이지 화면은 추후 구현 예정입니다."
                )
            }
        }
    }
}

@Composable
fun TimetableScreen(
    vm: BusViewModel,
    onScheduleClick: (BusSchedule) -> Unit
) {
    val hourList = when (vm.selectedRoute?.id) {
        1L -> listOf(7, 8, 9)
        2L -> listOf(14, 15, 17, 18)
        3L, 4L -> (8..20).toList()
        else -> (7..20).toList()
    }

    LaunchedEffect(vm.selectedRoute) {
        if (vm.selectedHour !in hourList) {
            vm.selectHour(hourList.first())
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(top = 24.dp)
    ) {
        CampusTabs(
            selectedType = vm.selectedCampusType,
            onTypeSelected = { vm.selectCampusType(it) }
        )

        Spacer(modifier = Modifier.height(16.dp))

        Column(modifier = Modifier.padding(horizontal = 20.dp)) {
            RouteDropdown(
                routes = vm.routes,
                selectedRoute = vm.selectedRoute,
                onRouteSelected = { vm.selectRoute(it) }
            )

            Spacer(modifier = Modifier.height(14.dp))

            HourChipRow(
                hours = hourList,
                selectedHour = vm.selectedHour,
                onHourSelected = { vm.selectHour(it) }
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (vm.isLoading) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = NavyDark)
            }
        } else if (vm.schedules.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "해당 시간대 운행 정보가 없습니다",
                    color = GrayText,
                    fontSize = 14.sp
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.padding(horizontal = 20.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                contentPadding = PaddingValues(bottom = 16.dp)
            ) {
                items(vm.schedules, key = { it.id }) { schedule ->
                    ScheduleCard(
                        schedule = schedule,
                        onClick = { onScheduleClick(schedule) }
                    )
                }
            }
        }
    }
}

@Composable
fun CampusTabs(
    selectedType: RouteType,
    onTypeSelected: (RouteType) -> Unit
) {
    Row(modifier = Modifier.fillMaxWidth()) {
        listOf(
            "교내" to RouteType.CAMPUS,
            "교외" to RouteType.OFF_CAMPUS
        ).forEach { (label, type) ->
            val isSelected = selectedType == type

            Column(
                modifier = Modifier
                    .weight(1f)
                    .clickable { onTypeSelected(type) },
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = label,
                    fontSize = 14.sp,
                    color = if (isSelected) NavyDark else GrayText,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                    modifier = Modifier.padding(vertical = 12.dp)
                )

                Box(
                    modifier = Modifier
                        .height(3.dp)
                        .fillMaxWidth()
                        .background(if (isSelected) NavyDark else Color.Transparent)
                )
            }
        }
    }
}

@Composable
fun RouteDropdown(
    routes: List<BusRoute>,
    selectedRoute: BusRoute?,
    onRouteSelected: (BusRoute) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Box {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(10.dp))
                .background(Color.White)
                .clickable { expanded = !expanded }
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = selectedRoute?.name ?: "노선을 선택하세요",
                fontSize = 13.sp,
                color = Color(0xFF333333),
                modifier = Modifier.weight(1f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Icon(
                imageVector = if (expanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                contentDescription = null,
                tint = GrayText
            )
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier.fillMaxWidth(0.9f)
        ) {
            routes.forEach { route ->
                DropdownMenuItem(
                    text = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = route.name,
                                modifier = Modifier.weight(1f)
                            )

                            if (route.id == selectedRoute?.id) {
                                Text(
                                    text = "✓",
                                    color = NavyDark,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    },
                    onClick = {
                        onRouteSelected(route)
                        expanded = false
                    }
                )
            }
        }
    }
}

@Composable
fun HourChipRow(
    hours: List<Int>,
    selectedHour: Int,
    onHourSelected: (Int) -> Unit
) {
    Row(
        modifier = Modifier.horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        hours.forEach { hour ->
            val isSelected = selectedHour == hour

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(if (isSelected) NavyDark else Color.White)
                    .clickable { onHourSelected(hour) }
                    .padding(horizontal = 14.dp, vertical = 8.dp)
            ) {
                Text(
                    text = "${hour}시",
                    fontSize = 12.sp,
                    color = if (isSelected) Color.White else GrayText,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                )
            }
        }
    }
}

@Composable
fun ScheduleCard(
    schedule: BusSchedule,
    onClick: () -> Unit
) {
    val seatColor = when {
        schedule.remainingSeats <= 10 -> RedAccent
        schedule.remainingSeats <= 30 -> OrangeAccent
        else -> NavyDark
    }

    val alpha = if (schedule.status == BusStatus.COMPLETED) 0.5f else 1f

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    text = schedule.departureTime,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF111111).copy(alpha = alpha)
                )

                Spacer(modifier = Modifier.width(6.dp))

                Text(
                    text = "(${schedule.remainingSeats}석)",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = seatColor.copy(alpha = alpha)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "현재 위치  |  ${schedule.currentLocation}",
                fontSize = 12.sp,
                color = GrayText.copy(alpha = alpha),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
fun NavigatorScreen(
    vm: BusViewModel,
    onBackClick: () -> Unit
) {
    val location = vm.busLocation

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.White)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBackClick) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "뒤로가기",
                        tint = Color(0xFF333333)
                    )
                }

                Text(
                    text = location?.routeName ?: "노선 정보",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF333333),
                    modifier = Modifier.weight(1f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                IconButton(onClick = { vm.refreshBusLocation() }) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "새로고침",
                        tint = NavyDark
                    )
                }
            }

            HorizontalDivider(color = Color(0xFFEEEEEE))

            if (vm.isNavLoading || location == null) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = NavyDark)
                }
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                ) {
                    BusStatusHeader(location)

                    FavoriteInfoCard(
                        location = location,
                        isFavorite = vm.isBusFavorite(location),
                        onFavoriteClick = {
                            vm.toggleFavoriteFromNavigator()
                        }
                    )

                    HorizontalDivider(color = Color(0xFFEEEEEE))

                    VerticalRouteMap(
                        stops = location.stops,
                        currentStopIndex = location.currentStopIndex
                    )
                }
            }
        }

        if (vm.isFavoriteSheetVisible) {
            FavoriteNotificationBottomSheet(vm = vm)
        }
    }
}

@Composable
fun BusStatusHeader(location: BusLocationInfo) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(12.dp))
                .background(RedAccent)
                .padding(horizontal = 12.dp, vertical = 4.dp)
        ) {
            Text(
                text = "잔여: ${location.remainingSeats}석",
                fontSize = 12.sp,
                color = Color.White,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        Box(
            modifier = Modifier
                .size(64.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(NavyDark),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "🚌",
                fontSize = 30.sp
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        Row {
            Text(
                text = "출발 예정 | ${location.departureTime}",
                fontSize = 13.sp,
                color = GrayText
            )

            Spacer(modifier = Modifier.width(16.dp))

            Text(
                text = "도착 예정 | ${location.arrivalTime}",
                fontSize = 13.sp,
                color = GrayText
            )
        }

        Spacer(modifier = Modifier.height(4.dp))

        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = "잔여석",
                fontSize = 12.sp,
                color = GrayText
            )

            Spacer(modifier = Modifier.width(4.dp))

            val seatColor = when {
                location.remainingSeats <= 10 -> RedAccent
                location.remainingSeats <= 25 -> OrangeAccent
                else -> NavyDark
            }

            Text(
                text = "${location.remainingSeats}석",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = seatColor
            )
        }
    }
}

@Composable
fun FavoriteInfoCard(
    location: BusLocationInfo,
    isFavorite: Boolean,
    onFavoriteClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 28.dp, vertical = 16.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 18.dp, vertical = 14.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(Color(0xFFF3F5F8))
                        .padding(horizontal = 14.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = location.currentStopName,
                        fontSize = 12.sp,
                        color = Color(0xFF333333),
                        fontWeight = FontWeight.Medium
                    )
                }

                Spacer(modifier = Modifier.weight(1f))

                Text(
                    text = if (isFavorite) "★" else "☆",
                    fontSize = 26.sp,
                    color = if (isFavorite) Color(0xFFFFD43B) else Color(0xFFD8DEE8),
                    modifier = Modifier.clickable { onFavoriteClick() }
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            Text(
                text = "도착 정보: 현재 ${location.currentStopName} 근처",
                fontSize = 12.sp,
                color = Color(0xFF333333)
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "여석: ${location.remainingSeats}석",
                fontSize = 12.sp,
                color = Color(0xFF333333)
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "노선: ${location.routeName}",
                fontSize = 12.sp,
                color = GrayText,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FavoriteNotificationBottomSheet(vm: BusViewModel) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = { vm.closeFavoriteSheet() },
        sheetState = sheetState,
        containerColor = Color.White,
        dragHandle = null
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 22.dp, vertical = 20.dp)
                .navigationBarsPadding()
        ) {
            Text(
                text = "알림을 받을 요일을 선택해 주세요",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF111111)
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = "중복 선택 가능",
                fontSize = 12.sp,
                color = GrayText
            )

            Spacer(modifier = Modifier.height(20.dp))

            NotificationDay.values().forEach { day ->
                val isSelected = vm.selectedNotificationDays.contains(day)

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(if (isSelected) Color(0xFFE9EDF3) else Color.White)
                        .clickable { vm.toggleNotificationDay(day) }
                        .padding(horizontal = 14.dp, vertical = 14.dp)
                ) {
                    Text(
                        text = day.label,
                        fontSize = 14.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                        color = if (isSelected) NavyDark else GrayText
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))
            }

            Spacer(modifier = Modifier.height(8.dp))

            Button(
                onClick = { vm.saveFavoriteWithNotificationDays() },
                enabled = vm.selectedNotificationDays.isNotEmpty(),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = NavyDark,
                    contentColor = Color.White,
                    disabledContainerColor = Color(0xFFF1F1F1),
                    disabledContentColor = GrayText
                )
            ) {
                Text(
                    text = "설정하기",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
fun VerticalRouteMap(
    stops: List<BusStop>,
    currentStopIndex: Int
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 40.dp, vertical = 16.dp)
    ) {
        stops.forEachIndexed { index, stop ->
            Row(
                modifier = Modifier.height(IntrinsicSize.Min),
                verticalAlignment = Alignment.Top
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.width(32.dp)
                ) {
                    if (index > 0) {
                        Box(
                            modifier = Modifier
                                .width(3.dp)
                                .height(20.dp)
                                .background(if (index <= currentStopIndex) NavyDark else GrayLine)
                        )
                    } else {
                        Spacer(modifier = Modifier.height(20.dp))
                    }

                    val markerColor = when {
                        index < currentStopIndex -> NavyDark
                        index == currentStopIndex -> RedAccent
                        else -> GrayLine
                    }

                    Box(
                        modifier = Modifier
                            .size(if (index == currentStopIndex) 18.dp else 14.dp)
                            .clip(CircleShape)
                            .background(markerColor)
                    )

                    if (index < stops.lastIndex) {
                        Box(
                            modifier = Modifier
                                .width(3.dp)
                                .height(20.dp)
                                .background(if (index < currentStopIndex) NavyDark else GrayLine)
                        )
                    } else {
                        Spacer(modifier = Modifier.height(20.dp))
                    }
                }

                Spacer(modifier = Modifier.width(16.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val textColor = when {
                        index < currentStopIndex -> NavyDark
                        index == currentStopIndex -> RedAccent
                        else -> GrayText
                    }

                    Text(
                        text = stop.name,
                        fontSize = 14.sp,
                        color = textColor,
                        fontWeight = if (index == currentStopIndex) FontWeight.Bold else FontWeight.Normal
                    )

                    if (index == currentStopIndex) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "🚌",
                            fontSize = 16.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun PassengerBottomBar(
    selectedTab: BottomTab,
    onTabClick: (BottomTab) -> Unit
) {
    NavigationBar(containerColor = NavyDark) {
        NavigationBarItem(
            selected = selectedTab == BottomTab.TIMETABLE,
            onClick = { onTabClick(BottomTab.TIMETABLE) },
            icon = {
                Text(
                    text = "🕐",
                    fontSize = 18.sp
                )
            },
            label = {
                Text(
                    text = "시간표",
                    color = Color.White,
                    fontSize = 11.sp
                )
            },
            colors = NavigationBarItemDefaults.colors(
                indicatorColor = Color.White.copy(alpha = 0.15f)
            )
        )

        NavigationBarItem(
            selected = selectedTab == BottomTab.FAVORITE,
            onClick = { onTabClick(BottomTab.FAVORITE) },
            icon = {
                Text(
                    text = "⭐",
                    fontSize = 18.sp
                )
            },
            label = {
                Text(
                    text = "즐겨찾기",
                    color = Color.White,
                    fontSize = 11.sp
                )
            },
            colors = NavigationBarItemDefaults.colors(
                indicatorColor = Color.White.copy(alpha = 0.15f)
            )
        )

        NavigationBarItem(
            selected = selectedTab == BottomTab.MYPAGE,
            onClick = { onTabClick(BottomTab.MYPAGE) },
            icon = {
                Text(
                    text = "👤",
                    fontSize = 18.sp
                )
            },
            label = {
                Text(
                    text = "마이페이지",
                    color = Color.White,
                    fontSize = 11.sp
                )
            },
            colors = NavigationBarItemDefaults.colors(
                indicatorColor = Color.White.copy(alpha = 0.15f)
            )
        )
    }
}

@Composable
fun FavoriteScreen(
    favorites: List<FavoriteBus>,
    onRemoveClick: (FavoriteBus) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BgLight)
            .padding(horizontal = 20.dp, vertical = 24.dp)
    ) {
        Text(
            text = "즐겨찾기",
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            color = NavyDark
        )

        Spacer(modifier = Modifier.height(16.dp))

        if (favorites.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "아직 추가된 즐겨찾기가 없습니다.",
                    fontSize = 14.sp,
                    color = GrayText
                )
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(bottom = 16.dp)
            ) {
                items(favorites, key = { it.id }) { favorite ->
                    FavoriteCard(
                        favorite = favorite,
                        onRemoveClick = { onRemoveClick(favorite) }
                    )
                }
            }
        }
    }
}

@Composable
fun FavoriteCard(
    favorite: FavoriteBus,
    onRemoveClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(18.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = favorite.routeName,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF111111),
                    modifier = Modifier.weight(1f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Text(
                    text = "★",
                    fontSize = 24.sp,
                    color = Color(0xFFFFD43B),
                    modifier = Modifier.clickable { onRemoveClick() }
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "현재 위치: ${favorite.currentStopName}",
                fontSize = 12.sp,
                color = Color(0xFF333333)
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = "출발: ${favorite.departureTime}  |  도착 예정: ${favorite.arrivalTime}",
                fontSize = 12.sp,
                color = GrayText
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = "여석: ${favorite.remainingSeats}석",
                fontSize = 12.sp,
                color = Color(0xFF333333)
            )

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = "알림 요일: ${favorite.notificationDays.joinToString(", ") { it.label }}",
                fontSize = 12.sp,
                color = NavyDark,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
fun PlaceholderScreen(
    title: String,
    message: String
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BgLight)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = title,
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            color = NavyDark
        )

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = message,
            fontSize = 14.sp,
            color = GrayText
        )
    }
}