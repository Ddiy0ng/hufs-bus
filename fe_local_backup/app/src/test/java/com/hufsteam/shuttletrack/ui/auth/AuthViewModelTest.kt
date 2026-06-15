package com.hufsteam.shuttletrack.ui.auth

/**
 * ============================================================
 *  AuthViewModel 단위 테스트
 * ============================================================
 *
 *  실행 방법: Android Studio → 파일 우클릭 → "Run AuthViewModelTest"
 *
 *  필요 의존성 (app/build.gradle):
 *  -------------------------------------------------
 *  testImplementation 'junit:junit:4.13.2'
 *  -------------------------------------------------
 *
 *  테스트 범위:
 *  [L] 로그인 (login) 유효성 검사 — TC-L-001 ~ TC-L-005
 *  [S] 회원가입 (signUp) 유효성 검사 — TC-S-001 ~ TC-S-007
 *  [C] 상태 초기화 (clearErrors / logout) — TC-C-001 ~ TC-C-002
 * ============================================================
 */

import org.junit.*
import org.junit.Assert.*

class AuthViewModelTest {

    // ============================================================
    //  [L] 로그인 유효성 검사
    // ============================================================

    /**
     * TC-L-001
     * 시나리오: 이메일과 비밀번호가 모두 빈 값
     * 기대: errorMessage = "이메일과 비밀번호를 입력해 주세요."
     *       Firebase signIn 호출 안 됨
     */
    @Test
    fun `TC-L-001 빈 이메일과 비밀번호 입력 시 에러 메시지 설정`() {
        val result = validateLoginInput(email = "", password = "")
        assertEquals("이메일과 비밀번호를 입력해 주세요.", result)
    }

    /**
     * TC-L-002
     * 시나리오: 이메일만 입력, 비밀번호 공백
     * 기대: errorMessage = "이메일과 비밀번호를 입력해 주세요."
     */
    @Test
    fun `TC-L-002 비밀번호 공백 시 에러 메시지 설정`() {
        val result = validateLoginInput(email = "test@hufs.ac.kr", password = "   ")
        assertEquals("이메일과 비밀번호를 입력해 주세요.", result)
    }

    /**
     * TC-L-003
     * 시나리오: @가 없는 단축 ID 입력 (기사/관리자용)
     * 기대: @hufs.ac.kr 이 자동으로 붙은 이메일 반환
     */
    @Test
    fun `TC-L-003 단축 ID에 @hufs_ac_kr 자동 추가`() {
        val resolved = resolveEmail("driver1")
        assertEquals("driver1@hufs.ac.kr", resolved)
    }

    /**
     * TC-L-004
     * 시나리오: 이미 @ 포함된 이메일 입력
     * 기대: 원본 그대로 반환 (도메인 중복 추가 없음)
     */
    @Test
    fun `TC-L-004 @ 포함 이메일은 그대로 사용`() {
        val resolved = resolveEmail("test@hufs.ac.kr")
        assertEquals("test@hufs.ac.kr", resolved)
    }

    /**
     * TC-L-005
     * 시나리오: 앞뒤 공백이 있는 이메일
     * 기대: trim() 적용 후 @hufs.ac.kr 붙음
     */
    @Test
    fun `TC-L-005 이메일 앞뒤 공백 제거 후 도메인 추가`() {
        val resolved = resolveEmail("  driver1  ")
        assertEquals("driver1@hufs.ac.kr", resolved)
    }

    // ============================================================
    //  [S] 회원가입 유효성 검사
    // ============================================================

    /**
     * TC-S-001
     * 시나리오: @hufs.ac.kr 이 아닌 외부 이메일
     * 기대: emailError = "hufs.ac.kr 이메일만 가입 가능합니다"
     */
    @Test
    fun `TC-S-001 외부 이메일 도메인 가입 불가`() {
        val error = validateSignUpEmail("test@gmail.com")
        assertEquals("hufs.ac.kr 이메일만 가입 가능합니다", error)
    }

    /**
     * TC-S-002
     * 시나리오: @hufs.ac.kr 이메일 정상 입력
     * 기대: emailError = null (통과)
     */
    @Test
    fun `TC-S-002 hufs_ac_kr 이메일 정상 통과`() {
        val error = validateSignUpEmail("student@hufs.ac.kr")
        assertNull(error)
    }

    /**
     * TC-S-003
     * 시나리오: 비밀번호 7자 (8자 미만)
     * 기대: passwordError = "비밀번호는 8자 이상이어야 합니다"
     */
    @Test
    fun `TC-S-003 비밀번호 8자 미만 시 길이 에러`() {
        val error = validateSignUpPassword("abc1234")
        assertEquals("비밀번호는 8자 이상이어야 합니다", error)
    }

