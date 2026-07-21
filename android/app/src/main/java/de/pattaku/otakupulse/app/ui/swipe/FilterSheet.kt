package de.pattaku.otakupulse.app.ui.swipe

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import de.pattaku.otakupulse.app.data.api.GenreDto
import de.pattaku.otakupulse.app.data.api.TagDto
import de.pattaku.otakupulse.app.domain.Auswahl
import de.pattaku.otakupulse.app.domain.DeckFilter
import de.pattaku.otakupulse.app.domain.STATUS_OPTIONEN
import de.pattaku.otakupulse.app.domain.SYNC_OPTIONEN
import de.pattaku.otakupulse.app.domain.TYP_OPTIONEN

/** Deutsche Namen für die Tag-Kategorien aus der Datenbank. */
private val KATEGORIE_LABEL = mapOf(
    "theme" to "Themen",
    "setting" to "Schauplatz",
    "demographic" to "Zielgruppe",
    "cast" to "Figuren",
    "technical" to "Technik",
    "sexual_content" to "Erotik",
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FilterSheet(
    filter: DeckFilter,
    genres: List<GenreDto>,
    tags: List<TagDto>,
    onAnwenden: (DeckFilter) -> Unit,
    onSchliessen: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var entwurf by remember { mutableStateOf(filter) }
    var tagSuche by remember { mutableStateOf("") }

    ModalBottomSheet(onDismissRequest = onSchliessen, sheetState = sheetState) {
        Column(
            Modifier
                .fillMaxWidth()
                .heightIn(max = 640.dp)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Filter", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                Spacer(Modifier.weight(1f))
                TextButton(onClick = { entwurf = DeckFilter(); tagSuche = "" }) {
                    Text("Zurücksetzen")
                }
            }

            Abschnitt("Sync") {
                AuswahlChips(SYNC_OPTIONEN, entwurf.languages) { neu ->
                    entwurf = entwurf.copy(languages = neu)
                }
            }

            Abschnitt("Typ") {
                AuswahlChips(TYP_OPTIONEN, entwurf.formats) { neu ->
                    entwurf = entwurf.copy(formats = neu)
                }
            }

            Abschnitt("Status") {
                AuswahlChips(STATUS_OPTIONEN, entwurf.statuses) { neu ->
                    entwurf = entwurf.copy(statuses = neu)
                }
            }

            Abschnitt("Genre") {
                AuswahlChips(genres.map { Auswahl(it.slug, it.name) }, entwurf.genres) { neu ->
                    entwurf = entwurf.copy(genres = neu)
                }
            }

            Abschnitt("Tag") {
                OutlinedTextField(
                    value = tagSuche,
                    onValueChange = { tagSuche = it },
                    label = { Text("Tag suchen (Magie, Isekai, Medical …)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(8.dp))

                // Gewählte Tags immer zeigen, sonst verschwinden sie hinter der Suche
                // und man kann sie nicht mehr abwählen.
                val gewaehlt = tags.filter { it.slug in entwurf.tags }
                val treffer = if (tagSuche.isBlank()) {
                    emptyList()
                } else {
                    tags.filter { it.name.contains(tagSuche, true) && it.slug !in entwurf.tags }
                        .take(30)
                }

                if (gewaehlt.isNotEmpty()) {
                    AuswahlChips(gewaehlt.map { Auswahl(it.slug, it.name) }, entwurf.tags) { neu ->
                        entwurf = entwurf.copy(tags = neu)
                    }
                    Spacer(Modifier.height(4.dp))
                }

                if (tagSuche.isBlank() && gewaehlt.isEmpty()) {
                    Text(
                        "Tippe, um zu suchen — es gibt über 350 Tags.",
                        style = MaterialTheme.typography.bodySmall,
                    )
                } else {
                    treffer.groupBy { it.category }.forEach { (kategorie, liste) ->
                        Text(
                            KATEGORIE_LABEL[kategorie] ?: kategorie ?: "Sonstige",
                            style = MaterialTheme.typography.labelMedium,
                            modifier = Modifier.padding(top = 6.dp),
                        )
                        AuswahlChips(liste.map { Auswahl(it.slug, it.name) }, entwurf.tags) { neu ->
                            entwurf = entwurf.copy(tags = neu)
                        }
                    }
                }
            }

            Spacer(Modifier.height(20.dp))
            Button(
                onClick = { onAnwenden(entwurf) },
                modifier = Modifier.fillMaxWidth(),
            ) { Text("Stapel neu mischen") }
            Spacer(Modifier.height(28.dp))
        }
    }
}

@Composable
private fun Abschnitt(titel: String, inhalt: @Composable () -> Unit) {
    Spacer(Modifier.height(18.dp))
    Text(titel, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
    Spacer(Modifier.height(6.dp))
    inhalt()
}

@OptIn(ExperimentalMaterial3Api::class, androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
private fun AuswahlChips(
    optionen: List<Auswahl>,
    gewaehlt: List<String>,
    onAendern: (List<String>) -> Unit,
) {
    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        optionen.forEach { option ->
            val aktiv = option.wert in gewaehlt
            FilterChip(
                selected = aktiv,
                onClick = {
                    onAendern(if (aktiv) gewaehlt - option.wert else gewaehlt + option.wert)
                },
                label = { Text(option.label) },
            )
        }
    }
}
