package com.hufsteam.shuttletrack.ui.driver

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hufsteam.shuttletrack.ui.common.BusIcon
import com.hufsteam.shuttletrack.ui.theme.NavyBlue

private val DriverAccentRed = Color(0xFFB83A25)
private val DriverSoftGray = Color(0xFFF4F6F8)

// ── 메인 화면 ─────────────────────────────────────────────────

@Composable
fun DriverOperationScreen(
    driverViewModel: DriverViewModel,
    onBack: () -> Unit
) {
    val route = driverViewModel.selectedRoute ?: return
    val state = driverViewModel.operationState
    val passengers = driverViewModel.passengerCount
    val total = route.totalSeats
    val remainingSeats = (total - passengers).coerceAtLeast(0)
    val actualTime = driverViewModel.actualDepartureTime
    val currentStop = driverViewModel.currentStopIndex
    val busStatus = driverViewModel.busStatus

    var showStatusMenu by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .background(Color.White)
    ) {
        // ── 상단 헤더 ──────────────────────────────────────────
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp)
        ) {
            Icon(
                Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "뒤로가기",
                tint     = Color.Black,
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .size(24.dp)
                    .clickable { onBack() }
            )
            Text(
                text      = route.routeName,
                fontSize  = 16.sp,
                fontWeight = FontWeight.SemiBold,
                color     = Color.Black,
                modifier  = Modifier.align(Alignment.Center)
            )
        }

        Column(
            modifier            = Modifier
                .weight(1f)
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(24.dp))

            // ── 버스 아이콘 ────────────────────────────────────
            BusIcon(size = 58.dp)

            Spacer(Modifier.height(20.dp))

            // ── 출발 시간 행 ───────────────────────────────────
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(31.dp)
                    .clip(RoundedCornerShape(50.dp))
                    .border(1.4.dp, Color(0xFFD8DEE8), RoundedCornerShape(50.dp))
                    .padding(horizontal = 20.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(
                    modifier            = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        "출발 계획 | ${route.scheduledTime}",
                        fontSize = 12.sp,
                        color    = Color(0xFF444444)
                    )
                    Text(
                        "실제 출발 | ${actualTime ?: "미정"}",
                        fontSize = 12.sp,
                        color    = Color(0xFF444444)
                    )
                }
            }

            Spacer(Modifier.height(32.dp))

            when (state) {
                // ── 출발 전 ────────────────────────────────────
                OperationState.BEFORE_DEPARTURE -> {
                    BeforeDepartureContent(
                        passengers  = passengers,
                        total       = total,
                        remainingSeats = remainingSeats
                    )
                }

                // ── 운행 중 ────────────────────────────────────
                OperationState.OPERATING -> {
                    OperatingContent(
                        passengers       = passengers,
                        total            = total,
                        remainingSeats   = remainingSeats,
                        stops            = route.stops,
                        currentStopIndex = currentStop,
                        busStatus        = busStatus,
                        showStatusMenu   = showStatusMenu,
                        onShowMenu       = { showStatusMenu = true },
                        onDismissMenu    = { showStatusMenu = false },
                        onStatusSelect   = { s ->
                            driverViewModel.updateStatus(s)
                            showStatusMenu = false
                        },
                        onIncrease       = { driverViewModel.increasePassengers() },
                        onDecrease       = { driverViewModel.decreasePassengers() },
                        onAdvanceStop    = { driverViewModel.advanceStop() }
                    )
                }

                // ── 운행 완료 ──────────────────────────────────
                OperationState.COMPLETED -> {
                    CompletedContent(
                        passengers = passengers,
                        total = total,
                        remainingSeats = remainingSeats
                    )
                }
            }
        }

        // ── 하단 버튼 ──────────────────────────────────────────
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 20.dp),
            contentAlignment = Alignment.Center
        ) {
            when (state) {
                OperationState.BEFORE_DEPARTURE -> {
                    OperationButton(
                        text    = "출발 등록하기",
                        enabled = true,
                        onClick = { driverViewModel.startOperation() }
                    )
                }
                OperationState.OPERATING -> {
                    OperationButton(
                        text    = "운행 종료하기",
                        enabled = true,
                        onClick = { driverViewModel.endOperation() }
                    )
                }
                OperationState.COMPLETED -> {
                    OperationButton(
                        text    = "운행 종료 완료",
                        enabled = false,
                        onClick = {}
                    )
                }
            }
        }

        // ── 하단 바 (시간표 / 즐겨찾기 / 마이페이지) ──────────
        BottomNavBar(
            selected      = BottomTab.Timetable,
            onTabSelected = {}
        )
    }
}

