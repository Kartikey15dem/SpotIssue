package org.example.project.core.components

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.example.project.theme.IssueSpotTypography

@Composable
fun AppErrorDialog(
    message: String,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        text = {
            Text(
                text = message,
                style = IssueSpotTypography.bodyLarge,
                fontSize = 16.sp,
            )
        },
        confirmButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.padding(8.dp),
            ) {
                Text(
                    text = "OK",
                    style = IssueSpotTypography.titleMedium,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                )
            }
        },
    )
}
