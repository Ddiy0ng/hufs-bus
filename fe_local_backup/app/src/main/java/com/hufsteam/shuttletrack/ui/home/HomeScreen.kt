package com.hufsteam.shuttletrack.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.hufsteam.shuttletrack.ui.common.BusIcon
import com.hufsteam.shuttletrack.ui.common.ShuttleButton

@Composable
fun HomeScreen(
    onGoSignUp: () -> Unit,
    onGoLogin:  () -> Unit
) {
    Column(
        modifier            = Modifier
            .fillMaxSize()
            .background(Color.White)
            .padding(horizontal = 32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        BusIcon()
        Spacer(Modifier.height(120.dp))
        ShuttleButton(text = "회원가입", enabled = true, onClick = onGoSignUp)
        Spacer(Modifier.height(16.dp))
        ShuttleButton(text = "로그인",   enabled = true, onClick = onGoLogin)
    }
}
