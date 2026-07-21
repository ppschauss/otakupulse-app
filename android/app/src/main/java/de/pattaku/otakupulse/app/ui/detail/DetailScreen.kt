package de.pattaku.otakupulse.app.ui.detail

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material3.AssistChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import de.pattaku.otakupulse.app.data.api.AnimeDetailDto
import de.pattaku.otakupulse.app.data.api.RelatedDto
import de.pattaku.otakupulse.app.ui.theme.Breite
import de.pattaku.otakupulse.app.ui.theme.LocalBreite

/** Deutsche Beschriftungen für die Enum-Werte aus der Datenbank. */
private val FORMAT = mapOf(
    "TV" to "Serie", "TV_SHORT" to "Kurzserie", "MOVIE" to "Film",
    "OVA" to "OVA", "ONA" to "ONA", "SPECIAL" to "Special", "MUSIC" to "Musikvideo",
)
private val STATUS = mapOf(
    "FINISHED" to "Abgeschlossen", "RELEASING" to "Läuft gerade",
    "NOT_YET_RELEASED" to "Angekündigt", "CANCELLED" to "Abgebrochen", "HIATUS" to "Pausiert",
)
private val SEASON = mapOf(
    "WINTER" to "Winter", "SPRING" to "Frühling", "SUMMER" to "Sommer", "FALL" to "Herbst",
)
private val SPRACHE = mapOf(
    "ja" to "Japanisch", "de-dub" to "Deutsche Synchro", "de-sub" to "Deutscher Sub",
    "en-dub" to "Englische Synchro", "en-sub" to "Englischer Sub",
)
private val ANBIETER = mapOf(
    "crunchyroll" to "Crunchyroll", "netflix" to "Netflix", "amazon" to "Prime Video",
    "disney-plus" to "Disney+", "wow" to "WOW", "joyn" to "Joyn", "rtl-plus" to "RTL+",
    "hulu" to "Hulu", "bilibili" to "Bilibili",
)
private val RELATION = mapOf(
    "PREQUEL" to "Vorgänger", "SEQUEL" to "Fortsetzung", "SIDE_STORY" to "Nebengeschichte",
    "PARENT" to "Hauptreihe", "SPIN_OFF" to "Ableger", "ALTERNATIVE" to "Alternative",
    "ADAPTATION" to "Adaption", "OTHER" to "Verwandt",
)

