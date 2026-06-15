package com.hufsteam.shuttletrack.ui.splash

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.hufsteam.shuttletrack.ui.auth.AuthViewModel
import com.hufsteam.shuttletrack.ui.common.BusIcon
import kotlinx.coroutines.delay

@Composable
fun SplashScreen(
    viewModel: AuthViewModel,
    onNavigate: (loggedIn: Boolean) -> Unit
) {
    var navigated by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        delay(1000)

        // Firebase 응답을 기다림
        viewModel.checkCurrentUser { loggedIn ->
            if (!navigated) {
                navigated = true
                onNavigate(loggedIn)
            }
        }

        // 5초 안에 응답 없으면 홈 화면으로 강제 이동
        delay(5000)
        if (!navigated) {
            navigated = true
            onNavigate(false)
        }
    }

    Box(
        modifier         = Modifier.fillMaxSize().background(Color.White),
        contentAlignment = Alignment.Center
    ) {
        BusIcon()
    }
}
