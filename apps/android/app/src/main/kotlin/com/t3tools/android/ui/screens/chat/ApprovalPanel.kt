package com.t3tools.android.ui.screens.chat

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.t3tools.android.data.model.ProviderApprovalDecision

@Composable
fun ApprovalPanel(
    approvals: List<PendingApprovalUiModel>,
    onDecision: (requestId: String, decision: ProviderApprovalDecision) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (approvals.isEmpty()) return

    val approval = approvals.first()

    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.tertiaryContainer,
        ),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Approval Required",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.tertiary,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = approval.summary,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onTertiaryContainer,
            )
            if (approvals.size > 1) {
                Text(
                    text = "+${approvals.size - 1} more pending",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onTertiaryContainer,
                )
            }
            Spacer(Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                OutlinedButton(
                    onClick = { onDecision(approval.requestId, ProviderApprovalDecision.accept) },
                    modifier = Modifier.weight(1f),
                ) { Text("Accept") }

                TextButton(
                    onClick = { onDecision(approval.requestId, ProviderApprovalDecision.acceptForSession) },
                    modifier = Modifier.weight(1f),
                ) { Text("Accept All") }

                OutlinedButton(
                    onClick = { onDecision(approval.requestId, ProviderApprovalDecision.decline) },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = MaterialTheme.colorScheme.error,
                    ),
                ) { Text("Decline") }
            }
        }
    }
}