@Composable
fun DetailScreen(
    anime: AnimeDetailDto,
    onBack: () -> Unit,
    onOeffneAnime: (Int) -> Unit = {},
) {
    val uri = LocalUriHandler.current

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
    ) {
        Kopf(anime, onBack)

        Column(Modifier.padding(horizontal = 20.dp)) {
            Spacer(Modifier.height(16.dp))

            // Kernangaben in einer Zeile — was man vor dem Anschauen wissen will.
            Text(
                listOfNotNull(
                    FORMAT[anime.format] ?: anime.format,
                    anime.episodes?.let { "$it Folgen" },
                    anime.duration?.let { "$it Min." },
                    STATUS[anime.status] ?: anime.status,
                ).joinToString("  ·  "),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            val jahreszeit = listOfNotNull(
                SEASON[anime.season],
                anime.seasonYear?.toString(),
            ).joinToString(" ")
            if (jahreszeit.isNotBlank()) {
                Spacer(Modifier.height(4.dp))
                Text(
                    jahreszeit,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            anime.description?.let { text ->
                Spacer(Modifier.height(16.dp))
                Text(
                    // Die Beschreibungen aus der Datenbank enthalten HTML-Reste.
                    text.replace(Regex("<br\\s*/?>"), "\n")
                        .replace(Regex("<[^>]*>"), "")
                        .trim(),
                    style = MaterialTheme.typography.bodyMedium,
                )
            }

            Abschnitt("Genres", anime.genres.isNotEmpty()) {
                Chips(anime.genres)
            }

            Abschnitt("Verfügbar als", anime.languages.isNotEmpty()) {
                Chips(anime.languages.map { SPRACHE[it] ?: it })
            }

            Abschnitt("Wo streamen", anime.providers.isNotEmpty()) {
                Chips(anime.providers.map { ANBIETER[it] ?: it.replaceFirstChar(Char::uppercase) })
            }

            Abschnitt("Studios", anime.studios.isNotEmpty()) {
                // Animationsstudios zuerst und hervorgehoben — der Rest ist
                // Produktion und Vertrieb, das interessiert seltener.
                Chips(
                    anime.studios.filter { it.isAnimation }.map { it.name }
                        .ifEmpty { anime.studios.take(3).map { it.name } },
                )
                val weitere = anime.studios.filterNot { it.isAnimation }
                if (weitere.isNotEmpty() && anime.studios.any { it.isAnimation }) {
                    Spacer(Modifier.height(6.dp))
                    Text(
                        "Produktion & Vertrieb: " + weitere.joinToString(", ") { it.name },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            Abschnitt("Tags", anime.tags.isNotEmpty()) {
                // Nach Rang sortiert geliefert: die treffendsten zuerst.
                Chips(anime.tags.take(14).map { tag ->
                    tag.rank?.let { "${tag.name} $it%" } ?: tag.name
                })
            }

            Abschnitt("Verwandte Titel", anime.related.isNotEmpty()) {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    items(anime.related, key = { it.id }) { verwandt ->
                        VerwandteKarte(verwandt) { onOeffneAnime(verwandt.id) }
                    }
                }
            }

            Abschnitt("Links", anime.links.isNotEmpty()) {
                LinkChips(anime.links) { url -> uri.openUri(url) }
            }

            Spacer(Modifier.height(12.dp))
            HorizontalDivider()
            Spacer(Modifier.height(8.dp))
            Text(
                if (anime.titleRomaji != anime.title) {
                    "Originaltitel: ${anime.titleRomaji}"
                } else {
                    "Originaltitel: ${anime.title}"
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(32.dp))
        }
    }
}

@Composable
private fun Kopf(anime: AnimeDetailDto, onBack: () -> Unit) {
    Box(Modifier.fillMaxWidth()) {
        AsyncImage(
            model = anime.bannerImageUrl ?: anime.coverImageUrl,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(16f / 9f),
        )
        // Verlauf, damit Titel und Zurück-Pfeil auf jedem Bild lesbar bleiben.
        Box(
            Modifier
                .fillMaxWidth()
                .aspectRatio(16f / 9f)
                .background(
                    Brush.verticalGradient(
                        0f to Color.Black.copy(alpha = 0.45f),
                        0.4f to Color.Transparent,
                        1f to Color.Black.copy(alpha = 0.85f),
                    ),
                ),
        )

        IconButton(onClick = onBack, modifier = Modifier.padding(4.dp)) {
            Icon(
                Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Zurück",
                tint = Color.White,
            )
        }

        Row(
            Modifier
                .align(Alignment.BottomStart)
                .padding(16.dp),
            verticalAlignment = Alignment.Bottom,
        ) {
            Text(
                anime.title,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                modifier = Modifier.weight(1f),
            )
            anime.averageScore?.let {
                Spacer(Modifier.width(10.dp))
                Text(
                    "★ $it",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                )
            }
        }
    }
}

@Composable
private fun Abschnitt(titel: String, sichtbar: Boolean, inhalt: @Composable () -> Unit) {
    // Leere Abschnitte gar nicht erst zeigen — eine Überschrift ohne Inhalt
    // sieht nach einem Fehler aus.
    if (!sichtbar) return
    Spacer(Modifier.height(20.dp))
    Text(titel, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
    Spacer(Modifier.height(8.dp))
    inhalt()
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun Chips(werte: List<String>) {
    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        werte.forEach { wert ->
            SuggestionChip(onClick = {}, label = { Text(wert) })
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun LinkChips(links: List<de.pattaku.otakupulse.app.data.api.LinkDto>, onOeffne: (String) -> Unit) {
    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        links.filter { !it.url.isNullOrBlank() }.forEach { link ->
            AssistChip(
                onClick = { link.url?.let(onOeffne) },
                label = { Text(link.platform ?: "Link") },
                trailingIcon = {
                    Icon(
                        Icons.AutoMirrored.Filled.OpenInNew,
                        contentDescription = null,
                        Modifier.size(16.dp),
                    )
                },
            )
        }
    }
}

@Composable
private fun VerwandteKarte(verwandt: RelatedDto, onKlick: () -> Unit) {
    val kachel = if (LocalBreite.current == Breite.KOMPAKT) 110.dp else 140.dp
    Column(
        Modifier
            .width(kachel)
            .clickable(onClick = onKlick),
    ) {
        AsyncImage(
            model = verwandt.coverImageUrl,
            contentDescription = verwandt.title,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .size(width = kachel, height = kachel * 1.41f)
                .clip(MaterialTheme.shapes.medium)
                .background(MaterialTheme.colorScheme.surfaceVariant),
        )
        Spacer(Modifier.height(6.dp))
        Text(
            RELATION[verwandt.relation] ?: verwandt.relation,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.SemiBold,
        )
        Text(verwandt.title, style = MaterialTheme.typography.bodySmall, maxLines = 2)
    }
}
