package com.hufsteam.shuttletrack.ui.admin

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.Timestamp
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

// ── 데이터 클래스 ──────────────────────────────────────────────

data class RouteItem(
    val id: String = "",
    val routeName: String = "",
    val stops: List<String> = emptyList()
)

data class StopItem(
    val id: String = "",
    val stopName: String = ""
)

data class TimetableItem(
    val id: String = "",
    val routeId: String = "",
    val routeName: String = "",
    val scheduledTime: String = "",
    val dayType: String = "평일"
)

// ── ViewModel ─────────────────────────────────────────────────

class AdminViewModel : ViewModel() {

    private val db = Firebase.firestore

    // ── 상태 ──────────────────────────────────────────────────
    var routes      by mutableStateOf<List<RouteItem>>(emptyList());     private set
    var stops       by mutableStateOf<List<StopItem>>(emptyList());      private set
    var timetables  by mutableStateOf<List<TimetableItem>>(emptyList()); private set
    var isLoading   by mutableStateOf(false);                            private set
    var errorMessage by mutableStateOf<String?>(null);                   private set
    var successMessage by mutableStateOf<String?>(null);                 private set

    // ═══════════════════════════════════════════════════════════
    //  노선 (routes)
    // ═══════════════════════════════════════════════════════════

    fun loadRoutes() {
        isLoading = true
        db.collection("routes")
            .addSnapshotListener { snap, e ->
                isLoading = false
                if (e != null) { errorMessage = "노선 로드 실패: ${e.localizedMessage}"; return@addSnapshotListener }
                routes = snap?.documents?.map { doc ->
                    RouteItem(
                        id        = doc.id,
                        routeName = doc.getString("routeName") ?: "",
                        stops     = (doc.get("stops") as? List<*>)?.filterIsInstance<String>() ?: emptyList()
                    )
                } ?: emptyList()
            }
    }

    fun addRoute(routeName: String, stops: List<String>) {
        if (routeName.isBlank()) { errorMessage = "노선명을 입력하세요"; return }
        viewModelScope.launch {
            try {
                db.collection("routes").add(
                    mapOf("routeName" to routeName.trim(), "stops" to stops, "createdAt" to Timestamp.now())
                ).await()
                successMessage = "노선이 추가되었습니다"
            } catch (e: Exception) { errorMessage = "추가 실패: ${e.localizedMessage}" }
        }
    }

    fun updateRoute(id: String, routeName: String, stops: List<String>) {
        if (routeName.isBlank()) { errorMessage = "노선명을 입력하세요"; return }
        viewModelScope.launch {
            try {
                db.collection("routes").document(id)
                    .update(mapOf("routeName" to routeName.trim(), "stops" to stops)).await()
                successMessage = "노선이 수정되었습니다"
            } catch (e: Exception) { errorMessage = "수정 실패: ${e.localizedMessage}" }
        }
    }

    fun deleteRoute(id: String) {
        viewModelScope.launch {
            try {
                db.collection("routes").document(id).delete().await()
                successMessage = "노선이 삭제되었습니다"
            } catch (e: Exception) { errorMessage = "삭제 실패: ${e.localizedMessage}" }
        }
    }

    // ═══════════════════════════════════════════════════════════
    //  정류장 (stops)
    // ═══════════════════════════════════════════════════════════

    fun loadStops() {
        isLoading = true
        db.collection("stops")
            .addSnapshotListener { snap, e ->
                isLoading = false
                if (e != null) { errorMessage = "정류장 로드 실패: ${e.localizedMessage}"; return@addSnapshotListener }
                stops = snap?.documents?.map { doc ->
                    StopItem(id = doc.id, stopName = doc.getString("stopName") ?: "")
                } ?: emptyList()
            }
    }

    fun addStop(stopName: String) {
        if (stopName.isBlank()) { errorMessage = "정류장명을 입력하세요"; return }
        viewModelScope.launch {
            try {
                db.collection("stops").add(
                    mapOf("stopName" to stopName.trim(), "createdAt" to Timestamp.now())
                ).await()
                successMessage = "정류장이 추가되었습니다"
            } catch (e: Exception) { errorMessage = "추가 실패: ${e.localizedMessage}" }
        }
    }

    fun updateStop(id: String, stopName: String) {
        if (stopName.isBlank()) { errorMessage = "정류장명을 입력하세요"; return }
        viewModelScope.launch {
            try {
                db.collection("stops").document(id).update("stopName", stopName.trim()).await()
                successMessage = "정류장이 수정되었습니다"
            } catch (e: Exception) { errorMessage = "수정 실패: ${e.localizedMessage}" }
        }
    }

    fun deleteStop(id: String) {
        viewModelScope.launch {
            try {
                db.collection("stops").document(id).delete().await()
                successMessage = "정류장이 삭제되었습니다"
            } catch (e: Exception) { errorMessage = "삭제 실패: ${e.localizedMessage}" }
        }
    }

    // ═══════════════════════════════════════════════════════════
    //  시간표 (timetable)
    // ═══════════════════════════════════════════════════════════

    fun loadTimetable() {
        isLoading = true
        db.collection("timetable")
            .addSnapshotListener { snap, e ->
                isLoading = false
                if (e != null) { errorMessage = "시간표 로드 실패: ${e.localizedMessage}"; return@addSnapshotListener }
                timetables = snap?.documents?.map { doc ->
                    TimetableItem(
                        id            = doc.id,
                        routeId       = doc.getString("routeId") ?: "",
                        routeName     = doc.getString("routeName") ?: "",
                        scheduledTime = doc.getString("scheduledTime") ?: "",
                        dayType       = doc.getString("dayType") ?: "평일"
                    )
                }?.sortedWith(compareBy({ it.routeName }, { it.scheduledTime })) ?: emptyList()
            }
    }

    fun addTimetable(routeId: String, routeName: String, scheduledTime: String, dayType: String) {
        if (routeId.isBlank() || scheduledTime.isBlank()) { errorMessage = "노선과 시간을 모두 입력하세요"; return }
        viewModelScope.launch {
            try {
                db.collection("timetable").add(
                    mapOf(
                        "routeId"       to routeId,
                        "routeName"     to routeName,
                        "scheduledTime" to scheduledTime,
                        "dayType"       to dayType,
                        "createdAt"     to Timestamp.now()
                    )
                ).await()
                successMessage = "시간표가 추가되었습니다"
            } catch (e: Exception) { errorMessage = "추가 실패: ${e.localizedMessage}" }
        }
    }

    fun updateTimetable(id: String, scheduledTime: String, dayType: String) {
        if (scheduledTime.isBlank()) { errorMessage = "시간을 입력하세요"; return }
        viewModelScope.launch {
            try {
                db.collection("timetable").document(id)
                    .update(mapOf("scheduledTime" to scheduledTime, "dayType" to dayType)).await()
                successMessage = "시간표가 수정되었습니다"
            } catch (e: Exception) { errorMessage = "수정 실패: ${e.localizedMessage}" }
        }
    }

    fun deleteTimetable(id: String) {
        viewModelScope.launch {
            try {
                db.collection("timetable").document(id).delete().await()
                successMessage = "시간표가 삭제되었습니다"
            } catch (e: Exception) { errorMessage = "삭제 실패: ${e.localizedMessage}" }
        }
    }

    fun dismissError()   { errorMessage   = null }
    fun dismissSuccess() { successMessage = null }
}
