package com.hufsteam.shuttletrack.ui.common

import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hufsteam.shuttletrack.ui.theme.DisabledGray
import com.hufsteam.shuttletrack.ui.theme.DisabledText
import com.hufsteam.shuttletrack.ui.theme.NavyBlue

@Composable
fun BusIcon(modifier: Modifier = Modifier, size: Dp = 90.dp) {
    val context = LocalContext.current
    val bitmap = remember {
        context.assets.open("frame/BUSICON.png").use { stream ->
            BitmapFactory.decodeStream(stream)
        }
    }
    Image(
        bitmap             = bitmap.asImageBitmap(),
        contentDescription = "버스 아이콘",
        contentScale       = ContentScale.Fit,
        modifier           = modifier.size(size)
    )
}

@Composable
fun LanguageButton(
    language: String = "KR",
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {}
) {
    Box(
        modifier = modifier
            .clickable(onClick = onClick)
            .border(1.5.dp, NavyBlue, RoundedCornerShape(6.dp))
            .padding(horizontal = 10.dp, vertical = 4.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text       = language,
            color      = NavyBlue,
            fontSize   = 12.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
fun ShuttleButton(
    text: String,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Button(
        onClick        = onClick,
        enabled        = enabled,
        shape          = RoundedCornerShape(12.dp),
        colors         = ButtonDefaults.buttonColors(
            containerColor         = NavyBlue,
            disabledContainerColor = DisabledGray,
            contentColor           = Color.White,
            disabledContentColor   = DisabledText
        ),
        contentPadding = PaddingValues(0.dp),
        modifier       = modifier
            .fillMaxWidth()
            .height(54.dp)
    ) {
        Text(text, fontSize = 16.sp, fontWeight = FontWeight.Medium)
    }
}
