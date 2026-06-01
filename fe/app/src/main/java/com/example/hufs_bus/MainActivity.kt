package com.example.hufs_bus

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DirectionsBus
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.hufs_bus.data.BusLocationInfo
import com.example.hufs_bus.data.BusRoute
import com.example.hufs_bus.data.BusSchedule
import com.example.hufs_bus.data.BusStatus
import com.example.hufs_bus.data.BusStop
import com.example.hufs_bus.data.FavoriteBus
import com.example.hufs_bus.data.NotificationDay
import com.example.hufs_bus.data.RouteType
import com.example.hufs_bus.ui.theme.Hufs_busTheme
import kotlinx.coroutines.delay

private val NavyDark = Color(0xFF073763)
private val NavyMuted = Color(0xFF244F78)
private val RedAccent = Color(0xFFE0462E)
private val OrangeAccent = Color(0xFFFB8C00)
private val GrayText = Color(0xFF8A94A3)
private val GrayLine = Color(0xFFD8DEE8)
private val BgLight = Color(0xFFF6F7F9)
private val FieldBg = Color(0xFFF0F3F6)
private const val SHUTTLE_NOTIFICATION_CHANNEL_ID = "hufs_shuttle_bus_alerts"

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
enum class AppScreen {
    LANDING,
    LOGIN,
    SIGN_UP,
    TIMETABLE,
    NAVIGATOR,
    FAVORITE,
    MYPAGE,
    DRIVER_HOME,
    DRIVER_OPERATION,
    ADMIN_HOME,
    ADMIN_ROUTES,
    ADMIN_STOPS,
    ADMIN_TIMETABLE
}

@Composable
fun PassengerApp(vm: BusViewModel = viewModel()) {
    val context = LocalContext.current
    var currentScreen by remember { mutableStateOf(AppScreen.LANDING) }
    var selectedTab by remember { mutableStateOf(BottomTab.TIMETABLE) }
    val showBottomBar = currentScreen in setOf(AppScreen.TIMETABLE, AppScreen.FAVORITE, AppScreen.MYPAGE)
    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = {}
    )

    LaunchedEffect(Unit) {
        createShuttleNotificationChannel(context)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    LaunchedEffect(vm.lastNotificationMessage) {
        vm.lastNotificationMessage?.let {
            showShuttleNotification(context, it)
            delay(3200)
            vm.clearNotificationMessage()
        }
    }

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
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
                AppScreen.LANDING -> LandingScreen(
                    onLoginClick = { currentScreen = AppScreen.LOGIN },
                    onSignUpClick = { currentScreen = AppScreen.SIGN_UP },
                    onDriverClick = { currentScreen = AppScreen.DRIVER_HOME },
                    onAdminClick = { currentScreen = AppScreen.ADMIN_HOME }
                )

                AppScreen.LOGIN -> LoginScreen(
                    vm = vm,
                    onBackClick = { currentScreen = AppScreen.LANDING },
                    onSignUpClick = { currentScreen = AppScreen.SIGN_UP },
                    onSuccess = {
                        selectedTab = BottomTab.TIMETABLE
                        currentScreen = AppScreen.TIMETABLE
                    }
                )

                AppScreen.SIGN_UP -> SignUpScreen(
                    vm = vm,
                    onBackClick = { currentScreen = AppScreen.LANDING },
                    onLoginClick = { currentScreen = AppScreen.LOGIN },
                    onSuccess = {
                        selectedTab = BottomTab.TIMETABLE
                        currentScreen = AppScreen.TIMETABLE
                    }
                )

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
                            selectedTab = BottomTab.TIMETABLE
                            currentScreen = AppScreen.TIMETABLE
                        }
                    )
                }

                AppScreen.FAVORITE -> FavoriteScreen(
                    favorites = vm.favoriteBuses,
                    onRemoveClick = { favorite -> vm.removeFavorite(favorite.busId, favorite.routeId) }
                )

                AppScreen.MYPAGE -> MyPageScreen(
                    vm = vm,
                    onSignOutClick = {
                        vm.signOut()
                        selectedTab = BottomTab.TIMETABLE
                        currentScreen = AppScreen.LANDING
                    }
                )

                AppScreen.DRIVER_HOME -> DriverHomeScreen(
                    onBackClick = { currentScreen = AppScreen.LANDING },
                    onStartOperation = { currentScreen = AppScreen.DRIVER_OPERATION }
                )

                AppScreen.DRIVER_OPERATION -> DriverOperationScreen(
                    onBackClick = { currentScreen = AppScreen.DRIVER_HOME }
                )

                AppScreen.ADMIN_HOME -> AdminHomeScreen(
                    onBackClick = { currentScreen = AppScreen.LANDING },
                    onGoRoutes = { currentScreen = AppScreen.ADMIN_ROUTES },
                    onGoStops = { currentScreen = AppScreen.ADMIN_STOPS },
                    onGoTimetable = { currentScreen = AppScreen.ADMIN_TIMETABLE }
                )

                AppScreen.ADMIN_ROUTES -> AdminRouteManagementScreen(
                    onBackClick = { currentScreen = AppScreen.ADMIN_HOME }
                )

                AppScreen.ADMIN_STOPS -> AdminStopManagementScreen(
                    onBackClick = { currentScreen = AppScreen.ADMIN_HOME }
                )

                AppScreen.ADMIN_TIMETABLE -> AdminTimetableManagementScreen(
                    onBackClick = { currentScreen = AppScreen.ADMIN_HOME }
                )
            }

            vm.lastNotificationMessage?.let {
                AppNotificationBanner(
                    message = it,
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(horizontal = 18.dp, vertical = 12.dp)
                )
            }
        }
    }
}

