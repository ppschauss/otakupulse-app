package de.pattaku.otakupulse.app.ui.theme

import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Wie breit das Fenster gerade ist.
 *
 * Bewusst aus der Fensterbreite abgeleitet und nicht aus dem Gerätemodell: ein
 * Tablet im geteilten Bildschirm ist genauso schmal wie ein Handy, und ein
 * gefaltetes Foldable wechselt im Betrieb.
 */
enum class Breite { KOMPAKT, MITTEL, WEIT }

val LocalBreite = staticCompositionLocalOf { Breite.KOMPAKT }

fun WindowWidthSizeClass.zuBreite(): Breite = when (this) {
    WindowWidthSizeClass.Compact -> Breite.KOMPAKT
    WindowWidthSizeClass.Medium -> Breite.MITTEL
    else -> Breite.WEIT
}

/**
 * Höchstbreite für Lesetexte und Formulare.
 *
 * Auf einem 4K-Tablet über die volle Breite gesetzter Fliesstext ist unlesbar —
 * das Auge verliert beim Zeilenwechsel den Anschluss.
 */
val Breite.inhaltsBreite: Dp
    get() = when (this) {
        Breite.KOMPAKT -> Dp.Unspecified
        Breite.MITTEL -> 640.dp
        Breite.WEIT -> 760.dp
    }

/** Wie viele Spalten eine Kachelliste bekommt. */
val Breite.spalten: Int
    get() = when (this) {
        Breite.KOMPAKT -> 1
        Breite.MITTEL -> 2
        Breite.WEIT -> 3
    }

/** Höchstbreite der Swipe-Karte — sie soll auf dem Tablet nicht ins Absurde wachsen. */
val Breite.kartenBreite: Dp
    get() = when (this) {
        Breite.KOMPAKT -> Dp.Unspecified
        Breite.MITTEL -> 420.dp
        Breite.WEIT -> 480.dp
    }

