package com.hufsteam.shuttletrack.ui.admin

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.hufsteam.shuttletrack.ui.theme.NavyBlue

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RouteManagementScreen(
    adminViewModel: AdminViewModel,
    onBack: () -> Unit
) {
    LaunchedEffect(Unit) { adminViewModel.loadRoutes() }

    val snackbarHostState = remember { SnackbarHostState() }
    val successMsg = adminViewModel.successMessage
    val errorMsg   = adminViewModel.errorMessage

    LaunchedEffect(successMsg) {
        if (successMsg != null) { snackbarHostState.showSnackbar(successMsg); adminViewModel.dismissSuccess() }
    }
    LaunchedEffect(errorMsg) {
        if (errorMsg != null) { snackbarHostState.showSnackbar(errorMsg); adminViewModel.dismissError() }
    }

    var showAddDialog  by remember { mutableStateOf(false) }
    var editTarget     by remember { mutableStateOf<RouteItem?>(null) }
    var deleteTarget   by remember { mutableStateOf<RouteItem?>(null) }

    if (showAddDialog) {
        RouteEditDialog(
            title    = "노선 추가",
            initial  = RouteItem(),
            onConfirm = { name, stops -> adminViewModel.addRoute(name, stops); showAddDialog = false },
            onDismiss = { showAddDialog = false }
        )
    }
    editTarget?.let { target ->
        RouteEditDialog(
            title     = "노선 수정",
            initial   = target,
            onConfirm = { name, stops -> adminViewModel.updateRoute(target.id, name, stops); editTarget = null },
            onDismiss = { editTarget = null }
        )
    }
    deleteTarget?.let { target ->
        ConfirmDialog(
            message   = "'${target.routeName}' 노선을 삭제하시겠습니까?",
            onConfirm = { adminViewModel.deleteRoute(target.id); deleteTarget = null },
            onDismiss = { deleteTarget = null }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("노선 관리", color = Color.White) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = NavyBlue),
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "뒤로가기", tint = Color.White)
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick           = { showAddDialog = true },
                containerColor    = NavyBlue,
                contentColor      = Color.White
            ) { Icon(Icons.Filled.Add, "노선 추가") }
        },
        snackbarHost    = { SnackbarHost(snackbarHostState) },
        containerColor  = Color.White
    ) { padding ->
        if (adminViewModel.isLoading) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = NavyBlue)
            }
        } else if (adminViewModel.routes.isEmpty()) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Text("등록된 노선이 없습니다\n+ 버튼으로 추가하세요", color = Color(0xFF999999), fontSize = 14.sp)
            }
        } else {
            LazyColumn(
                modifier            = Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding      = PaddingValues(vertical = 16.dp)
            ) {
                items(adminViewModel.routes, key = { it.id }) { route ->
                    RouteCard(
                        route     = route,
                        onEdit    = { editTarget = route },
                        onDelete  = { deleteTarget = route }
                    )
                }
            }
        }
    }
}

@Composable
private fun RouteCard(route: RouteItem, onEdit: () -> Unit, onDelete: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .border(1.dp, Color(0xFFE0E0E0), RoundedCornerShape(10.dp))
            .padding(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(route.routeName, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
            IconButton(onClick = onEdit,   modifier = Modifier.size(32.dp)) {
                Icon(Icons.Filled.Edit,   "수정", tint = NavyBlue,          modifier = Modifier.size(18.dp))
            }
            IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
                Icon(Icons.Filled.Delete, "삭제", tint = Color(0xFFCC2200), modifier = Modifier.size(18.dp))
            }
        }
        if (route.stops.isNotEmpty()) {
            Spacer(Modifier.height(6.dp))
            Text(
                route.stops.joinToString(" → "),
                fontSize = 12.sp, color = Color(0xFF777777)
            )
        }
    }
}

@Composable
private fun RouteEditDialog(
    title: String,
    initial: RouteItem,
    onConfirm: (String, List<String>) -> Unit,
    onDismiss: () -> Unit
) {
    var routeName  by remember { mutableStateOf(initial.routeName) }
    var stops      by remember { mutableStateOf(initial.stops.toMutableList()) }
    var newStop    by remember { mutableStateOf("") }

    Dialog(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .clip(RoundedCornerShape(16.dp))
                .background(Color.White)
                .padding(24.dp)
        ) {
            Text(title, fontSize = 17.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(16.dp))

            OutlinedTextField(
                value         = routeName,
                onValueChange = { routeName = it },
                label         = { Text("노선명") },
                singleLine    = true,
                shape         = RoundedCornerShape(8.dp),
                modifier      = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(12.dp))

            Text("정류장 목록", fontSize = 13.sp, color = Color(0xFF555555), fontWeight = FontWeight.Medium)
            Spacer(Modifier.height(6.dp))

            stops.forEachIndexed { index, stop ->
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 2.dp)) {
                    Text("${index + 1}.", fontSize = 13.sp, color = Color(0xFF555555), modifier = Modifier.width(24.dp))
                    Text(stop, fontSize = 13.sp, modifier = Modifier.weight(1f))
                    Icon(
                        Icons.Filled.Close, "삭제", tint = Color(0xFF999999),
                        modifier = Modifier.size(18.dp).clickable { stops = stops.toMutableList().also { it.removeAt(index) } }
                    )
                }
            }

            Spacer(Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    value         = newStop,
                    onValueChange = { newStop = it },
                    placeholder   = { Text("정류장 추가") },
                    singleLine    = true,
                    shape         = RoundedCornerShape(8.dp),
                    modifier      = Modifier.weight(1f),
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words)
                )
                Spacer(Modifier.width(8.dp))
                IconButton(
                    onClick = { if (newStop.isNotBlank()) { stops = stops.toMutableList().also { it.add(newStop.trim()) }; newStop = "" } }
                ) { Icon(Icons.Filled.Add, "추가", tint = NavyBlue) }
            }

            Spacer(Modifier.height(20.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = onDismiss, modifier = Modifier.weight(1f), shape = RoundedCornerShape(8.dp)) {
                    Text("취소")
                }
                Button(
                    onClick  = { onConfirm(routeName, stops.toList()) },
                    modifier = Modifier.weight(1f),
                    shape    = RoundedCornerShape(8.dp),
                    colors   = ButtonDefaults.buttonColors(containerColor = NavyBlue)
                ) { Text("저장", color = Color.White) }
            }
        }
    }
}
