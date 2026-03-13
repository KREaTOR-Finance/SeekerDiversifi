package com.diversify.presentation.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

val MatrixGreen = Color(0xFF00FF9D)
val MatrixMagenta = Color(0xFFFF00FF)
val MatrixCyan = Color(0xFF00FFFF)
val MatrixBlack = Color(0xFF0A0A0A)
val MatrixDarkGreen = Color(0xFF003B2F)
val MatrixWhite = Color(0xFFE0E0E0)

val MatrixFontFamily = FontFamily.Monospace

@Composable
fun MatrixTheme(
    darkTheme: Boolean = true,
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) {
        darkColorScheme(
            primary = MatrixGreen,
            secondary = MatrixMagenta,
            tertiary = MatrixCyan,
            background = MatrixBlack,
            surface = MatrixBlack,
            onPrimary = MatrixBlack,
            onSecondary = MatrixBlack,
            onBackground = MatrixWhite,
            onSurface = MatrixWhite
        )
    } else {
        lightColorScheme(
            primary = MatrixGreen,
            secondary = MatrixMagenta,
            tertiary = MatrixCyan,
            background = MatrixWhite,
            surface = MatrixWhite,
            onPrimary = MatrixBlack,
            onSecondary = MatrixBlack,
            onBackground = MatrixBlack,
            onSurface = MatrixBlack
        )
    }
    
    val typography = Typography(
        headlineLarge = TextStyle(
            fontFamily = MatrixFontFamily,
            fontSize = 32.sp,
            fontWeight = FontWeight.Bold,
            color = MatrixGreen
        ),
        headlineMedium = TextStyle(
            fontFamily = MatrixFontFamily,
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = MatrixGreen
        ),
        bodyLarge = TextStyle(
            fontFamily = MatrixFontFamily,
            fontSize = 16.sp,
            color = MatrixWhite
        ),
        bodyMedium = TextStyle(
            fontFamily = MatrixFontFamily,
            fontSize = 14.sp,
            color = MatrixWhite
        ),
        labelLarge = TextStyle(
            fontFamily = MatrixFontFamily,
            fontSize = 14.sp,
            color = MatrixCyan
        )
    )
    
    MaterialTheme(
        colorScheme = colorScheme,
        typography = typography,
        content = content
    )
}
