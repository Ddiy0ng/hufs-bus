package com.hufsteam.shuttletrack.ui.auth

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * 로그인 화면
 *
 * [필드]
 * - 이메일
 * - 비밀번호
 *
 * [버튼]
 * - 로그인   → Firebase Auth 인증 후 역할별 화면으로 이동
 * - 회원가입 → SignUpScreen으로 이동
 */
@Composable
fun LoginScreen(
    viewModel: AuthViewModel,
    onLoginSuccess: () -> Unit,
    onGoSignUp: () -> Unit
) {
    var email    by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier            = Modifier
                .fillMaxSize()
                .padding(horizontal = 32.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // 앱 제목
            Text(
                text       = "외대 셔틀",
                fontSize   = 28.sp,
                fontWeight = FontWeight.Bold,
                color      = MaterialTheme.colorScheme.primary
            )
            Text(
                text     = "실시간 위치 안내 서비스",
                fontSize = 14.sp,
                color    = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(40.dp))

            // 이메일 입력
            OutlinedTextField(
                value         = email,
                onValueChange = { email = it },
                label         = { Text("이메일") },
                placeholder   = { Text("example@hufs.ac.kr") },
                singleLine    = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                modifier      = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(12.dp))

            // 비밀번호 입력
            OutlinedTextField(
                value                  = password,
                onValueChange          = { password = it },
                label                  = { Text("비밀번호") },
                singleLine             = true,
                visualTransformation   = PasswordVisualTransformation(),
                keyboardOptions        = KeyboardOptions(keyboardType = KeyboardType.Password),
                modifier               = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(24.dp))

            // 로그인 버튼
            Button(
                onClick  = { viewModel.login(email, password, onLoginSuccess) },
                enabled  = !viewModel.isLoading,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
            ) {
                if (viewModel.isLoading) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                } else {
                    Text("로그인", fontSize = 16.sp)
                }
            }
            Spacer(modifier = Modifier.height(8.dp))

            // 회원가입 이동
            TextButton(onClick = onGoSignUp) {
                Text("계정이 없으신가요? 회원가입")
            }

            // 오류 메시지 표시
            viewModel.errorMessage?.let { msg ->
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text  = msg,
                    color = MaterialTheme.colorScheme.error,
                    fontSize = 13.sp
                )
            }
        }
    }
}
