package com.hufsteam.shuttletrack.ui.auth

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.hufsteam.shuttletrack.data.model.User
import com.hufsteam.shuttletrack.data.model.UserRole
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class AuthViewModel : ViewModel() {

    private val auth = Firebase.auth
    private val db   = Firebase.firestore

    var userRole        by mutableStateOf<UserRole?>(null);   private set
    var isLoading       by mutableStateOf(false);              private set
    var errorMessage    by mutableStateOf<String?>(null);      private set
    var emailError      by mutableStateOf<String?>(null);      private set
    var passwordError   by mutableStateOf<String?>(null);      private set
    var signupSuccess   by mutableStateOf(false);              private set
    private var devLoginEmail by mutableStateOf<String?>(null)

    val currentEmail: String get() = devLoginEmail ?: auth.currentUser?.email ?: ""

    // ── 앱 시작: 자동 로그인 체크 ──────────────────────────────
    fun checkCurrentUser(onResult: (Boolean) -> Unit) {
        val currentUser = auth.currentUser
        if (currentUser == null) { onResult(false); return }
        fetchUserRole(currentUser.uid) { onResult(true) }
    }

    // ── 로그인 ─────────────────────────────────────────────────
    fun login(email: String, password: String, onSuccess: () -> Unit) {
        clearErrors()
        if (email.isBlank() || password.isBlank()) {
            errorMessage = "이메일과 비밀번호를 입력해 주세요."
            return
        }
        // @가 없으면 @hufs.ac.kr 자동 추가 (기사/관리자 단축 ID 지원)
        val resolvedEmail = if (email.contains("@")) email.trim() else "${email.trim()}@hufs.ac.kr"

        // 개발 테스트용 관리자 계정: 백엔드 role 연동 전 UI 확인용
        if (resolvedEmail.equals("admin@hufs.ac.kr", ignoreCase = true) && password == "admin1234") {
            devLoginEmail = "admin@hufs.ac.kr"
            userRole = UserRole.ADMIN
            isLoading = false
            onSuccess()
            return
        }

        isLoading = true

        // 10초 타임아웃
        viewModelScope.launch {
            delay(10_000)
            if (isLoading) {
                isLoading = false
                errorMessage = "서버 응답이 없습니다. 네트워크를 확인해 주세요."
            }
        }

        auth.signInWithEmailAndPassword(resolvedEmail, password)
            .addOnSuccessListener { result ->
                fetchUserRole(result.user!!.uid) {
                    isLoading = false
                    onSuccess()
                }
            }
            .addOnFailureListener {
                isLoading = false
                errorMessage = "이메일 또는 비밀번호가 올바르지 않습니다."
            }
    }

    // ── 회원가입 ───────────────────────────────────────────────
    fun signUp(email: String, password: String, onSuccess: () -> Unit) {
        clearErrors()
        var hasError = false

        if (!email.trim().endsWith("@hufs.ac.kr")) {
            emailError = "hufs.ac.kr 이메일만 가입 가능합니다"
            hasError = true
        }
        val hasLetter = password.any { it.isLetter() }
        val hasDigit  = password.any { it.isDigit() }
        if (password.length < 6) {
            passwordError = "비밀번호는 6자 이상이어야 합니다"
            hasError = true
        } else if (!hasLetter || !hasDigit) {
            passwordError = "영문과 숫자를 반드시 포함하여야 합니다"
            hasError = true
        }
        if (hasError) return

        isLoading = true

        // 10초 타임아웃
        viewModelScope.launch {
            delay(10_000)
            if (isLoading) {
                isLoading = false
                errorMessage = "서버 응답이 없습니다. Firebase 설정을 확인해 주세요."
            }
        }

        auth.createUserWithEmailAndPassword(email.trim(), password)
            .addOnSuccessListener { result ->
                val uid  = result.user!!.uid
                val user = User(uid = uid, email = email.trim(), role = UserRole.STUDENT.name)
                db.collection("users").document(uid).set(user)
                    .addOnSuccessListener {
                        userRole      = UserRole.STUDENT
                        isLoading     = false
                        signupSuccess = true
                        onSuccess()
                    }
                    .addOnFailureListener { e ->
                        isLoading    = false
                        errorMessage = "계정은 생성됐지만 정보 저장에 실패했습니다: ${e.localizedMessage}"
                    }
            }
            .addOnFailureListener { e ->
                isLoading = false
                val msg = e.localizedMessage ?: ""
                when {
                    msg.contains("already") || msg.contains("already-in-use") ->
                        emailError = "이미 가입된 이메일입니다"
                    msg.contains("network") || msg.contains("NETWORK") ->
                        errorMessage = "네트워크 오류입니다. 인터넷 연결을 확인해 주세요."
                    msg.contains("CONFIGURATION_NOT_FOUND") || msg.contains("disabled") ->
                        errorMessage = "Firebase 이메일 로그인이 비활성화되어 있습니다. Firebase Console을 확인하세요."
                    else ->
                        errorMessage = "회원가입 실패: $msg"
                }
            }
    }

    // ── 로그아웃 ───────────────────────────────────────────────
    fun logout() {
        auth.signOut()
        devLoginEmail = null
        userRole      = null
        signupSuccess = false
        clearErrors()
    }

    fun dismissSignupSuccess() { signupSuccess = false }

    fun clearErrors() {
        errorMessage  = null
        emailError    = null
        passwordError = null
    }

    // ── Firestore 역할 조회 ────────────────────────────────────
    private fun fetchUserRole(uid: String, onDone: () -> Unit) {
        db.collection("users").document(uid).get()
            .addOnSuccessListener { doc ->
                val roleStr = doc.getString("role") ?: UserRole.STUDENT.name
                userRole = runCatching { UserRole.valueOf(roleStr) }.getOrDefault(UserRole.STUDENT)
                onDone()
            }
            .addOnFailureListener {
                userRole = UserRole.STUDENT
                onDone()
            }
    }
}
