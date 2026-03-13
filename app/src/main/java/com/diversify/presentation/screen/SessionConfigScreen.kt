package com.diversify.presentation.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.ExposedDropdownMenu
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SessionConfigScreen(
    defaultConfig: SessionViewModel.SessionConfig,
    onStartSession: (SessionViewModel.SessionConfig) -> Unit,
    onViewLeaderboard: () -> Unit
) {
    var transactionCount by remember { mutableStateOf(defaultConfig.transactionCount) }
    var bundlerRatio by remember { mutableStateOf(defaultConfig.bundlerRatio) }
    var curatedTokens by remember { mutableStateOf(defaultConfig.curatedTokens) }
    var fundingAsset by remember { mutableStateOf(defaultConfig.fundingAsset) }

    var expanded by remember { mutableStateOf(false) }
    val fundingOptions = listOf("SOL", "SKR")

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

        Spacer(modifier = Modifier.height(32.dp))

        Text(
            text = "Cycles: $transactionCount",
            style = MaterialTheme.typography.bodyLarge,
            color = MatrixGreen
        )

        Slider(
            value = transactionCount.toFloat(),
            onValueChange = { transactionCount = it.toInt() },
            valueRange = 1f..50f,
            steps = 49,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(16.dp))
        Text("Select curated tokens (comma separated)")
        OutlinedTextField(
            value = curatedTokens.joinToString(","),
            onValueChange = { curatedTokens = it.split(",").map { token -> token.trim() }.filter { token -> token.isNotBlank() } },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(16.dp))
        Text("Funding asset")
        ExposedDropdownMenuBox(
            expanded = expanded,
            onExpandedChange = { expanded = !expanded }
        ) {
            TextField(
                value = fundingAsset,
                onValueChange = {},
                readOnly = true,
                label = { Text("Asset") },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
                modifier = Modifier.menuAnchor().fillMaxWidth()
            )
            ExposedDropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false }
            ) {
                fundingOptions.forEach { selectionOption ->
                    DropdownMenuItem(
                        text = { Text(selectionOption) },
                        onClick = {
                            fundingAsset = selectionOption
                            expanded = false
                        }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(32.dp))
        CyberButton(
            onClick = {
                onStartSession(
                    SessionViewModel.SessionConfig(
                        transactionCount = transactionCount,
                        bundlerRatio = bundlerRatio,
                        curatedTokens = curatedTokens,
                        fundingAsset = fundingAsset
                    )
                )
            }
        ) {
            Text("[ START SESSION ]")
        }

        Spacer(modifier = Modifier.height(16.dp))
        CyberButton(onClick = onViewLeaderboard) {
            Text("VIEW LEADERBOARD")
        }
    }
}
