package com.hufsteam.shuttletrack.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.hufsteam.shuttletrack.data.model.UserRole
import com.hufsteam.shuttletrack.ui.admin.AdminMainScreen
import com.hufsteam.shuttletrack.ui.admin.AdminViewModel
import com.hufsteam.shuttletrack.ui.admin.RouteManagementScreen
import com.hufsteam.shuttletrack.ui.admin.StopManagementScreen
import com.hufsteam.shuttletrack.ui.admin.TimetableManagementScreen
import com.hufsteam.shuttletrack.ui.auth.AuthViewModel
import com.hufsteam.shuttletrack.ui.auth.LoginScreen
import com.hufsteam.shuttletrack.ui.auth.SignUpScreen
import com.hufsteam.shuttletrack.ui.driver.DriverMainScreen
import com.hufsteam.shuttletrack.ui.driver.DriverOperationScreen
import com.hufsteam.shuttletrack.ui.driver.DriverViewModel
import com.hufsteam.shuttletrack.ui.home.HomeScreen
import com.hufsteam.shuttletrack.ui.splash.SplashScreen
import com.hufsteam.shuttletrack.ui.student.StudentMainScreen
import com.hufsteam.shuttletrack.ui.terms.TermsScreen
import com.hufsteam.shuttletrack.ui.terms.TermsType
import com.hufsteam.shuttletrack.ui.timetable.TimetableScreen

@Composable
fun AppNavGraph(
    authViewModel: AuthViewModel = viewModel(),
    driverViewModel: DriverViewModel = viewModel(),
    adminViewModel: AdminViewModel = viewModel()
) {
    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = Screen.Splash.route) {

        composable(Screen.Splash.route) {
            SplashScreen(viewModel = authViewModel, onNavigate = { loggedIn ->
                if (loggedIn) navigateByRole(navController, authViewModel.userRole)
                else navController.navigate(Screen.Home.route) {
                    popUpTo(Screen.Splash.route) { inclusive = true }
                }
            })
        }

        composable(Screen.Home.route) {
            HomeScreen(
                onGoLogin  = { navController.navigate(Screen.Login.route) },
                onGoSignUp = { navController.navigate(Screen.SignUp.route) }
            )
        }

        composable(Screen.Login.route) {
            LoginScreen(
                viewModel      = authViewModel,
                onLoginSuccess = { navigateByRole(navController, authViewModel.userRole) },
                onGoSignUp     = { navController.navigate(Screen.SignUp.route) },
                onBack         = { navController.popBackStack() }
            )
        }

        composable(Screen.SignUp.route) {
            SignUpScreen(
                viewModel        = authViewModel,
                onGoLogin        = {
                    navController.navigate(Screen.Login.route) {
                        popUpTo(Screen.Home.route)
                    }
                },
                onGoServiceTerms = { navController.navigate(Screen.ServiceTerms.route) },
                onGoPrivacyTerms = { navController.navigate(Screen.PrivacyTerms.route) },
            )
        }

        composable(Screen.StudentMain.route) {
            StudentMainScreen(
                viewModel = authViewModel,
                onLogout  = { doLogout(navController, authViewModel) },
                onGoServiceTerms = { navController.navigate(Screen.ServiceTerms.route) },
                onGoPrivacyTerms = { navController.navigate(Screen.PrivacyTerms.route) },
                onGoDriverTimetable = { navController.navigate(Screen.Timetable.createRoute("driver")) },
                onGoDriverOperation = { route ->
                    driverViewModel.selectRoute(route)
                    navController.navigate(Screen.DriverOperation.route)
                },
                onGoAdminRouteManagement = { navController.navigate(Screen.RouteManagement.route) },
                onGoAdminStopManagement = { navController.navigate(Screen.StopManagement.route) },
                onGoAdminTimetableManagement = { navController.navigate(Screen.TimetableManagement.route) },
                driverViewModel = driverViewModel
            )
        }

        composable(Screen.DriverMain.route) {
            DriverMainScreen(
                viewModel       = authViewModel,
                driverViewModel = driverViewModel,
                onLogout        = { doLogout(navController, authViewModel) },
                onGoTimetable   = { navController.navigate(Screen.Timetable.createRoute("driver")) },
                onGoOperation   = { navController.navigate(Screen.DriverOperation.route) }
            )
        }

        composable(Screen.DriverOperation.route) {
            if (authViewModel.userRole != UserRole.DRIVER) {
                LaunchedEffect(authViewModel.userRole) {
                    navController.popBackStack()
                }
            } else {
                DriverOperationScreen(
                    driverViewModel = driverViewModel,
                    onBack          = { navController.popBackStack() }
                )
            }
        }

        composable(Screen.AdminMain.route) {
            AdminMainScreen(
                viewModel               = authViewModel,
                onLogout                = { doLogout(navController, authViewModel) },
                onGoTimetable           = { navController.navigate(Screen.Timetable.createRoute("admin")) },
                onGoRouteManagement     = { navController.navigate(Screen.RouteManagement.route) },
                onGoStopManagement      = { navController.navigate(Screen.StopManagement.route) },
                onGoTimetableManagement = { navController.navigate(Screen.TimetableManagement.route) }
            )
        }

        composable(Screen.RouteManagement.route) {
            RouteManagementScreen(
                adminViewModel = adminViewModel,
                onBack         = { navController.popBackStack() }
            )
        }

        composable(Screen.StopManagement.route) {
            StopManagementScreen(
                adminViewModel = adminViewModel,
                onBack         = { navController.popBackStack() }
            )
        }

        composable(Screen.TimetableManagement.route) {
            TimetableManagementScreen(
                adminViewModel = adminViewModel,
                onBack         = { navController.popBackStack() }
            )
        }

        composable(Screen.ServiceTerms.route) {
            TermsScreen(type = TermsType.SERVICE, onBack = { navController.popBackStack() })
        }

        composable(Screen.PrivacyTerms.route) {
            TermsScreen(type = TermsType.PRIVACY, onBack = { navController.popBackStack() })
        }

        composable(Screen.Timetable.route) { backStackEntry ->
            val role = backStackEntry.arguments?.getString("role") ?: "driver"
            TimetableScreen(
                role          = role,
                onGoMyPage    = { navController.popBackStack() },
                onGoFavorites = {},
                onDriverScheduleClick = { route ->
                    if (role == "driver" && authViewModel.userRole == UserRole.DRIVER) {
                        driverViewModel.selectRoute(route)
                        navController.navigate(Screen.DriverOperation.route)
                    }
                }
            )
        }
    }
}

private fun navigateByRole(navController: NavController, role: UserRole?) {
    navController.navigate(Screen.StudentMain.route) {
        popUpTo(0) { inclusive = true }
    }
}

private fun doLogout(navController: NavController, viewModel: AuthViewModel) {
    viewModel.logout()
    navController.navigate(Screen.Home.route) { popUpTo(0) { inclusive = true } }
}
