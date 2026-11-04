package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp

private val ConduitDarkColorScheme = darkColorScheme(
    primary = BeeperPrimaryDark,
    onPrimary = BeeperOnPrimaryDark,
    primaryContainer = BeeperContainerDark,
    onPrimaryContainer = BeeperOnContainerDark,
    secondary = DarkMutedForeground,
    onSecondary = DarkBackground,
    secondaryContainer = DarkSurfaceContainerHigh,
    onSecondaryContainer = DarkForeground,
    tertiary = BeeperPrimaryDark,
    onTertiary = BeeperOnPrimaryDark,
    background = DarkBackground,
    onBackground = DarkForeground,
    surface = DarkSurface,
    onSurface = DarkForeground,
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = DarkMutedForeground,
    surfaceContainer = DarkSurfaceContainer,
    surfaceContainerHigh = DarkSurfaceContainerHigh,
    outline = DarkBorder,
    outlineVariant = DarkBorder,
    error = ErrorDark
)

private val ConduitLightColorScheme = lightColorScheme(
    primary = BeeperPrimaryLight,
    onPrimary = BeeperOnPrimaryLight,
    primaryContainer = BeeperContainerLight,
    onPrimaryContainer = BeeperOnContainerLight,
    secondary = LightMutedForeground,
    onSecondary = LightSurface,
    secondaryContainer = LightSurfaceContainerHigh,
    onSecondaryContainer = LightForeground,
    tertiary = BeeperPrimaryLight,
    onTertiary = BeeperOnPrimaryLight,
    background = LightBackground,
    onBackground = LightForeground,
    surface = LightSurface,
    onSurface = LightForeground,
    surfaceVariant = LightSurfaceVariant,
    onSurfaceVariant = LightMutedForeground,
    surfaceContainer = LightSurfaceContainer,
    surfaceContainerHigh = LightSurfaceContainerHigh,
    outline = LightBorder,
    outlineVariant = LightBorder,
    error = ErrorLight
)

val ConduitShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(14.dp),
    large = RoundedCornerShape(20.dp),
    extraLarge = RoundedCornerShape(28.dp)
)

@Composable
fun ConduitTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) ConduitDarkColorScheme else ConduitLightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        shapes = ConduitShapes,
        content = content
    )
}

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    ConduitTheme(darkTheme = darkTheme, content = content)
}
