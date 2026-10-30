package com.nhacongdong.runmate.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.graphics.Color

// Ánh xạ sang Material 3 theo DESIGN 4.4. Các vai trò DESIGN không nhắc tới cũng gán màu
// trung tính của RunMate, để không lẫn màu tím mặc định của Material 3.
private val LightColors = lightColorScheme(
    primary = LightInverse,
    onPrimary = LightOnInverse,
    primaryContainer = LightSurfaceContainerHigh,
    onPrimaryContainer = LightTextPrimary,
    inversePrimary = DarkInverse,
    secondary = LightTextSecondary,
    onSecondary = LightOnInverse,
    secondaryContainer = LightSurfaceContainerHigh,
    onSecondaryContainer = LightTextPrimary,
    tertiary = LightTextSecondary,
    onTertiary = LightOnInverse,
    tertiaryContainer = LightSurfaceContainerHigh,
    onTertiaryContainer = LightTextPrimary,
    background = LightBackground,
    onBackground = LightTextPrimary,
    surface = LightBackground,
    onSurface = LightTextPrimary,
    surfaceVariant = LightSurfaceContainer,
    onSurfaceVariant = LightTextSecondary,
    surfaceTint = Color.Transparent,
    inverseSurface = LightInverse,
    inverseOnSurface = LightOnInverse,
    error = LightError,
    onError = LightOnInverse,
    errorContainer = LightErrorContainer,
    onErrorContainer = LightError,
    outline = LightOutlineStrong,
    outlineVariant = LightOutline,
    surfaceBright = LightBackground,
    surfaceDim = LightSurfaceContainerHigh,
    surfaceContainerLowest = LightBackground,
    surfaceContainerLow = LightSurfaceContainer,
    surfaceContainer = LightSurfaceContainer,
    surfaceContainerHigh = LightSurfaceContainerHigh,
    surfaceContainerHighest = LightSurfaceContainerHigh,
)

private val DarkColors = darkColorScheme(
    primary = DarkInverse,
    onPrimary = DarkOnInverse,
    primaryContainer = DarkSurfaceContainerHigh,
    onPrimaryContainer = DarkTextPrimary,
    inversePrimary = LightInverse,
    secondary = DarkTextSecondary,
    onSecondary = DarkOnInverse,
    secondaryContainer = DarkSurfaceContainerHigh,
    onSecondaryContainer = DarkTextPrimary,
    tertiary = DarkTextSecondary,
    onTertiary = DarkOnInverse,
    tertiaryContainer = DarkSurfaceContainerHigh,
    onTertiaryContainer = DarkTextPrimary,
    background = DarkBackground,
    onBackground = DarkTextPrimary,
    surface = DarkBackground,
    onSurface = DarkTextPrimary,
    surfaceVariant = DarkSurfaceContainer,
    onSurfaceVariant = DarkTextSecondary,
    surfaceTint = Color.Transparent,
    inverseSurface = DarkInverse,
    inverseOnSurface = DarkOnInverse,
    error = DarkError,
    onError = DarkOnInverse,
    errorContainer = DarkErrorContainer,
    onErrorContainer = DarkError,
    outline = DarkOutlineStrong,
    outlineVariant = DarkOutline,
    surfaceBright = DarkSurfaceContainerHigh,
    surfaceDim = DarkBackground,
    surfaceContainerLowest = DarkBackground,
    surfaceContainerLow = DarkSurfaceContainer,
    surfaceContainer = DarkSurfaceContainer,
    surfaceContainerHigh = DarkSurfaceContainerHigh,
    surfaceContainerHighest = DarkSurfaceContainerHigh,
)

// Sáng hoặc tối theo cài đặt hệ thống (SPEC 10.5). Không dùng màu động (DESIGN 4, 13.5)
@Composable
fun RunMateTheme(darkTheme: Boolean = isSystemInDarkTheme(), content: @Composable () -> Unit) {
    CompositionLocalProvider(LocalRunMateColors provides if (darkTheme) DarkRunMateColors else LightRunMateColors) {
        MaterialTheme(
            colorScheme = if (darkTheme) DarkColors else LightColors,
            typography = RunMateTypography,
            shapes = RunMateShapes,
            content = content,
        )
    }
}

// Màu riêng của RunMate, dùng như MaterialTheme.colorScheme: RunMateTheme.colors.success
object RunMateTheme {
    val colors: RunMateColors
        @Composable
        @ReadOnlyComposable
        get() = LocalRunMateColors.current
}
