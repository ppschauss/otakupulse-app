package de.pattaku.otakupulse.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// Markenfarben von OtakuPulse — bewusst Pink/Violett statt Crunchyroll-Orange.
val OtakuPink = Color(0xFFFF4D6D)
val OtakuViolet = Color(0xFF8B5CF6)

// Rückmeldung beim Wischen.
val SwipeLike = Color(0xFF22C55E)
val SwipeNope = Color(0xFFEF4444)
val SwipeSuper = OtakuViolet

// Zinc-Palette wie auf der Website (Tailwind zinc-50 … zinc-950).
private val Zinc50 = Color(0xFFFAFAFA)
private val Zinc100 = Color(0xFFF4F4F5)
private val Zinc200 = Color(0xFFE4E4E7)
private val Zinc400 = Color(0xFFA1A1AA)
private val Zinc500 = Color(0xFF71717A)
private val Zinc700 = Color(0xFF3F3F46)
private val Zinc800 = Color(0xFF27272A)
private val Zinc900 = Color(0xFF18181B)
private val Zinc950 = Color(0xFF09090B)

/**
 * Dunkel ist die Leitfassung — die Website steht fest auf `color-scheme: dark`,
 * und Anime-Cover wirken auf dunklem Grund kräftiger.
 */
private val DarkColors = darkColorScheme(
    primary = OtakuPink,
    onPrimary = Color.White,
    primaryContainer = Color(0xFF4A1523),
    onPrimaryContainer = Color(0xFFFFD9E0),
    secondary = OtakuViolet,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFF2E1F52),
    onSecondaryContainer = Color(0xFFE5DBFF),
    background = Zinc950,
    onBackground = Zinc100,
    surface = Zinc900,
    onSurface = Zinc100,
    surfaceVariant = Zinc800,
    // Kein Zinc-500 hier: auf Zinc-800 käme das nur auf 3,6:1 und wäre für
    // Fließtext zu schwach. Zinc-400 liegt bei 6,8:1.
    onSurfaceVariant = Zinc400,
    surfaceContainer = Zinc800,
    surfaceContainerHigh = Zinc700,
    outline = Zinc700,
    outlineVariant = Zinc800,
    error = SwipeNope,
)

/** Helle Fassung: dieselben Akzente, neutraler Grund, keine bloße Umkehrung. */
private val LightColors = lightColorScheme(
    primary = Color(0xFFD11F45),      // dunkler als das Marken-Pink; auf Weiß sonst nur 3,1:1
    onPrimary = Color.White,
    primaryContainer = Color(0xFFFFD9E0),
    onPrimaryContainer = Color(0xFF3F0715),
    secondary = Color(0xFF6D3FE0),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFE9E0FF),
    onSecondaryContainer = Color(0xFF23104F),
    background = Zinc50,
    onBackground = Zinc950,
    surface = Color.White,
    onSurface = Zinc950,
    surfaceVariant = Zinc100,
    onSurfaceVariant = Zinc500,
    surfaceContainer = Zinc100,
    surfaceContainerHigh = Zinc200,
    outline = Zinc400,
    outlineVariant = Zinc200,
    error = Color(0xFFC62828),
)

/** `rounded-xl` von der Website entspricht 12 dp; Karten dürfen runder sein. */
private val CompanionShapes = Shapes(
    extraSmall = RoundedCornerShape(6.dp),
    small = RoundedCornerShape(8.dp),
    medium = RoundedCornerShape(12.dp),
    large = RoundedCornerShape(16.dp),
    extraLarge = RoundedCornerShape(24.dp),
)

/**
 * Material-Typenskala mit engem Verhältnis (~1,15).
 * In der Produkt-Oberfläche gibt es viele Textrollen — starke Kontraste erzeugen Unruhe.
 */
private val CompanionTypography = Typography().run {
    copy(
        headlineSmall = headlineSmall.copy(fontWeight = FontWeight.Bold, letterSpacing = (-0.4).sp),
        titleLarge = titleLarge.copy(fontWeight = FontWeight.SemiBold),
        titleMedium = titleMedium.copy(fontWeight = FontWeight.SemiBold),
        labelLarge = labelLarge.copy(fontWeight = FontWeight.Medium),
    )
}

/** Welche Fassung gilt. */
enum class ThemeModus { SYSTEM, HELL, DUNKEL }

@Composable
fun CompanionTheme(
    modus: ThemeModus = ThemeModus.SYSTEM,
    content: @Composable () -> Unit,
) {
    val dunkel = when (modus) {
        ThemeModus.SYSTEM -> isSystemInDarkTheme()
        ThemeModus.HELL -> false
        ThemeModus.DUNKEL -> true
    }
    MaterialTheme(
        colorScheme = if (dunkel) DarkColors else LightColors,
        shapes = CompanionShapes,
        typography = CompanionTypography,
        content = content,
    )
}

/** Für Vorschauen und Stellen ohne Modus-Angabe. */
val LeereTextStyle = TextStyle.Default
