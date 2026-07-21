package de.pattaku.otakupulse.app.ui.watchlist

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Badge
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import de.pattaku.otakupulse.app.data.local.WatchStatus
import de.pattaku.otakupulse.app.data.local.WatchlistEntry

private val TABS = listOf(
    WatchStatus.WATCHING to "Am Schauen",
    WatchStatus.PLANNED to "Geplant",
    WatchStatus.COMPLETED to "Fertig",
    WatchStatus.DROPPED to "Abgebrochen",
)

@Composable
fun WatchlistScreen(viewModel: WatchlistViewModel, onOeffneAnime: (Int) -> Unit = {}) {
    var tab by remember { mutableIntStateOf(0) }
    val entries by viewModel.entries.collectAsStateWithLifecycle()
    val status = TABS[tab].first
    val gefiltert = entries.filter { it.status == status }

    Column(Modifier.fillMaxSize()) {
        ScrollableTabRow(selectedTabIndex = tab, edgePadding = 12.dp) {
            TABS.forEachIndexed { index, (s, label) ->
                val anzahl = entries.count { it.status == s }
                Tab(
                    selected = tab == index,
                    onClick = { tab = index },
                    text = { Text(if (anzahl > 0) "$label ($anzahl)" else label) },
                )
            }
        }

        if (gefiltert.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    leerText(status),
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(32.dp),
                )
            }
        } else {
            LazyColumn(
                contentPadding = androidx.compose.foundation.layout.PaddingValues(12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                items(gefiltert, key = { it.animeId }) { entry ->
                    WatchlistZeile(
                        entry = entry,
                        onKlick = { onOeffneAnime(entry.animeId) },
                        onFolgePlus = { viewModel.folgeAbhaken(entry) },
                        onStatus = { viewModel.setzeStatus(entry.animeId, it) },
                    )
                }
            }
        }
    }
}

private fun leerText(status: WatchStatus): String = when (status) {
    WatchStatus.WATCHING -> "Nichts am Laufen. Setze einen geplanten Titel auf „Am Schauen“."
    WatchStatus.PLANNED -> "Noch nichts gemerkt. Wisch im Stapel nach rechts."
    WatchStatus.COMPLETED -> "Noch nichts durchgeschaut."
    WatchStatus.DROPPED -> "Nichts abgebrochen — bisher."
}

@Composable
private fun WatchlistZeile(
    entry: WatchlistEntry,
    onKlick: () -> Unit,
    onFolgePlus: () -> Unit,
    onStatus: (WatchStatus) -> Unit,
) {
    Card(modifier = Modifier.clickable(onClick = onKlick)) {
        Row(Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
            AsyncImage(
                model = entry.coverImageUrl,
                contentDescription = entry.title,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(width = 56.dp, height = 80.dp)
                    .clip(RoundedCornerShape(8.dp)),
            )
            Spacer(Modifier.width(12.dp))

            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        entry.title,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.weight(1f, fill = false),
                    )
                    if (entry.hasUnseenEpisode) {
                        Spacer(Modifier.width(6.dp))
                        Badge { Text("neu") }
                    }
                }
                Spacer(Modifier.height(4.dp))
                Text(
                    fortschritt(entry),
                    style = MaterialTheme.typography.bodySmall,
                )
                Row {
                    if (entry.status != WatchStatus.WATCHING) {
                        TextButton(onClick = { onStatus(WatchStatus.WATCHING) }) { Text("Schauen") }
                    }
                    if (entry.status != WatchStatus.COMPLETED) {
                        TextButton(onClick = { onStatus(WatchStatus.COMPLETED) }) { Text("Fertig") }
                    }
                }
            }

            if (entry.status == WatchStatus.WATCHING) {
                TextButton(onClick = onFolgePlus) { Text("+1") }
            }
        }
    }
}

private fun fortschritt(entry: WatchlistEntry): String {
    val gesamt = entry.episodes
    val basis = if (gesamt != null) "Folge ${entry.progress}/$gesamt" else "Folge ${entry.progress}"
    val rest = listOfNotNull(entry.seasonYear?.toString(), entry.format).joinToString(" · ")
    return if (rest.isEmpty()) basis else "$basis  ·  $rest"
}
