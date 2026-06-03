package com.hufsteam.shuttletrack.ui.auth

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hufsteam.shuttletrack.data.model.UserRole

/**
 * 회원가입 화면
 *
 * [필드]
 * - 이름
 * - 이메일
 * - 비밀번호
 * - 역할 선택 (학생 / 기사 / 관리자)
 *
 * ※ 실제 서비스에서는 DRIVER / ADMIN 은 관리자가 직접 등록하거나
 *    별도 인증 코드를 사용하는 것이 보안상 좋습니다.
 */
@Composable
fun SignUpScreen(
    viewModel: AuthViewModel,
    onSignUpSuccess: () -> Unit,
    onGoLogin: () -> Unit
) {
    var name            by remember { mutableStateOf("") }
    var email           by remember { mutableStateOf("") }
    var password        by remember { mutableStateOf("") }
    var selectedRole    by remember { mutableStateOf(UserRole.STUDENT) }

    val roleOptions = listOf(
        UserRole.STUDENT to "학생",
        UserRole.DRIVER  to "버스 기사",
        UserRole.ADMIN   to "관리자"
    )

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 32.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(40.dp))

            Text(
                text       = "회원가입",
                fontSize   = 24.sp,
                fontWeight = FontWeight.Bold,
                color      = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(32.dp))

            // 이름 입력
            OutlinedTextField(
                value         = name,
                onValueChange = { name = it },
                label         = { Text("이름") },
                singleLine    = true,
                modifier      = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(12.dp))

            // 이메일 입력
            OutlinedTextField(
                value           = email,
                onValueChange   = { email = it },
                label           = { Text("이메일") },
                singleLine      = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                modifier        = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(12.dp))

            // 비밀번호 입력
            OutlinedTextField(
                value                = password,
                onValueChange        = { password = it },
                label                = { Text("비밀번호 (6자 이상)") },
                singleLine           = true,
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions      = KeyboardOptions(keyboardType = KeyboardType.Password),
                modifier             = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(20.dp))

            // 역할 선택 라디오 버튼
            Text(
                text     = "역할 선택",
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.align(Alignment.Start)
            )
            Spacer(modifier = Modifier.height(8.dp))
            roleOptions.forEach { (role, label) ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .selectable(
                            selected = selectedRole == role,
                            onClick  = { selectedRole = role },
                            role     = Role.RadioButton
                        )
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(
                        selected = selectedRole == role,
                        onClick  = null   // selectable Row가 처리
                    )
                    Text(
                        text     = label,
                        modifier = Modifier.padding(start = 8.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.height(24.dp))

            // 회원가입 버튼
            Button(
                onClick  = {
                    viewModel.signUp(email, password, name, selectedRole, onSignUpSuccess)
                },
                enabled  = !viewModel.isLoading,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
            ) {
                if (viewModel.isLoading) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                } else {
                    Text("회원가입", fontSize = 16.sp)
                }
            }
            Spacer(modifier = Modifier.height(8.dp))

            // 로그인으로 이동
            TextButton(onClick = onGoLogin) {
                Text("이미 계정이 있으신가요? 로그인")
            }

            // 오류 메시지
            viewModel.errorMessage?.let { msg ->
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text     = msg,
                    color    = MaterialTheme.colorScheme.error,
                    fontSize = 13.sp
                )
            }
            Spacer(modifier = Modifier.height(40.dp))
        }
    }
}
