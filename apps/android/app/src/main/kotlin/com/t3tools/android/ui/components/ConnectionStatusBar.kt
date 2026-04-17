package com.t3tools.android.ui.components

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.t3tools.android.data.rpc.WsConnectionState

@Composable
fun ConnectionStatusBar(
    state: WsConnectionState,
    modifier: Modifier = Modifier,
) {
    val (color, label) = when (state) {
        is WsConnectionState.Connected -> Color(0xFF4CAF50) to "Connected"
        is WsConnectionState.Connecting -> Color(0xFFFF9800) to "Connecting…"
        is WsConnectionState.Reconnecting -> Color(0xFFFF9800) to "Reconnecting (${state.attempt})…"
        is WsConnectionState.Failed -> Color(0xFFF44336) to state.message
        else -> Color(0xFF9E9E9E) to "Disconnected"
    }

    val isPulsing = state is WsConnectionState.Connecting || state is WsConnectionState.Reconnecting
    val infiniteTransition = rememberInfiniteTransition(label = "status-pulse")
    val pulsingAlpha by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 0.3f,
        animationSpec = infiniteRepeatable(tween(700), RepeatMode.Reverse),
        label = "dot-alpha",
    )
    val dotAlpha = if (isPulsing) pulsingAlpha else 1f

    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(horizontal = 16.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(color)
                .alpha(dotAlpha),
        )
        Spacer(Modifier.width(8.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
