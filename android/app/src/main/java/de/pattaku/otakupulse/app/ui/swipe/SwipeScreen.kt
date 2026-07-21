package de.pattaku.otakupulse.app.ui.swipe

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.IconButton
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import de.pattaku.otakupulse.app.domain.Anime
import de.pattaku.otakupulse.app.domain.SwipeDirection
import de.pattaku.otakupulse.app.domain.anzahlAktiv
import de.pattaku.otakupulse.app.ui.theme.SwipeLike
import de.pattaku.otakupulse.app.ui.theme.SwipeNope
import de.pattaku.otakupulse.app.ui.theme.SwipeSuper

@Composable
fun SwipeScreen(
    viewModel: SwipeViewModel,
    onOpenDetail: (Anime) -> Unit,
    modifier: Modifier = Modifier,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var filterOffen by remember { mutableStateOf(false) }

    if (filterOffen) {
        FilterSheet(
            filter = state.filter,
            genres = state.genres,
            tags = state.tags,
            onAnwenden = { viewModel.setzeFilter(it); filterOffen = false },
            onSchliessen = { filterOffen = false },
        )
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            val aktiv = state.filter.anzahlAktiv()
            Text(
                if (aktiv == 0) "Alle Anime" else "Gefiltert",
                style = MaterialTheme.typography.titleMedium,
            )
            Spacer(Modifier.weight(1f))
            BadgedBox(badge = { if (aktiv > 0) Badge { Text("$aktiv") } }) {
                IconButton(onClick = { filterOffen = true }) {
                    Icon(Icons.Default.FilterList, contentDescription = "Filter")
                }
            }
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            contentAlignment = Alignment.Center,
        ) {
            when {
                state.loading -> CircularProgressIndicator()

                state.error != null -> Hinweis(
                    titel = "Da klemmt etwas",
                    // Fast immer ist die Serveradresse schuld — deshalb der Verweis.
                    text = state.error!! + "\n\nStimmt die Adresse unter „Server“?",
                    knopf = "Nochmal versuchen",
                    onKnopf = viewModel::retry,
                )

                state.exhausted || state.cards.isEmpty() -> Hinweis(
                    titel = "Stapel leer",
                    text = "Keine weiteren Titel für diese Filter. Lockere sie etwas — " +
                        "dann kommt wieder Nachschub.",
                )

                else -> {
                    // Nur die obersten drei zeichnen; darunter sieht man ohnehin nichts.
                    // Rückwärts, damit die oberste Karte zuletzt und damit vorne landet.
                    state.cards.take(3).reversed().forEach { anime ->
                        val istOben = anime.id == state.cards.first().id
                        SwipeCard(
                            anime = anime,
                            onSwiped = { richtung -> viewModel.onSwiped(richtung) },
                            onTap = { onOpenDetail(anime) },
                            modifier = Modifier
                                .fillMaxWidth(if (istOben) 1f else 0.95f)
                                .aspectRatio(0.68f),
                        )
                    }
                }
            }
        }

        if (state.cards.isNotEmpty() && state.error == null) {
            Spacer(Modifier.height(16.dp))
            AktionsLeiste(
                onNope = { viewModel.onSwiped(SwipeDirection.LEFT) },
                onSuper = { viewModel.onSwiped(SwipeDirection.SUPER) },
                onLike = { viewModel.onSwiped(SwipeDirection.RIGHT) },
            )
        }
    }
}

/** Knöpfe als Alternative zum Wischen — einhändig und barrierefreundlicher. */
@Composable
private fun AktionsLeiste(onNope: () -> Unit, onSuper: () -> Unit, onLike: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly,
    ) {
        RundKnopf(Icons.Default.Close, "Kein Interesse", SwipeNope, onNope)
        RundKnopf(Icons.Default.Star, "Super-Swipe an die Party", SwipeSuper, onSuper)
        RundKnopf(Icons.Default.Favorite, "Auf die Watchlist", SwipeLike, onLike)
    }
}

@Composable
private fun RundKnopf(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    beschreibung: String,
    farbe: Color,
    onClick: () -> Unit,
) {
    FilledTonalIconButton(
        onClick = onClick,
        colors = IconButtonDefaults.filledTonalIconButtonColors(
            containerColor = farbe.copy(alpha = 0.18f),
            contentColor = farbe,
        ),
    ) {
        Icon(icon, contentDescription = beschreibung)
    }
}

@Composable
private fun Hinweis(titel: String, text: String, knopf: String? = null, onKnopf: () -> Unit = {}) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.padding(32.dp),
    ) {
        Text(titel, style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.height(8.dp))
        Text(text, style = MaterialTheme.typography.bodyMedium, textAlign = TextAlign.Center)
        if (knopf != null) {
            Spacer(Modifier.height(16.dp))
            Button(onClick = onKnopf) { Text(knopf) }
        }
    }
}
