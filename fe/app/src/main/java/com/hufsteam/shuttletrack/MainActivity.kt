package com.hufsteam.shuttletrack

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.hufsteam.shuttletrack.data.remote.TokenStore
import com.hufsteam.shuttletrack.ui.navigation.AppNavGraph
import com.hufsteam.shuttletrack.ui.theme.HufsBusTheme

/**
 * 앱의 유일한 Activity
 * - Compose UI 전체를 여기서 시작
 * - AppNavGraph가 모든 화면 전환을 담당
 */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        TokenStore.initialize(this)
        enableEdgeToEdge()
        setContent {
            HufsBusTheme {
                AppNavGraph()
            }
        }
    }
}
