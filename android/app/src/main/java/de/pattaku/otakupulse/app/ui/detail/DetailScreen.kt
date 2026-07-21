package de.pattaku.otakupulse.app.ui.detail

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import de.pattaku.otakupulse.app.domain.Anime

/** Detailansicht — beim Antippen einer Karte. */
@Composable
fun DetailScreen(anime: Anime, onBack: () -> Unit) {
    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
    ) {
        Box(Modifier.fillMaxWidth()) {
            AsyncImage(
                model = anime.bannerImageUrl ?: anime.coverImageUrl,
                contentDescription = anime.title,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(16f / 9f),
            )
            IconButton(onClick = onBack, modifier = Modifier.padding(8.dp)) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Zurück")
            }
        }

        Column(Modifier.padding(20.dp)) {
            Text(
                anime.title,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
            )
            if (anime.titleRomaji != anime.title) {
                Text(anime.titleRomaji, style = MaterialTheme.typography.bodyMedium)
            }

            Spacer(Modifier.height(12.dp))
            Text(
                listOfNotNull(
                    anime.seasonYear?.toString(),
                    anime.format,
                    anime.status,
                    anime.episodes?.let { "$it Folgen" },
                    anime.averageScore?.let { "★ $it" },
                ).joinToString(" · "),
                style = MaterialTheme.typography.labelLarge,
            )

            anime.description?.let {
                Spacer(Modifier.height(16.dp))
                // Die Beschreibungen aus der OtakuPulse-Datenbank enthalten HTML-Reste.
                Text(
                    it.replace(Regex("<br\\s*/?>"), "\n").replace(Regex("<[^>]*>"), ""),
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        }
    }
}
