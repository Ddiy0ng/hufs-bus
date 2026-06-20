package com.example.hufs_bus.data

class BusRepository {

    private val api = RetrofitClient.api

    suspend fun signUp(
        email: String,
        password: String,
        privacyTermAgree: Boolean,
        serviceTermAgree: Boolean
    ): Boolean {
        return try {
            val response = api.signup(
                SignupRequest(
                    email = email.trim(),
                    password = password,
                    privacyTermAgree = privacyTermAgree,
                    serviceTermAgree = serviceTermAgree
                )
            )
            response.isSuccessful
        } catch (e: Exception) {
            false
        }
    }

    suspend fun signIn(email: String, password: String): AuthResult? {
        return try {
            val response = api.login(LoginRequest(email.trim(), password))
            val data = response.body()?.data
            if (response.isSuccessful && data != null) {
                RetrofitClient.setAccessToken(data.accessToken)
                AuthResult(
                    accessToken = data.accessToken,
                    userId = data.userId,
                    email = data.email,
                    role = data.role
                )
            } else {
                null
            }
        } catch (e: Exception) {
            null
        }
    }

    fun clearAuth() {
        RetrofitClient.setAccessToken(null)
    }

    suspend fun getRoutes(type: RouteType): List<BusRoute> {
        return try {
            val response = api.getRoutes(type.toServerValue())
            if (response.isSuccessful) {
                response.body()?.data?.map { it.toBusRoute(type) } ?: emptyList()
            } else {
                getDummyRoutes(type)
            }
        } catch (e: Exception) {
            getDummyRoutes(type)
        }
    }

    suspend fun getSchedules(routeId: Long, hour: Int?): List<BusSchedule> {
        return try {
            val response = api.getSchedules(routeId = routeId, startTime = hour)
            if (response.isSuccessful) {
                response.body()?.data?.map { it.toBusSchedule() } ?: emptyList()
            } else {
                getDummySchedules(routeId, hour)
            }
        } catch (e: Exception) {
            getDummySchedules(routeId, hour)
        }
    }

    suspend fun getBusLocation(scheduleId: Long): BusLocationInfo {
        return try {
            val liveResponse = api.getLiveTimetable(scheduleId)
            if (liveResponse.isSuccessful && liveResponse.body() != null) {
                liveResponse.body()!!.toBusLocationInfo(scheduleId)
            } else {
                getDummyBusLocation(scheduleId)
            }
        } catch (e: Exception) {
            getDummyBusLocation(scheduleId)
        }
    }

    suspend fun saveFavorite(timetableId: Long, days: Set<NotificationDay>): Boolean {
        return try {
            val request = FavoriteCreateRequest(
                timetableId = timetableId,
                days = days.map { it.serverValue }.toSet()
            )
            val response = api.createFavorite(request)
            response.isSuccessful
        } catch (e: Exception) {
            false
        }
    }

