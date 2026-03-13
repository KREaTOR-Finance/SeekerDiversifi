package com.diversify.presentation.component

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.unit.dp
import com.diversify.presentation.theme.MatrixBlack
import com.diversify.presentation.theme.MatrixGreen
import kotlinx.coroutines.delay

@Composable
fun CyberButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    glitch: Boolean = false,
    content: @Composable RowScope.() -> Unit
) {
    var glitchOffset by remember { mutableStateOf(0f) }

    LaunchedEffect(glitch) {
        if (glitch) {
            glitchOffset = 2f
            delay(50)
            glitchOffset = -2f
            delay(50)
            glitchOffset = 1f
            delay(50)
            glitchOffset = 0f
        }
    }

    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier,
        shape = RectangleShape,
        colors = ButtonDefaults.buttonColors(
            containerColor = MatrixBlack,
            contentColor = MatrixGreen,
            disabledContainerColor = MatrixBlack.copy(alpha = 0.5f),
            disabledContentColor = MatrixGreen.copy(alpha = 0.5f)
        ),
        border = BorderStroke(2.dp, MatrixGreen)
    ) {
        Box(
            modifier = Modifier.offset(x = glitchOffset.dp),
            contentAlignment = Alignment.Center
        ) {
            Row(
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = "<", modifier = Modifier.padding(end = 4.dp), color = MatrixGreen)
                content()
                Text(text = ">", modifier = Modifier.padding(start = 4.dp), color = MatrixGreen)
            }
        }
    }
}

@Composable
fun CyberOutlinedButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    content: @Composable RowScope.() -> Unit
) {
    OutlinedButton(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier,
        shape = RectangleShape,
        colors = ButtonDefaults.outlinedButtonColors(
            contentColor = MatrixGreen,
            disabledContentColor = MatrixGreen.copy(alpha = 0.5f)
        ),
        border = BorderStroke(1.dp, MatrixGreen)
    ) {
        Text("<", modifier = Modifier.padding(end = 4.dp))
        content()
        Text(">", modifier = Modifier.padding(start = 4.dp))
    }
}
