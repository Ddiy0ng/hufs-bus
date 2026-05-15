package com.hufsteam.shuttletrack.ui.auth

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.hufsteam.shuttletrack.data.model.User
import com.hufsteam.shuttletrack.data.model.UserRole

/**
 * 로그인 / 회원가입 / 역할 조회를 담당하는 ViewModel
 *
 * 상태(State):
 *   userRole     - 현재 로그인된 유저의 역할 (null이면 비로그인)
 *   isLoading    - 네트워크 작업 중 여부 (로딩 스피너 표시용)
 *   errorMessage - 오류 메시지 (null이면 오류 없음)
 */
class AuthViewModel : ViewModel() {

    private val auth = Firebase.auth
    private val db   = Firebase.firestore

    var userRole     by mutableStateOf<UserRole?>(null); private set
    var isLoading    by mutableStateOf(false);            private set
    var errorMessage by mutableStateOf<String?>(null);    private set

    // ──────────────────────────────────────────────────
    // 앱 시작 시: 이미 로그인된 세션이 있는지 확인
    // ──────────────────────────────────────────────────
    fun checkCurrentUser(onResult: (Boolean) -> Unit) {
        val currentUser = auth.currentUser
        if (currentUser == null) {
            onResult(false)
            return
        }
        fetchUserRole(currentUser.uid) { onResult(true) }
    }

    // ──────────────────────────────────────────────────
    // 로그인
    // ──────────────────────────────────────────────────
    fun login(email: String, password: String, onSuccess: () -> Unit) {
        if (email.isBlank() || password.isBlank()) {
            errorMessage = "이메일과 비밀번호를 입력해 주세요."
            return
        }
        isLoading = true
        errorMessage = null

        auth.signInWithEmailAndPassword(email.trim(), password)
            .addOnSuccessListener { result ->
                fetchUserRole(result.user!!.uid) {
                    isLoading = false
                    onSuccess()
                }
            }
            .addOnFailureListener { e ->
                isLoading = false
                errorMessage = "로그인 실패: ${e.localizedMessage}"
            }
    }

    // ──────────────────────────────────────────────────
    // 회원가입
    // ──────────────────────────────────────────────────
    fun signUp(
        email: String,
        password: String,
        name: String,
        role: UserRole,
        onSuccess: () -> Unit
    ) {
        if (email.isBlank() || password.isBlank() || name.isBlank()) {
            errorMessage = "모든 항목을 입력해 주세요."
            return
        }
        isLoading = true
        errorMessage = null

        auth.createUserWithEmailAndPassword(email.trim(), password)
            .addOnSuccessListener { result ->
                val uid  = result.user!!.uid
                val user = User(uid = uid, email = email.trim(), name = name.trim(), role = role.name)

                db.collection("users").document(uid).set(user)
                    .addOnSuccessListener {
                        userRole  = role
                        isLoading = false
                        onSuccess()
                    }
                    .addOnFailureListener { e ->
                        isLoading = false
                        errorMessage = "사용자 정보 저장 실패: ${e.localizedMessage}"
                    }
            }
            .addOnFailureListener { e ->
                isLoading = false
                errorMessage = "회원가입 실패: ${e.localizedMessage}"
            }
    }

    // ──────────────────────────────────────────────────
    // 로그아웃
    // ──────────────────────────────────────────────────
    fun logout() {
        auth.signOut()
        userRole     = null
        errorMessage = null
    }

    // ──────────────────────────────────────────────────
    // 오류 메시지 초기화
    // ──────────────────────────────────────────────────
    fun clearError() { errorMessage = null }

    // ──────────────────────────────────────────────────
    // Firestore에서 역할 조회 (내부 함수)
    // ──────────────────────────────────────────────────
    private fun fetchUserRole(uid: String, onDone: () -> Unit) {
        db.collection("users").document(uid).get()
            .addOnSuccessListener { doc ->
                val roleStr = doc.getString("role") ?: UserRole.STUDENT.name
                userRole = try {
                    UserRole.valueOf(roleStr)
                } catch (e: IllegalArgumentException) {
                    UserRole.STUDENT
                }
                onDone()
            }
            .addOnFailureListener {
                // 역할 조회 실패 시 학생으로 기본 처리
                userRole = UserRole.STUDENT
                onDone()
            }
    }
}
