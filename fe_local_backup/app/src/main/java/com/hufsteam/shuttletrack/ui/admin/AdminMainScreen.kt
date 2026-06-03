package com.hufsteam.shuttletrack.ui.admin

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * 관리자 메인 화면 (기본 구조)
 *
 * 추후 추가 기능:
 * - 버스 노선 관리 (교내 / 서현 / 판교)
 * - 운행 중인 버스 현황 모니터링
 * - 기사 계정 관리
 * - 공지사항 / 운행 중단 알림 발송
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminMainScreen(onLogout: () -> Unit) {
    Scaffold(
        topBar = {
            TopAppBar(
                title  = { Text("관리자 화면", color = Color.White) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary
                ),
                actions = {
                    IconButton(onClick = onLogout) {
                        Icon(
                            imageVector        = Icons.Default.ExitToApp,
                            contentDescription = "로그아웃",
                            tint               = Color.White
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text       = "관리 메뉴",
                fontSize   = 18.sp,
                fontWeight = FontWeight.Bold
            )

            // 관리 메뉴 카드들
            AdminMenuCard(emoji = "🚌", title = "버스 노선 관리",  description = "교내·서현·판교 노선 설정")
            AdminMenuCard(emoji = "📍", title = "실시간 현황",     description = "운행 중인 버스 위치 모니터링")
            AdminMenuCard(emoji = "👤", title = "기사 계정 관리",  description = "기사 등록 및 권한 관리")
            AdminMenuCard(emoji = "🔔", title = "공지사항 발송",   description = "운행 변경·취소 알림 전송")
        }
    }
}

@Composable
private fun AdminMenuCard(emoji: String, title: String, description: String) {
    Card(
        modifier  = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier          = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(emoji, fontSize = 28.sp, modifier = Modifier.size(40.dp))
            Column(modifier = Modifier.padding(start = 12.dp)) {
                Text(text = title, fontWeight = FontWeight.SemiBold)
                Text(
                    text     = description,
                    fontSize = 13.sp,
                    color    = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
