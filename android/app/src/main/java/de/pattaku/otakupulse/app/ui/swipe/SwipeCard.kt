package de.pattaku.otakupulse.app.ui.swipe

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import de.pattaku.otakupulse.app.domain.Anime
import de.pattaku.otakupulse.app.domain.SwipeDirection
import de.pattaku.otakupulse.app.ui.theme.SwipeLike
import de.pattaku.otakupulse.app.ui.theme.SwipeNope
import de.pattaku.otakupulse.app.ui.theme.SwipeSuper
import kotlinx.coroutines.launch
import kotlin.math.abs

/**
 * Eine Swipe-Karte, die dem Finger folgt.
 *
 * Bewusst selbst gebaut statt einer Fremdbibliothek: es sind rund hundert Zeilen,
 * und die App hängt nicht an einem Paket, das irgendwann nicht mehr gepflegt wird.
 */
@Composable
fun SwipeCard(
    anime: Anime,
    onSwiped: (SwipeDirection) -> Unit,
    onTap: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val scope = rememberCoroutineScope()
    val offsetX = remember(anime.id) { Animatable(0f) }
    val offsetY = remember(anime.id) { Animatable(0f) }
    var dismissed by remember(anime.id) { mutableStateOf(false) }

    val screenWidthPx = with(LocalDensity.current) {
        LocalConfiguration.current.screenWidthDp.dp.toPx()
    }
    // Ab einem Viertel der Bildschirmbreite gilt die Karte als weggewischt.
    val threshold = screenWidthPx / 4f
    val superThreshold = threshold * 1.2f

    fun fling(direction: SwipeDirection) {
        if (dismissed) return
        dismissed = true
        scope.launch {
            val targetX = when (direction) {
                SwipeDirection.LEFT -> -screenWidthPx * 1.5f
                SwipeDirection.RIGHT -> screenWidthPx * 1.5f
                SwipeDirection.SUPER -> offsetX.value
            }
            val targetY = if (direction == SwipeDirection.SUPER) -screenWidthPx * 2f else offsetY.value
            launch { offsetX.animateTo(targetX, tween(220)) }
            offsetY.animateTo(targetY, tween(220))
            onSwiped(direction)
        }
    }

    val rotation = (offsetX.value / screenWidthPx) * 18f

    Box(
        modifier = modifier
            .graphicsLayer {
                translationX = offsetX.value
                translationY = offsetY.value
                rotationZ = rotation
            }
            .pointerInput(anime.id) {
                detectDragGestures(
                    onDragEnd = {
                        when {
                            -offsetY.value > superThreshold -> fling(SwipeDirection.SUPER)
                            offsetX.value > threshold -> fling(SwipeDirection.RIGHT)
                            offsetX.value < -threshold -> fling(SwipeDirection.LEFT)
                            else -> scope.launch {
                                launch { offsetX.animateTo(0f, tween(180)) }
                                offsetY.animateTo(0f, tween(180))
                            }
                        }
                    },
                    onDrag = { change, delta ->
                        change.consume()
                        scope.launch {
                            offsetX.snapTo(offsetX.value + delta.x)
                            offsetY.snapTo(offsetY.value + delta.y)
                        }
                    },
                )
            }
            .pointerInput(anime.id) {
                detectTapGestures(onTap = { onTap() })
            },
    ) {
        CardContent(anime)
        SwipeOverlay(offsetX.value, offsetY.value, threshold, superThreshold)
    }
}

@Composable
private fun CardContent(anime: Anime) {
    Card(shape = RoundedCornerShape(24.dp), modifier = Modifier.fillMaxSize()) {
        Box(Modifier.fillMaxSize()) {
            AsyncImage(
                model = anime.coverImageUrl,
                contentDescription = anime.title,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
            // Verlauf nach unten, damit die weiße Schrift auf jedem Cover lesbar bleibt.
            Box(
                Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            0.45f to Color.Transparent,
                            1f to Color.Black.copy(alpha = 0.88f),
                        ),
                    ),
            )
            Column(
                Modifier
                    .align(Alignment.BottomStart)
                    .padding(20.dp),
            ) {
                Text(
                    text = anime.title,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    text = listOfNotNull(
                        anime.seasonYear?.toString(),
                        anime.format,
                        anime.episodes?.let { "$it Folgen" },
                        anime.averageScore?.let { "★ $it" },
                    ).joinToString(" · "),
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.85f),
                )
                anime.description?.let { text ->
                    Spacer(Modifier.height(10.dp))
                    Text(
                        text = text.replace(Regex("<[^>]*>"), "").take(180) + "…",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.75f),
                        maxLines = 4,
                    )
                }
            }
        }
    }
}

/** Farbige Rückmeldung, während gezogen wird. */
@Composable
private fun SwipeOverlay(x: Float, y: Float, threshold: Float, superThreshold: Float) {
    val superProgress = (-y / superThreshold).coerceIn(0f, 1f)
    val sideProgress = (abs(x) / threshold).coerceIn(0f, 1f)

    val (color, label, strength) = when {
        superProgress > sideProgress && superProgress > 0.05f ->
            Triple(SwipeSuper, "SUPER", superProgress)
        x > 0 -> Triple(SwipeLike, "MERKEN", sideProgress)
        else -> Triple(SwipeNope, "NÖ", sideProgress)
    }
    if (strength <= 0.05f) return

    Box(
        Modifier
            .fillMaxSize()
            .alpha(strength * 0.55f)
            .background(color, RoundedCornerShape(24.dp)),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.displaySmall,
            fontWeight = FontWeight.Black,
            color = Color.White,
        )
    }
}