@Composable
private fun LandingScreen(
    onLoginClick: () -> Unit,
    onSignUpClick: () -> Unit,
    onDriverClick: () -> Unit,
    onAdminClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
            .padding(horizontal = 28.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.weight(1f))

        BusLogo(size = 74)

        Spacer(modifier = Modifier.weight(1f))

        PrimaryButton(text = "회원가입", onClick = onSignUpClick)

        Spacer(modifier = Modifier.height(12.dp))

        PrimaryButton(text = "로그인", onClick = onLoginClick)

        Spacer(modifier = Modifier.height(18.dp))

        Text("역할별 화면 확인", fontSize = 12.sp, color = GrayText)

        Spacer(modifier = Modifier.height(8.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            SecondaryRoleButton(
                text = "기사",
                onClick = onDriverClick,
                modifier = Modifier.weight(1f)
            )
            SecondaryRoleButton(
                text = "관리자",
                onClick = onAdminClick,
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(52.dp))
    }
}

@Composable
private fun LoginScreen(
    vm: BusViewModel,
    onBackClick: () -> Unit,
    onSignUpClick: () -> Unit,
    onSuccess: () -> Unit
) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }

    AuthFrame(
        title = "로그인",
        onBackClick = onBackClick,
        bottomText = "회원가입",
        onBottomClick = onSignUpClick
    ) {
        AuthTextField(value = email, onValueChange = { email = it }, placeholder = "이메일을 입력해 주세요", icon = Icons.Default.Email)
        Spacer(modifier = Modifier.height(10.dp))
        AuthTextField(
            value = password,
            onValueChange = { password = it },
            placeholder = "비밀번호를 입력해 주세요",
            icon = Icons.Default.Lock,
            isPassword = true
        )
        AuthError(error)
        PrimaryButton(
            text = "로그인",
            onClick = {
                if (vm.signIn(email, password)) onSuccess() else error = "이메일과 비밀번호를 확인해 주세요."
            },
            enabled = email.isNotBlank() && password.isNotBlank()
        )
    }
}

