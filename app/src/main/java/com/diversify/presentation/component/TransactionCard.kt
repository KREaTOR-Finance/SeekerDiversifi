package com.diversify.presentation.component

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RectangleShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.diversify.domain.model.Transaction
import com.diversify.domain.model.TransactionType
import com.diversify.presentation.theme.MatrixGreen
import com.diversify.presentation.theme.MatrixMagenta
import com.diversify.presentation.theme.MatrixCyan
import com.diversify.presentation.theme.MatrixBlack

@Composable
fun TransactionCard(
    transaction: Transaction,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RectangleShape,
        colors = CardDefaults.cardColors(
            containerColor = MatrixBlack
        ),
        border = BorderStroke(2.dp, MatrixGreen)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "> TRANSACTION #${transaction.id.takeLast(4)}",
                    color = MatrixMagenta,
                    style = MaterialTheme.typography.labelLarge
                )
                
                Text(
                    text = when (transaction.type) {
                        TransactionType.BUNDLER_TRANSFER -> "BUNDLER"
                        TransactionType.CYCLER_BUY, TransactionType.CYCLER_TRADE_BUY -> "BUY"
                        TransactionType.CYCLER_SELL, TransactionType.CYCLER_TRADE_SELL -> "SELL"
                    },
                    color = MatrixCyan,
                    style = MaterialTheme.typography.labelLarge
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            val assetLabel = when {
                transaction.type == TransactionType.BUNDLER_TRANSFER && transaction.token != null -> transaction.token
                transaction.type == TransactionType.CYCLER_BUY || transaction.type == TransactionType.CYCLER_TRADE_BUY -> "SOL"
                transaction.type == TransactionType.CYCLER_SELL || transaction.type == TransactionType.CYCLER_TRADE_SELL -> "SKR"
                else -> "SOL"
            }
            DetailRow("AMOUNT:", "${transaction.amount} $assetLabel")
            
            if (transaction.type == TransactionType.BUNDLER_TRANSFER) {
                DetailRow("TO:", transaction.destinationWallet?.let { 
                    "${it.take(8)}...${it.takeLast(8)}" 
                } ?: "Unknown")
            }
            
            if (transaction.type == TransactionType.CYCLER_BUY ||
                transaction.type == TransactionType.CYCLER_SELL ||
                transaction.type == TransactionType.CYCLER_TRADE_BUY ||
                transaction.type == TransactionType.CYCLER_TRADE_SELL) {
                DetailRow("TOKEN:", transaction.token ?: "Unknown")
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "⚡ THUMBPRINT REQUIRED",
                    color = MatrixMagenta,
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}

@Composable
private fun DetailRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    ) {
        Text(
            text = label,
            color = MatrixCyan,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.width(80.dp)
        )
        
        Text(
            text = value,
            color = MatrixGreen,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.weight(1f)
        )
    }
}
