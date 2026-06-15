package com.hufsteam.shuttletrack.ui.driver

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import com.hufsteam.shuttletrack.ui.theme.DriverBadge
import com.hufsteam.shuttletrack.ui.theme.DriverBadgeText
import com.hufsteam.shuttletrack.ui.theme.NavyBlue

@Composable
fun DriverMainScreen(
    viewModel: AuthViewModel,
    driverViewModel: DriverViewModel,
    onLogout: () -> Unit,
    onGoTimetable: () -> Unit = {},
    onGoOperation: (DriverRoute) -> Unit = {}
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .background(Color.White)
    ) {
        // ── 프로필 헤더 (연한 회색 배경 밴드) ─────────────────
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
                            .background(DriverBadge)
                            .padding(horizontal = 10.dp, vertical = 3.dp)
                    ) {
                        Text("버스기사", color = DriverBadgeText, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                    }
                    Spacer(Modifier.height(4.dp))
                    Text(viewModel.currentEmail, color = Color(0xFF555555), fontSize = 14.sp)
                }
            }
        }
        HorizontalDivider(color = DividerColor)

        // ── 스크롤 콘텐츠 ──────────────────────────────────────
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
        ) {
            Spacer(Modifier.height(20.dp))

            // 오늘 운행 일정
            Text("오늘 운행 일정", fontSize = 15.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(12.dp))

            mockDriverRoutes.forEach { route ->
                DriverRouteCard(
                    route   = route,
                    onClick = {
                        driverViewModel.selectRoute(route)
                        onGoOperation(route)
                    }
                )
                Spacer(Modifier.height(10.dp))
            }

            Spacer(Modifier.height(16.dp))
            HorizontalDivider(color = DividerColor)
            Spacer(Modifier.height(20.dp))

            DriverSectionHeader("서비스 안내")
            DriverMenuRow("개인정보 처리 방침") {}
            DriverMenuRow("서비스 이용 약관") {}
            Spacer(Modifier.height(24.dp))

            DriverSectionHeader("계정 설정")
            DriverMenuRow("로그아웃", onClick = onLogout)
            DriverMenuRow("탈퇴하기") {}
            Spacer(Modifier.height(16.dp))
        }

        BottomNavBar(
            selected      = BottomTab.MyPage,
            onTabSelected = { tab -> if (tab == BottomTab.Timetable) onGoTimetable() }
        )
    }
}

// ── 운행 노선 카드 ─────────────────────────────────────────────

@Composable
private fun DriverRouteCard(route: DriverRoute, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .border(1.dp, Color(0xFFE0E0E0), RoundedCornerShape(10.dp))
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(route.routeName, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF1A1A1A))
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
private fun DriverSectionHeader(title: String) {
    Text(title, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Color.Black)
    Spacer(Modifier.height(8.dp))
}

@Composable
private fun DriverMenuRow(label: String, onClick: () -> Unit = {}) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp)
    ) {
        Text(label, fontSize = 15.sp, color = Color(0xFF333333))
    }
}

// ── 하단 탭바 ─────────────────────────────────────────────────

enum class BottomTab { Timetable, Favorites, MyPage }

@Composable
fun BottomNavBar(
    selected: BottomTab,
    onTabSelected: (BottomTab) -> Unit = {}
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(NavyBlue)
            .padding(vertical = 8.dp)
    ) {
        BottomNavItem(Icons.Filled.Schedule, "시간표",   selected == BottomTab.Timetable, { onTabSelected(BottomTab.Timetable) }, Modifier.weight(1f))
        BottomNavItem(Icons.Filled.Star,     "즐겨찾기", selected == BottomTab.Favorites, { onTabSelected(BottomTab.Favorites) }, Modifier.weight(1f))
        BottomNavItem(Icons.Filled.Person,   "마이페이지", selected == BottomTab.MyPage,  { onTabSelected(BottomTab.MyPage)    }, Modifier.weight(1f))
    }
}

@Composable
private fun BottomNavItem(
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
