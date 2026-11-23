package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

private val MonochromeDarkColorScheme = darkColorScheme(
    primary = PureWhite,
    onPrimary = PureBlack,
    primaryContainer = CharcoalHighest,
    onPrimaryContainer = PureWhite,
    secondary = SoftWhite,
    onSecondary = PureBlack,
    secondaryContainer = CharcoalElevated,
    onSecondaryContainer = SoftWhite,
    tertiary = MutedSilver,
    onTertiary = PureBlack,
    tertiaryContainer = CharcoalElevated,
    onTertiaryContainer = SoftWhite,
    error = SoftWhite,
    onError = PureBlack,
    errorContainer = CharcoalHighest,
    onErrorContainer = PureWhite,
    background = PureBlack,
    onBackground = PureWhite,
    surface = CharcoalSurface,
    onSurface = PureWhite,
    surfaceVariant = CharcoalElevated,
    onSurfaceVariant = MutedSilver,
    surfaceDim = PureBlack,
    surfaceBright = CharcoalHighest,
    surfaceContainerLowest = PureBlack,
    surfaceContainerLow = CharcoalSurface,
    surfaceContainer = CharcoalElevated,
    surfaceContainerHigh = CharcoalHighest,
    surfaceContainerHighest = CharcoalBorder,
    outline = CharcoalBorder,
    outlineVariant = CharcoalBorderSubtle
)

private val MonochromeLightColorScheme = lightColorScheme(
    primary = InkBlack,
    onPrimary = PaperWhite,
    primaryContainer = StoneSurface,
    onPrimaryContainer = InkBlack,
    secondary = CharcoalText,
    onSecondary = PaperWhite,
    secondaryContainer = AlabasterSurface,
    onSecondaryContainer = CharcoalText,
    tertiary = SlateGrayText,
    onTertiary = PaperWhite,
    tertiaryContainer = StoneSurface,
    onTertiaryContainer = CharcoalText,
    error = InkBlack,
    onError = PaperWhite,
    errorContainer = StoneSurface,
    onErrorContainer = InkBlack,
    background = PaperWhite,
    onBackground = InkBlack,
    surface = PaperWhite,
    onSurface = InkBlack,
    surfaceVariant = AlabasterSurface,
    onSurfaceVariant = SlateGrayText,
    surfaceDim = StoneSurface,
    surfaceBright = PaperWhite,
    surfaceContainerLowest = PaperWhite,
    surfaceContainerLow = AlabasterSurface,
    surfaceContainer = StoneSurface,
    surfaceContainerHigh = StoneHighest,
    surfaceContainerHighest = LightBorder,
    outline = LightBorder,
    outlineVariant = LightBorderSubtle
)

val JarvisShapes = Shapes(
    extraSmall = RoundedCornerShape(6.dp),
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(14.dp),
    large = RoundedCornerShape(18.dp),
    extraLarge = RoundedCornerShape(24.dp)
)

@Immutable
data class JarvisSpacing(
    val xs: Dp = 4.dp,
    val sm: Dp = 8.dp,
    val md: Dp = 16.dp,
    val lg: Dp = 24.dp,
    val xl: Dp = 32.dp
)

val LocalJarvisSpacing = staticCompositionLocalOf { JarvisSpacing() }

@Composable
fun JarvisCoreTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> MonochromeDarkColorScheme
        else -> MonochromeLightColorScheme
    }

    CompositionLocalProvider(LocalJarvisSpacing provides JarvisSpacing()) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = JarvisTypography,
            shapes = JarvisShapes,
            content = content
        )
    }
}
