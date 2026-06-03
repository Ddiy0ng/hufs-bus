package com.hufsteam.shuttletrack.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme = darkColorScheme(
    primary = HufsBlue,
    secondary = HufsLightBlue,
    tertiary = HufsGold
)

private val LightColorScheme = lightColorScheme(
    primary = HufsBlue,
    secondary = HufsLightBlue,
    tertiary = HufsGold
)

@Composable
fun HufsBusTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Dynamic color: Android 12+ 기기에서 자동 색상 (원하면 false로 고정)
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
