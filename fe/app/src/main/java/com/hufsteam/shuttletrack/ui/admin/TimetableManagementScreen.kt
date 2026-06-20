package com.hufsteam.shuttletrack.ui.admin

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.hufsteam.shuttletrack.ui.theme.NavyBlue

private val DAY_TYPES = listOf("평일", "주말", "전체")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimetableManagementScreen(
    adminViewModel: AdminViewModel,
    onBack: () -> Unit
) {
    LaunchedEffect(Unit) {
        adminViewModel.loadTimetable()
        adminViewModel.loadRoutes()
    }

    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(adminViewModel.successMessage) {
        adminViewModel.successMessage?.let { snackbarHostState.showSnackbar(it); adminViewModel.dismissSuccess() }
    }
    LaunchedEffect(adminViewModel.errorMessage) {
        adminViewModel.errorMessage?.let { snackbarHostState.showSnackbar(it); adminViewModel.dismissError() }
    }

    var showAddDialog by remember { mutableStateOf(false) }
    var editTarget    by remember { mutableStateOf<TimetableItem?>(null) }
    var deleteTarget  by remember { mutableStateOf<TimetableItem?>(null) }

    if (showAddDialog) {
        TimetableAddDialog(
            routes    = adminViewModel.routes,
            onConfirm = { routeId, routeName, time, dayType ->
                adminViewModel.addTimetable(routeId, routeName, time, dayType)
                showAddDialog = false
            },
            onDismiss = { showAddDialog = false }
        )
    }
    editTarget?.let { target ->
        TimetableEditDialog(
            initial   = target,
            onConfirm = { time, dayType -> adminViewModel.updateTimetable(target.id, time, dayType); editTarget = null },
            onDismiss = { editTarget = null }
        )
    }
    deleteTarget?.let { target ->
        ConfirmDialog(
            message   = "'${target.routeName} ${target.scheduledTime}' 항목을 삭제하시겠습니까?",
            onConfirm = { adminViewModel.deleteTimetable(target.id); deleteTarget = null },
            onDismiss = { deleteTarget = null }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("시간표 관리", color = Color.White) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = NavyBlue),
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "뒤로가기", tint = Color.White)
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { showAddDialog = true }, containerColor = NavyBlue, contentColor = Color.White) {
                Icon(Icons.Filled.Add, "시간표 추가")
            }
        },
        snackbarHost   = { SnackbarHost(snackbarHostState) },
        containerColor = Color.White
    ) { padding ->
        if (adminViewModel.isLoading) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = NavyBlue)
            }
        } else if (adminViewModel.timetables.isEmpty()) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Text("등록된 시간표가 없습니다\n+ 버튼으로 추가하세요", color = Color(0xFF999999), fontSize = 14.sp)
            }
        } else {
            // 노선별 그룹핑
            val grouped = adminViewModel.timetables.groupBy { it.routeName }
            LazyColumn(
                modifier            = Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding      = PaddingValues(vertical = 16.dp)
            ) {
                grouped.forEach { (routeName, items) ->
                    item {
                        Text(
                            routeName,
                            fontSize   = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color      = NavyBlue,
                            modifier   = Modifier.padding(top = 8.dp, bottom = 4.dp)
                        )
                    }
                    items(items, key = { it.id }) { item ->
                        TimetableCard(
                            item     = item,
                            onEdit   = { editTarget = item },
                            onDelete = { deleteTarget = item }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun TimetableCard(item: TimetableItem, onEdit: () -> Unit, onDelete: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .border(1.dp, Color(0xFFE0E0E0), RoundedCornerShape(10.dp))
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(item.scheduledTime, fontSize = 16.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.width(56.dp))
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(4.dp))
                .background(
                    when (item.dayType) {
                        "평일" -> Color(0xFFE3F2FD)
                        "주말" -> Color(0xFFFCE4EC)
                        else  -> Color(0xFFEDE7F6)
                    }
                )
                .padding(horizontal = 8.dp, vertical = 2.dp)
        ) {
            Text(
                item.dayType, fontSize = 11.sp,
                color = when (item.dayType) {
                    "평일" -> Color(0xFF1565C0)
                    "주말" -> Color(0xFFC62828)
                    else  -> Color(0xFF4527A0)
                }
            )
        }
        Spacer(Modifier.weight(1f))
        IconButton(onClick = onEdit,   modifier = Modifier.size(32.dp)) {
            Icon(Icons.Filled.Edit,   "수정", tint = NavyBlue,          modifier = Modifier.size(18.dp))
        }
        IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
            Icon(Icons.Filled.Delete, "삭제", tint = Color(0xFFCC2200), modifier = Modifier.size(18.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TimetableAddDialog(
    routes: List<RouteItem>,
    onConfirm: (String, String, String, String) -> Unit,
    onDismiss: () -> Unit
) {
    var selectedRoute by remember { mutableStateOf<RouteItem?>(null) }
    var showRouteMenu by remember { mutableStateOf(false) }
    var timeInput     by remember { mutableStateOf("") }
    var dayType       by remember { mutableStateOf("평일") }

    Dialog(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier.clip(RoundedCornerShape(16.dp)).background(Color.White).padding(24.dp)
        ) {
            Text("시간표 추가", fontSize = 17.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(16.dp))

            // 노선 선택
            ExposedDropdownMenuBox(expanded = showRouteMenu, onExpandedChange = { showRouteMenu = it }) {
                OutlinedTextField(
                    value         = selectedRoute?.routeName ?: "노선 선택",
                    onValueChange = {},
                    readOnly      = true,
                    label         = { Text("노선") },
                    trailingIcon  = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = showRouteMenu) },
                    shape         = RoundedCornerShape(8.dp),
                    modifier      = Modifier.fillMaxWidth().menuAnchor()
                )
                ExposedDropdownMenu(expanded = showRouteMenu, onDismissRequest = { showRouteMenu = false }) {
                    routes.forEach { route ->
                        DropdownMenuItem(
                            text    = { Text(route.routeName) },
                            onClick = { selectedRoute = route; showRouteMenu = false }
                        )
                    }
                }
            }
            Spacer(Modifier.height(12.dp))

            // 시간 입력
            OutlinedTextField(
                value         = timeInput,
                onValueChange = { timeInput = it },
                label         = { Text("출발 시간 (HH:mm)") },
                placeholder   = { Text("예: 08:30") },
                singleLine    = true,
                shape         = RoundedCornerShape(8.dp),
                modifier      = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(12.dp))

            // 운행 유형
            Text("운행 유형", fontSize = 13.sp, color = Color(0xFF555555))
            Spacer(Modifier.height(6.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                DAY_TYPES.forEach { type ->
                    FilterChip(
                        selected = dayType == type,
                        onClick  = { dayType = type },
                        label    = { Text(type) },
                        colors   = FilterChipDefaults.filterChipColors(
                            selectedContainerColor    = NavyBlue,
                            selectedLabelColor        = Color.White
                        )
                    )
                }
            }

            Spacer(Modifier.height(20.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = onDismiss, modifier = Modifier.weight(1f), shape = RoundedCornerShape(8.dp)) {
                    Text("취소")
                }
                Button(
                    onClick  = {
                        val route = selectedRoute
                        if (route != null) onConfirm(route.id, route.routeName, timeInput, dayType)
                    },
                    modifier = Modifier.weight(1f),
                    shape    = RoundedCornerShape(8.dp),
                    colors   = ButtonDefaults.buttonColors(containerColor = NavyBlue)
                ) { Text("저장", color = Color.White) }
            }
        }
    }
}

@Composable
private fun TimetableEditDialog(
    initial: TimetableItem,
    onConfirm: (String, String) -> Unit,
    onDismiss: () -> Unit
) {
    var timeInput by remember { mutableStateOf(initial.scheduledTime) }
    var dayType   by remember { mutableStateOf(initial.dayType) }

    Dialog(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier.clip(RoundedCornerShape(16.dp)).background(Color.White).padding(24.dp)
        ) {
            Text("시간표 수정", fontSize = 17.sp, fontWeight = FontWeight.Bold)
            Text(initial.routeName, fontSize = 13.sp, color = Color(0xFF777777))
            Spacer(Modifier.height(16.dp))

            OutlinedTextField(
                value         = timeInput,
                onValueChange = { timeInput = it },
                label         = { Text("출발 시간 (HH:mm)") },
                singleLine    = true,
                shape         = RoundedCornerShape(8.dp),
                modifier      = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(12.dp))

            Text("운행 유형", fontSize = 13.sp, color = Color(0xFF555555))
            Spacer(Modifier.height(6.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                DAY_TYPES.forEach { type ->
                    FilterChip(
                        selected = dayType == type,
                        onClick  = { dayType = type },
                        label    = { Text(type) },
                        colors   = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = NavyBlue,
                            selectedLabelColor     = Color.White
                        )
                    )
                }
            }
            Spacer(Modifier.height(20.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = onDismiss, modifier = Modifier.weight(1f), shape = RoundedCornerShape(8.dp)) {
                    Text("취소")
                }
                Button(
                    onClick  = { onConfirm(timeInput, dayType) },
                    modifier = Modifier.weight(1f),
                    shape    = RoundedCornerShape(8.dp),
                    colors   = ButtonDefaults.buttonColors(containerColor = NavyBlue)
                ) { Text("저장", color = Color.White) }
            }
        }
    }
}
