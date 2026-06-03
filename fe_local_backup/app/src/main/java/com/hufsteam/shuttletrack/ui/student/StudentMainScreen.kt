package com.hufsteam.shuttletrack.ui.student

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExitToApp
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
 * 학생 메인 화면 (기본 구조)
 *
 * 추후 추가 기능:
 * - 실시간 셔틀 지도 표시 (Google Maps / Naver Maps)
 * - 정류장별 예상 도착 시간 목록
 * - 탑승 가능 여부 (혼잡도) 표시
 * - 출발 알림 설정
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StudentMainScreen(onLogout: () -> Unit) {
    Scaffold(
        topBar = {
            TopAppBar(
                title  = { Text("외대 셔틀", color = Color.White) },
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
        Box(
            modifier        = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("🚌", fontSize = 48.sp)
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text       = "실시간 셔틀 위치",
                    fontSize   = 20.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text  = "지도 및 도착 정보가 여기에 표시됩니다",
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
