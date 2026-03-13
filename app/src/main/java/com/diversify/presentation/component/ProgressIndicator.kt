package com.diversify.presentation.component

import androidx.compose.foundation.layout.*
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.diversify.presentation.theme.MatrixGreen

@Composable
fun ProgressIndicator(
    progress: Int,
    total: Int,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "$progress/$total",
            color = MatrixGreen,
            style = MaterialTheme.typography.bodyLarge
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        LinearProgressIndicator(
            progress = progress.toFloat() / total.toFloat(),
            modifier = Modifier.fillMaxWidth(),
            color = MatrixGreen
        )
    }
}
