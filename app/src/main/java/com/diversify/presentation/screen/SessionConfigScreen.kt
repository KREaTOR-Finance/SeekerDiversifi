package com.diversify.presentation.screen

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.diversify.presentation.component.CyberButton
import com.diversify.presentation.theme.MatrixGreen
import com.diversify.presentation.theme.MatrixMagenta
import com.diversify.presentation.viewmodel.SessionViewModel

@Composable
fun SessionConfigScreen(
    defaultConfig: SessionViewModel.SessionConfig,
    allowlistTokens: List<String>,
    onStartSession: (SessionViewModel.SessionConfig) -> Unit
) {
    var totalTransactions by remember { mutableStateOf(defaultConfig.totalTransactions) }
    var sessionAmountText by remember { mutableStateOf(defaultConfig.sessionAmount.toString()) }
    var fundingAsset by remember { mutableStateOf(defaultConfig.fundingAsset) }

    var expanded by remember { mutableStateOf(false) }
    val fundingOptions = listOf("SOL", "USDC")
    val activeAllowlist = allowlistTokens.filter { it != fundingAsset }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "> SESSION CONFIGURATION",
            style = MaterialTheme.typography.headlineMedium,
            color = MatrixMagenta
        )

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "Total transactions (even): $totalTransactions",
            style = MaterialTheme.typography.bodyLarge,
            color = MatrixGreen
        )

        Slider(
            value = totalTransactions.toFloat(),
            onValueChange = {
                val rounded = it.toInt().coerceIn(2, 100)
                totalTransactions = if (rounded % 2 == 0) rounded else rounded + 1
            },
            valueRange = 2f..100f,
            steps = 48,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(12.dp))

        OutlinedTextField(
            value = sessionAmountText,
            onValueChange = { sessionAmountText = it },
            label = { Text("Session amount") },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(16.dp))
        Text("Funding asset")

        Box(modifier = Modifier.fillMaxWidth()) {
            OutlinedTextField(
                value = fundingAsset,
                onValueChange = {},
                readOnly = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { expanded = true },
                label = { Text("Asset") }
            )

            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false }
            ) {
                fundingOptions.forEach { option ->
                    DropdownMenuItem(
                        text = { Text(option) },
                        onClick = {
                            fundingAsset = option
                            expanded = false
                        }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Cycler allowlist: ${activeAllowlist.joinToString(", ")}",
            color = MatrixGreen,
            style = MaterialTheme.typography.bodyMedium
        )

        Spacer(modifier = Modifier.height(24.dp))

        CyberButton(
            onClick = {
                val amount = sessionAmountText.toDoubleOrNull() ?: 0.0
                onStartSession(
                    SessionViewModel.SessionConfig(
                        totalTransactions = totalTransactions,
                        sessionAmount = amount,
                        fundingAsset = fundingAsset
                    )
                )
            }
        ) {
            Text("[ START CYCLER SESSION ]")
        }
    }
}
