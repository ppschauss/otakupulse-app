package de.pattaku.otakupulse.app.ui.calendar

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import de.pattaku.otakupulse.app.domain.Airing
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

private val UHRZEIT = DateTimeFormatter.ofPattern("HH:mm")

@Composable
fun CalendarScreen(viewModel: CalendarViewModel) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    when {
        state.laden -> Box(Modifier.fillMaxSize(), Alignment.Center) { CircularProgressIndicator() }

        state.fehler != null -> Box(Modifier.fillMaxSize(), Alignment.Center) {
            Text(
                state.fehler!!,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(32.dp),
            )
        }

        state.tage.isEmpty() -> Box(Modifier.fillMaxSize(), Alignment.Center) {
            Text(
                "Diese Woche steht nichts an.",
                modifier = Modifier.padding(32.dp),
            )
        }

        else -> LazyColumn(
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            state.tage.forEach { tag ->
                item(key = "tag-${tag.datum}") {
                    Spacer(Modifier.height(6.dp))
                    Text(
                        tagesUeberschrift(tag.datum),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                    )
                    HorizontalDivider(Modifier.padding(vertical = 4.dp))
                }
                items(tag.folgen, key = { "${tag.datum}-${it.animeId}-${it.episode}" }) { folge ->
                    FolgenZeile(folge)
                }
            }
        }
    }
}

/** Heute und morgen werden benannt — das liest sich schneller als ein Datum. */
private fun tagesUeberschrift(datum: LocalDate): String {
    val heute = LocalDate.now()
    return when (datum) {
        heute -> "Heute"
        heute.plusDays(1) -> "Morgen"
        else -> datum.format(DateTimeFormatter.ofPattern("EEEE, d. MMMM", Locale.GERMAN))
    }
}

@Composable
private fun FolgenZeile(folge: Airing) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            folge.airingAt.atZone(ZoneId.systemDefault()).format(UHRZEIT),
            style = MaterialTheme.typography.labelLarge,
            modifier = Modifier.width(52.dp),
        )
        AsyncImage(
            model = folge.coverImageUrl,
            contentDescription = folge.title,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .size(width = 34.dp, height = 48.dp)
                .clip(RoundedCornerShape(6.dp)),
        )
        Spacer(Modifier.width(10.dp))
        Column {
            Text(folge.title, style = MaterialTheme.typography.bodyMedium, maxLines = 2)
            folge.episode?.let {
                Text("Folge $it", style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}
