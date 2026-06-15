package com.hufsteam.shuttletrack.ui.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hufsteam.shuttletrack.ui.common.BusIcon
import com.hufsteam.shuttletrack.ui.common.LanguageButton
import com.hufsteam.shuttletrack.ui.common.ShuttleButton
import com.hufsteam.shuttletrack.ui.theme.NavyBlue
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton

@Composable
fun LoginScreen(
    viewModel: AuthViewModel,
    onLoginSuccess: () -> Unit,
    onGoSignUp: () -> Unit,
    onBack: () -> Unit
) {
    var email        by remember { mutableStateOf("") }
    var password     by remember { mutableStateOf("") }
    var pwVisible    by remember { mutableStateOf(false) }

    val isEnabled = email.isNotBlank() && password.isNotBlank()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
            .statusBarsPadding()
    ) {
        IconButton(
            onClick = onBack,
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(top = 8.dp, start = 8.dp)
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "뒤로가기",
                tint = Color.Black
            )
        }

        // 우측 상단 언어 버튼
        LanguageButton(modifier = Modifier
            .align(Alignment.TopEnd)
            .padding(top = 16.dp, end = 16.dp))

        Column(
            modifier            = Modifier
                .fillMaxSize()
                .padding(horizontal = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(80.dp))
            BusIcon()
            Spacer(Modifier.height(12.dp))
            Text("로그인", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color.Black)
            Spacer(Modifier.height(32.dp))

            // 이메일 입력
            OutlinedTextField(
                value           = email,
                onValueChange   = { email = it; viewModel.clearErrors() },
                placeholder     = { Text("이메일을 입력해 주세요", color = Color(0xFFBBBBBB)) },
                singleLine      = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                shape           = RoundedCornerShape(10.dp),
                colors          = fieldColors(),
                modifier        = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(10.dp))

            // 비밀번호 입력
            OutlinedTextField(
                value                = password,
                onValueChange        = { password = it; viewModel.clearErrors() },
                placeholder          = { Text("비밀번호를 입력해 주세요", color = Color(0xFFBBBBBB)) },
                singleLine           = true,
                visualTransformation = if (pwVisible) VisualTransformation.None else PasswordVisualTransformation(),
                keyboardOptions      = KeyboardOptions(keyboardType = KeyboardType.Password),
                trailingIcon         = {
                    IconButton(onClick = { pwVisible = !pwVisible }) {
                        Icon(
                            imageVector = if (pwVisible) Icons.Filled.Visibility else Icons.Filled.VisibilityOff,
                            contentDescription = if (pwVisible) "비밀번호 숨기기" else "비밀번호 보기",
                            tint = Color(0xFFBBBBBB)
                        )
                    }
                },
                shape   = RoundedCornerShape(10.dp),
                colors  = fieldColors(),
                modifier = Modifier.fillMaxWidth()
            )

            // 오류 메시지
            viewModel.errorMessage?.let {
                Spacer(Modifier.height(8.dp))
                Text(it, color = Color(0xFFE53935), fontSize = 13.sp)
            }

            Spacer(Modifier.weight(1f))

            // 로그인 버튼
            ShuttleButton(
                text    = "로그인",
                enabled = isEnabled,
                onClick = { viewModel.login(email, password, onLoginSuccess) }
            )
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
fun fieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor   = NavyBlue,
    unfocusedBorderColor = Color(0xFFDDDDDD),
    errorBorderColor     = Color(0xFFE53935),
    focusedContainerColor   = Color.White,
    unfocusedContainerColor = Color.White
)