@Composable
private fun SignUpScreen(
    vm: BusViewModel,
    onBackClick: () -> Unit,
    onLoginClick: () -> Unit,
    onSuccess: () -> Unit
) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirm by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }

    AuthFrame(
        title = "회원가입",
        onBackClick = onBackClick,
        bottomText = "로그인",
        onBottomClick = onLoginClick
    ) {
        AuthTextField(value = email, onValueChange = { email = it }, placeholder = "이메일을 입력해 주세요", icon = Icons.Default.Email)
        Spacer(modifier = Modifier.height(10.dp))
        AuthTextField(value = password, onValueChange = { password = it }, placeholder = "비밀번호를 입력해 주세요", icon = Icons.Default.Lock, isPassword = true)
        Spacer(modifier = Modifier.height(10.dp))
        AuthTextField(value = confirm, onValueChange = { confirm = it }, placeholder = "비밀번호를 다시 입력해 주세요", icon = Icons.Default.Lock, isPassword = true)
        Spacer(modifier = Modifier.height(12.dp))
        RequirementRow("이메일 형식 입력", email.contains("@"))
        RequirementRow("비밀번호 6자 이상", password.length >= 6)
        RequirementRow("비밀번호 일치", password.isNotBlank() && password == confirm)
        AuthError(error)
        PrimaryButton(
            text = "회원가입",
            onClick = {
                if (vm.signUp(email, password, confirm)) onSuccess() else error = "가입 정보를 다시 확인해 주세요."
            },
            enabled = email.isNotBlank() && password.isNotBlank() && confirm.isNotBlank()
        )
    }
}