// ── 출발 전 콘텐츠 ─────────────────────────────────────────────

@Composable
private fun BeforeDepartureContent(passengers: Int, total: Int, remainingSeats: Int) {
    SeatSummaryContent(
        passengers = passengers,
        total = total,
        remainingSeats = remainingSeats
    )
}

// ── 운행 중 콘텐츠 ─────────────────────────────────────────────

@Composable
private fun OperatingContent(
    passengers: Int,
    total: Int,
    remainingSeats: Int,
    stops: List<String>,
    currentStopIndex: Int,
    busStatus: BusStatus,
    showStatusMenu: Boolean,
    onShowMenu: () -> Unit,
    onDismissMenu: () -> Unit,
    onStatusSelect: (BusStatus) -> Unit,
    onIncrease: () -> Unit,
    onDecrease: () -> Unit,
    onAdvanceStop: () -> Unit
) {
    // 혼잡도 상태 배지
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        val (badgeColor, textColor) = when (busStatus) {
            BusStatus.NORMAL    -> Color(0xFFE8F5E9) to Color(0xFF2E7D32)
            BusStatus.DELAYED   -> Color(0xFFFFF3E0) to Color(0xFFE65100)
            BusStatus.SUSPENDED -> Color(0xFFFFEBEE) to Color(0xFFC62828)
        }
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(50.dp))
                .background(badgeColor)
                .padding(horizontal = 12.dp, vertical = 4.dp)
        ) {
            Text(busStatus.label, color = textColor, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
        }

        // 상태 변경 버튼
        Box {
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(50.dp))
                    .border(1.dp, Color(0xFFDDDDDD), RoundedCornerShape(50.dp))
                    .clickable { onShowMenu() }
                    .padding(horizontal = 12.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("상태 변경", fontSize = 13.sp, color = Color(0xFF555555))
                Icon(Icons.Filled.KeyboardArrowDown, null, tint = Color(0xFF555555), modifier = Modifier.size(16.dp))
            }
            DropdownMenu(expanded = showStatusMenu, onDismissRequest = onDismissMenu) {
                BusStatus.values().forEach { s ->
                    DropdownMenuItem(
                        text    = { Text(s.label) },
                        onClick = { onStatusSelect(s) }
                    )
                }
            }
        }
    }

    Spacer(Modifier.height(22.dp))

    SeatSummaryContent(
        passengers = passengers,
        total = total,
        remainingSeats = remainingSeats
    )

    Spacer(Modifier.height(18.dp))
    PassengerStepper(
        passengers = passengers,
        total = total,
        onIncrease = onIncrease,
        onDecrease = onDecrease
    )

    Spacer(Modifier.height(28.dp))

    // ── 정류장 진행 바 ─────────────────────────────────────────
    StopProgressBar(stops = stops, currentIndex = currentStopIndex)

    Spacer(Modifier.height(16.dp))

    // 정류장 출발 버튼 (마지막 정류장이 아닐 때만)
    if (currentStopIndex < stops.size - 1) {
        OutlinedButton(
            onClick = onAdvanceStop,
            shape   = RoundedCornerShape(8.dp),
            colors  = ButtonDefaults.outlinedButtonColors(contentColor = NavyBlue),
            border  = ButtonDefaults.outlinedButtonBorder,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("다음 정류장 출발 →", fontSize = 14.sp)
        }
    }
}

// ── 운행 완료 콘텐츠 ────────────────────────────────────────────

@Composable
private fun CompletedContent(passengers: Int, total: Int, remainingSeats: Int) {
    Spacer(Modifier.height(16.dp))
    SeatSummaryContent(
        passengers = passengers,
        total = total,
        remainingSeats = remainingSeats
    )
    Spacer(Modifier.height(24.dp))
    Text(
        "운행이 완료되었습니다",
        fontSize  = 16.sp,
        color     = Color(0xFF888888),
        textAlign = TextAlign.Center
    )
}

