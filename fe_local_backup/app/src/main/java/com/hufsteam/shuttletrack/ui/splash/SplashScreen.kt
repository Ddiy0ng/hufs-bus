package com.hufsteam.shuttletrack.ui.splash

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hufsteam.shuttletrack.ui.auth.AuthViewModel

/**
 * 앱 시작 화면
 * - 이미 로그인된 세션이 있으면 바로 역할별 메인으로 이동
 * - 로그인 세션이 없으면 로그인 화면으로 이동
 */
@Composable
fun SplashScreen(
    viewModel: AuthViewModel,
    onNavigate: (loggedIn: Boolean) -> Unit
) {
    // 화면이 표시될 때 한 번만 실행
    LaunchedEffect(Unit) {
        viewModel.checkCurrentUser { loggedIn ->
            onNavigate(loggedIn)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.primary),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text       = "외대 셔틀",
                color      = Color.White,
                fontSize   = 32.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text     = "실시간 위치 안내",
                color    = Color.White.copy(alpha = 0.8f),
                fontSize = 16.sp
            )
            Spacer(modifier = Modifier.height(48.dp))
            CircularProgressIndicator(
                color    = Color.White,
                modifier = Modifier.size(32.dp)
            )
        }
    }
}