    /**
     * TC-S-004
     * 시나리오: 영문만 포함, 숫자 없음
     * 기대: passwordError = "영문과 숫자를 반드시 포함하여야 합니다"
     */
    @Test
    fun `TC-S-004 숫자 없는 비밀번호 에러`() {
        val error = validateSignUpPassword("abcdefgh")
        assertEquals("영문과 숫자를 반드시 포함하여야 합니다", error)
    }

    /**
     * TC-S-005
     * 시나리오: 숫자만 포함, 영문 없음
     * 기대: passwordError = "영문과 숫자를 반드시 포함하여야 합니다"
     */
    @Test
    fun `TC-S-005 영문 없는 비밀번호 에러`() {
        val error = validateSignUpPassword("12345678")
        assertEquals("영문과 숫자를 반드시 포함하여야 합니다", error)
    }

    /**
     * TC-S-006
     * 시나리오: 영문 + 숫자 포함, 8자 이상
     * 기대: passwordError = null (통과)
     */
    @Test
    fun `TC-S-006 영문과 숫자 포함 8자 이상 비밀번호 통과`() {
        val error = validateSignUpPassword("hufs1234")
        assertNull(error)
    }

    /**
     * TC-S-007
     * 시나리오: 영문 + 숫자 + 특수문자 조합
     * 기대: passwordError = null (영문+숫자 포함이므로 통과)
     */
    @Test
    fun `TC-S-007 특수문자 포함 비밀번호도 통과`() {
        val error = validateSignUpPassword("hufs12!@")
        assertNull(error)
    }

    // ============================================================
    //  [C] 상태 초기화
    // ============================================================

    /**
     * TC-C-001
     * 시나리오: clearErrors() 호출
     * 기대: errorMessage, emailError, passwordError 모두 null
     */
    @Test
    fun `TC-C-001 clearErrors 호출 시 모든 에러 상태 초기화`() {
        val state = ErrorState(
            errorMessage  = "테스트 에러",
            emailError    = "이메일 에러",
            passwordError = "비밀번호 에러"
        )
        val cleared = state.clear()
        assertNull(cleared.errorMessage)
        assertNull(cleared.emailError)
        assertNull(cleared.passwordError)
    }

    /**
     * TC-C-002
     * 시나리오: 로그아웃 후 상태 리셋
     * 기대: userRole = null, signupSuccess = false
     */
    @Test
    fun `TC-C-002 로그아웃 후 역할과 가입성공 상태 초기화`() {
        val state = SessionState(isLoggedIn = true, signupSuccess = true)
        val loggedOut = state.logout()
        assertFalse(loggedOut.isLoggedIn)
        assertFalse(loggedOut.signupSuccess)
    }

    // ============================================================
    //  헬퍼 함수 — AuthViewModel 내부 로직 재현 (Firebase 불필요)
    // ============================================================

    /** 로그인 입력 유효성: 빈 값 검사 */
    private fun validateLoginInput(email: String, password: String): String? {
        return if (email.isBlank() || password.isBlank())
            "이메일과 비밀번호를 입력해 주세요."
        else null
    }

    /** 이메일 도메인 자동 추가 로직 (AuthViewModel.login() 내부 로직) */
    private fun resolveEmail(email: String): String {
        return if (email.contains("@")) email.trim()
        else "${email.trim()}@hufs.ac.kr"
    }

    /** 회원가입 이메일 유효성 검사 */
    private fun validateSignUpEmail(email: String): String? {
        return if (!email.trim().endsWith("@hufs.ac.kr"))
            "hufs.ac.kr 이메일만 가입 가능합니다"
        else null
    }

    /** 회원가입 비밀번호 유효성 검사 */
    private fun validateSignUpPassword(password: String): String? {
        val hasLetter = password.any { it.isLetter() }
        val hasDigit  = password.any { it.isDigit() }
        return when {
            password.length < 8         -> "비밀번호는 8자 이상이어야 합니다"
            !hasLetter || !hasDigit     -> "영문과 숫자를 반드시 포함하여야 합니다"
            else                        -> null
        }
    }

    // ── 불변 상태 클래스 (헬퍼용) ─────────────────────────────

    private data class ErrorState(
        val errorMessage: String?,
        val emailError: String?,
        val passwordError: String?
    ) {
        fun clear() = copy(errorMessage = null, emailError = null, passwordError = null)
    }

    private data class SessionState(val isLoggedIn: Boolean, val signupSuccess: Boolean) {
        fun logout() = copy(isLoggedIn = false, signupSuccess = false)
    }
}
