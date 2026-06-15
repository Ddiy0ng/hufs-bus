package com.hufsteam.shuttletrack.ui.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.hufsteam.shuttletrack.ui.common.BusIcon
import com.hufsteam.shuttletrack.ui.common.LanguageButton
import com.hufsteam.shuttletrack.ui.common.ShuttleButton
import com.hufsteam.shuttletrack.ui.theme.CheckNavy
import com.hufsteam.shuttletrack.ui.theme.DividerColor
import com.hufsteam.shuttletrack.ui.theme.ErrorRed
import com.hufsteam.shuttletrack.ui.theme.NavyBlue
import com.hufsteam.shuttletrack.ui.theme.OverlayGray
import com.hufsteam.shuttletrack.ui.theme.TermsRed

@Composable
fun SignUpScreen(
    viewModel: AuthViewModel,
    onGoLogin: () -> Unit,
    onGoServiceTerms: () -> Unit,
    onGoPrivacyTerms: () -> Unit
) {
    var email        by remember { mutableStateOf("") }
    var password     by remember { mutableStateOf("") }
    var pwVisible    by remember { mutableStateOf(false) }
    var agreeService by remember { mutableStateOf(false) }
    var agreePrivacy by remember { mutableStateOf(false) }
    val agreeAll     = agreeService && agreePrivacy

    val isEnabled = email.isNotBlank() && password.isNotBlank() && agreeAll

    // 회원가입 성공 모달
    if (viewModel.signupSuccess) {
        SignupSuccessDialog(onConfirm = {
            viewModel.dismissSignupSuccess()
            onGoLogin()
        })
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
            .statusBarsPadding()
    ) {
// 우측 상단 언어 버튼
        LanguageButton(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(top = 16.dp, end = 16.dp)
        )

        Column(
            modifier            = Modifier
                .fillMaxSize()
                .padding(horizontal = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(80.dp))

            // ── 버스 아이콘 + 타이틀 ───────────────────────────
            BusIcon()
            Spacer(Modifier.height(12.dp))
            Text("회원가입", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color.Black)
            Spacer(Modifier.height(32.dp))

            // ── 이메일 입력 ────────────────────────────────────
            OutlinedTextField(
                value           = email,
                onValueChange   = { email = it; viewModel.clearErrors() },
                placeholder     = { Text("이메일을 입력해 주세요", color = Color(0xFFBBBBBB)) },
                singleLine      = true,
                isError         = viewModel.emailError != null,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                shape           = RoundedCornerShape(10.dp),
                colors          = fieldColors(),
                modifier        = Modifier.fillMaxWidth()
            )
            viewModel.emailError?.let {
                Text(
                    it, color = ErrorRed, fontSize = 12.sp,
                    modifier = Modifier
                        .align(Alignment.Start)
                        .padding(start = 4.dp, top = 2.dp)
                )
            }
            Spacer(Modifier.height(10.dp))

            // ── 비밀번호 입력 ──────────────────────────────────
            OutlinedTextField(
                value                = password,
                onValueChange        = { password = it; viewModel.clearErrors() },
                placeholder          = { Text("비밀번호를 입력해 주세요", color = Color(0xFFBBBBBB)) },
                singleLine           = true,
                isError              = viewModel.passwordError != null,
                visualTransformation = if (pwVisible) VisualTransformation.None else PasswordVisualTransformation(),
                keyboardOptions      = KeyboardOptions(keyboardType = KeyboardType.Password),
                trailingIcon         = {
                    IconButton(onClick = { pwVisible = !pwVisible }) {
                        Icon(
                            if (pwVisible) Icons.Filled.Visibility else Icons.Filled.VisibilityOff,
                            contentDescription = null,
                            tint = Color(0xFFBBBBBB)
                        )
                    }
                },
                shape    = RoundedCornerShape(10.dp),
                colors   = fieldColors(),
                modifier = Modifier.fillMaxWidth()
            )
            viewModel.passwordError?.let {
                Text(
                    it, color = ErrorRed, fontSize = 12.sp,
                    modifier = Modifier
                        .align(Alignment.Start)
                        .padding(start = 4.dp, top = 2.dp)
                )
            }

            // ── 여백 → 약관을 화면 하단으로 밀어냄 ────────────
            Spacer(Modifier.weight(1f))

            // ── 전체 동의 ──────────────────────────────────────
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        val next = !agreeAll
                        agreeService = next
                        agreePrivacy = next
                    },
                verticalAlignment = Alignment.CenterVertically
            ) {
                CircleCheckbox(checked = agreeAll)
                Text("  전체 동의", fontSize = 15.sp, fontWeight = FontWeight.Medium)
            }
            HorizontalDivider(color = DividerColor, modifier = Modifier.padding(vertical = 8.dp))

            // 서비스 이용 약관
            TermsRow(
                checked = agreeService,
                label   = "서비스 이용 약관 동의",
                onCheck = { agreeService = !agreeService },
                onArrow = onGoServiceTerms
            )
            Spacer(Modifier.height(4.dp))

            // 개인정보 수집 및 이용 동의
            TermsRow(
                checked = agreePrivacy,
                label   = "개인정보 수집 및 이용 동의",
                onCheck = { agreePrivacy = !agreePrivacy },
                onArrow = onGoPrivacyTerms
            )

            Spacer(Modifier.height(16.dp))

            // ── 일반 오류 메시지 ───────────────────────────────
            viewModel.errorMessage?.let {
                Text(
                    it, color = ErrorRed, fontSize = 13.sp,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
            }

            // ── 회원가입 버튼 / 로딩 ──────────────────────────
            if (viewModel.isLoading) {
                CircularProgressIndicator(color = NavyBlue)
            } else {
                ShuttleButton(
                    text    = "회원가입",
                    enabled = isEnabled,
                    onClick = {
                        viewModel.signUp(
                            email = email,
                            password = password,
                            privacyTermAgree = agreePrivacy,
                            serviceTermAgree = agreeService
                        ) { }
                    }
                )
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}

// ── 원형 체크박스 ────────────────────────────────────────────

@Composable
private fun CircleCheckbox(checked: Boolean) {
    Box(
        modifier = Modifier
            .size(22.dp)
            .clip(CircleShape)
            .background(if (checked) CheckNavy else Color.Transparent)
            .border(1.5.dp, if (checked) CheckNavy else Color(0xFFCCCCCC), CircleShape),
        contentAlignment = Alignment.Center
    ) {
        if (checked) Icon(Icons.Filled.Check, null, tint = Color.White, modifier = Modifier.size(14.dp))
    }
}

// ── 약관 행 ──────────────────────────────────────────────────

@Composable
private fun TermsRow(
    checked: Boolean,
    label: String,
    onCheck: () -> Unit,
    onArrow: () -> Unit
) {
    Row(
        modifier          = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(modifier = Modifier.clickable(onClick = onCheck)) {
            CircleCheckbox(checked = checked)
        }
        Text(
            text     = "  $label ",
            fontSize = 14.sp,
            modifier = Modifier.weight(1f)
        )
        Text("(필수)", color = TermsRed, fontSize = 13.sp)
        IconButton(onClick = onArrow, modifier = Modifier.size(32.dp)) {
            Icon(Icons.Filled.ChevronRight, null, tint = Color(0xFF999999))
        }
    }
}

// ── 회원가입 성공 다이얼로그 ─────────────────────────────────

@Composable
private fun SignupSuccessDialog(onConfirm: () -> Unit) {
    Dialog(onDismissRequest = {}) {
        Box(
            modifier         = Modifier
                .fillMaxSize()
                .background(OverlayGray),
            contentAlignment = Alignment.Center
        ) {
            Column(
                modifier            = Modifier
                    .padding(32.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color.White)
                    .padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .clip(CircleShape)
                        .border(2.dp, NavyBlue, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Filled.Check, null, tint = NavyBlue, modifier = Modifier.size(36.dp))
                }
                Spacer(Modifier.height(16.dp))
                Text("회원가입 성공", fontSize = 17.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(20.dp))
                ShuttleButton(text = "확인", enabled = true, onClick = onConfirm)
            }
        }
    }
}