    suspend fun getFavorites(type: RouteType, day: NotificationDay): List<FavoriteResponse> {
        return try {
            val response = api.getFavorites(type.toServerValue(), day.serverValue)
            if (response.isSuccessful) response.body()?.data ?: emptyList() else emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun removeFavorite(favoriteId: Long, day: NotificationDay): Boolean {
        return try {
            api.deleteFavorite(favoriteId, day.serverValue).isSuccessful
        } catch (e: Exception) {
            false
        }
    }

    private fun RouteType.toServerValue(): String {
        return when (this) {
            RouteType.CAMPUS -> "IN_CAMPUS"
            RouteType.OFF_CAMPUS -> "OUT_CAMPUS"
        }
    }

    private fun BusRouteResponse.toBusRoute(fallbackType: RouteType): BusRoute {
        return BusRoute(
            id = routeId,
            name = route.ifBlank { "$startStop → $endStop" },
            departure = startStop,
            destination = endStop,
            type = when (inOutCampus) {
                "IN_CAMPUS", "교내" -> RouteType.CAMPUS
                "OUT_CAMPUS", "교외" -> RouteType.OFF_CAMPUS
                else -> fallbackType
            }
        )
    }

    private fun TimetableResponse.toBusSchedule(): BusSchedule {
        return BusSchedule(
            id = timetableId,
            routeId = routeId,
            departureTime = departAt,
            totalSeats = 45,
            remainingSeats = 45,
            currentLocation = "운행 대기",
            status = BusStatus.SCHEDULED
        )
    }

    private fun LiveTimetableResponse.toBusLocationInfo(scheduleId: Long): BusLocationInfo {
        val orderedStops = stops.sortedBy { it.sequence }
        val currentStop = orderedStops.firstOrNull { !it.eta.isNullOrBlank() } ?: orderedStops.firstOrNull()
        val stopModels = orderedStops.mapIndexed { index, stop ->
            BusStop(index.toLong() + 1, stop.stopName, stop.sequence)
        }
        val seats = currentSeats ?: 45

        return BusLocationInfo(
            busId = timetableId,
            routeId = scheduleId,
            routeName = "실시간 셔틀",
            currentStopIndex = stopModels.indexOfFirst { it.name == currentStop?.stopName }.coerceAtLeast(0),
            currentStopName = currentStop?.stopName ?: "위치 확인 중",
            stops = stopModels,
            departureTime = actualDepartureTime ?: plannedDepartureTime ?: "-",
            arrivalTime = currentStop?.eta ?: "계산 중",
            remainingSeats = seats,
            totalSeats = 45,
            status = status.toBusStatus()
        )
    }

    private fun String?.toBusStatus(): BusStatus {
        return when (this) {
            "IN_OPERATION", "RUNNING", "운행중" -> BusStatus.IN_OPERATION
            "COMPLETED", "ARRIVED", "운행완료" -> BusStatus.COMPLETED
            "CANCELLED", "취소" -> BusStatus.CANCELLED
            else -> BusStatus.SCHEDULED
        }
    }

    // ===== 노선 목록 =====
    private fun getDummyRoutes(type: RouteType): List<BusRoute> {
        return when (type) {
            RouteType.OFF_CAMPUS -> listOf(
                BusRoute(1, "판교역 → 글로벌캠퍼스 (등교)", "판교역", "인문경상관 앞", RouteType.OFF_CAMPUS),
                BusRoute(2, "글로벌캠퍼스 → 판교역 (하교)", "백년관 앞", "판교역", RouteType.OFF_CAMPUS),
            )
            RouteType.CAMPUS -> listOf(
                BusRoute(3, "지석묘 앞 → 인문경상관 앞 (상행)", "지석묘 앞", "인문경상관 앞", RouteType.CAMPUS),
                BusRoute(4, "인문경상관 앞 → 지석묘 앞 (하행)", "인문경상관 회차장 앞", "지석묘 앞", RouteType.CAMPUS),
            )
        }
    }

    // ===== 시간표 =====
    private fun getDummySchedules(routeId: Long, hour: Int?): List<BusSchedule> {
        val all = when (routeId) {
            // 교외 등교: 판교역 → 글로벌캠퍼스
            1L -> listOf(
                BusSchedule(1001, 1, "07:40", 45, 45, "운행 대기", BusStatus.SCHEDULED),
                BusSchedule(1002, 1, "07:45", 45, 45, "운행 대기", BusStatus.SCHEDULED),
                BusSchedule(1003, 1, "07:50", 45, 45, "운행 대기", BusStatus.SCHEDULED),
                BusSchedule(1004, 1, "09:40", 45, 45, "운행 대기", BusStatus.SCHEDULED),
                BusSchedule(1005, 1, "09:50", 45, 45, "운행 대기", BusStatus.SCHEDULED),
            )
            // 교외 하교: 글로벌캠퍼스 → 판교역
            2L -> listOf(
                BusSchedule(2001, 2, "14:10", 45, 45, "운행 대기", BusStatus.SCHEDULED),
                BusSchedule(2002, 2, "15:10", 45, 45, "운행 대기", BusStatus.SCHEDULED),
                BusSchedule(2003, 2, "15:25", 45, 45, "운행 대기", BusStatus.SCHEDULED),
                BusSchedule(2004, 2, "17:30", 45, 45, "운행 대기", BusStatus.SCHEDULED),
                BusSchedule(2005, 2, "18:20", 45, 45, "운행 대기", BusStatus.SCHEDULED),
            )
            // 교내 상행: 지석묘 앞 → 인문경상관 앞
            3L -> listOf(
                BusSchedule(3001, 3, "08:20", 45, 40, "지석묘 앞", BusStatus.SCHEDULED),
                BusSchedule(3002, 3, "08:30", 45, 45, "운행 대기", BusStatus.SCHEDULED),
                BusSchedule(3003, 3, "08:40", 45, 45, "운행 대기", BusStatus.SCHEDULED),
                BusSchedule(3004, 3, "08:50", 45, 45, "운행 대기", BusStatus.SCHEDULED),
                BusSchedule(3005, 3, "09:00", 45, 45, "운행 대기", BusStatus.SCHEDULED),
                BusSchedule(3006, 3, "09:15", 45, 45, "운행 대기", BusStatus.SCHEDULED),
                BusSchedule(3007, 3, "09:30", 45, 45, "운행 대기", BusStatus.SCHEDULED),
                BusSchedule(3008, 3, "09:40", 45, 45, "운행 대기", BusStatus.SCHEDULED),
                BusSchedule(3009, 3, "09:50", 45, 45, "운행 대기", BusStatus.SCHEDULED),
                BusSchedule(3010, 3, "10:00", 45, 45, "운행 대기", BusStatus.SCHEDULED),
                BusSchedule(3011, 3, "10:15", 45, 45, "운행 대기", BusStatus.SCHEDULED),
                BusSchedule(3012, 3, "10:30", 45, 45, "운행 대기", BusStatus.SCHEDULED),
                BusSchedule(3013, 3, "10:40", 45, 45, "운행 대기", BusStatus.SCHEDULED),
                BusSchedule(3014, 3, "10:50", 45, 45, "운행 대기", BusStatus.SCHEDULED),
                BusSchedule(3015, 3, "11:00", 45, 45, "운행 대기", BusStatus.SCHEDULED),
                BusSchedule(3016, 3, "11:15", 45, 45, "운행 대기", BusStatus.SCHEDULED),
                BusSchedule(3017, 3, "11:30", 45, 45, "운행 대기", BusStatus.SCHEDULED),
                BusSchedule(3018, 3, "11:35", 45, 45, "운행 대기", BusStatus.SCHEDULED),
                BusSchedule(3019, 3, "11:40", 45, 45, "운행 대기", BusStatus.SCHEDULED),
                BusSchedule(3020, 3, "11:50", 45, 45, "운행 대기", BusStatus.SCHEDULED),
                BusSchedule(3021, 3, "12:10", 45, 45, "운행 대기", BusStatus.SCHEDULED),
                BusSchedule(3022, 3, "12:50", 45, 45, "운행 대기", BusStatus.SCHEDULED),
                BusSchedule(3023, 3, "13:00", 45, 45, "운행 대기", BusStatus.SCHEDULED),
                BusSchedule(3024, 3, "13:15", 45, 45, "운행 대기", BusStatus.SCHEDULED),
                BusSchedule(3025, 3, "13:30", 45, 45, "운행 대기", BusStatus.SCHEDULED),
                BusSchedule(3026, 3, "13:40", 45, 45, "운행 대기", BusStatus.SCHEDULED),
                BusSchedule(3027, 3, "13:50", 45, 45, "운행 대기", BusStatus.SCHEDULED),
                BusSchedule(3028, 3, "14:00", 45, 45, "운행 대기", BusStatus.SCHEDULED),
                BusSchedule(3029, 3, "14:15", 45, 45, "운행 대기", BusStatus.SCHEDULED),
                BusSchedule(3030, 3, "14:30", 45, 45, "운행 대기", BusStatus.SCHEDULED),
                BusSchedule(3031, 3, "14:40", 45, 45, "운행 대기", BusStatus.SCHEDULED),
                BusSchedule(3032, 3, "14:50", 45, 45, "운행 대기", BusStatus.SCHEDULED),
                BusSchedule(3033, 3, "15:00", 45, 45, "운행 대기", BusStatus.SCHEDULED),
                BusSchedule(3034, 3, "15:30", 45, 45, "운행 대기", BusStatus.SCHEDULED),
                BusSchedule(3035, 3, "15:40", 45, 45, "운행 대기", BusStatus.SCHEDULED),
                BusSchedule(3036, 3, "15:50", 45, 45, "운행 대기", BusStatus.SCHEDULED),
                BusSchedule(3037, 3, "16:00", 45, 45, "운행 대기", BusStatus.SCHEDULED),
                BusSchedule(3038, 3, "16:20", 45, 45, "운행 대기", BusStatus.SCHEDULED),
                BusSchedule(3039, 3, "16:40", 45, 45, "운행 대기", BusStatus.SCHEDULED),
                BusSchedule(3040, 3, "17:00", 45, 45, "운행 대기", BusStatus.SCHEDULED),
                BusSchedule(3041, 3, "17:20", 45, 45, "운행 대기", BusStatus.SCHEDULED),
                BusSchedule(3042, 3, "17:40", 45, 45, "운행 대기", BusStatus.SCHEDULED),
                BusSchedule(3043, 3, "18:00", 45, 45, "운행 대기", BusStatus.SCHEDULED),
                BusSchedule(3044, 3, "18:30", 45, 45, "운행 대기", BusStatus.SCHEDULED),
                BusSchedule(3045, 3, "19:00", 45, 45, "운행 대기", BusStatus.SCHEDULED),
                BusSchedule(3046, 3, "19:30", 45, 45, "운행 대기", BusStatus.SCHEDULED),
                BusSchedule(3047, 3, "20:00", 45, 45, "운행 대기", BusStatus.SCHEDULED),
                BusSchedule(3048, 3, "20:30", 45, 45, "운행 대기", BusStatus.SCHEDULED),
            )
            // 교내 하행: 인문경상관 앞 → 지석묘 앞
            4L -> listOf(
                BusSchedule(4001, 4, "08:30", 45, 45, "운행 대기", BusStatus.SCHEDULED),
                BusSchedule(4002, 4, "08:40", 45, 45, "운행 대기", BusStatus.SCHEDULED),
                BusSchedule(4003, 4, "08:50", 45, 45, "운행 대기", BusStatus.SCHEDULED),
                BusSchedule(4004, 4, "09:00", 45, 45, "운행 대기", BusStatus.SCHEDULED),
                BusSchedule(4005, 4, "09:10", 45, 45, "운행 대기", BusStatus.SCHEDULED),
                BusSchedule(4006, 4, "09:25", 45, 45, "운행 대기", BusStatus.SCHEDULED),
                BusSchedule(4007, 4, "09:40", 45, 45, "운행 대기", BusStatus.SCHEDULED),
                BusSchedule(4008, 4, "09:50", 45, 45, "운행 대기", BusStatus.SCHEDULED),
                BusSchedule(4009, 4, "10:00", 45, 45, "운행 대기", BusStatus.SCHEDULED),
                BusSchedule(4010, 4, "10:10", 45, 45, "운행 대기", BusStatus.SCHEDULED),
                BusSchedule(4011, 4, "10:25", 45, 45, "운행 대기", BusStatus.SCHEDULED),
                BusSchedule(4012, 4, "10:40", 45, 45, "운행 대기", BusStatus.SCHEDULED),
                BusSchedule(4013, 4, "10:50", 45, 45, "운행 대기", BusStatus.SCHEDULED),
                BusSchedule(4014, 4, "11:00", 45, 45, "운행 대기", BusStatus.SCHEDULED),
                BusSchedule(4015, 4, "11:10", 45, 45, "운행 대기", BusStatus.SCHEDULED),
                BusSchedule(4016, 4, "11:25", 45, 45, "운행 대기", BusStatus.SCHEDULED),
                BusSchedule(4017, 4, "11:40", 45, 45, "운행 대기", BusStatus.SCHEDULED),
                BusSchedule(4018, 4, "11:45", 45, 45, "운행 대기", BusStatus.SCHEDULED),
                BusSchedule(4019, 4, "11:50", 45, 45, "운행 대기", BusStatus.SCHEDULED),
                BusSchedule(4020, 4, "12:00", 45, 45, "운행 대기", BusStatus.SCHEDULED),
                BusSchedule(4021, 4, "12:20", 45, 45, "운행 대기", BusStatus.SCHEDULED),
                BusSchedule(4022, 4, "13:00", 45, 45, "운행 대기", BusStatus.SCHEDULED),
                BusSchedule(4023, 4, "13:10", 45, 45, "운행 대기", BusStatus.SCHEDULED),
                BusSchedule(4024, 4, "13:25", 45, 45, "운행 대기", BusStatus.SCHEDULED),
                BusSchedule(4025, 4, "13:40", 45, 45, "운행 대기", BusStatus.SCHEDULED),
                BusSchedule(4026, 4, "13:50", 45, 45, "운행 대기", BusStatus.SCHEDULED),
                BusSchedule(4027, 4, "14:00", 45, 45, "운행 대기", BusStatus.SCHEDULED),
                BusSchedule(4028, 4, "14:10", 45, 45, "운행 대기", BusStatus.SCHEDULED),
                BusSchedule(4029, 4, "14:25", 45, 45, "운행 대기", BusStatus.SCHEDULED),
                BusSchedule(4030, 4, "14:40", 45, 45, "운행 대기", BusStatus.SCHEDULED),
                BusSchedule(4031, 4, "14:50", 45, 45, "운행 대기", BusStatus.SCHEDULED),
                BusSchedule(4032, 4, "15:00", 45, 45, "운행 대기", BusStatus.SCHEDULED),
                BusSchedule(4033, 4, "15:10", 45, 45, "운행 대기", BusStatus.SCHEDULED),
                BusSchedule(4034, 4, "15:40", 45, 45, "운행 대기", BusStatus.SCHEDULED),
                BusSchedule(4035, 4, "15:50", 45, 45, "운행 대기", BusStatus.SCHEDULED),
                BusSchedule(4036, 4, "16:00", 45, 45, "운행 대기", BusStatus.SCHEDULED),
                BusSchedule(4037, 4, "16:10", 45, 45, "운행 대기", BusStatus.SCHEDULED),
                BusSchedule(4038, 4, "16:30", 45, 45, "운행 대기", BusStatus.SCHEDULED),
                BusSchedule(4039, 4, "16:50", 45, 45, "운행 대기", BusStatus.SCHEDULED),
                BusSchedule(4040, 4, "17:10", 45, 45, "운행 대기", BusStatus.SCHEDULED),
                BusSchedule(4041, 4, "17:30", 45, 45, "운행 대기", BusStatus.SCHEDULED),
                BusSchedule(4042, 4, "17:50", 45, 45, "운행 대기", BusStatus.SCHEDULED),
                BusSchedule(4043, 4, "18:10", 45, 45, "운행 대기", BusStatus.SCHEDULED),
                BusSchedule(4044, 4, "18:40", 45, 45, "운행 대기", BusStatus.SCHEDULED),
                BusSchedule(4045, 4, "19:10", 45, 45, "운행 대기", BusStatus.SCHEDULED),
                BusSchedule(4046, 4, "19:40", 45, 45, "운행 대기", BusStatus.SCHEDULED),
                BusSchedule(4047, 4, "20:10", 45, 45, "운행 대기", BusStatus.SCHEDULED),
                BusSchedule(4048, 4, "20:40", 45, 45, "운행 대기", BusStatus.SCHEDULED),
            )
            else -> emptyList()
        }

        return if (hour != null) {
            all.filter { it.departureTime.substringBefore(":").toIntOrNull() == hour }
        } else all
    }

    // ===== 버스 위치 (네비게이터) =====
    private fun getDummyBusLocation(scheduleId: Long): BusLocationInfo {
        // scheduleId 앞자리로 어떤 노선인지 판단
        val routeId = when {
            scheduleId in 1000..1999 -> 1L
            scheduleId in 2000..2999 -> 2L
            scheduleId in 3000..3999 -> 3L
            scheduleId in 4000..4999 -> 4L
            else -> 1L
        }

        val (routeName, stops, currentIdx) = when (routeId) {
            1L -> Triple(
                "판교역 → 글로벌캠퍼스 (등교)",
                listOf(
                    BusStop(1, "판교역", 0),
                    BusStop(2, "성남역", 1),
                    BusStop(3, "서현역", 2),
                    BusStop(4, "외대사거리", 3),
                    BusStop(5, "도서관 앞", 4),
                    BusStop(6, "학생회관 앞", 5),
                    BusStop(7, "인문경상관 앞", 6),
                ),
                2
            )
            2L -> Triple(
                "글로벌캠퍼스 → 판교역 (하교)",
                listOf(
                    BusStop(1, "백년관 앞", 0),
                    BusStop(2, "서현역", 1),
                    BusStop(3, "성남역", 2),
                    BusStop(4, "판교역", 3),
                ),
                1
            )
            3L -> Triple(
                "지석묘 앞 → 인문경상관 앞 (상행)",
                listOf(
                    BusStop(1, "지석묘 앞", 0),
                    BusStop(2, "기숙사 사거리", 1),
                    BusStop(3, "도서관 앞", 2),
                    BusStop(4, "인문경상관 앞", 3),
                ),
                1
            )
            4L -> Triple(
                "인문경상관 앞 → 지석묘 앞 (하행)",
                listOf(
                    BusStop(1, "인문경상관 회차장 앞", 0),
                    BusStop(2, "교양관 앞", 1),
                    BusStop(3, "후생복지관 앞", 2),
                    BusStop(4, "공학관 앞", 3),
                    BusStop(5, "백년관 앞", 4),
                    BusStop(6, "국제사회교육원 앞", 5),
                    BusStop(7, "지석묘 앞", 6),
                ),
                2
            )
            else -> Triple("", emptyList(), 0)
        }

        return BusLocationInfo(
            busId = scheduleId,
            routeId = routeId,
            routeName = routeName,
            currentStopIndex = currentIdx,
            currentStopName = stops.getOrNull(currentIdx)?.name ?: "",
            stops = stops,
            departureTime = "08:30",
            arrivalTime = "08:52",
            remainingSeats = 32,
            totalSeats = 45,
            status = BusStatus.IN_OPERATION
        )
    }
}
