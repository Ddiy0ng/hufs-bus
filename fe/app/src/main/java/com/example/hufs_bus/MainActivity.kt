package com.example.hufs_bus

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.hufs_bus.ui.theme.Hufs_busTheme

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

enum class BottomTab {
    TIMETABLE, FAVORITE, MYPAGE
}

enum class Screen {
    TIMETABLE, ROUTE_STATUS, FAVORITE, MYPAGE
}

data class BusSchedule(
    val id: Int,
    val routeName: String,
    val departureTime: String,
    val remainingSeats: Int,
    val totalSeats: Int,
    val currentLocation: String
)

val sampleSchedules = listOf(
    BusSchedule(
        id = 1,
        routeName = "경기광주역 → 외대(글)",
        departureTime = "08:20",
        remainingSeats = 8,
        totalSeats = 45,
        currentLocation = "법학관을 지나고 있습니다"
    ),
    BusSchedule(
        id = 2,
        routeName = "경기광주역 → 외대(글)",
        departureTime = "08:30",
        remainingSeats = 48,
        totalSeats = 48,
        currentLocation = "위치 확인 중입니다"
    ),
    BusSchedule(
        id = 3,
        routeName = "경기광주역 → 외대(글)",
        departureTime = "08:40",
        remainingSeats = 44,
        totalSeats = 45,
        currentLocation = "내리실 정거장에 접근 중입니다"
    ),
    BusSchedule(
        id = 4,
        routeName = "경기광주역 → 외대(글)",
        departureTime = "08:40",
        remainingSeats = 42,
        totalSeats = 45,
        currentLocation = "인문경상관 근처입니다"
    )
)

@Composable
fun PassengerApp() {
    var currentScreen by remember { mutableStateOf(Screen.TIMETABLE) }
    var selectedTab by remember { mutableStateOf(BottomTab.TIMETABLE) }
    var selectedSchedule by remember { mutableStateOf(sampleSchedules.first()) }

    Scaffold(
        bottomBar = {
            PassengerBottomBar(
                selectedTab = selectedTab,
                onTabClick = { tab ->
                    selectedTab = tab
                    currentScreen = when (tab) {
                        BottomTab.TIMETABLE -> Screen.TIMETABLE
                        BottomTab.FAVORITE -> Screen.FAVORITE
                        BottomTab.MYPAGE -> Screen.MYPAGE
                    }
                }
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(Color(0xFFF6F7F9))
        ) {
            when (currentScreen) {
                Screen.TIMETABLE -> TimetableScreen(
                    schedules = sampleSchedules,
                    onScheduleClick = {
                        selectedSchedule = it
                        currentScreen = Screen.ROUTE_STATUS
                    }
                )

                Screen.ROUTE_STATUS -> RouteStatusScreen(
                    schedule = selectedSchedule,
                    onBackClick = {
                        currentScreen = Screen.TIMETABLE
                        selectedTab = BottomTab.TIMETABLE
                    }
                )

                Screen.FAVORITE -> SimplePlaceholderScreen("즐겨찾기", "아직 추가된 즐겨찾기가 없습니다.")
                Screen.MYPAGE -> SimplePlaceholderScreen("마이페이지", "마이페이지 화면은 추후 구현 예정입니다.")
            }
        }
    }
}

@Composable
fun TimetableScreen(
    schedules: List<BusSchedule>,
    onScheduleClick: (BusSchedule) -> Unit
) {
    var selectedCampus by remember { mutableStateOf("교외") }
    var selectedRoute by remember { mutableStateOf("경기광주역 → 외대(글)") }
    var selectedHour by remember { mutableStateOf("08시") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp)
            .padding(top = 24.dp)
    ) {
        CampusTabs(
            selectedCampus = selectedCampus,
            onCampusSelected = { selectedCampus = it }
        )

        Spacer(modifier = Modifier.height(16.dp))

        RouteSelector(
            selectedRoute = selectedRoute,
            onRouteSelected = { selectedRoute = it }
        )

        Spacer(modifier = Modifier.height(14.dp))

        TimeChipRow(
            selectedHour = selectedHour,
            onHourSelected = { selectedHour = it }
        )

        Spacer(modifier = Modifier.height(16.dp))

        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(schedules) { schedule ->
                TimetableCard(
                    schedule = schedule,
                    onClick = { onScheduleClick(schedule) }
                )
            }
        }
    }
}

@Composable
fun CampusTabs(
    selectedCampus: String,
    onCampusSelected: (String) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth()
    ) {
        listOf("교내", "교외").forEach { item ->
            Column(
                modifier = Modifier
                    .weight(1f)
                    .clickable { onCampusSelected(item) },
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = item,
                    fontSize = 14.sp,
                    color = if (selectedCampus == item) Color(0xFF102A56) else Color.Gray,
                    fontWeight = if (selectedCampus == item) FontWeight.Bold else FontWeight.Normal
                )

                Spacer(modifier = Modifier.height(8.dp))

                Box(
                    modifier = Modifier
                        .height(2.dp)
                        .fillMaxWidth()
                        .background(
                            if (selectedCampus == item) Color(0xFF102A56) else Color.Transparent
                        )
                )
            }
        }
    }
}

