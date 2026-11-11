package com.example.harry_potter.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.Shapes
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp

private val DarkColorScheme = darkColorScheme(
    primary = Parchment,
    onPrimary = Ink,
    primaryContainer = SpellRedDark,
    onPrimaryContainer = Parchment,
    secondary = VictoryGold,
    onSecondary = Ink,
    tertiary = SpellRed,
    onTertiary = Parchment,
    background = Ink,
    onBackground = Parchment,
    surface = Charcoal,
    onSurface = Parchment,
    surfaceVariant = RaisedInk,
    onSurfaceVariant = MutedParchment,
    outline = LineGold,
    error = SpellRed,
    onError = Parchment
)

private val HogwartsShapes = Shapes(
    extraSmall = androidx.compose.foundation.shape.RoundedCornerShape(4.dp),
    small = androidx.compose.foundation.shape.RoundedCornerShape(6.dp),
    medium = androidx.compose.foundation.shape.RoundedCornerShape(8.dp),
    large = androidx.compose.foundation.shape.RoundedCornerShape(10.dp),
    extraLarge = androidx.compose.foundation.shape.RoundedCornerShape(14.dp)
)

@Composable
fun HogwartsLegacy2Theme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = DarkColorScheme,
        typography = Typography,
        shapes = HogwartsShapes,
        content = content
    )
}
