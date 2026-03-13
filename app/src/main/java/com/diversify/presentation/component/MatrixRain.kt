package com.diversify.presentation.component

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.nativeCanvas
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
    val screenWidthDp = configuration.screenWidthDp.dp
    val screenHeightDp = configuration.screenHeightDp.dp

    val density = LocalDensity.current
    val fontSizePx = with(density) { 14.dp.toPx() }
    val screenHeightPx = with(density) { screenHeightDp.toPx() }

    val columns = (screenWidthDp.value * intensity / (fontSizePx / density.density)).toInt().coerceAtLeast(20)

    val drops = remember(columns, screenHeightPx) {
        MutableList(columns) {
            Drop(
                x = it * fontSizePx,
                y = Random.nextFloat() * -screenHeightPx,
                speed = Random.nextFloat() * 8 + 4,
                chars = generateRainChars()
            )
        }
    }
    val headPaint = remember {
        android.graphics.Paint().apply {
            isAntiAlias = false
            textSize = fontSizePx
            typeface = android.graphics.Typeface.MONOSPACE
        }
    }
    val trailPaint = remember {
        android.graphics.Paint().apply {
            isAntiAlias = false
            textSize = fontSizePx
            typeface = android.graphics.Typeface.MONOSPACE
        }
    }
    var frameTick by remember { mutableIntStateOf(0) }

    LaunchedEffect(columns, screenHeightPx) {
        while (true) {
            delay(50L)
            drops.forEach { drop ->
                drop.y += drop.speed
                drop.chars = drop.chars.next()

                if (drop.y > screenHeightPx + 100f) {
                    drop.y = -Random.nextFloat() * 100f
                    drop.speed = Random.nextFloat() * 8 + 4
                    drop.chars = generateRainChars()
                }
            }
            frameTick++
        }
    }

    Canvas(modifier = modifier.fillMaxSize()) {
        @Suppress("UNUSED_VARIABLE")
        val redrawTick = frameTick
        drawRect(backgroundColor)

        headPaint.color = android.graphics.Color.parseColor(color.toHexString())
        drops.forEach { drop ->
            drawContext.canvas.nativeCanvas.drawText(
                drop.chars.currentChar.toString(),
                drop.x,
                drop.y,
                headPaint
            )

            val trailLength = 5
            for (i in 1..trailLength) {
                val alpha = 1.0f - (i.toFloat() / trailLength)
                trailPaint.color = android.graphics.Color.parseColor(
                    color.copy(alpha = alpha * 0.5f).toHexString()
                )
                drawContext.canvas.nativeCanvas.drawText(
                    drop.chars.getTrailChar(i - 1).toString(),
                    drop.x,
                    drop.y - (i * fontSizePx * 1.5f),
                    trailPaint
                )
            }
        }
    }
}

private fun generateRainChars(): RainChars {
    val chars = "01ABCDEFGHIJKLMNOPQRSTUVWXYZ"
    return RainChars(
        currentChar = chars[Random.nextInt(chars.length)],
        trailChars = List(5) { chars[Random.nextInt(chars.length)] }
    )
}

private data class Drop(
    val x: Float,
    var y: Float,
    var speed: Float,
    var chars: RainChars
)

private data class RainChars(
    val currentChar: Char,
    val trailChars: List<Char>
) {
    fun next(): RainChars {
        val chars = "01ABCDEFGHIJKLMNOPQRSTUVWXYZ"
        return copy(
            currentChar = chars[Random.nextInt(chars.length)],
            trailChars = listOf(currentChar) + trailChars.dropLast(1)
        )
    }

    fun getTrailChar(index: Int): Char {
        return if (index in trailChars.indices) trailChars[index] else currentChar
    }
}

fun Color.toHexString(): String {
    return String.format("#%02X%02X%02X", (red * 255).toInt(), (green * 255).toInt(), (blue * 255).toInt())
}
