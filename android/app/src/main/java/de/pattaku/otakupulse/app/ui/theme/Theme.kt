package de.pattaku.otakupulse.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// Markenfarben von OtakuPulse — bewusst Pink/Violett statt Crunchyroll-Orange.
val OtakuPink = Color(0xFFFF4D6D)
val OtakuViolet = Color(0xFF8B5CF6)

// Rückmeldung beim Wischen.
val SwipeLike = Color(0xFF22C55E)
val SwipeNope = Color(0xFFEF4444)
val SwipeSuper = OtakuViolet

private val DarkColors = darkColorScheme(
    primary = OtakuPink,
    secondary = OtakuViolet,
    background = Color(0xFF0F0F14),
    surface = Color(0xFF1A1A22),
)

private val LightColors = lightColorScheme(
    primary = OtakuPink,
    secondary = OtakuViolet,
)

@Composable
fun CompanionTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        content = content,
    )
}
