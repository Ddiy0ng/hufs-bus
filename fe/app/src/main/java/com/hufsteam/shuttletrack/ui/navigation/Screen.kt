package com.hufsteam.shuttletrack.ui.navigation

sealed class Screen(val route: String) {
    object Splash          : Screen("splash")
    object Home            : Screen("home")
    object Login           : Screen("login")
    object SignUp          : Screen("signup")
    object StudentMain     : Screen("student_main")
    object DriverMain      : Screen("driver_main")
    object AdminMain       : Screen("admin_main")
    object ServiceTerms    : Screen("service_terms")
    object PrivacyTerms    : Screen("privacy_terms")
    object DriverOperation : Screen("driver_operation")
    object Timetable       : Screen("timetable/{role}") {
        fun createRoute(role: String) = "timetable/$role"
    }
    object RouteManagement     : Screen("admin_route")
    object StopManagement      : Screen("admin_stop")
    object TimetableManagement : Screen("admin_timetable")
}
