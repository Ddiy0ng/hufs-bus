package com.hufsteam.shuttletrack.ui.timetable

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hufsteam.shuttletrack.ui.driver.BottomNavBar
import com.hufsteam.shuttletrack.ui.driver.BottomTab
import com.hufsteam.shuttletrack.ui.driver.DriverRoute
import com.hufsteam.shuttletrack.ui.theme.NavyBlue

// ── 데이터 모델 ─────────────────────────────────────────────────
data class TimetableEntry(
    val time: String,
    val seats: Int,
    val location: String,
    val timetableId: Long? = null,
    val status: String = "WAITING"
)

data class RouteInfo(
    val label: String,
    val entries: Map<Int, List<TimetableEntry>>  // 시(hour) → 시간표 목록
)

// ── 목업 데이터 ─────────────────────────────────────────────────
private val campusRoutes = listOf(
    RouteInfo(
        label = "지석묘 → 인문경상관",
        entries = mapOf(
            8  to listOf(TimetableEntry("08:20", 8, "백현마을3단지"), TimetableEntry("08:30", 48, "위치입니다"), TimetableEntry("08:40", 44, "나름적당히긴위치이름입니다"), TimetableEntry("08:50", 42, "엄청엄청긴위치이름입니다초")),
            9  to listOf(TimetableEntry("09:20", 8, "백현마을3단지"), TimetableEntry("09:30", 48, "위치입니다"), TimetableEntry("09:40", 44, "나름적당히긴위치이름입니다"), TimetableEntry("09:50", 42, "엄청엄청긴위치이름입니다초")),
            10 to listOf(TimetableEntry("10:10", 20, "인문관 앞"), TimetableEntry("10:30", 35, "경상관 뒤")),
            11 to listOf(TimetableEntry("11:00", 15, "지석묘"), TimetableEntry("11:30", 50, "중앙도서관")),
            12 to listOf(TimetableEntry("12:10", 30, "학생회관"), TimetableEntry("12:40", 22, "공학관"))
        )
    )
)

private val offCampusRoutes = listOf(
    RouteInfo(
        label = "경기광주역 → 외대(글)",
        entries = mapOf(
            8  to listOf(TimetableEntry("08:20", 8, "백현마을3단지"), TimetableEntry("08:30", 48, "위치입니다"), TimetableEntry("08:40", 44, "나름적당히긴위치이름입니다"), TimetableEntry("08:50", 42, "엄청엄청긴위치이름입니다초")),
            9  to listOf(TimetableEntry("09:10", 12, "경기광주역"), TimetableEntry("09:40", 33, "모란역")),
            10 to listOf(TimetableEntry("10:00", 25, "경기광주역"), TimetableEntry("10:30", 18, "초월읍")),
            11 to listOf(TimetableEntry("11:20", 40, "경기광주역")),
            12 to listOf(TimetableEntry("12:00", 28, "경기광주역"))
        )
    ),
    RouteInfo(
        label = "판교역 → 외대(글)",
        entries = mapOf(
            8  to listOf(TimetableEntry("08:10", 45, "판교역"), TimetableEntry("08:50", 20, "판교역")),
            9  to listOf(TimetableEntry("09:00", 38, "판교역"), TimetableEntry("09:30", 15, "판교역")),
            10 to listOf(TimetableEntry("10:20", 50, "판교역")),
            11 to listOf(TimetableEntry("11:10", 42, "판교역")),
            12 to listOf(TimetableEntry("12:30", 10, "판교역"))
        )
    ),
    RouteInfo(
        label = "외대(글) → 판교역",
        entries = mapOf(
            8  to listOf(TimetableEntry("08:30", 30, "외대 정문")),
            9  to listOf(TimetableEntry("09:20", 25, "외대 정문"), TimetableEntry("09:50", 18, "외대 정문")),
            10 to listOf(TimetableEntry("10:10", 35, "외대 정문")),
            11 to listOf(TimetableEntry("11:40", 22, "외대 정문")),
            12 to listOf(TimetableEntry("12:20", 48, "외대 정문"))
        )
    )
)

private val hours = listOf(8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18)