@Composable
private fun SeatSummaryContent(
    passengers: Int,
    total: Int,
    remainingSeats: Int
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            "탑승 수",
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = DriverAccentRed
        )
        Spacer(Modifier.height(6.dp))
        Text(
            "${passengers.toString().padStart(2, '0')}/$total",
            fontSize = 48.sp,
            fontWeight = FontWeight.Bold,
            color = DriverAccentRed,
            lineHeight = 48.sp
        )
        Spacer(Modifier.height(12.dp))
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(50.dp))
                .background(DriverSoftGray)
                .padding(horizontal = 18.dp, vertical = 7.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                "남은 여석 ${remainingSeats}석",
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = NavyBlue
            )
        }
    }
}

@Composable
private fun PassengerStepper(
    passengers: Int,
    total: Int,
    onIncrease: () -> Unit,
    onDecrease: () -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
        modifier = Modifier.fillMaxWidth()
    ) {
        SeatAdjustButton(text = "−", enabled = passengers > 0, filled = false, onClick = onDecrease)
        Spacer(Modifier.width(14.dp))
        Text(
            "탑승 인원 조절",
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            color = Color(0xFF666666)
        )
        Spacer(Modifier.width(14.dp))
        SeatAdjustButton(text = "+", enabled = passengers < total, filled = true, onClick = onIncrease)
    }
}

@Composable
private fun SeatAdjustButton(
    text: String,
    enabled: Boolean,
    filled: Boolean,
    onClick: () -> Unit
) {
    val background = when {
        !enabled -> Color(0xFFE6E8EC)
        filled -> NavyBlue
        else -> Color.White
    }
    val foreground = when {
        !enabled -> Color(0xFF9AA1AB)
        filled -> Color.White
        else -> NavyBlue
    }
    Box(
        modifier = Modifier
            .size(34.dp)
            .clip(CircleShape)
            .background(background)
            .border(1.2.dp, if (filled || !enabled) background else NavyBlue, CircleShape)
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(text, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = foreground)
    }
}

// ── 정류장 진행 바 ─────────────────────────────────────────────

@Composable
private fun StopProgressBar(stops: List<String>, currentIndex: Int) {
    Column {
        Row(
            modifier            = Modifier.fillMaxWidth(),
            verticalAlignment   = Alignment.CenterVertically
        ) {
            stops.forEachIndexed { index, _ ->
                val isPassed  = index < currentIndex
                val isCurrent = index == currentIndex
                val circleColor = when {
                    isCurrent -> Color(0xFFCC2200)
                    isPassed  -> NavyBlue
                    else      -> Color.White
                }
                val borderColor = if (isPassed || isCurrent) NavyBlue else Color(0xFFCCCCCC)

                // 왼쪽 연결선
                if (index > 0) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(3.dp)
                            .background(if (index <= currentIndex) NavyBlue else Color(0xFFCCCCCC))
                    )
                }

                // 원
                Box(
                    modifier = Modifier
                        .size(if (isCurrent) 22.dp else 18.dp)
                        .clip(CircleShape)
                        .background(circleColor)
                        .border(2.dp, borderColor, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    if (isCurrent) {
                        // 현재 위치: 핀 모양 대신 강조 원
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(Color.White)
                        )
                    }
                }
            }
        }

        Spacer(Modifier.height(6.dp))

        // 정류장 이름
        Row(modifier = Modifier.fillMaxWidth()) {
            stops.forEachIndexed { index, stop ->
                if (index > 0) Spacer(Modifier.weight(1f))
                val isCurrent = index == currentIndex
                Text(
                    text      = stop,
                    fontSize  = 11.sp,
                    color     = if (isCurrent) Color(0xFFCC2200) else Color(0xFF555555),
                    fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Normal,
                    textAlign = if (index == 0) TextAlign.Start
                                else if (index == stops.size - 1) TextAlign.End
                                else TextAlign.Center,
                    modifier  = Modifier.width(60.dp)
                )
            }
        }
    }
}

// ── 공통 버튼 ──────────────────────────────────────────────────

@Composable
private fun OperationButton(text: String, enabled: Boolean, onClick: () -> Unit) {
    Button(
        onClick  = onClick,
        enabled  = enabled,
        shape    = RoundedCornerShape(7.dp),
        colors   = ButtonDefaults.buttonColors(
            containerColor         = NavyBlue,
            disabledContainerColor = Color(0xFFF0F1F3)
        ),
        modifier = Modifier
            .width(150.dp)
            .height(36.dp)
    ) {
        Text(
            text,
            fontSize   = 12.sp,
            fontWeight = FontWeight.SemiBold,
            color      = if (enabled) Color.White else Color(0xFF9AA1AB)
        )
    }
}
