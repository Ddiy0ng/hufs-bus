package com.hufsteam.shuttletrack.ui.student

import androidx.compose.foundation.background
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
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hufsteam.shuttletrack.ui.auth.AuthViewModel
import com.hufsteam.shuttletrack.ui.common.BusIcon
import com.hufsteam.shuttletrack.ui.theme.DividerColor
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

private val offCampusSchedules = listOf(
    BusSchedule(1, "경기광주역 → 외대(글)", "08:20", 8,  45, "법학관을 지나고 있습니다"),
    BusSchedule(2, "경기광주역 → 외대(글)", "08:30", 48, 48, "위치 확인 중입니다"),
    BusSchedule(3, "경기광주역 → 외대(글)", "08:40", 44, 45, "내리실 정거장에 접근 중입니다"),
    BusSchedule(4, "경기광주역 → 외대(글)", "08:50", 42, 45, "인문경상관 근처입니다")
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
fun StudentMainScreen(viewModel: AuthViewModel, onLogout: () -> Unit) {
    var currentScreen    by remember { mutableStateOf(StudentScreen.TIMETABLE) }
    var selectedTab      by remember { mutableStateOf(StudentTab.TIMETABLE) }
    var selectedSchedule by remember { mutableStateOf(offCampusSchedules.first()) }

    Scaffold(
        bottomBar = {
            if (currentScreen != StudentScreen.ROUTE_STATUS) {
                StudentBottomBar(
                    selected  = selectedTab,
                    onTabClick = { tab ->
                        selectedTab   = tab
                        currentScreen = when (tab) {
                            StudentTab.TIMETABLE  -> StudentScreen.TIMETABLE
                            StudentTab.FAVORITES  -> StudentScreen.FAVORITES
                            StudentTab.MYPAGE     -> StudentScreen.MYPAGE
                        }
                    }
                )
            }
        }
    ) { innerPadding ->
        Box(modifier = Modifier.padding(innerPadding)) {
            when (currentScreen) {
                StudentScreen.TIMETABLE -> StudentTimetableContent(
                    onScheduleClick = {
                        selectedSchedule = it
                        currentScreen    = StudentScreen.ROUTE_STATUS
                    }
                )
                StudentScreen.ROUTE_STATUS -> StudentRouteStatusContent(
                    schedule    = selectedSchedule,
                    onBackClick = {
                        currentScreen = StudentScreen.TIMETABLE
                        selectedTab   = StudentTab.TIMETABLE
                    }
                )
                StudentScreen.FAVORITES -> StudentFavoritesContent()
                StudentScreen.MYPAGE    -> StudentMyPageContent(viewModel = viewModel, onLogout = onLogout)
            }
        }
    }
}

// ── 시간표 화면 ─────────────────────────────────────────────────
@Composable
private fun StudentTimetableContent(onScheduleClick: (BusSchedule) -> Unit) {
    var selectedCampus by remember { mutableStateOf(0) }   // 0=교내, 1=교외
    val routes         = if (selectedCampus == 0) onCampusRoutes else offCampusRoutes
    var selectedRoute  by remember(selectedCampus) { mutableStateOf(routes[0]) }
    var dropdownOpen   by remember { mutableStateOf(false) }
    var selectedHour   by remember { mutableStateOf(if (selectedCampus == 0) 9 else 8) }

    val schedules = if (selectedCampus == 0) onCampusSchedules else offCampusSchedules

    Column(modifier = Modifier.fillMaxSize().background(Color.White)) {
        // 교내 / 교외 탭
        TabRow(
            selectedTabIndex = selectedCampus,
            containerColor   = Color.White,
            contentColor     = NavyBlue
        ) {
            listOf("교내", "교외").forEachIndexed { idx, label ->
                Tab(
                    selected = selectedCampus == idx,
                    onClick  = {
                        selectedCampus = idx
                        selectedRoute  = if (idx == 0) onCampusRoutes[0] else offCampusRoutes[0]
                        dropdownOpen   = false
                        selectedHour   = if (idx == 0) 9 else 8
                    },
                    text = {
                        Text(
                            label,
                            fontWeight = if (selectedCampus == idx) FontWeight.Bold else FontWeight.Normal,
                            color      = if (selectedCampus == idx) NavyBlue else Color(0xFF999999)
                        )
                    }
                )
            }
        }

        Column(modifier = Modifier.weight(1f).padding(horizontal = 16.dp)) {
            Spacer(Modifier.height(12.dp))

            // 노선 드롭다운
            Box {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color.White)
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
                    expanded         = dropdownOpen,
                    onDismissRequest = { dropdownOpen = false },
                    modifier         = Modifier.background(Color.White)
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

            // 시간대 칩
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(hours) { hour ->
                    val isSelected = hour == selectedHour
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(if (isSelected) NavyBlue else Color.White)
                            .clickable { selectedHour = hour }
                            .padding(horizontal = 14.dp, vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            "${hour.toString().padStart(2, '0')}시",
                            fontSize   = 13.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            color      = if (isSelected) Color.White else Color(0xFF555555)
                        )
                    }
                }
            }

            Spacer(Modifier.height(12.dp))

            // 시간표 카드 목록
            LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                items(schedules) { schedule ->
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
        colors   = CardDefaults.cardColors(containerColor = Color.White),
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
private fun StudentRouteStatusContent(schedule: BusSchedule, onBackClick: () -> Unit) {
    val stops            = listOf("경기광주역", "도서관", "본관", "인문관", "외대 정문")
    val currentStopIndex = 2

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
            .padding(horizontal = 20.dp)
            .padding(top = 20.dp)
    ) {
        // 뒤로가기
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBackClick) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "뒤로가기", tint = NavyBlue)
            }
            Text(schedule.routeName, fontSize = 15.sp, fontWeight = FontWeight.Bold)
        }

        Spacer(Modifier.height(40.dp))

        // 운행 상태
        Text(
            "운행 중",
            color      = Color(0xFFE53935),
            fontSize   = 14.sp,
            fontWeight = FontWeight.Bold,
            modifier   = Modifier.align(Alignment.CenterHorizontally)
        )
        Spacer(Modifier.height(8.dp))
        Text(
            "${schedule.remainingSeats}/${schedule.totalSeats}",
            color      = Color(0xFFE53935),
            fontSize   = 32.sp,
            fontWeight = FontWeight.Bold,
            modifier   = Modifier.align(Alignment.CenterHorizontally)
        )

        Spacer(Modifier.height(28.dp))

        // 현재 위치 + 도착 예정
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .background(Color(0xFFF1F3F6))
                .padding(horizontal = 16.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("현재 위치  ${stops[currentStopIndex]}", fontSize = 12.sp)
            Text("도착 예정  08:32", fontSize = 12.sp)
        }

        Spacer(Modifier.height(34.dp))

        // 정류장 진행 바
        RouteProgressBar(stops = stops, currentStopIndex = currentStopIndex)

        Spacer(Modifier.height(36.dp))

        Button(
            onClick  = {},
            modifier = Modifier.fillMaxWidth().height(48.dp),
            colors   = ButtonDefaults.buttonColors(containerColor = NavyBlue),
            shape    = RoundedCornerShape(10.dp)
        ) {
            Text("경로 확인하기", color = Color.White)
        }
    }
}

