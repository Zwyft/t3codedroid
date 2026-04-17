package com.t3tools.android.ui.screens.chat

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.t3tools.android.data.model.RuntimeMode

@Composable
fun ComposerBar(
    text: String,
    onTextChange: (String) -> Unit,
    runtimeMode: RuntimeMode,
    isRunning: Boolean,
    onSend: () -> Unit,
    onInterrupt: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
        // Runtime mode chip
        val modeLabel = when (runtimeMode) {
            RuntimeMode.full_access -> "Full Access"
            RuntimeMode.auto_accept_edits -> "Auto Accept"
            RuntimeMode.approval_required -> "Approval Required"
        }
        FilterChip(
            selected = runtimeMode != RuntimeMode.full_access,
            onClick = { /* mode cycling handled in parent via future setting */ },
            label = { Text(modeLabel, style = MaterialTheme.typography.labelSmall) },
        )
        Spacer(Modifier.height(4.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.Bottom,
        ) {
            OutlinedTextField(
                value = text,
                onValueChange = onTextChange,
                modifier = Modifier
                    .weight(1f)
                    .heightIn(min = 48.dp, max = 160.dp),
                placeholder = { Text("Message…") },
                maxLines = 6,
                shape = MaterialTheme.shapes.medium,
            )
            Spacer(Modifier.width(8.dp))
            if (isRunning) {
                IconButton(
                    onClick = onInterrupt,
                    modifier = Modifier.size(48.dp),
                ) {
                    Icon(
                        Icons.Default.Stop,
                        contentDescription = "Stop",
                        tint = MaterialTheme.colorScheme.error,
                    )
                }
            } else {
                IconButton(
                    onClick = onSend,
                    enabled = text.isNotBlank(),
                    modifier = Modifier.size(48.dp),
                ) {
                    Icon(
                        Icons.AutoMirrored.Filled.Send,
                        contentDescription = "Send",
                        tint = if (text.isNotBlank())
                            MaterialTheme.colorScheme.primary
                        else
                            MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}
