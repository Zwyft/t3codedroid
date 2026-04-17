package com.t3tools.android.ui.components

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.t3tools.android.data.model.OrchestrationSessionStatus

@Composable
fun ThreadStatusIcon(status: OrchestrationSessionStatus?, modifier: Modifier = Modifier) {
    val color = when (status) {
        OrchestrationSessionStatus.running, OrchestrationSessionStatus.starting -> Color(0xFF4CAF50)
        OrchestrationSessionStatus.error -> Color(0xFFF44336)
        OrchestrationSessionStatus.interrupted -> Color(0xFFFF9800)
        else -> Color(0xFF9E9E9E)
    }

    val isAnimating = status == OrchestrationSessionStatus.running ||
        status == OrchestrationSessionStatus.starting

    val infiniteTransition = rememberInfiniteTransition(label = "status-pulse")
    val pulsingAlpha by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 0.3f,
        animationSpec = infiniteRepeatable(tween(600), RepeatMode.Reverse),
        label = "status-alpha",
    )
    val dotAlpha = if (isAnimating) pulsingAlpha else 1f

    Box(
        modifier = modifier
            .size(8.dp)
            .clip(CircleShape)
            .background(color)
            .alpha(dotAlpha),
    )
}