@Composable
private fun AuthFrame(
    title: String,
    onBackClick: () -> Unit,
    bottomText: String,
    onBottomClick: () -> Unit,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
            .padding(horizontal = 24.dp, vertical = 18.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBackClick) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "뒤로가기", tint = NavyDark)
            }
            Spacer(modifier = Modifier.weight(1f))
            TextButton(onClick = onBottomClick) {
                Text(bottomText, color = NavyDark, fontSize = 12.sp)
            }
        }

        Spacer(modifier = Modifier.height(36.dp))
        BusLogo(size = 58)
        Spacer(modifier = Modifier.height(8.dp))
        Text(title, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = NavyDark)
        Spacer(modifier = Modifier.height(34.dp))

        Column(modifier = Modifier.fillMaxWidth()) {
            content()
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
        CampusTabs(selectedType = vm.selectedCampusType, onTypeSelected = { vm.selectCampusType(it) })

        Spacer(modifier = Modifier.height(16.dp))

        Column(modifier = Modifier.padding(horizontal = 20.dp)) {
            RouteDropdown(
                routes = vm.routes,
                selectedRoute = vm.selectedRoute,
                onRouteSelected = { vm.selectRoute(it) }
            )

            Spacer(modifier = Modifier.height(14.dp))
            HourChipRow(hours = hourList, selectedHour = vm.selectedHour, onHourSelected = { vm.selectHour(it) })
        }

        Spacer(modifier = Modifier.height(16.dp))

        when {
            vm.isLoading -> CenterLoading()
            vm.schedules.isEmpty() -> EmptyState("해당 시간대 운행 정보가 없습니다.")
            else -> LazyColumn(
                modifier = Modifier.padding(horizontal = 20.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                contentPadding = PaddingValues(bottom = 16.dp)
            ) {
                items(vm.schedules, key = { it.id }) { schedule ->
                    ScheduleCard(schedule = schedule, onClick = { onScheduleClick(schedule) })
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
        listOf("교내" to RouteType.CAMPUS, "교외" to RouteType.OFF_CAMPUS).forEach { (label, type) ->
            val selected = selectedType == type
            Column(
                modifier = Modifier
                    .weight(1f)
                    .clickable { onTypeSelected(type) },
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = label,
                    fontSize = 14.sp,
                    color = if (selected) NavyDark else GrayText,
                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                    modifier = Modifier.padding(vertical = 12.dp)
                )
                Box(
                    modifier = Modifier
                        .height(3.dp)
                        .fillMaxWidth()
                        .background(if (selected) NavyDark else Color.Transparent)
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
                text = selectedRoute?.name ?: "노선을 선택해 주세요",
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

        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            routes.forEach { route ->
                DropdownMenuItem(
                    text = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(route.name, modifier = Modifier.weight(1f))
                            if (route.id == selectedRoute?.id) {
                                Icon(Icons.Default.CheckCircle, contentDescription = null, tint = NavyDark, modifier = Modifier.size(16.dp))
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
            val selected = selectedHour == hour
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(if (selected) NavyDark else Color.White)
                    .clickable { onHourSelected(hour) }
                    .padding(horizontal = 14.dp, vertical = 8.dp)
            ) {
                Text(
                    text = "${hour}시",
                    fontSize = 12.sp,
                    color = if (selected) Color.White else GrayText,
                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
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
    val seatColor = seatColor(schedule.remainingSeats, schedule.totalSeats)
    val alpha = if (schedule.status == BusStatus.COMPLETED) 0.45f else 1f

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
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
        Column(modifier = Modifier.fillMaxSize()) {
            TopBar(
                title = location?.routeName ?: "노선 정보",
                onBackClick = onBackClick,
                trailing = {
                    IconButton(onClick = { vm.refreshBusLocation() }) {
                        Icon(Icons.Default.Refresh, contentDescription = "새로고침", tint = NavyDark)
                    }
                }
            )

            HorizontalDivider(color = Color(0xFFEEEEEE))

            if (vm.isNavLoading || location == null) {
                CenterLoading()
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
                        onFavoriteClick = { vm.toggleFavoriteFromNavigator() }
                    )
                    HorizontalDivider(color = Color(0xFFEEEEEE))
                    VerticalRouteMap(stops = location.stops, currentStopIndex = location.currentStopIndex)
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
        SeatPill(location.remainingSeats, location.totalSeats)
        Spacer(modifier = Modifier.height(12.dp))
        BusLogo(size = 64)
        Spacer(modifier = Modifier.height(12.dp))
        Row {
            MetaText("출발 예정 | ${location.departureTime}")
            Spacer(modifier = Modifier.width(16.dp))
            MetaText("도착 예정 | ${location.arrivalTime}")
        }
        Spacer(modifier = Modifier.height(4.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("여석", fontSize = 12.sp, color = GrayText)
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = "${location.remainingSeats}/${location.totalSeats}",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = seatColor(location.remainingSeats, location.totalSeats)
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
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(horizontal = 18.dp, vertical = 14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(FieldBg)
                        .padding(horizontal = 14.dp, vertical = 6.dp)
                ) {
                    Text(location.currentStopName, fontSize = 12.sp, color = Color(0xFF333333), fontWeight = FontWeight.Medium)
                }
                Spacer(modifier = Modifier.weight(1f))
                IconButton(onClick = onFavoriteClick) {
                    Icon(
                        Icons.Default.Star,
                        contentDescription = "즐겨찾기",
                        tint = if (isFavorite) Color(0xFFFFC400) else GrayLine
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))
            InfoLine("현재 위치", "현재 ${location.currentStopName} 근처")
            InfoLine("좌석", "${location.remainingSeats}/${location.totalSeats}석")
            InfoLine("노선", location.routeName)
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
            Text("알림 받을 요일을 선택해 주세요", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color(0xFF111111))
            Spacer(modifier = Modifier.height(6.dp))
            Text("즐겨찾기 등록 후 상단 알림 배너로 설정 결과를 보여줍니다.", fontSize = 12.sp, color = GrayText)
            Spacer(modifier = Modifier.height(20.dp))

            NotificationDay.values().forEach { day ->
                val selected = vm.selectedNotificationDays.contains(day)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(if (selected) Color(0xFFE9EDF3) else Color.White)
                        .clickable { vm.toggleNotificationDay(day) }
                        .padding(horizontal = 14.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = day.label,
                        fontSize = 14.sp,
                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                        color = if (selected) NavyDark else GrayText,
                        modifier = Modifier.weight(1f)
                    )
                    if (selected) {
                        Icon(Icons.Default.CheckCircle, contentDescription = null, tint = NavyDark, modifier = Modifier.size(18.dp))
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
            }

            Spacer(modifier = Modifier.height(8.dp))
            PrimaryButton(
                text = "설정하기",
                onClick = { vm.saveFavoriteWithNotificationDays() },
                enabled = vm.selectedNotificationDays.isNotEmpty()
            )
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
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.width(32.dp)) {
                    Box(
                        modifier = Modifier
                            .width(3.dp)
                            .height(20.dp)
                            .background(if (index > 0 && index <= currentStopIndex) NavyDark else Color.Transparent)
                    )

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

                    Box(
                        modifier = Modifier
                            .width(3.dp)
                            .height(20.dp)
                            .background(if (index < stops.lastIndex && index < currentStopIndex) NavyDark else if (index < stops.lastIndex) GrayLine else Color.Transparent)
                    )
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
                        Icon(Icons.Default.DirectionsBus, contentDescription = null, tint = RedAccent, modifier = Modifier.size(18.dp))
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
        BottomNavItem(selectedTab == BottomTab.TIMETABLE, "시간표", Icons.Default.Event) { onTabClick(BottomTab.TIMETABLE) }
        BottomNavItem(selectedTab == BottomTab.FAVORITE, "즐겨찾기", Icons.Default.Star) { onTabClick(BottomTab.FAVORITE) }
        BottomNavItem(selectedTab == BottomTab.MYPAGE, "마이페이지", Icons.Default.Person) { onTabClick(BottomTab.MYPAGE) }
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
        Text("즐겨찾기", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = NavyDark)
        Spacer(modifier = Modifier.height(16.dp))

        if (favorites.isEmpty()) {
            EmptyState("아직 추가된 즐겨찾기가 없습니다.")
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(bottom = 16.dp)
            ) {
                items(favorites, key = { it.id }) { favorite ->
                    FavoriteCard(favorite = favorite, onRemoveClick = { onRemoveClick(favorite) })
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
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = favorite.routeName,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF111111),
                    modifier = Modifier.weight(1f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                IconButton(onClick = onRemoveClick) {
                    Icon(Icons.Default.Star, contentDescription = "즐겨찾기 해제", tint = Color(0xFFFFC400))
                }
            }

            InfoLine("현재 위치", favorite.currentStopName)
            InfoLine("출발", favorite.departureTime)
            InfoLine("도착 예정", favorite.arrivalTime)
            InfoLine("좌석", "${favorite.remainingSeats}/${favorite.totalSeats}석")
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
fun MyPageScreen(
    vm: BusViewModel,
    onSignOutClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BgLight)
            .padding(horizontal = 20.dp, vertical = 24.dp)
    ) {
        Text("마이페이지", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = NavyDark)
        Spacer(modifier = Modifier.height(16.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White)
        ) {
            Column(modifier = Modifier.padding(18.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    BusLogo(size = 44)
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(vm.userName, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color(0xFF111111))
                        Text(vm.userEmail.ifBlank { "guest@hufs.ac.kr" }, fontSize = 12.sp, color = GrayText)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        SettingRow(
            title = "상단 알림",
            value = "즐겨찾기 버스 도착 전 알림",
            icon = Icons.Default.Notifications
        )
        SettingRow(
            title = "등록된 즐겨찾기",
            value = "${vm.favoriteBuses.size}개",
            icon = Icons.Default.Star
        )

        Spacer(modifier = Modifier.weight(1f))

        Button(
            onClick = onSignOutClick,
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp),
            shape = RoundedCornerShape(10.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = RedAccent)
        ) {
            Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text("로그아웃", fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun SettingRow(
    title: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 10.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, contentDescription = null, tint = NavyDark, modifier = Modifier.size(22.dp))
            Spacer(modifier = Modifier.width(12.dp))
            Text(title, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color(0xFF222222), modifier = Modifier.weight(1f))
            Text(value, fontSize = 12.sp, color = GrayText)
        }
    }
}

@Composable
private fun AppNotificationBanner(message: String, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFEAF2FA)),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            BusLogo(size = 32)
            Spacer(modifier = Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text("셔틀 알림", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = NavyDark)
                Text(message, fontSize = 12.sp, color = Color(0xFF333333), maxLines = 2, overflow = TextOverflow.Ellipsis)
            }
        }
    }
}

@Composable
private fun TopBar(
    title: String,
    onBackClick: () -> Unit,
    trailing: @Composable () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onBackClick) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "뒤로가기", tint = Color(0xFF333333))
        }
        Text(
            text = title,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF333333),
            modifier = Modifier.weight(1f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        trailing()
    }
}

@Composable
private fun BusLogo(size: Int) {
    Box(
        modifier = Modifier
            .size(size.dp)
            .clip(RoundedCornerShape((size / 5).dp))
            .background(NavyDark),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            Icons.Default.DirectionsBus,
            contentDescription = null,
            tint = Color.White,
            modifier = Modifier.size((size * 0.58f).dp)
        )
    }
}

@Composable
private fun PrimaryButton(
    text: String,
    onClick: () -> Unit,
    enabled: Boolean = true
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier
            .fillMaxWidth()
            .height(50.dp),
        shape = RoundedCornerShape(10.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = NavyDark,
            contentColor = Color.White,
            disabledContainerColor = Color(0xFFF1F1F1),
            disabledContentColor = GrayText
        )
    ) {
        Text(text, fontSize = 14.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun AuthTextField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    isPassword: Boolean = false
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = Modifier.fillMaxWidth(),
        placeholder = { Text(placeholder, fontSize = 12.sp) },
        leadingIcon = { Icon(icon, contentDescription = null, tint = GrayText, modifier = Modifier.size(18.dp)) },
        singleLine = true,
        shape = RoundedCornerShape(8.dp),
        visualTransformation = if (isPassword) PasswordVisualTransformation() else androidx.compose.ui.text.input.VisualTransformation.None,
        keyboardOptions = KeyboardOptions(keyboardType = if (isPassword) KeyboardType.Password else KeyboardType.Email),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = NavyDark,
            unfocusedBorderColor = GrayLine,
            focusedContainerColor = Color.White,
            unfocusedContainerColor = Color.White
        )
    )
}

@Composable
private fun RequirementRow(text: String, checked: Boolean) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(bottom = 4.dp)) {
        Icon(Icons.Default.CheckCircle, contentDescription = null, tint = if (checked) NavyDark else GrayLine, modifier = Modifier.size(14.dp))
        Spacer(modifier = Modifier.width(6.dp))
        Text(text, fontSize = 12.sp, color = if (checked) NavyDark else GrayText)
    }
}

@Composable
private fun AuthError(error: String?) {
    Spacer(modifier = Modifier.height(10.dp))
    Text(
        text = error.orEmpty(),
        fontSize = 12.sp,
        color = RedAccent,
        minLines = 1
    )
    Spacer(modifier = Modifier.height(4.dp))
}

@Composable
private fun InfoLine(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, fontSize = 12.sp, color = GrayText, modifier = Modifier.width(72.dp))
        Text(
            value,
            fontSize = 12.sp,
            color = Color(0xFF333333),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun SeatPill(remainingSeats: Int, totalSeats: Int) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .background(seatColor(remainingSeats, totalSeats))
            .padding(horizontal = 12.dp, vertical = 4.dp)
    ) {
        Text("여석: $remainingSeats/$totalSeats", fontSize = 12.sp, color = Color.White, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun MetaText(text: String) {
    Text(text, fontSize = 13.sp, color = GrayText)
}

@Composable
private fun CenterLoading() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator(color = NavyDark)
    }
}

@Composable
private fun EmptyState(message: String) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(
            text = message,
            color = GrayText,
            fontSize = 14.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 24.dp)
        )
    }
}

@Composable
private fun RowScope.BottomNavItem(
    selected: Boolean,
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit
) {
    NavigationBarItem(
        selected = selected,
        onClick = onClick,
        icon = { Icon(icon, contentDescription = label, tint = Color.White, modifier = Modifier.size(20.dp)) },
        label = { Text(label, color = Color.White, fontSize = 11.sp) },
        colors = NavigationBarItemDefaults.colors(indicatorColor = Color.White.copy(alpha = 0.15f))
    )
}

@Composable
private fun SecondaryRoleButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Button(
        onClick = onClick,
        modifier = modifier.height(44.dp),
        shape = RoundedCornerShape(10.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = FieldBg,
            contentColor = NavyDark
        )
    ) {
        Text(text, fontSize = 13.sp, fontWeight = FontWeight.Bold)
    }
}

private data class AdminRouteItem(
    val name: String,
    val stops: List<String>
)

private data class AdminStopItem(
    val name: String,
    val memo: String
)

private data class AdminTimetableItem(
    val route: String,
    val time: String,
    val dayType: String
)

@Composable
private fun AdminHomeScreen(
    onBackClick: () -> Unit,
    onGoRoutes: () -> Unit,
    onGoStops: () -> Unit,
    onGoTimetable: () -> Unit
) {
    RoleFrame(
        title = "관리자",
        subtitle = "노선/정류장/시간표 관리",
        onBackClick = onBackClick
    ) {
        AdminMenuCard("노선 관리", "운행 노선과 경유 정류장을 관리합니다.", onGoRoutes)
        AdminMenuCard("정류장 관리", "정류장 이름과 안내 정보를 관리합니다.", onGoStops)
        AdminMenuCard("시간표 관리", "노선별 출발 시간과 운행 유형을 관리합니다.", onGoTimetable)

        Spacer(modifier = Modifier.height(18.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("통합 상태", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = NavyDark)
                Spacer(modifier = Modifier.height(8.dp))
                InfoLine("기준 폴더", "fe/app")
                InfoLine("이전 위치", "fe_local_backup")
                InfoLine("UI 규격", "승객 화면과 동일한 컬러/카드/버튼 기준")
            }
        }
    }
}

@Composable
private fun AdminRouteManagementScreen(onBackClick: () -> Unit) {
    val routes = remember {
        listOf(
            AdminRouteItem("판교역 → 글로벌캠퍼스", listOf("판교역", "성남역", "서현역", "인문경상관")),
            AdminRouteItem("글로벌캠퍼스 → 판교역", listOf("백년관", "서현역", "성남역", "판교역")),
            AdminRouteItem("교내 순환 상행", listOf("지석묘", "도서관", "학생회관", "인문경상관"))
        )
    }

    RoleFrame(title = "노선 관리", subtitle = "관리자", onBackClick = onBackClick) {
        routes.forEach {
            AdminListCard(
                title = it.name,
                body = it.stops.joinToString(" → "),
                badge = "${it.stops.size}개 정류장"
            )
        }
        AdminPendingAction("노선 추가/수정/삭제 API 연결 예정")
    }
}

@Composable
private fun AdminStopManagementScreen(onBackClick: () -> Unit) {
    val stops = remember {
        listOf(
            AdminStopItem("판교역", "교외 등교 노선 기점"),
            AdminStopItem("서현역", "교외 노선 주요 경유지"),
            AdminStopItem("인문경상관", "글로벌캠퍼스 주요 승하차 지점"),
            AdminStopItem("백년관", "하교 노선 출발 지점")
        )
    }

    RoleFrame(title = "정류장 관리", subtitle = "관리자", onBackClick = onBackClick) {
        stops.forEach {
            AdminListCard(title = it.name, body = it.memo, badge = "정류장")
        }
        AdminPendingAction("정류장 추가/수정/삭제 API 연결 예정")
    }
}

@Composable
private fun AdminTimetableManagementScreen(onBackClick: () -> Unit) {
    val timetables = remember {
        listOf(
            AdminTimetableItem("판교역 → 글로벌캠퍼스", "07:40", "평일"),
            AdminTimetableItem("판교역 → 글로벌캠퍼스", "08:20", "평일"),
            AdminTimetableItem("글로벌캠퍼스 → 판교역", "17:30", "평일"),
            AdminTimetableItem("교내 순환 상행", "09:00", "전체")
        )
    }

    RoleFrame(title = "시간표 관리", subtitle = "관리자", onBackClick = onBackClick) {
        timetables.forEach {
            AdminListCard(title = it.time, body = it.route, badge = it.dayType)
        }
        AdminPendingAction("시간표 등록/수정/삭제 API 연결 예정")
    }
}

@Composable
private fun DriverHomeScreen(
    onBackClick: () -> Unit,
    onStartOperation: () -> Unit
) {
    RoleFrame(
        title = "기사",
        subtitle = "오늘 운행 일정",
        onBackClick = onBackClick
    ) {
        AdminListCard(
            title = "판교역 → 글로벌캠퍼스",
            body = "출발 08:20 · 총 45석",
            badge = "운행 대기",
            onClick = onStartOperation
        )
        AdminListCard(
            title = "글로벌캠퍼스 → 판교역",
            body = "출발 17:30 · 총 45석",
            badge = "운행 대기",
            onClick = onStartOperation
        )
    }
}

@Composable
private fun DriverOperationScreen(onBackClick: () -> Unit) {
    var passengers by remember { mutableStateOf(0) }
    var currentStop by remember { mutableStateOf(0) }
    val stops = listOf("판교역", "성남역", "서현역", "인문경상관")

    RoleFrame(
        title = "운행 화면",
        subtitle = "기사",
        onBackClick = onBackClick
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White)
        ) {
            Column(
                modifier = Modifier.padding(18.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                BusLogo(size = 58)
                Spacer(modifier = Modifier.height(14.dp))
                Text("탑승 인원", color = RedAccent, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                Text(
                    text = "%02d/45".format(passengers),
                    color = RedAccent,
                    fontSize = 34.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(10.dp))
                Text("현재 정류장: ${stops[currentStop]}", fontSize = 14.sp, color = NavyDark, fontWeight = FontWeight.Bold)
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            SecondaryRoleButton(
                text = "-1",
                onClick = { if (passengers > 0) passengers-- },
                modifier = Modifier.weight(1f)
            )
            SecondaryRoleButton(
                text = "+1",
                onClick = { if (passengers < 45) passengers++ },
                modifier = Modifier.weight(1f)
            )
        }

        PrimaryButton(
            text = "다음 정류장",
            onClick = { if (currentStop < stops.lastIndex) currentStop++ },
            enabled = currentStop < stops.lastIndex
        )
    }
}

@Composable
private fun RoleFrame(
    title: String,
    subtitle: String,
    onBackClick: () -> Unit,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BgLight)
    ) {
        TopBar(
            title = title,
            onBackClick = onBackClick,
            trailing = {
                Text(
                    text = subtitle,
                    color = GrayText,
                    fontSize = 12.sp,
                    modifier = Modifier.padding(end = 12.dp)
                )
            }
        )
        HorizontalDivider(color = GrayLine)
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            content()
        }
    }
}

