package de.pattaku.otakupulse.app.ui.theme

import android.content.Context
import android.provider.Settings
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.EaseOutQuart
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Ob das System Animationen abgeschaltet hat.
 *
 * Android bündelt „Animationen entfernen" in der Bedienungshilfe unter dieser
 * Einstellung. Sie zu ignorieren ist kein Detail: für Menschen mit
 * vestibulären Beschwerden können bewegte Flächen Übelkeit auslösen.
 */
fun animationenAus(context: Context): Boolean =
    Settings.Global.getFloat(
        context.contentResolver,
        Settings.Global.ANIMATOR_DURATION_SCALE,
        1f,
    ) == 0f

/**
 * Auftakt beim App-Start: der Puls-Ring schlägt zweimal, der Schriftzug fährt ein.
 *
 * Bewusst kurz (rund 1,4 s) und einmalig pro Kaltstart — niemand will beim
 * zweiten Öffnen am selben Abend eine Show sehen.
 */
@Composable
fun IntroScreen(onFertig: () -> Unit) {
    val context = LocalContext.current
    val ohneBewegung = remember { animationenAus(context) }

    val ringSkalierung = remember { Animatable(0.65f) }
    val ringDeckkraft = remember { Animatable(0f) }
    val textDeckkraft = remember { Animatable(0f) }
    val textVersatz = remember { Animatable(14f) }

    LaunchedEffect(Unit) {
        if (ohneBewegung) {
            onFertig()
            return@LaunchedEffect
        }
        launch {
            ringDeckkraft.animateTo(1f, tween(260, easing = EaseOutQuart))
            ringSkalierung.animateTo(1.06f, tween(420, easing = EaseOutQuart))
            ringSkalierung.animateTo(1f, tween(260, easing = EaseOutQuart))
        }
        delay(260)
        launch { textDeckkraft.animateTo(1f, tween(340, easing = EaseOutQuart)) }
        textVersatz.animateTo(0f, tween(420, easing = EaseOutQuart))
        delay(520)
        onFertig()
    }

    Box(
        Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(contentAlignment = Alignment.Center) {
                Canvas(
                    Modifier
                        .size(96.dp)
                        .scale(ringSkalierung.value)
                        .alpha(ringDeckkraft.value),
                ) {
                    // Zwei Ringe im Markenverlauf — der Puls von „OtakuPulse".
                    drawCircle(
                        brush = Brush.linearGradient(
                            listOf(OtakuPink, OtakuViolet),
                            start = Offset.Zero,
                            end = Offset(size.width, size.height),
                        ),
                        radius = size.minDimension / 2f,
                        style = androidx.compose.ui.graphics.drawscope.Stroke(width = 6f * density),
                    )
                    drawCircle(
                        color = OtakuPink.copy(alpha = 0.22f),
                        radius = size.minDimension / 3.1f,
                    )
                }
            }

            Spacer(Modifier.height(28.dp))

            Column(
                Modifier
                    .alpha(textDeckkraft.value)
                    .androidxOffsetY(textVersatz.value),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    "OtakuPulse",
                    fontWeight = FontWeight.Black,
                    fontSize = 30.sp,
                    letterSpacing = (-0.8).sp,
                    color = MaterialTheme.colorScheme.onBackground,
                )
                Text(
                    "Companion",
                    fontWeight = FontWeight.Medium,
                    fontSize = 14.sp,
                    letterSpacing = 5.sp,
                    textAlign = TextAlign.Center,
                    color = OtakuPink,
                )
            }
        }
    }
}

/** Vertikaler Versatz über die Zeichenebene — verschiebt, ohne das Layout neu zu berechnen. */
private fun Modifier.androidxOffsetY(dpWert: Float): Modifier =
    this.graphicsLayer { translationY = dpWert * density }

/**
 * Ladeanzeige im Markenstil: drei Punkte, die nacheinander pulsieren.
 *
 * Für Momente ohne Platzhalterstruktur (Kalender, Party). Wo eine Karte oder
 * Liste geladen wird, ist ein Gerüst die bessere Wahl — siehe [SkelettKarte].
 */
@Composable
fun MarkenLadeanzeige(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    if (remember { animationenAus(context) }) {
        Text("Lädt…", style = MaterialTheme.typography.bodyMedium, modifier = modifier)
        return
    }

    val transition = rememberInfiniteTransition(label = "laden")
    Box(modifier, contentAlignment = Alignment.Center) {
        androidx.compose.foundation.layout.Row(
            horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(7.dp),
        ) {
            repeat(3) { index ->
                val wert by transition.animateFloat(
                    initialValue = 0.3f,
                    targetValue = 1f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(560, delayMillis = index * 140, easing = LinearEasing),
                        repeatMode = RepeatMode.Reverse,
                    ),
                    label = "punkt$index",
                )
                Box(
                    Modifier
                        .size(9.dp)
                        .alpha(wert)
                        .background(
                            if (index == 1) OtakuViolet else OtakuPink,
                            androidx.compose.foundation.shape.CircleShape,
                        ),
                )
            }
        }
    }
}

/** Sanft schimmernde Fläche als Platzhalter — ruhiger als ein Kreisel im Inhalt. */
@Composable
fun SchimmerFlaeche(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val basis = MaterialTheme.colorScheme.surfaceVariant
    if (remember { animationenAus(context) }) {
        Box(modifier.background(basis))
        return
    }

    val transition = rememberInfiniteTransition(label = "schimmer")
    val fortschritt by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(1400, easing = LinearEasing)),
        label = "verlauf",
    )
    Box(
        modifier.background(
            Brush.linearGradient(
                colors = listOf(basis, basis.copy(alpha = 0.45f), basis),
                start = Offset(fortschritt * 600f - 300f, 0f),
                end = Offset(fortschritt * 600f, 300f),
            ),
        ),
    )
}