@Composable
private fun RouteProgressBar(stops: List<String>, currentStopIndex: Int) {
    Column {
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            stops.forEachIndexed { index, _ ->
                Box(
                    modifier = Modifier
                        .size(if (index == currentStopIndex) 18.dp else 14.dp)
                        .clip(CircleShape)
                        .background(if (index <= currentStopIndex) NavyBlue else Color(0xFFD8DEE8))
                )
                if (index != stops.lastIndex) {
                    Box(
                        modifier = Modifier
                            .height(3.dp)
                            .weight(1f)
                            .background(if (index < currentStopIndex) NavyBlue else Color(0xFFD8DEE8))
                    )
                }
            }
        }
        Spacer(Modifier.height(10.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            stops.forEach { stop ->
                Text(stop, fontSize = 10.sp, color = Color(0xFF333333))
            }
        }
    }
}

// ── 즐겨찾기 (플레이스홀더) ──────────────────────────────────────
@Composable
private fun StudentFavoritesContent() {
    Box(modifier = Modifier.fillMaxSize().background(Color.White), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("★", fontSize = 40.sp, color = Color(0xFFDDDDDD))
            Spacer(Modifier.height(12.dp))
            Text("아직 추가된 즐겨찾기가 없습니다.", color = Color(0xFF999999), fontSize = 14.sp)
        }
    }
}

// ── 학생 마이페이지 ─────────────────────────────────────────────
@Composable
private fun StudentMyPageContent(viewModel: AuthViewModel, onLogout: () -> Unit) {
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
                            .background(StudentBadge)
                            .padding(horizontal = 10.dp, vertical = 3.dp)
                    ) {
                        Text("사용자", color = StudentBadgeText, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
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
