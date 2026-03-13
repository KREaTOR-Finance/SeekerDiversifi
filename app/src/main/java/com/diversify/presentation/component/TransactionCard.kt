package com.diversify.presentation.component

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.unit.dp
import com.diversify.domain.model.Transaction
import com.diversify.domain.model.TransactionType
import com.diversify.presentation.theme.MatrixBlack
import com.diversify.presentation.theme.MatrixCyan
import com.diversify.presentation.theme.MatrixGreen
import com.diversify.presentation.theme.MatrixMagenta

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
                        TransactionType.BUY -> "BUY"
                        TransactionType.SELL -> "SELL"
                    },
                    color = MatrixCyan,
                    style = MaterialTheme.typography.labelLarge
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            val amountLabel = if (transaction.type == TransactionType.SELL && transaction.amount <= 0.0) {
                "MAX ${transaction.token}"
            } else {
                "${transaction.amount} ${transaction.token}"
            }
            DetailRow("AMOUNT:", amountLabel)
            DetailRow("TOKEN:", transaction.token)

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "SIGNATURE REQUIRED",
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