@Composable
private fun AdminMenuCard(
    title: String,
    body: String,
    onClick: () -> Unit
) {
    AdminListCard(title = title, body = body, badge = "관리", onClick = onClick)
}

@Composable
private fun AdminListCard(
    title: String,
    body: String,
    badge: String,
    onClick: (() -> Unit)? = null
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable { onClick() } else Modifier),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(title, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Color(0xFF222222))
                Spacer(modifier = Modifier.height(4.dp))
                Text(body, fontSize = 12.sp, color = GrayText, maxLines = 2, overflow = TextOverflow.Ellipsis)
            }
            Spacer(modifier = Modifier.width(12.dp))
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(Color(0xFFEAF2FA))
                    .padding(horizontal = 10.dp, vertical = 5.dp)
            ) {
                Text(badge, fontSize = 11.sp, color = NavyDark, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun AdminPendingAction(text: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(FieldBg)
            .padding(14.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(text, fontSize = 12.sp, color = GrayText, textAlign = TextAlign.Center)
    }
}

private fun seatColor(remainingSeats: Int, totalSeats: Int): Color {
    val ratio = if (totalSeats == 0) 0f else remainingSeats.toFloat() / totalSeats
    return when {
        remainingSeats == 0 -> RedAccent
        ratio <= 0.35f -> RedAccent
        ratio <= 0.7f -> OrangeAccent
        else -> NavyDark
    }
}

private fun createShuttleNotificationChannel(context: Context) {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return

    val channel = NotificationChannel(
        SHUTTLE_NOTIFICATION_CHANNEL_ID,
        "셔틀버스 알림",
        NotificationManager.IMPORTANCE_DEFAULT
    ).apply {
        description = "즐겨찾기 셔틀버스 도착 및 여석 알림"
    }

    val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    manager.createNotificationChannel(channel)
}

private fun showShuttleNotification(context: Context, message: String) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
        ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
    ) {
        return
    }

    val notification = NotificationCompat.Builder(context, SHUTTLE_NOTIFICATION_CHANNEL_ID)
        .setSmallIcon(android.R.drawable.ic_dialog_info)
        .setContentTitle("셔틀 알림")
        .setContentText(message)
        .setStyle(NotificationCompat.BigTextStyle().bigText(message))
        .setPriority(NotificationCompat.PRIORITY_DEFAULT)
        .setAutoCancel(true)
        .build()

    runCatching {
        NotificationManagerCompat.from(context).notify(1001, notification)
    }
}
