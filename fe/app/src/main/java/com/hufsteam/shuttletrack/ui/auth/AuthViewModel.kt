package com.hufsteam.shuttletrack.ui.auth

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hufsteam.shuttletrack.data.model.UserRole
import com.hufsteam.shuttletrack.data.remote.TokenStore
import com.hufsteam.shuttletrack.data.repository.AuthRepository
import kotlinx.coroutines.launch

class AuthViewModel : ViewModel() {

    private val repository = AuthRepository()

    var userRole        by mutableStateOf<UserRole?>(null);   private set
    var isLoading       by mutableStateOf(false);              private set
    var errorMessage    by mutableStateOf<String?>(null);      private set
    var emailError      by mutableStateOf<String?>(null);      private set
    var passwordError   by mutableStateOf<String?>(null);      private set
    var signupSuccess   by mutableStateOf(false);              private set
    private var loggedInEmail by mutableStateOf<String?>(null)

    val currentEmail: String get() = loggedInEmail.orEmpty()

    fun checkCurrentUser(onResult: (Boolean) -> Unit) {
        if (TokenStore.accessToken.isNullOrBlank()) {
            onResult(false)
            return
        }

        viewModelScope.launch {
            repository.getCurrentUser()
                .onSuccess { session ->
                    loggedInEmail = session.email
                    userRole = session.role
                    onResult(true)
                }
                .onFailure {
                    repository.logout()
                    loggedInEmail = null
                    userRole = null
                    onResult(false)
                }
        }
    }

    fun login(email: String, password: String, onSuccess: () -> Unit) {
        clearErrors()
        if (email.isBlank() || password.isBlank()) {
            errorMessage = "이메일과 비밀번호를 입력해 주세요."
            return
        }

        val resolvedEmail = if (email.contains("@")) email.trim() else "${email.trim()}@hufs.ac.kr"

        isLoading = true
        viewModelScope.launch {
            repository.login(resolvedEmail, password)
                .onSuccess { session ->
                    loggedInEmail = session.email
                    userRole = session.role
                    isLoading = false
                    onSuccess()
                }
                .onFailure { throwable ->
                    isLoading = false
                    errorMessage = throwable.message ?: "이메일 또는 비밀번호가 올바르지 않습니다."
                }
        }
    }

    fun signUp(
        email: String,
        password: String,
        privacyTermAgree: Boolean,
        serviceTermAgree: Boolean,
        onSuccess: () -> Unit
    ) {
        clearErrors()
        var hasError = false

        if (!email.trim().endsWith("@hufs.ac.kr")) {
            emailError = "hufs.ac.kr 이메일만 가입 가능합니다"
            hasError = true
        }
        val hasLetter = password.any { it.isLetter() }
        val hasDigit  = password.any { it.isDigit() }
        if (password.length < 8) {
            passwordError = "비밀번호는 8자 이상이어야 합니다"
            hasError = true
        } else if (!hasLetter || !hasDigit) {
            passwordError = "영문과 숫자를 반드시 포함하여야 합니다"
            hasError = true
        }
        if (!privacyTermAgree || !serviceTermAgree) {
            errorMessage = "필수 약관에 모두 동의해 주세요."
            hasError = true
        }
        if (hasError) return

        isLoading = true
        viewModelScope.launch {
            repository.signup(
                email = email.trim(),
                password = password,
                privacyTermAgree = privacyTermAgree,
                serviceTermAgree = serviceTermAgree
            )
                .onSuccess { session ->
                    loggedInEmail = session.email
                    userRole = session.role
                    isLoading = false
                    signupSuccess = true
                    onSuccess()
                }
                .onFailure { throwable ->
                    isLoading = false
                    val msg = throwable.message.orEmpty()
                    when {
                        msg.contains("already", ignoreCase = true) ->
                            emailError = "이미 가입된 이메일입니다"
                        msg.contains("network", ignoreCase = true) ->
                            errorMessage = "네트워크 오류입니다. 인터넷 연결을 확인해 주세요."
                        else -> errorMessage = "회원가입 실패: ${msg.ifBlank { "서버 응답을 확인해 주세요." }}"
                    }
                }
        }
    }

    fun logout() {
        repository.logout()
        loggedInEmail = null
        userRole      = null
        signupSuccess = false
        clearErrors()
    }

    fun withdraw(onSuccess: () -> Unit) {
        clearErrors()
        isLoading = true
        viewModelScope.launch {
            repository.withdraw()
                .onSuccess {
                    loggedInEmail = null
                    userRole = null
                    signupSuccess = false
                    isLoading = false
                    onSuccess()
                }
                .onFailure { throwable ->
                    isLoading = false
                    errorMessage = throwable.message ?: "회원 탈퇴에 실패했습니다."
                }
        }
    }

    fun dismissSignupSuccess() { signupSuccess = false }

    fun clearErrors() {
        errorMessage  = null
        emailError    = null
        passwordError = null
    }
}
