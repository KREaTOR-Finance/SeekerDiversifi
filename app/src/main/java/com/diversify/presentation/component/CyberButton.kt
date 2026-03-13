package com.diversify.presentation.component

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RectangleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.diversify.presentation.theme.MatrixGreen
import com.diversify.presentation.theme.MatrixBlack
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
                verticalAlignment = Alignment.CenterVertically,
                content = content
            )
        }
        
        Text(
            text = "◄",
            modifier = Modifier.padding(end = 4.dp),
            color = MatrixGreen
        )
        
        Row(content = content)
        
        Text(
            text = "►",
            modifier = Modifier.padding(start = 4.dp),
            color = MatrixGreen
        )
        
        if (glitch) {
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .offset(x = 2.dp, y = (-2).dp)
            ) {
                Row(
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically,
                    content = content
                )
            }
            
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .offset(x = (-2).dp, y = 2.dp)
            ) {
                Row(
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically,
                    content = content
                )
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
        Text("◄", modifier = Modifier.padding(end = 4.dp))
        Row(content = content)
        Text("►", modifier = Modifier.padding(start = 4.dp))
    }
}
