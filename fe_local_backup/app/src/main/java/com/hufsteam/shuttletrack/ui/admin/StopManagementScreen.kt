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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StopManagementScreen(
    adminViewModel: AdminViewModel,
    onBack: () -> Unit
) {
    LaunchedEffect(Unit) { adminViewModel.loadStops() }

    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(adminViewModel.successMessage) {
        adminViewModel.successMessage?.let { snackbarHostState.showSnackbar(it); adminViewModel.dismissSuccess() }
    }
    LaunchedEffect(adminViewModel.errorMessage) {
        adminViewModel.errorMessage?.let { snackbarHostState.showSnackbar(it); adminViewModel.dismissError() }
    }

    var showAddDialog by remember { mutableStateOf(false) }
    var editTarget    by remember { mutableStateOf<StopItem?>(null) }
    var deleteTarget  by remember { mutableStateOf<StopItem?>(null) }

    if (showAddDialog) {
        StopEditDialog(
            title     = "정류장 추가",
            initial   = "",
            onConfirm = { name -> adminViewModel.addStop(name); showAddDialog = false },
            onDismiss = { showAddDialog = false }
        )
    }
    editTarget?.let { target ->
        StopEditDialog(
            title     = "정류장 수정",
            initial   = target.stopName,
            onConfirm = { name -> adminViewModel.updateStop(target.id, name); editTarget = null },
            onDismiss = { editTarget = null }
        )
    }
    deleteTarget?.let { target ->
        ConfirmDialog(
            message   = "'${target.stopName}' 정류장을 삭제하시겠습니까?",
            onConfirm = { adminViewModel.deleteStop(target.id); deleteTarget = null },
            onDismiss = { deleteTarget = null }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("정류장 관리", color = Color.White) },
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
                Icon(Icons.Filled.Add, "정류장 추가")
            }
        },
        snackbarHost   = { SnackbarHost(snackbarHostState) },
        containerColor = Color.White
    ) { padding ->
        if (adminViewModel.isLoading) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = NavyBlue)
            }
        } else if (adminViewModel.stops.isEmpty()) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Text("등록된 정류장이 없습니다\n+ 버튼으로 추가하세요", color = Color(0xFF999999), fontSize = 14.sp)
            }
        } else {
            LazyColumn(
                modifier            = Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding      = PaddingValues(vertical = 16.dp)
            ) {
                items(adminViewModel.stops, key = { it.id }) { stop ->
                    StopCard(
                        stop     = stop,
                        onEdit   = { editTarget = stop },
                        onDelete = { deleteTarget = stop }
                    )
                }
            }
        }
    }
}

@Composable
private fun StopCard(stop: StopItem, onEdit: () -> Unit, onDelete: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .border(1.dp, Color(0xFFE0E0E0), RoundedCornerShape(10.dp))
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(stop.stopName, fontSize = 15.sp, fontWeight = FontWeight.Medium, modifier = Modifier.weight(1f))
        IconButton(onClick = onEdit,   modifier = Modifier.size(32.dp)) {
            Icon(Icons.Filled.Edit,   "수정", tint = NavyBlue,          modifier = Modifier.size(18.dp))
        }
        IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
            Icon(Icons.Filled.Delete, "삭제", tint = Color(0xFFCC2200), modifier = Modifier.size(18.dp))
        }
    }
}

@Composable
private fun StopEditDialog(
    title: String,
    initial: String,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var name by remember { mutableStateOf(initial) }

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
                value         = name,
                onValueChange = { name = it },
                label         = { Text("정류장명") },
                singleLine    = true,
                shape         = RoundedCornerShape(8.dp),
                modifier      = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(20.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = onDismiss, modifier = Modifier.weight(1f), shape = RoundedCornerShape(8.dp)) {
                    Text("취소")
                }
                Button(
                    onClick  = { onConfirm(name) },
                    modifier = Modifier.weight(1f),
                    shape    = RoundedCornerShape(8.dp),
                    colors   = ButtonDefaults.buttonColors(containerColor = NavyBlue)
                ) { Text("저장", color = Color.White) }
            }
        }
    }
}
