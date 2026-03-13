package com.diversify.presentation.component

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlin.random.Random

@Composable
fun MatrixRain(
    modifier: Modifier = Modifier,
    intensity: Float = 0.8f,
    color: Color = Color(0xFF00FF9D),
    backgroundColor: Color = Color(0xFF0A0A0A)
) {
    val configuration = LocalConfiguration.current
    val screenWidth = configuration.screenWidthDp.dp
    val screenHeight = configuration.screenHeightDp.dp
    
    val density = LocalDensity.current
    val fontSize = with(density) { 14.dp.toPx() }
    
    val columns = (screenWidth.value * intensity / fontSize).toInt().coerceAtLeast(20)
    
    val drops = remember { 
        MutableList(columns) { 
            Drop(
                x = it * fontSize,
                y = Random.nextFloat() * -screenHeight.value,
                speed = Random.nextFloat() * 8 + 4,
                chars = generateCharSequence()
            )
        }
    }
    
    LaunchedEffect(Unit) {
        while (true) {
            delay(50L)
            drops.forEach { drop ->
                drop.y += drop.speed
                
                if (drop.y > screenHeight.value + 100) {
                    drop.y = -Random.nextFloat() * 100
                    drop.speed = Random.nextFloat() * 8 + 4
                    drop.chars = generateCharSequence()
                }
            }
        }
    }
    
    Canvas(modifier = modifier.fillMaxSize()) {
        drawRect(backgroundColor)
        
        drops.forEach { drop ->
            drawContext.canvas.nativeCanvas.drawText(
                drop.chars.currentChar.toString(),
                drop.x,
                drop.y,
                android.graphics.Paint().apply {
                    this.color = android.graphics.Color.parseColor(color.toHexString())
                    textSize = fontSize
                    typeface = android.graphics.Typeface.MONOSPACE
                }
            )
            
            val trailLength = 5
            for (i in 1..trailLength) {
                val alpha = 1.0f - (i.toFloat() / trailLength)
                drawContext.canvas.nativeCanvas.drawText(
                    drop.chars.getTrailChar(i).toString(),
                    drop.x,
                    drop.y - (i * fontSize * 1.5f),
                    android.graphics.Paint().apply {
                        this.color = android.graphics.Color.parseColor(
                            color.copy(alpha = alpha * 0.5f).toHexString()
                        )
                        textSize = fontSize
                        typeface = android.graphics.Typeface.MONOSPACE
                    }
                )
            }
        }
    }
}

private fun generateCharSequence(): CharSequence {
    val chars = "01アイウエオカキクケコサシスセソタチツテトナニヌネノハヒフヘホマミムメモヤユヨラリルレロワヲン"
    return CharSequence(
        currentChar = chars[Random.nextInt(chars.length)],
        trailChars = List(5) { chars[Random.nextInt(chars.length)] }
    )
}

private data class Drop(
    val x: Float,
    var y: Float,
    var speed: Float,
    var chars: CharSequence
)

private data class CharSequence(
    val currentChar: Char,
    val trailChars: List<Char>
) {
    fun getTrailChar(index: Int): Char {
        return if (index < trailChars.size) trailChars[index] else currentChar
    }
}

fun Color.toHexString(): String {
    return String.format("#%02X%02X%02X", (red * 255).toInt(), (green * 255).toInt(), (blue * 255).toInt())
}
