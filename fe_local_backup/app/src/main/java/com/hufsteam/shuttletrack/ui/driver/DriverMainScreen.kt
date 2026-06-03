package com.hufsteam.shuttletrack.ui.driver

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * 기사 메인 화면 (기본 구조)
 *
 * 추후 추가 기능:
 * - GPS 위치 전송 ON/OFF 토글 (백그라운드 서비스 연동)
 * - NFC 태그 감지 → 탑승/하차 인원 집계
 * - 현재 탑승 인원 표시
 * - 운행 시작/종료 상태 관리
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DriverMainScreen(onLogout: () -> Unit) {
    // 위치 전송 ON/OFF 상태 (추후 실제 서비스와 연결)
    var isTransmitting by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title  = { Text("기사 화면", color = Color.White) },
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
            modifier            = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("📍", fontSize = 48.sp)
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text       = if (isTransmitting) "위치 전송 중" else "위치 전송 대기",
                fontSize   = 20.sp,
                fontWeight = FontWeight.Bold,
                color      = if (isTransmitting) MaterialTheme.colorScheme.primary
                             else MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(32.dp))

            // 위치 전송 ON/OFF 버튼
            Button(
                onClick = { isTransmitting = !isTransmitting },
                colors  = ButtonDefaults.buttonColors(
                    containerColor = if (isTransmitting) Color(0xFFE53935) // 빨간색 (중지)
                                     else MaterialTheme.colorScheme.primary
                ),
                modifier = Modifier.size(width = 200.dp, height = 56.dp)
            ) {
                Text(
                    text     = if (isTransmitting) "운행 종료" else "운행 시작",
                    fontSize = 16.sp
                )
            }
            Spacer(modifier = Modifier.height(32.dp))

            // 탑승 인원 (추후 NFC 연동)
            Text(
                text  = "현재 탑승 인원",
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text       = "0 명",
                fontSize   = 36.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text  = "NFC 탑승 집계 기능이 여기에 연동됩니다",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 13.sp
            )
        }
    }
}
