package com.diversify.presentation.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.diversify.presentation.component.CyberButton
import com.diversify.presentation.theme.MatrixCyan
import com.diversify.presentation.theme.MatrixGreen
import com.diversify.presentation.theme.MatrixMagenta

@Composable
fun QuickStartTutorialScreen(
    onContinue: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.Start,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "> QUICKSTART: RANDOMIZED BUY/SELL",
            style = MaterialTheme.typography.headlineMedium,
            color = MatrixMagenta
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "No payment required. Diversify is free to use.",
            style = MaterialTheme.typography.bodyLarge,
            color = MatrixGreen
        )
        Text(
            text = "Wallet connection is required for secure signing on Solana mainnet.",
            style = MaterialTheme.typography.bodyLarge,
            color = MatrixGreen
        )

        Spacer(modifier = Modifier.height(24.dp))

        StepLine("BUY", "Acquire an allowlisted token using your funding asset.")
        StepLine("SELL", "Sell a random held token back into the funding asset.")
        StepLine("PUZZLE", "Solve the puzzle before each transaction approval.")
        StepLine("REPEAT", "Run an even number of total transactions for a full cycle.")

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "Cycler randomizes transaction order while preserving balanced buy/sell execution for volume runs.",
            style = MaterialTheme.typography.bodyMedium,
            color = MatrixCyan
        )

        Spacer(modifier = Modifier.height(24.dp))

        CyberButton(
            onClick = onContinue,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("CONTINUE TO SESSION SETUP")
        }
    }
}

@Composable
private fun StepLine(title: String, description: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleMedium,
        color = MatrixMagenta
    )
    Text(
        text = description,
        style = MaterialTheme.typography.bodyMedium,
        color = MatrixGreen
    )
    Spacer(modifier = Modifier.height(12.dp))
}
