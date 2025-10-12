package com.example.fridgetracker.utilities

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

enum class SnackbarType {
    SUCCESS,
    ERROR,
    INFO,
    WARNING
}

@Composable
fun CustomSnackbar(
    message: String,
    type: SnackbarType,
    onDismiss: () -> Unit
) {
    val backgroundColor = when (type) {
        SnackbarType.SUCCESS -> Color(0xFF4CAF50)
        SnackbarType.ERROR -> Color(0xFFEF5350)
        SnackbarType.INFO -> Color(0xFF2196F3)
        SnackbarType.WARNING -> Color(0xFFFFA726)
    }

    val icon = when (type) {
        SnackbarType.SUCCESS -> Icons.Default.CheckCircle
        SnackbarType.ERROR -> Icons.Default.Error
        SnackbarType.INFO -> Icons.Default.Info
        SnackbarType.WARNING -> Icons.Default.Warning
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        shape = RoundedCornerShape(12.dp),
        backgroundColor = backgroundColor,
        elevation = 8.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(24.dp)
            )

            Spacer(Modifier.width(12.dp))

            Text(
                text = message,
                color = Color.White,
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.body1
            )

            IconButton(onClick = onDismiss) {
                Icon(
                    Icons.Default.Close,
                    contentDescription = "Dismiss",
                    tint = Color.White
                )
            }
        }
    }
}