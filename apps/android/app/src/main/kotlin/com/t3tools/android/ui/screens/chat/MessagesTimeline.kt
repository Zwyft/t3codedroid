package com.t3tools.android.ui.screens.chat

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.t3tools.android.data.model.OrchestrationMessage
import com.t3tools.android.data.model.OrchestrationThreadActivity

@Composable
fun MessagesTimeline(
    messages: List<OrchestrationMessage>,
    activities: List<OrchestrationThreadActivity>,
    isRunning: Boolean,
    listState: LazyListState,
    modifier: Modifier = Modifier,
) {
    // Build a merged, sorted timeline of messages and activities
    data class TimelineItem(val id: String, val createdAt: String, val isMessage: Boolean)

    val timeline = (
        messages.map { TimelineItem(it.id, it.createdAt, true) } +
            activities.map { TimelineItem(it.id, it.createdAt, false) }
        ).sortedBy { it.createdAt }

    val messageMap = messages.associateBy { it.id }
    val activityMap = activities.associateBy { it.id }

    LaunchedEffect(timeline.size) {
        if (timeline.isNotEmpty()) {
            listState.animateScrollToItem(timeline.size - 1)
        }
    }

    LazyColumn(
        modifier = modifier,
        state = listState,
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        items(timeline, key = { it.id }) { item ->
            if (item.isMessage) {
                val msg = messageMap[item.id] ?: return@items
                when (msg.role) {
                    "user" -> UserMessageRow(msg)
                    "assistant" -> AssistantMessageRow(msg)
                    else -> {}
                }
            } else {
                val activity = activityMap[item.id] ?: return@items
                ActivityRow(activity)
            }
        }

        if (isRunning) {
            item("streaming-indicator") {
                Row(
                    modifier = Modifier.padding(start = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                    Text(
                        "Thinking…",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

@Composable
private fun UserMessageRow(message: OrchestrationMessage) {
    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.CenterEnd) {
        Surface(
            color = MaterialTheme.colorScheme.primaryContainer,
            shape = RoundedCornerShape(topStart = 16.dp, topEnd = 4.dp, bottomStart = 16.dp, bottomEnd = 16.dp),
        ) {
            Text(
                text = message.text,
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
            )
        }
    }
}

@Composable
private fun AssistantMessageRow(message: OrchestrationMessage) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Surface(
            color = MaterialTheme.colorScheme.surfaceVariant,
            shape = RoundedCornerShape(topStart = 4.dp, topEnd = 16.dp, bottomStart = 16.dp, bottomEnd = 16.dp),
            modifier = Modifier.fillMaxWidth(0.92f),
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                MarkdownContent(
                    markdown = message.text.ifBlank { if (message.streaming) "▊" else "" },
                    modifier = Modifier.fillMaxWidth(),
                )
                if (message.streaming) {
                    Spacer(Modifier.height(4.dp))
                    CircularProgressIndicator(
                        modifier = Modifier.size(12.dp).align(Alignment.End),
                        strokeWidth = 1.5.dp,
                    )
                }
            }
        }
    }
}

@Composable
private fun ActivityRow(activity: OrchestrationThreadActivity) {
    val (icon, tint) = when (activity.tone) {
        "tool" -> Icons.Default.Build to MaterialTheme.colorScheme.secondary
        "approval" -> Icons.Default.Warning to MaterialTheme.colorScheme.tertiary
        "error" -> Icons.Default.Warning to MaterialTheme.colorScheme.error
        else -> Icons.Default.Info to MaterialTheme.colorScheme.onSurfaceVariant
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.surface)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = tint,
            modifier = Modifier.size(16.dp),
        )
        Spacer(Modifier.width(8.dp))
        Text(
            text = activity.summary,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