// ── 화면 ────────────────────────────────────────────────────────
@Composable
fun TimetableScreen(
    role: String = "driver",
    onGoMyPage: () -> Unit,
    onGoFavorites: () -> Unit = {},
    onDriverScheduleClick: (DriverRoute) -> Unit = {}
) {
    var selectedTab     by remember { mutableStateOf(0) }  // 0=교내, 1=교외
    val routes          = if (selectedTab == 0) campusRoutes else offCampusRoutes
    var selectedRoute   by remember(selectedTab) { mutableStateOf(routes[0]) }
    var dropdownOpen    by remember { mutableStateOf(false) }
    var selectedHour    by remember { mutableStateOf(9) }

    val entries = selectedRoute.entries[selectedHour] ?: emptyList()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .background(Color.White)
    ) {
        // 교내 / 교외 탭
        TabRow(
            selectedTabIndex = selectedTab,
            containerColor   = Color.White,
            contentColor     = NavyBlue
        ) {
            listOf("교내", "교외").forEachIndexed { idx, label ->
                Tab(
                    selected = selectedTab == idx,
                    onClick  = {
                        selectedTab   = idx
                        selectedRoute = if (idx == 0) campusRoutes[0] else offCampusRoutes[0]
                        dropdownOpen  = false
                    },
                    text = {
                        Text(
                            label,
                            fontWeight = if (selectedTab == idx) FontWeight.Bold else FontWeight.Normal,
                            color = if (selectedTab == idx) NavyBlue else Color(0xFF999999)
                        )
                    }
                )
            }
        }

        Column(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 16.dp)
        ) {
            Spacer(Modifier.height(12.dp))

            // 노선 드롭다운
            Box {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .border(1.dp, Color(0xFFDDDDDD), RoundedCornerShape(8.dp))
                        .clickable { dropdownOpen = !dropdownOpen }
                        .padding(horizontal = 14.dp, vertical = 12.dp)
                ) {
                    Row(
                        modifier            = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment   = Alignment.CenterVertically
                    ) {
                        Text(selectedRoute.label, fontSize = 14.sp, color = Color(0xFF333333))
                        Icon(
                            if (dropdownOpen) Icons.Filled.KeyboardArrowUp else Icons.Filled.KeyboardArrowDown,
                            contentDescription = null,
                            tint = Color(0xFF888888)
                        )
                    }
                }

                DropdownMenu(
                    expanded         = dropdownOpen,
                    onDismissRequest = { dropdownOpen = false },
                    modifier         = Modifier
                        .fillMaxWidth()
                        .background(Color.White)
                ) {
                    routes.forEach { route ->
                        DropdownMenuItem(
                            text = {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(route.label, fontSize = 14.sp)
                                    if (route == selectedRoute) {
                                        Text("✓", color = NavyBlue, fontWeight = FontWeight.Bold)
                                    }
                                }
                            },
                            onClick = {
                                selectedRoute = route
                                dropdownOpen  = false
                            }
                        )
                    }
                }
            }

            Spacer(Modifier.height(12.dp))

            // 시간대 칩 (가로 스크롤)
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(hours) { hour ->
                    val isSelected = hour == selectedHour
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(if (isSelected) NavyBlue else Color.Transparent)
                            .border(
                                width = if (isSelected) 0.dp else 1.dp,
                                color = if (isSelected) Color.Transparent else Color(0xFFDDDDDD),
                                shape = RoundedCornerShape(20.dp)
                            )
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
            if (entries.isEmpty()) {
                Box(
                    modifier         = Modifier.fillMaxWidth().padding(top = 40.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("해당 시간대 운행 정보가 없습니다.", color = Color(0xFF999999), fontSize = 14.sp)
                }
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    items(entries) { entry ->
                        TimetableCard(
                            entry = entry,
                            enabled = role == "driver",
                            onClick = { onDriverScheduleClick(selectedRoute.toDriverRoute(entry)) }
                        )
                    }
                    item { Spacer(Modifier.height(8.dp)) }
                }
            }
        }

        // 하단 탭바
        BottomNavBar(
            selected       = BottomTab.Timetable,
            onTabSelected  = { tab ->
                when (tab) {
                    BottomTab.MyPage    -> onGoMyPage()
                    BottomTab.Favorites -> onGoFavorites()
                    else                -> {}
                }
            }
        )
    }
}

@Composable
private fun TimetableCard(
    entry: TimetableEntry,
    enabled: Boolean,
    onClick: () -> Unit
) {
    val status = entry.status.trim().uppercase()
    val statusLabel = when (status) {
        "RUNNING" -> "(${entry.seats}석)"
        "DONE" -> "(운행 종료)"
        else -> "(운행 전)"
    }
    val statusColor = when (status) {
        "RUNNING" -> Color(0xFFE53935)
        else -> Color(0xFF9AA0A6)
    }
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(Color.White)
            .border(1.dp, Color(0xFFEEEEEE), RoundedCornerShape(10.dp))
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp)
    ) {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    entry.time,
                    fontSize   = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color      = Color.Black
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    statusLabel,
                    fontSize   = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color      = statusColor
                )
            }
            Spacer(Modifier.height(4.dp))
            Text(
                "현재 위치 | ${entry.location}",
                fontSize = 13.sp,
                color    = Color(0xFF888888)
            )
        }
    }
}

private fun RouteInfo.toDriverRoute(entry: TimetableEntry): DriverRoute {
    return DriverRoute(
        id = "${label}_${entry.time}".filter { it.isLetterOrDigit() || it == '_' },
        routeName = label,
        scheduledTime = entry.time,
        totalSeats = 45,
        stops = stopsForRoute(label),
        timetableId = entry.timetableId,
        busId = null
    )
}

private fun stopsForRoute(label: String): List<String> {
    return when {
        label.contains("경기광주역") && label.contains("외대") ->
            listOf("경기광주역(기점)", "기숙사", "백년관", "인경관(종점)")
        label.contains("판교역") && label.startsWith("판교역") ->
            listOf("판교역(기점)", "성남역", "서현역", "외대-글(종점)")
        label.contains("판교역") ->
            listOf("외대-글(기점)", "서현역", "성남역", "판교역(종점)")
        label.contains("지석묘") ->
            listOf("지석묘(기점)", "기숙사", "도서관", "인경관(종점)")
        else ->
            listOf("기점", "경유지", "종점")
    }
}
