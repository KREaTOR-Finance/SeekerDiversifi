package com.diversify.presentation.screen

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.diversify.domain.model.Transaction
import com.diversify.presentation.component.*

@Composable
fun ActiveSessionScreen(
    currentTransaction: Transaction?,
    progress: Int,
    total: Int,
    isSigning: Boolean,
    onAnswerSubmit: (String) -> Unit
) {
    var answer by remember { mutableStateOf("") }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        ProgressIndicator(
            progress = progress,
            total = total,
            modifier = Modifier.fillMaxWidth()
        )
        
        Spacer(modifier = Modifier.height(32.dp))
        
        currentTransaction?.let { transaction ->
            TransactionCard(transaction = transaction)
            
            Spacer(modifier = Modifier.height(32.dp))
            
            Text(
                text = "PUZZLE: ${transaction.puzzle.question}",
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.primary
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            OutlinedTextField(
                value = answer,
                onValueChange = { answer = it },
                label = { Text("Enter answer") },
                modifier = Modifier.fillMaxWidth(0.8f),
                enabled = !isSigning
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            CyberButton(
                onClick = { onAnswerSubmit(answer) },
                enabled = answer.isNotBlank() && !isSigning
            ) {
                if (isSigning) {
                    Text("SIGNING...")
                } else {
                    Text("APPROVE TRANSACTION")
                }
            }
        }
    }
}