@Composable
fun RouteSelector(
    selectedRoute: String,
    onRouteSelected: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    val routeList = listOf(
        "경기광주역 → 외대(글)",
        "외대(글) → 경기광주역",
        "모현 → 외대(글)"
    )

    Box {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(10.dp))
                .background(Color.White)
                .clickable { expanded = true }
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = selectedRoute,
                fontSize = 13.sp,
                color = Color(0xFF333333),
                modifier = Modifier.weight(1f)
            )

            Text(
                text = "⌄",
                fontSize = 16.sp,
                color = Color.Gray
            )
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            routeList.forEach { route ->
                DropdownMenuItem(
                    text = { Text(route) },
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
fun TimeChipRow(
    selectedHour: String,
    onHourSelected: (String) -> Unit
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        listOf("08시", "09시", "10시", "11시", "12시").forEach { hour ->
            val selected = selectedHour == hour

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(if (selected) Color(0xFF102A56) else Color.White)
                    .clickable { onHourSelected(hour) }
                    .padding(horizontal = 14.dp, vertical = 8.dp)
            ) {
                Text(
                    text = hour,
                    fontSize = 12.sp,
                    color = if (selected) Color.White else Color.Gray,
                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
                )
            }
        }
    }
}

@Composable
fun TimetableCard(
    schedule: BusSchedule,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.White
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.Bottom
            ) {
                Text(
                    text = schedule.departureTime,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF111111)
                )

                Spacer(modifier = Modifier.width(6.dp))

                Text(
                    text = "(${schedule.remainingSeats}석)",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (schedule.remainingSeats <= 10) Color(0xFFE0462E) else Color(0xFF102A56)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "현재 위치  |  ${schedule.currentLocation}",
                fontSize = 12.sp,
                color = Color.Gray
            )
        }
    }
}

@Composable
fun RouteStatusScreen(
    schedule: BusSchedule,
    onBackClick: () -> Unit
) {
    val stops = listOf("경기광주역", "도서관", "본관", "인문관", "외대 정문")
    val currentStopIndex = 2

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
            .padding(horizontal = 20.dp)
            .padding(top = 20.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "‹",
                fontSize = 32.sp,
                modifier = Modifier.clickable { onBackClick() }
            )

            Spacer(modifier = Modifier.width(12.dp))

            Text(
                text = schedule.routeName,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(modifier = Modifier.height(40.dp))

        Text(
            text = "운행 중",
            color = Color(0xFFE0462E),
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.align(Alignment.CenterHorizontally)
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "${schedule.remainingSeats}/${schedule.totalSeats}",
            color = Color(0xFFE0462E),
            fontSize = 32.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.align(Alignment.CenterHorizontally)
        )

        Spacer(modifier = Modifier.height(28.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .background(Color(0xFFF1F3F6))
                .padding(horizontal = 16.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("현재 위치 ${stops[currentStopIndex]}", fontSize = 12.sp)
            Text("도착 예정 08:32", fontSize = 12.sp)
        }

        Spacer(modifier = Modifier.height(34.dp))

        RouteProgressBar(
            stops = stops,
            currentStopIndex = currentStopIndex
        )

        Spacer(modifier = Modifier.height(36.dp))

        Button(
            onClick = { },
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFF102A56)
            ),
            shape = RoundedCornerShape(10.dp)
        ) {
            Text("경로 확인하기")
        }
    }
}

@Composable
fun RouteProgressBar(
    stops: List<String>,
    currentStopIndex: Int
) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            stops.forEachIndexed { index, _ ->
                Box(
                    modifier = Modifier
                        .size(if (index == currentStopIndex) 18.dp else 14.dp)
                        .clip(CircleShape)
                        .background(
                            if (index <= currentStopIndex) Color(0xFF102A56)
                            else Color.White
                        )
                )

                if (index != stops.lastIndex) {
                    Box(
                        modifier = Modifier
                            .height(3.dp)
                            .weight(1f)
                            .background(
                                if (index < currentStopIndex) Color(0xFF102A56)
                                else Color(0xFFD8DEE8)
                            )
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            stops.forEach { stop ->
                Text(
                    text = stop,
                    fontSize = 10.sp,
                    color = Color(0xFF333333)
                )
            }
        }
    }
}

@Composable
fun PassengerBottomBar(
    selectedTab: BottomTab,
    onTabClick: (BottomTab) -> Unit
) {
    NavigationBar(
        containerColor = Color(0xFF102A56)
    ) {
        NavigationBarItem(
            selected = selectedTab == BottomTab.TIMETABLE,
            onClick = { onTabClick(BottomTab.TIMETABLE) },
            icon = { Text("▦", color = Color.White) },
            label = { Text("시간표", color = Color.White, fontSize = 11.sp) }
        )

        NavigationBarItem(
            selected = selectedTab == BottomTab.FAVORITE,
            onClick = { onTabClick(BottomTab.FAVORITE) },
            icon = { Text("★", color = Color.White) },
            label = { Text("즐겨찾기", color = Color.White, fontSize = 11.sp) }
        )

        NavigationBarItem(
            selected = selectedTab == BottomTab.MYPAGE,
            onClick = { onTabClick(BottomTab.MYPAGE) },
            icon = { Text("●", color = Color.White) },
            label = { Text("마이페이지", color = Color.White, fontSize = 11.sp) }
        )
    }
}

@Composable
fun SimplePlaceholderScreen(
    title: String,
    message: String
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF6F7F9))
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = title,
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF102A56)
        )

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = message,
            fontSize = 14.sp,
            color = Color.Gray
        )
    }
}