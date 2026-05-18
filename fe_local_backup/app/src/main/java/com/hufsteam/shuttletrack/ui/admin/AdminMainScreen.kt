package com.hufsteam.shuttletrack.ui.admin

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hufsteam.shuttletrack.ui.auth.AuthViewModel
import com.hufsteam.shuttletrack.ui.common.BusIcon
import com.hufsteam.shuttletrack.ui.driver.BottomNavBar
import com.hufsteam.shuttletrack.ui.driver.BottomTab
import com.hufsteam.shuttletrack.ui.theme.AdminBadge
import com.hufsteam.shuttletrack.ui.theme.AdminBadgeText
import com.hufsteam.shuttletrack.ui.theme.DividerColor
import com.hufsteam.shuttletrack.ui.theme.NavyBlue
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun AdminMainScreen(
    viewModel: AuthViewModel,
    onLogout: () -> Unit,
    onGoTimetable: () -> Unit = {}
) {
    var hasFile      by remember { mutableStateOf(false) }
    var isUploaded   by remember { mutableStateOf(false) }
    var showSnackbar by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(showSnackbar) {
        if (showSnackbar) { delay(3000); showSnackbar = false }
    }

    Box(modifier = Modifier.fillMaxSize().statusBarsPadding()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.White)
        ) {
            // ── 프로필 헤더 (연한 회색 배경 밴드) ─────────────
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
                                .background(AdminBadge)
                                .padding(horizontal = 10.dp, vertical = 3.dp)
                        ) {
                            Text("관리자", color = AdminBadgeText, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                        }
                        Spacer(Modifier.height(4.dp))
                        Text(viewModel.currentEmail, color = Color(0xFF555555), fontSize = 14.sp)
                    }
                }
            }
            HorizontalDivider(color = DividerColor)

            // ── 스크롤 콘텐츠 ──────────────────────────────────
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp)
            ) {
                Spacer(Modifier.height(20.dp))

                // 버스 시간표 등록
                Text("버스 시간표 등록", fontSize = 15.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(10.dp))

                if (!hasFile) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(110.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color(0xFFF5F5F5))
                            .border(1.dp, Color(0xFFDDDDDD), RoundedCornerShape(10.dp))
                            .clickable { hasFile = true; isUploaded = false },
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Filled.Add, null, tint = Color(0xFFAAAAAA), modifier = Modifier.size(28.dp))
                            Spacer(Modifier.height(6.dp))
                            Text(
                                "파일은 최대 10MB 이하까지만 첨부할 수 있어요",
                                color = Color(0xFFAAAAAA), fontSize = 12.sp, textAlign = TextAlign.Center
                            )
                        }
                    }
                } else {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .border(1.dp, Color(0xFFDDDDDD), RoundedCornerShape(10.dp))
                            .padding(horizontal = 14.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("파일.xlsx", fontSize = 14.sp, color = Color(0xFF333333), modifier = Modifier.weight(1f))
                        Text(" (10mb)", fontSize = 14.sp, color = Color(0xFF999999))
                        if (!isUploaded) {
                            Spacer(Modifier.width(8.dp))
                            Icon(
                                Icons.Filled.Close, contentDescription = "파일 제거",
                                tint = Color(0xFF999999),
                                modifier = Modifier.size(20.dp).clickable { hasFile = false; isUploaded = false }
                            )
                        }
                    }
                }

                Spacer(Modifier.height(8.dp))
                Row(modifier = Modifier.fillMaxWidth()) {
                    Spacer(Modifier.weight(1f))
                    Button(
                        onClick = {
                            if (!isUploaded) { isUploaded = true; showSnackbar = true }
                            else { hasFile = false; isUploaded = false }
                        },
                        enabled = hasFile,
                        shape   = RoundedCornerShape(8.dp),
                        colors  = ButtonDefaults.buttonColors(
                            containerColor         = NavyBlue,
                            disabledContainerColor = Color(0xFFD9D9D9)
                        )
                    ) {
                        Text(if (isUploaded) "수정" else "등록", color = Color.White)
                    }
                }

                Spacer(Modifier.height(16.dp))
                HorizontalDivider(color = DividerColor)
                Spacer(Modifier.height(20.dp))

                AdminSectionHeader("서비스 안내")
                AdminMenuRow("개인정보 처리 방침") {}
                AdminMenuRow("서비스 이용 약관") {}
                Spacer(Modifier.height(24.dp))

                AdminSectionHeader("계정 설정")
                AdminMenuRow("로그아웃", onClick = onLogout)
                AdminMenuRow("탈퇴하기") {}
                Spacer(Modifier.height(16.dp))
            }

            BottomNavBar(
                selected      = BottomTab.MyPage,
                onTabSelected = { tab -> if (tab == BottomTab.Timetable) onGoTimetable() }
            )
        }

        // 하단 스낵바
        if (showSnackbar) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 72.dp, start = 16.dp, end = 16.dp)
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0xFF1A1A2E))
                    .padding(horizontal = 16.dp, vertical = 14.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Filled.Info, null, tint = Color.White, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("버스 시간표가 저장되었습니다", color = Color.White, fontSize = 14.sp, modifier = Modifier.weight(1f))
                    Icon(Icons.Filled.Close, "닫기", tint = Color.White,
                        modifier = Modifier.size(18.dp).clickable { showSnackbar = false })
                }
            }
        }
    }
}

@Composable
private fun AdminSectionHeader(title: String) {
    Text(title, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Color.Black)
    Spacer(Modifier.height(8.dp))
}

@Composable
private fun AdminMenuRow(label: String, onClick: () -> Unit = {}) {
    Box(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick).padding(vertical = 12.dp)
    ) {
        Text(label, fontSize = 15.sp, color = Color(0xFF333333))
    }
}
