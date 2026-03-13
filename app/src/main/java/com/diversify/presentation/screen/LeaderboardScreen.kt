package com.diversify.presentation.screen

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.diversify.presentation.theme.MatrixMagenta
import com.diversify.presentation.viewmodel.SessionViewModel

@Composable
fun LeaderboardScreen(
    entries: List<SessionViewModel.LeaderboardEntry>,
    onClose: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Leaderboard", style = MaterialTheme.typography.headlineLarge, color = MatrixMagenta)
            TextButton(onClick = onClose) {
                Text("CLOSE")
            }
        }
        Spacer(modifier = Modifier.height(16.dp))
        LazyColumn(modifier = Modifier.fillMaxSize()) {
            items(entries) { entry ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(entry.userId)
                    Text(entry.completions.toString())
                }
                Divider()
            }
        }
    }
}