package com.diversify.presentation.screen

import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.diversify.presentation.component.CyberButton
import com.diversify.presentation.theme.MatrixMagenta

@Composable
fun NeutralZoneScreen(
    onPlayAgain: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "> SESSION COMPLETE",
            style = MaterialTheme.typography.headlineLarge,
            color = MatrixMagenta
        )
        
        Spacer(modifier = Modifier.height(32.dp))
        
        Text(
            text = "Welcome to the Neutral Zone",
            style = MaterialTheme.typography.headlineMedium
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Text(
            text = "All 50 transactions have been processed successfully!",
            style = MaterialTheme.typography.bodyLarge
        )
        
        Spacer(modifier = Modifier.height(32.dp))
        
        CyberButton(onClick = onPlayAgain) {
            Text("PLAY AGAIN")
        }
    }
}
