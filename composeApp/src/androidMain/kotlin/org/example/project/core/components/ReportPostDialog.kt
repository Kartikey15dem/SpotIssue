package org.example.project.core.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import org.example.project.theme.IssueSpotColors

@Composable
fun ReportPostDialog(
    onDismiss: () -> Unit,
    onSubmit: (String) -> Unit,
) {
    val reportReasons =
        listOf(
            "Spam or Misleading",
            "Harassment or Hate Speech",
            "Inappropriate Content",
            "False Information",
            "Other",
        )
    var selectedReason by rememberSaveable { mutableStateOf(reportReasons.first()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(text = "Report Post", fontWeight = FontWeight.Bold)
        },
        text = {
            Column {
                Text("Why are you reporting this post?", modifier = Modifier.padding(bottom = 8.dp))
                reportReasons.forEach { reason ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .clickable { selectedReason = reason }
                                .padding(vertical = 4.dp),
                    ) {
                        RadioButton(
                            selected = (reason == selectedReason),
                            onClick = { selectedReason = reason },
                            colors = RadioButtonDefaults.colors(selectedColor = IssueSpotColors.Primary),
                        )
                        Text(
                            text = reason,
                            modifier = Modifier.padding(start = 8.dp),
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onSubmit(selectedReason) }) {
                Text("Submit", color = IssueSpotColors.Primary)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = IssueSpotColors.OnSurfaceVariant)
            }
        },
        containerColor = IssueSpotColors.Surface,
    )
}
