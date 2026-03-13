package com.diversify.presentation.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.diversify.presentation.component.CyberButton
import com.diversify.presentation.theme.MatrixGreen
import com.diversify.presentation.theme.MatrixMagenta

@Composable
fun ConnectVaultScreen(
    onConnect: () -> Unit,
    isLoading: Boolean,
    error: String?
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "> SEED VAULT REQUIRED",
            style = MaterialTheme.typography.headlineMedium,
            color = MatrixMagenta
        )

        Spacer(modifier = Modifier.height(32.dp))

        Text(
            text = "Diversify needs Seed Vault access to request secure wallet signatures.",
            style = MaterialTheme.typography.bodyLarge,
            color = MatrixGreen
        )

        Spacer(modifier = Modifier.height(32.dp))

        CyberButton(
            onClick = onConnect,
            enabled = !isLoading
        ) {
            if (isLoading) {
                Text("CONNECTING...")
            } else {
                Text("[ CONNECT SEED VAULT ]")
            }
        }

        error?.let {
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "> ERROR: $it",
                color = Color.Red,
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}
