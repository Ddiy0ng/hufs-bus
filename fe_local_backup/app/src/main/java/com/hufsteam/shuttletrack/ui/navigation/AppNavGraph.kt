package com.hufsteam.shuttletrack.ui.navigation

import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.hufsteam.shuttletrack.data.model.UserRole
import com.hufsteam.shuttletrack.ui.admin.AdminMainScreen
import com.hufsteam.shuttletrack.ui.auth.AuthViewModel
import com.hufsteam.shuttletrack.ui.auth.LoginScreen
import com.hufsteam.shuttletrack.ui.auth.SignUpScreen
import com.hufsteam.shuttletrack.ui.driver.DriverMainScreen
import com.hufsteam.shuttletrack.ui.splash.SplashScreen
import com.hufsteam.shuttletrack.ui.student.StudentMainScreen

/**
 * 앱 전체 Navigation 그래프
 *
 * 역할(role) 에 따른 분기:
 *   STUDENT → StudentMainScreen
 *   DRIVER  → DriverMainScreen
 *   ADMIN   → AdminMainScreen
 */
@Composable
fun AppNavGraph(
    viewModel: AuthViewModel = viewModel()
) {
    val navController = rememberNavController()

    NavHost(
        navController    = navController,
        startDestination = Screen.Splash.route
    ) {
        // ── 스플래시 (자동 로그인 체크) ──────────────────────────
        composable(Screen.Splash.route) {
            SplashScreen(
                viewModel  = viewModel,
                onNavigate = { loggedIn ->
                    if (loggedIn) {
                        navigateByRole(navController, viewModel.userRole)
                    } else {
                        navController.navigate(Screen.Login.route) {
                            popUpTo(Screen.Splash.route) { inclusive = true }
                        }
                    }
                }
            )
        }

        // ── 로그인 ───────────────────────────────────────────────
        composable(Screen.Login.route) {
            LoginScreen(
                viewModel      = viewModel,
                onLoginSuccess = { navigateByRole(navController, viewModel.userRole) },
                onGoSignUp     = { navController.navigate(Screen.SignUp.route) }
            )
        }

        // ── 회원가입 ─────────────────────────────────────────────
        composable(Screen.SignUp.route) {
            SignUpScreen(
                viewModel        = viewModel,
                onSignUpSuccess  = { navigateByRole(navController, viewModel.userRole) },
                onGoLogin        = { navController.popBackStack() }
            )
        }

        // ── 학생 메인 ────────────────────────────────────────────
        composable(Screen.StudentMain.route) {
            StudentMainScreen(
                onLogout = { logout(navController, viewModel) }
            )
        }

        // ── 기사 메인 ────────────────────────────────────────────
        composable(Screen.DriverMain.route) {
            DriverMainScreen(
                onLogout = { logout(navController, viewModel) }
            )
        }

        // ── 관리자 메인 ──────────────────────────────────────────
        composable(Screen.AdminMain.route) {
            AdminMainScreen(
                onLogout = { logout(navController, viewModel) }
            )
        }
    }
}

/** 역할에 따라 메인 화면으로 이동 (백스택 전부 제거) */
private fun navigateByRole(navController: NavController, role: UserRole?) {
    val route = when (role) {
        UserRole.DRIVER -> Screen.DriverMain.route
        UserRole.ADMIN  -> Screen.AdminMain.route
        else            -> Screen.StudentMain.route
    }
    navController.navigate(route) {
        popUpTo(0) { inclusive = true }
    }
}

/** 로그아웃 후 로그인 화면으로 이동 */
private fun logout(navController: NavController, viewModel: AuthViewModel) {
    viewModel.logout()
    navController.navigate(Screen.Login.route) {
        popUpTo(0) { inclusive = true }
    }
}
